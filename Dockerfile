# Stage 1: Build
FROM clojure:temurin-24-tools-deps AS builder

WORKDIR /app

COPY deps.edn build.clj ./
COPY deps.edn build.clj workspace.edn ./
COPY components/ components/
COPY bases/ bases/
COPY projects/ projects/
COPY development/ development/

RUN clojure -P -M:dev:test

COPY . .

RUN clojure -T:build uberjar :project service

FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

COPY --from=builder /app/projects/service/target/service.jar app.jar

EXPOSE 8080

ENV PORT=8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]