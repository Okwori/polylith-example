# Contributing

## Prerequisites

- Java 21+
- Clojure CLI
- Docker (optional)
- Datomic Cloud access (for staging/prod Ion deployments)

## First-time setup

```shell
# Install git hooks (runs format-check + tests before every commit)
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
│   └── service/         # REST API entry point (routes, handlers)
│       ├── src/         # Production source
│       ├── test/        # unit tests
│       └── spec/        # Speclj BDD specs
├── projects/
│   └── service/         # Deployable artefact (uberjar deps)
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

## Branching & deployment

```
feature/* → PR → CI (test + lint)
                      ↓ merge
                   main
                      ↓ auto
                  staging (Datomic Ion)
                      ↓ manual trigger
                  prod (Datomic Ion)
```

To deploy manually to prod, go to **Actions → Deploy — Production → Run workflow**.

Ion deployments use `clojure -A:dev:ion push / deploy`. See `.github/workflows/` for details.
