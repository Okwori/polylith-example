# Centripetal Network Assigment
Your objective is to build a simple JSON based REST Microservice. The service will provide search capabilities 
on an open source intelligence feed provided by AlienVault OTX. REST and microservices are fairly common, 
the goal here is to show how this might be done in a Clojure application using its functional capabilities.

## Stack
[Clojure](https://clojure.org/), [Datomic DB Local](https://docs.datomic.com/datomic-local.html), [Polylith Architecture](https://github.com/polyfy/polylith)  
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
#### API UI
[http://localhost:8080/api-docs/](http://localhost:8080/api-docs/#/)

#### Endpoints
* GET /indicators/:id
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

Crafter: Simon Okwori
