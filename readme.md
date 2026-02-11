# Centripetal Network Assigment
Your objective is to build a simple JSON based REST Microservice. The service will provide search capabilities 
on an open source intelligence feed provided by AlienVault OTX. REST and microservices are fairly common, 
the goal here is to show how this might be done in a Clojure application using its functional capabilities.

## Stack
[Clojure](https://clojure.org/), [Datomic DB Local](https://docs.datomic.com/datomic-local.html), [Polylith Architecture](https://github.com/polyfy/polylith), [Component](https://github.com/stuartsierra/component)
## Running
### Docker
#### Build
```shell
docker build -t service .
```
#### Run
```shell
docker run -p 8080:8080 service
```

## Usage
### API UI
[http://localhost:8080/api-docs/](http://localhost:8080/api-docs/#/)

### Endpoints
* GET /indicators/:id

  (Btw, a thought that ran through my mind but couldn't get to ask question, is could this also be `indicator/id`
  a long. This is using `document/id` which is the top level string ID)
```shell
curl http://localhost:8080/indicators/5b433d8fe822e72e3c57d26c
```
* GET /indicators
```shell
curl http://localhost:8080/indicators
```
* GET /indicators?type=IPv4
```shell
curl "http://localhost:8080/indicators?type=IPv4"
```
* POST /indicators/search
```shell
curl -X POST http://localhost:8080/indicators/search \
  -H "Content-Type: application/json" \
  -d '{"adversary": "Plead"}'
  
curl -X POST http://localhost:8080/indicators/search \
  -H "Content-Type: application/json" \
  -d '{"author_name": "AlienVault"}'
```

## Testing
Work on unit and integration tesing with [Speclj](https://github.com/slagyr/speclj) 
and [Etaoin](https://github.com/clj-commons/etaoin) is WIP.

However, run regular clojure test gen unit tests:
```shell
clojure -M:poly test
```


Crafter: Simon Okwori
