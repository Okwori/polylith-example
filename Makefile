# ==============================================================================
# Service API - Makefile
# ==============================================================================

.PHONY: help install clean build run test spec-test install-hooks docker-build \
        docker-run docker-stop format-check api-health api-indicators \
        api-indicators-type api-indicator api-search info

# Default target
.DEFAULT_GOAL := help

# Variables
PROJECT_NAME := service
JAR_FILE := projects/service/target/service.jar
DOCKER_IMAGE := service
DOCKER_CONTAINER := service
PORT := 8080

# ==============================================================================
# DEVELOPMENT
# ==============================================================================

run: ## Run application locally
	@echo "🚀 Starting server on port $(PORT)..."
	clojure -M::dev -m com.pringwa.service.main

run-jar: build ## Run the built JAR
	@echo "🚀 Running JAR on port $(PORT)..."
	java -jar $(JAR_FILE)

# ==============================================================================
# BUILD
# ==============================================================================

clean: ## Clean build artifacts
	@echo "🧹 Cleaning..."
	rm -rf target
	rm -rf projects/service/target
	rm -rf .cpcache
	@echo "✅ Clean complete"

build: clean ## Build uberjar
	@echo "🔨 Building uberjar..."
	clojure -T:build uberjar :project service
	@echo "✅ Built: $(JAR_FILE)"

# ==============================================================================
# TESTING
# ==============================================================================

test: ## Run clojure.test unit tests
	@echo "🧪 Running unit tests..."
	clojure -M:poly test

spec-test: ## Run Speclj specs
	@echo "🧪 Running specs..."
	clojure -M:dev:test

# ==============================================================================
# GIT HOOKS
# ==============================================================================

install-hooks: ## Install git hooks (run once after cloning)
	@echo "🔧 Installing git hooks..."
	git config core.hooksPath .githooks
	@echo "✅ Hooks installed — pre-commit will run format-check + tests"

# ==============================================================================
# DOCKER
# ==============================================================================

docker-build: ## Build Docker image
	@echo "🐳 Building Docker image..."
	docker build -t $(DOCKER_IMAGE) .
	@echo "✅ Docker image built: $(DOCKER_IMAGE)"

docker-run: docker-stop ## Run Docker container
	@echo "🐳 Starting Docker container on port $(PORT)..."
	docker run -d -p $(PORT):$(PORT) --name $(DOCKER_CONTAINER) $(DOCKER_IMAGE)
	@echo "✅ Container running: $(DOCKER_CONTAINER)"
	@echo "🌐 Access at: http://localhost:$(PORT)"

docker-stop: ## Stop Docker container
	@echo "🛑 Stopping Docker container..."
	-docker stop $(DOCKER_CONTAINER) 2>/dev/null
	-docker rm $(DOCKER_CONTAINER) 2>/dev/null
	@echo "✅ Container stopped"

docker-clean: docker-stop ## Remove Docker image
	@echo "🧹 Removing Docker image..."
	-docker rmi $(DOCKER_IMAGE) 2>/dev/null
	@echo "✅ Docker image removed"

# ==============================================================================
# CODE QUALITY
# ==============================================================================

format-check: ## Check code formatting
	@echo "🔍 Checking formatting..."
	clojure -M:cljfmt check

# ==============================================================================
# API TESTING (curl commands)
# ==============================================================================

api-health: ## Test healthcheck endpoint
	@echo "🏥 Testing healthcheck..."
	curl -s -o /dev/null -w "%{http_code}" http://localhost:$(PORT)/healthcheck
	@echo ""

api-metrics: ## Get JVM metrics
	@echo "📊 Getting metrics..."
	curl -s http://localhost:$(PORT)/metrics | jq .

api-indicators: ## Get all indicators
	@echo "📋 Getting all indicators..."
	curl -s http://localhost:$(PORT)/v1/indicators | jq .

api-indicators-type: ## Get indicators by type (usage: make api-indicators-type TYPE=IPv4)
	@echo "📋 Getting indicators by type: $(TYPE)..."
	curl -s "http://localhost:$(PORT)/v1/indicators?type=$(TYPE)" | jq .

api-indicator: ## Get indicator by ID (usage: make api-indicator ID=xxx)
	@echo "📋 Getting indicator: $(ID)..."
	curl -s http://localhost:$(PORT)/v1/indicators/$(ID) | jq .

api-search: ## Search indicators (usage: make api-search QUERY='{"adversary":"Plead"}')
	@echo "🔍 Searching indicators..."
	curl -s -X POST http://localhost:$(PORT)/v1/indicators/search \
		-H "Content-Type: application/json" \
		-d '$(QUERY)' | jq .

# ==============================================================================
# INFO
# ==============================================================================

info:
	@echo ""
	@echo "╔══════════════════════════════════════════════════════════════════╗"
	@echo "║                    PROJECT INFORMATION                           ║"
	@echo "╚══════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "  Project:     $(PROJECT_NAME)"
	@echo "  JAR File:    $(JAR_FILE)"
	@echo "  Docker:      $(DOCKER_IMAGE)"
	@echo "  Port:        $(PORT)"
	@echo ""
	@echo "  Endpoints:"
	@echo "    - GET  /healthcheck                  =>  make api-health"
	@echo "    - GET  /metrics                      =>  make api-metrics"
	@echo "    - GET  /v1/indicators                =>  make api-indicators"
	@echo "    - GET  /v1/indicators?type={type}    =>  make api-indicators-type TYPE=IPv4"
	@echo "    - GET  /v1/indicators/:id            =>  make api-indicator ID=5b3cb789bd391e24a8b1dc53"
	@echo "    - POST /v1/indicators/search         =>  make api-search QUERY='{\"adversary\":\"Plead\"}'"
	@echo "    - GET  /api-docs (Swagger UI)        =>  http://localhost:8080/api-docs"
	@echo ""

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'
