# Centripetal Network Assignment
Your objective is to build a simple JSON based REST Microservice. The service will provide search capabilities 
on an open source intelligence feed provided by AlienVault OTX. REST and microservices are fairly common, 
the goal here is to show how this might be done in a Clojure application using its functional capabilities.

## Prerequisites
### Tools
* [Java 17+](https://adoptium.net/en-GB/temurin/releases?version=25&os=any&arch=any)
* [Clojure](https://clojure.org/reference/clojure_cli) 
* [Docker](https://www.docker.com/get-started/)
### Helpful Libs
* [Datomic DB Local](https://docs.datomic.com/datomic-local.html)
* [Polylith Architecture](https://github.com/polyfy/polylith)
* [Component](https://github.com/stuartsierra/component)

## Running

### Run Locally
```shell
# Run the application
make run
```

```shell
# Or build and run the JAR
make build
make run-jar
```

### Via Docker
```shell
# Build and run
make docker-build
make docker-run

# Or stop the container
make docker-stop
```

## Usage
### API UI
[http://localhost:8080/api-docs](http://localhost:8080/api-docs)

### Endpoints
* GET _/indicators/:id_ :
Note: This currently uses `document/id`, which is the top-level string ID.
Alternatively, `indicator/id` (a long value within the inner vector) could be what is expected?
```shell
# curl http://localhost:8080/indicators/5b433d8fe822e72e3c57d26c
make api-indicator ID=5b3cb789bd391e24a8b1dc53
```
* GET _/indicators_ :
```shell
# curl http://localhost:8080/indicators
make api-indicators
```
* GET _/indicators?type=IPv4_ :
```shell
# curl "http://localhost:8080/indicators?type=IPv4"
make api-indicators-type TYPE=IPv4
```
* POST _/indicators/search_ :
```shell
# curl -X POST http://localhost:8080/indicators/search \
#  -H "Content-Type: application/json" \
#  -d '{"adversary": "Plead"}'
make api-search QUERY='{"adversary":"Plead"}'
# curl -X POST http://localhost:8080/indicators/search \
#  -H "Content-Type: application/json" \
#  -d '{"author_name": "AlienVault"}'
make api-search QUERY='{"author_name":"AlienVault"}'
```
Search Criteria Options

| Field                | Type            | Example                                             |
|----------------------|-----------------|-----------------------------------------------------|
| `adversary`          | string          | `{"adversary": "Plead"}`                            |
| `tlp`                | string          | `{"tlp": "white"}`                                  |
| `author_name`        | string          | `{"author_name": "AlienVault"}`                     |
| `tags`               | string or array | `{"tags": "china"}` or `{"tags": ["china", "apt"]}` |
| `industries`         | string or array | `{"industries": "tech"}`                            |
| `targeted_countries` | string or array | `{"targeted_countries": "Kuwait"}`                  |
| `revision`           | number          | `{"revision": 1}`                                   |
| `public`             | number (0 or 1) | `{"public": 1}`                                     |

* Check Health
```shell
make api-health
```

### Explore Project
```shell
make info
```

### Check Code Formatting
```shell
make format-check
```
### Available Commands

| Command             | Description                |
|---------------------|----------------------------|
| `make run`          | Run application locally    |
| `make run-jar`      | Build and run the JAR file |
| `make build`        | Build uberjar              |
| `make clean`        | Clean build artifacts      |
| `make test`         | Run unit tests             |
| `make format-check` | Check code formatting      |
| `make docker-build` | Build Docker image         |
| `make docker-run`   | Run Docker container       |
| `make docker-stop`  | Stop Docker container      |
| `make docker-clean` | Remove Docker image        |
| `make info`         | Show project information   |


## Testing
WIP on unit and integration testing with [Speclj](https://github.com/slagyr/speclj) 
and [Etaoin](https://github.com/clj-commons/etaoin).

However, you can run the existing Clojure unit tests::
```shell
make test
```

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
