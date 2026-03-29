# Centripetal Network Assignment

A simple JSON-based REST Microservice built with Clojure, demonstrating functional programming principles and the [Polylith Architecture](https://polylith.gitbook.io/polylith/). This service provides search capabilities on an open-source intelligence feed from AlienVault OTX.

## Table of Contents
- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [First-time Setup](#first-time-setup)
- [Running](#running)
  - [Run Locally](#run-locally)
  - [Via Docker](#via-docker)
- [Configuration](#configuration)
- [Usage](#usage)
  - [Endpoints](#endpoints)
  - [Search Criteria](#search-criteria)
- [Testing & Quality](#testing--quality)
- [Available Commands](#available-commands)
- [MCP Server](#mcp-server)
- [Deployment](#deployment)
- [License](#license)

## Overview
This project is an example of a microservice designed with a focus on modularity and clear separation of concerns. It leverages:
- **Clojure**: For its functional power and brevity.
- **Polylith Architecture**: To manage complexity through a modular codebase.
- **Datomic DB Local**: For high-performance, immutable data storage.
- **Component**: For lifecycle management.

## Prerequisites
### Tools
* [Java 21+](https://adoptium.net/en-GB/temurin/releases?version=21&os=any&arch=any)
* [Clojure](https://clojure.org/reference/clojure_cli) (CLI Tools)
* [Docker](https://www.docker.com/get-started/) (Optional, for containerized execution)

## Project Structure
The repository follows the Polylith structure:
- `bases/`: Contains the external entry points (e.g., REST API).
  - `service`: The main API service base.
  - `mcp-server`: MCP server for AI tool access and fine-tuning dataset export.
- `components/`: Modular building blocks used by bases and other components.
  - `persistence`: Data access layer and schema.
  - `server`: HTTP server implementation and middleware.
- `projects/`: Configuration for deployable artifacts (e.g., uberjars).
- `development/`: REPL and development-only code.
- `deps.edn`: Main dependency configuration and aliases.
- `workspace.edn`: Polylith workspace configuration.

## First-time Setup

```shell
# Install git hooks (runs format-check before every commit)
make install-hooks
```

## Running

### Run Locally
You can run the application directly using Clojure CLI:
```shell
make run
```

Or build and run the JAR:
```shell
make build
make run-jar
```

### Via Docker
Build and run using the provided Dockerfile:
```shell
make docker-build
make docker-run

# Stop the container
make docker-stop
```

## Configuration
Configuration is managed via `aero` in `bases/service/resources/service/config.edn`.

| Environment Variable | Description | Default |
|----------------------|-------------|---------|
| `APP_PROFILE`        | Configuration profile (`dev`, `staging`, `prod`) | `dev` |
| `HOST`               | Server host binding | `localhost` |
| `PORT`               | Server port | `8080` |
| `DATOMIC_DB_NAME`    | Name of the Datomic database | `indicators` |
| `MULOG_LOG_GROUP`    | CloudWatch Logs log group name (staging/prod) | `/pringwa/service` |
| `AWS_REGION`         | AWS region for CloudWatch Logs (staging/prod) | `us-east-1` |

## Usage
### API UI
The service includes a Swagger UI for API exploration and testing:
[http://localhost:8080/api-docs](http://localhost:8080/api-docs)

### Endpoints
All business endpoints are versioned under `/v1`. Infrastructure endpoints are unversioned.

* `GET /healthcheck` — Liveness probe (204)
* `GET /metrics` — JVM metrics (heap, threads, uptime)
* `GET /v1/indicators` — List all indicators
* `GET /v1/indicators?type=IPv4` — Filter indicators by type
* `GET /v1/indicators/:id` — Get a single indicator by document ID
* `POST /v1/indicators/search` — Search indicators by criteria
* `GET /api-docs` — Swagger UI

Example `curl` commands are available in the `Makefile`:
```shell
make api-health
make api-metrics
make api-indicators
make api-indicators-type TYPE=IPv4
make api-indicator ID=5b3cb789bd391e24a8b1dc53
make api-search QUERY='{"adversary":"Plead"}'
```

### Search Criteria
The `POST /v1/indicators/search` endpoint accepts a JSON body with the following fields:

| Field | Type | Example |
|-------|------|---------|
| `adversary` | string | `{"adversary": "Plead"}` |
| `tlp` | string | `{"tlp": "white"}` |
| `author_name` | string | `{"author_name": "AlienVault"}` |
| `tags` | string or array | `{"tags": "china"}` |
| `industries` | string or array | `{"industries": "tech"}` |
| `targeted_countries` | string or array | `{"targeted_countries": "Kuwait"}` |
| `name` | string | `{"name": "Plead Downloader"}` |
| `id`                 | string          | `{"id": "5b433d8fe822e72e3c57d26c"}`                |
| `description` | string | `{"description": "APT"}` |

## Testing & Quality
The project uses [Speclj](https://github.com/slagyr/speclj) for testing.

### Run Tests
```shell
make test
```

### Code Quality
```shell
make format-check    # Check code formatting
```

## Available Commands

| Command | Description |
|---------|-------------|
| `make help` | Show help for all commands |
| `make run` | Run application locally |
| `make run-mcp` | Run MCP server (stdio by default) |
| `make run-jar` | Build and run the JAR file |
| `make build` | Build uberjar |
| `make clean` | Clean build artifacts |
| `make test` | Run Speclj specs |
| `make install-hooks` | Install git pre-commit hooks |
| `make format-check` | Check code formatting |
| `make outdated` | Check for outdated dependencies |
| `make export-openapi` | Export OpenAPI spec to `openapi.json` |
| `make docker-build` | Build Docker image |
| `make docker-run` | Run Docker container |
| `make docker-stop` | Stop Docker container |
| `make info` | Show project information |

## MCP Server

An [MCP (Model Context Protocol)](https://modelcontextprotocol.io) server that exposes the indicators API as AI-callable tools, and can export all data as fine-tuning datasets.

### Running

```shell
# stdio (default — for Claude Desktop and most MCP clients)
clojure -M:dev -m com.pringwa.mcp-server.main

# HTTP/SSE (for remote or shared use)
MCP_TRANSPORT=sse MCP_PORT=3001 clojure -M:dev -m com.pringwa.mcp-server.main

# Both transports simultaneously
MCP_TRANSPORT=both clojure -M:dev -m com.pringwa.mcp-server.main
```

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `MCP_TRANSPORT` | Transport mode: `stdio`, `sse`, or `both` | `stdio` |
| `MCP_PORT` | Port for SSE transport | `3001` |
| `MCP_SERVICE_URL` | Base URL of the indicators service | `http://localhost:8080` |
| `MCP_SERVICE_TOKEN` | Bearer token for service auth (optional) | — |

### Tools

| Tool | Description |
|---|---|
| `list-indicators` | List indicators, optionally filtered by type |
| `get-indicator` | Fetch a single indicator by ID |
| `search-indicators` | Search by criteria (AND logic) |
| `export-dataset` | Export all indicators as JSONL to `export/` |

### Dataset Export

The `export-dataset` tool writes three files to `export/` (gitignored):
- `export/anthropic.jsonl` — chat format for Anthropic fine-tuning
- `export/openai.jsonl` — chat format for OpenAI fine-tuning
- `export/llama.jsonl` — Alpaca instruction format for open-source models

## Deployment
See [CONTRIBUTING.md](CONTRIBUTING.md) for details on the deployment pipeline, including Datomic Ion integration for staging and production environments.

## License
This project is licensed under the [MIT License](LICENSE).

---
##### Created by: Simon Okwori
