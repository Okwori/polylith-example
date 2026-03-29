# Contributing

## Prerequisites

- Java 21+
- Clojure CLI
- Docker (optional)
- Datomic Cloud access (for staging/prod Ion deployments)

## First-time setup

```shell
# Install git hooks (runs format-check before every commit)
make install-hooks
```

## Project structure

This project uses [Polylith](https://polylith.gitbook.io/polylith/) — code is split into three kinds of building blocks:

```
polylith-example/
├── components/          # Reusable modules with a public interface
│   ├── persistence/     # Datomic layer (schema, queries, data loading)
│   └── server/          # Jetty HTTP server + middleware
├── bases/
│   ├── service/         # REST API entry point (routes, handlers)
│   │   ├── src/         # Production source
│   │   ├── test/        # unit tests
│   │   └── spec/        # Speclj BDD specs
│   └── mcp-server/      # MCP server (AI tool access + fine-tuning export)
├── projects/
│   └── service/         # Deployable artefact (uberjar deps)
├── infra/               # Terraform — WAF, CloudWatch alarms, SNS, API GW stage
└── development/         # REPL utilities
```

**Rule of thumb:**
- Logic that could be reused → `components/`
- HTTP/entry-point concerns → `bases/service/`
- Never import a base from a component

## Adding a new component

```shell
clojure -M:poly create component name:<your-name>
```

This creates `components/<your-name>/src/.../interface.clj` and a matching `test/` directory. Expose only what consumers need through `interface.clj`.

## Running locally

```shell
make run          # starts Jetty on :8080 with APP_PROFILE=dev (Datomic Local)
```

Environment profiles are controlled by `APP_PROFILE`:

| Value | Database | Used by |
|---|---|---|
| `dev` (default) | Datomic Local `:mem` | local REPL / `make run` |
| `staging` | Datomic Cloud staging system | Ion staging deploy |
| `prod` | Datomic Cloud prod system | Ion prod deploy |

## Testing

```shell
make test    # Speclj BDD specs
```

Run by the pre-commit hook and by the CI workflow on every PR.

## Observability

Structured logging uses [mulog](https://github.com/BrunoBonacci/mulog). Each request emits a `::http-request` event with method, URI, status, and duration. The correlation ID set by `wrap-correlation-id` flows into every event automatically via mulog's thread-local context.

In `dev`, events are printed to stdout as pretty JSON. In `staging`/`prod`, they are published to CloudWatch Logs as structured JSON — use CloudWatch Logs Insights to query them and CloudWatch Metric Filters to extract latency and error rate metrics.

## Code style

```shell
make format-check
```

Formatting is enforced in CI and in the pre-commit hook.

## API

All business endpoints are versioned under `/v1`. Infra endpoints (`/healthcheck`, `/metrics`) are unversioned.

```
GET  /healthcheck
GET  /metrics
GET  /v1/indicators
GET  /v1/indicators?type=IPv4
GET  /v1/indicators/:id
POST /v1/indicators/search
GET  /api-docs   (Swagger UI)
```

## Infrastructure

AWS infrastructure is managed with Terraform in `infra/`. It provisions:
- **WAF** (WAFv2 REGIONAL) on the API Gateway stage — AWS managed rules + IP rate limiting
- **CloudWatch Metric Filters** on the mulog log group — extracts 5xx count and request latency
- **CloudWatch Alarms** — fires to SNS on high error rate or p99 latency breach
- **SNS topic** — email notifications for alarms
- **API Gateway stage** — access logging to CloudWatch + throttling settings

```shell
cd infra
cp example.tfvars staging.tfvars   # fill in your values
terraform init \
  -backend-config="bucket=<tfstate-bucket>" \
  -backend-config="key=pringwa-service/staging/terraform.tfstate"
terraform import aws_api_gatewayv2_stage.api <api-id>/$default
terraform apply -var-file=staging.tfvars
```

Repeat with `prod.tfvars` for production. State files and `*.tfvars` are gitignored.

## Branching & deployment

```
feature/* → PR → CI (test + lint)
                      ↓ merge
                   main
                      ↓ auto
                  staging (Datomic Ion)
                      ↓ auto (queues, awaits approval)
                  prod (Datomic Ion)
```

### How Ion deployment works

Every deploy is a two-step process:

1. **Push** — packages the application code and uploads it to S3 as a named revision identified by the Git SHA (`--uname`). This does not affect running instances.
2. **Deploy** — activates the uploaded revision on the target Datomic Cloud compute group. The running Ion instances are replaced with the new code.
3. **Deploy-status** — polls until the deployment completes or fails. The workflow blocks here so a failed deploy surfaces as a failed GitHub Actions step, not a silent background failure.

The entry point registered in `ion-config.edn` is `com.pringwa.service.ion/handler`. The `:app-name` (`pringwa-service`) and `:region` must match your Datomic Cloud setup. API Gateway routes HTTP traffic to this handler — authentication (JWT) is validated at the API Gateway layer before requests reach the Ion handler.

### Prerequisites

Before any deploy can run the following must be in place:

- A Datomic Cloud system provisioned per environment (staging and prod) in AWS
- API Gateway (HTTP API) configured with a route pointing to the Ion HTTP Direct integration
- A JWT Authorizer attached to the API Gateway routes that validates tokens from your identity provider
- An IAM user or role with permissions to push to S3 and invoke the Ion deploy API

### GitHub secrets and variables

Configure these in **Settings → Environments** for the `staging` and `production` environments respectively.

**Secrets** (encrypted, same values used across both environments unless noted):

| Secret | Description |
|--------|-------------|
| `AWS_ACCESS_KEY_ID` | IAM access key with Ion push/deploy permissions |
| `AWS_SECRET_ACCESS_KEY` | Corresponding secret key |
| `AWS_REGION` | AWS region where your Datomic Cloud system is provisioned (e.g. `us-east-1`) |

**Variables** (non-sensitive, per environment):

| Variable | Description |
|----------|-------------|
| `STAGING_SYSTEM` | Name of the Datomic Cloud system for staging (e.g. `indicators-staging`) |
| `PROD_SYSTEM` | Name of the Datomic Cloud system for production (e.g. `indicators-prod`) |

The `production` environment in GitHub should have a **required reviewer** configured so that production deploys require manual approval before the workflow runs.

### Staging deploy

Staging deploys automatically on every merge to `main`. No manual action required.

The `APP_PROFILE=staging` environment variable is set by the workflow, which causes the application to read the staging profile from `config.edn` — connecting to the staging Datomic Cloud system and applying staging CORS, rate-limit, and circuit-breaker settings.

### Production deploy

Production deploys trigger automatically once staging succeeds, but are gated by a required reviewer in the `production` GitHub environment.

1. Merge to `main` — staging deploys automatically
2. Once staging passes, the prod workflow queues and pauses — GitHub notifies the required reviewer
3. Approve the deployment in **Actions → Deploy — Production → Review deployments**
4. Monitor the **Wait for deploy** step — it polls `deploy-status` until the Ion compute group confirms the new revision is live

The prod workflow always deploys the exact Git SHA that was just verified on staging.

**Rollback:** re-run the prod workflow on any previous staging run that deployed the SHA you want to revert to.

### Running a deploy manually from the CLI

If you need to deploy outside of CI (e.g. for debugging):

```shell
# Ensure AWS credentials are configured locally
export AWS_REGION=us-east-1

# Push the current code as a named revision
clojure -A:dev:ion push --uname <your-label>

# Deploy to staging
clojure -A:dev:ion deploy --system indicators-staging --uname <your-label>

# Poll until complete
clojure -A:dev:ion deploy-status --system indicators-staging --uname <your-label>
```

Replace `indicators-staging` with `indicators-prod` and your `PROD_SYSTEM` value for production.
