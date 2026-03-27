# Centripetal Network Assignment
Your objective is to build a simple JSON based REST Microservice. The service will provide search capabilities
on an open source intelligence feed provided by AlienVault OTX. REST and microservices are fairly common,
the goal here is to show how this might be done in a Clojure application using its functional capabilities.

## Prerequisites
### Tools
* [Java 21+](https://adoptium.net/en-GB/temurin/releases?version=21&os=any&arch=any)
* [Clojure](https://clojure.org/reference/clojure_cli)
* [Docker](https://www.docker.com/get-started/) (optional)
### Helpful Libs
* [Datomic DB Local](https://docs.datomic.com/datomic-local.html)
* [Polylith Architecture](https://github.com/polyfy/polylith)
* [Component](https://github.com/stuartsierra/component)

## First-time setup

```shell
# Install git hooks (runs lint + tests before every commit)
make install-hooks
```

## Running

### Run Locally
```shell
make run
```

```shell
# Or build and run the JAR
make build
make run-jar
```

### Via Docker
```shell
make docker-build
make docker-run

# Stop the container
make docker-stop
```

## Usage
### API UI
[http://localhost:8080/api-docs](http://localhost:8080/api-docs)

### Endpoints
All business endpoints are versioned under `/v1`. Infrastructure endpoints are unversioned.

* `GET /healthcheck` — liveness probe (204)
* `GET /metrics` — JVM heap, threads, uptime
* `GET /v1/indicators` — all indicators
* `GET /v1/indicators?type=IPv4` — filter by indicator type
* `GET /v1/indicators/:id` — single indicator by document ID (returns 404 if not found)
* `POST /v1/indicators/search` — search by criteria
* `GET /api-docs` — Swagger UI

```shell
make api-health
make api-metrics
make api-indicators
make api-indicators-type TYPE=IPv4
make api-indicator ID=5b3cb789bd391e24a8b1dc53
make api-search QUERY='{"adversary":"Plead"}'
```

### Search Criteria

| Field                | Type            | Example                                             |
|----------------------|-----------------|-----------------------------------------------------|
| `adversary`          | string          | `{"adversary": "Plead"}`                            |
| `tlp`                | string          | `{"tlp": "white"}`                                  |
| `author_name`        | string          | `{"author_name": "AlienVault"}`                     |
| `tags`               | string or array | `{"tags": "china"}` or `{"tags": ["china", "apt"]}` |
| `industries`         | string or array | `{"industries": "tech"}`                            |
| `targeted_countries` | string or array | `{"targeted_countries": "Kuwait"}`                  |
| `name`               | string          | `{"name": "Plead Downloader"}`                      |
| `id`                 | string          | `{"id": "5b433d8fe822e72e3c57d26c"}`                |
| `description`        | string          | `{"description": "APT"}`                            |

Unknown criteria keys return `400 Bad Request`.

## Testing

```shell
make test        # clojure.test unit tests (via poly test runner)
make spec-test   # Speclj BDD specs
```

## Code Quality

```shell
make format-check              # check formatting
clojure -M:cljfmt fix          # auto-fix formatting
clj-kondo --lint bases components   # lint
```

### Available Commands

| Command               | Description                       |
|-----------------------|-----------------------------------|
| `make run`            | Run application locally           |
| `make run-jar`        | Build and run the JAR file        |
| `make build`          | Build uberjar                     |
| `make clean`          | Clean build artifacts             |
| `make test`           | Run clojure.test unit tests       |
| `make spec-test`      | Run Speclj specs                  |
| `make install-hooks`  | Install git pre-commit hooks      |
| `make format-check`   | Check code formatting             |
| `make export-openapi` | Export OpenAPI spec to openapi.json |
| `make docker-build`   | Build Docker image                |
| `make docker-run`     | Run Docker container              |
| `make docker-stop`    | Stop Docker container             |
| `make docker-clean`   | Remove Docker image               |
| `make info`           | Show project information          |

## Deployment

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full deployment pipeline (Datomic Ion, staging/prod environments).

## Reference
* [clj-poly-doc](https://cljdoc.org/d/polylith/clj-poly/0.3.32/doc/readme)
* [component](https://github.com/stuartsierra/component)
* [meetup](https://www.youtube.com/watch?v=_tpNKAv4fro)
* [polylith-gitbook](https://polylith.gitbook.io/polylith/)
* [usermanager-example](https://github.com/seancorfield/usermanager-example/tree/polylith)
* [furkan3ayraktar/clojure-polylith-realworld-example-app](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app)
* [clojure-doc](https://clojure-doc.org/)
* AI tools for certain syntax reminder and unit tests

##### Created by: Simon Okwori
