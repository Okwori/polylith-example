# Centripetal Network Assigment
Your objective is to build a simple JSON based REST Microservice. The service will provide search capabilities 
on an open source intelligence feed provided by AlienVault OTX. REST and microservices are fairly common, 
the goal here is to show how this might be done in a Clojure application using its functional capabilities.

## Prerequisite
### Tools
* [Java 17+](https://adoptium.net/en-GB/temurin/releases?version=25&os=any&arch=any)
* [Clojure](https://clojure.org/reference/clojure_cli) 
* [Docker](https://www.docker.com/get-started/)
### Helpful Libs
* [Datomic DB Local](https://docs.datomic.com/datomic-local.html)
* [Polylith Architecture](https://github.com/polyfy/polylith)
* [Component](https://github.com/stuartsierra/component)

## Running
### Via Docker
#### Build
```shell
docker build -t service .
```
#### Run
```shell
docker run -p 8080:8080 service
```

### Via Clojure CLI
```shell
clojure -M:test:dev -m com.pringwa.service.main
```

## Usage
### API UI
[http://localhost:8080/api-docs/](http://localhost:8080/api-docs/#/)

### Endpoints
* GET _/indicators/:id_ :

  (Btw, could this also be `indicator/id`
  a long - within the inner vector. This is using `document/id` which is the top level string ID)
```shell
curl http://localhost:8080/indicators/5b433d8fe822e72e3c57d26c
```
* GET _/indicators_ :
```shell
curl http://localhost:8080/indicators
```
* GET _/indicators?type=IPv4_ :
```shell
curl "http://localhost:8080/indicators?type=IPv4"
```
* POST _/indicators/search_ :
```shell
curl -X POST http://localhost:8080/indicators/search \
  -H "Content-Type: application/json" \
  -d '{"adversary": "Plead"}'
  
curl -X POST http://localhost:8080/indicators/search \
  -H "Content-Type: application/json" \
  -d '{"author_name": "AlienVault"}'
```
### Explore Polylith
Via Clojure CLI:
```shell
clojure -M:poly info
clojure -M:poly help
```

## Testing
WIP on unit and integration testing with [Speclj](https://github.com/slagyr/speclj) 
and [Etaoin](https://github.com/clj-commons/etaoin).

However, run regular clojure unit tests:
```shell
clojure -M:poly test
```

## Reference
* [clj-poly-doc](https://cljdoc.org/d/polylith/clj-poly/0.3.32/doc/readme)
* [component](https://github.com/stuartsierra/component)
* [meetup](https://www.youtube.com/watch?v=_tpNKAv4fro)
* [polylith-gitbook](https://polylith.gitbook.io/polylith/)
* [usermanager-example](https://github.com/seancorfield/usermanager-example/tree/polylith)
* [serefayar/ayatori](https://github.com/serefayar/ayatori)
* [furkan3ayraktar/clojure-polylith-realworld-example-app](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app)
* [clojure-doc](https://clojure-doc.org/)
* AI tools for certain syntax reminder and unit tests

##### Crafter: Simon Okwori
