ifneq (,$(wildcard env/.env))
	include env/.env
	export
endif

.DEFAULT_GOAL := help

# Run Server and all Clients in parallel (Local development)
.PHONY: dev
dev:
	@echo "Starting Server, Web Client, Partner Client, and Owner Client services..."
	@make -j 4 dev-server dev-web dev-partner dev-owner

# Run Backend only
.PHONY: dev-server
dev-server:
	@echo "Starting Spring Boot Server..."
	@make clear-logs && cd Server && ./mvnw clean compile -DskipTests spring-boot:run

# Run all Clients in parallel
.PHONY: dev-client
dev-client:
	@echo "Starting Next.js Web, Partner, and Owner Clients..."
	@make -j 3 dev-web dev-partner dev-owner

# Run Web Client only (Port 3000)
.PHONY: dev-web
dev-web:
	@echo "Starting Next.js Web Client on port 3000..."
	@PORT=3000 npm run dev --workspace=apps/web --prefix Client

# Run Partner Client only (Port 3002)
.PHONY: dev-partner
dev-partner:
	@echo "Starting Next.js Partner Client on port 3002..."
	@PORT=3002 npm run dev --workspace=apps/partner --prefix Client

# Run Owner Client only (Port 3005)
.PHONY: dev-owner
dev-owner:
	@echo "Starting Next.js Owner Client on port 3005..."
	@PORT=3005 npm run dev --workspace=apps/owner --prefix Client

.PHONY: monitoring
monitoring:
	@echo "Starting Prometheus and Grafana..."
	@docker-compose -p omnibooking-dev --env-file env/.env up -d prometheus grafana
	@echo "Prometheus is running at http://localhost:9090"
	@echo "Grafana is running at http://localhost:3001"

.PHONY: test-server
test-server:
	@echo "Running Server unit tests..."
	@cd Server && ./mvnw clean test

# Performance & Load Testing with k6
.PHONY: test-load
test-load:
	@echo "Running performance load test with default scenario (50 VUs)..."
	@k6 run Server/load-tests/performance-test.js

.PHONY: test-load-quick
test-load-quick:
	@echo "Running quick performance test check (10 VUs, 15 seconds)..."
	@k6 run --vus 10 --duration 15s Server/load-tests/performance-test.js

.PHONY: test-load-high
test-load-high:
	@echo "Running high-intensity stress test (200 VUs, ramp-up 1m, sustained 2m)..."
	@k6 run --vus 200 --duration 3m Server/load-tests/performance-test.js

# Seeding Commands
.PHONY: generate-mock-users
generate-mock-users:
	@echo "Generating 10,000 mock users..."
	@node scripts/generate-users.js

# Docker Development (Local Infrastructure)
.PHONY: docker-infra
docker-infra:
	@echo "Starting infrastructure..."
	@docker-compose -p omnibooking-dev --env-file env/.env up -d db redis kafka kafdrop elasticsearch kibana prometheus grafana
	@echo "Waiting for Elasticsearch to be ready..."
	@until [ "$$(docker inspect --format='{{.State.Health.Status}}' omnibooking-elastic)" = "healthy" ]; do \
		printf '.'; \
		sleep 2; \
	done
	@echo "\nInfrastructure (DB, Redis, Kafka, ES, Prometheus, Grafana) is Ready..."

.PHONY: docker-stop
docker-stop:
	@echo "Stopping Development Docker infrastructure services..."
	@docker-compose -p omnibooking-dev --env-file env/.env stop

.PHONY: docker-down
docker-down:
	@echo "Stopping and removing Development Docker infrastructure services..."
	@docker-compose -p omnibooking-dev --env-file env/.env down -v

.PHONY: docker-restart
docker-restart:
	@echo "Rebuilding and restarting all Development Docker infrastructure services..."
	@docker-compose -p omnibooking-dev --env-file env/.env up -d --build

# Docker Production Stack
.PHONY: docker-build
docker-build:
	@cp env/.env.prod Client/.env
	@echo "Building and starting all services in Production Docker..."
	@docker-compose -p omnibooking-prod --env-file env/.env.prod -f docker-compose.prod.yml up --build -d
	@rm -f Client/.env

.PHONY: docker-rebuild
docker-rebuild:
	@echo "Stopping running production containers to free up RAM & CPU..."
	@docker-compose -p omnibooking-prod --env-file env/.env.prod -f docker-compose.prod.yml stop
	@cp env/.env.prod Client/.env
	@echo "Building and starting all services in Production Docker..."
	@docker-compose -p omnibooking-prod --env-file env/.env.prod -f docker-compose.prod.yml up --build -d
	@rm -f Client/.env
	@echo "Rebuild and restart completed successfully!"

.PHONY: docker-restart-prod
docker-restart-prod:
	@read -p "Are you sure you want to rebuild and restart all Production Docker services? (y/n): " ans; \
	if [ "$$ans" = "y" ] || [ "$$ans" = "Y" ]; then \
		cp env/.env.prod Client/.env; \
		echo "Rebuilding and restarting all Production Docker services..."; \
		docker-compose -p omnibooking-prod --env-file env/.env.prod -f docker-compose.prod.yml up -d --build; \
		rm -f Client/.env; \
	else \
		echo "Aborted..."; \
	fi

.PHONY: docker-start-prod
docker-start-prod:
	@echo "Starting all Production Docker services..."
	@docker-compose -p omnibooking-prod --env-file env/.env.prod -f docker-compose.prod.yml start

.PHONY: docker-stop-prod
docker-stop-prod:
	@read -p "Are you sure you want to stop all Production Docker services? (y/n): " ans; \
	if [ "$$ans" = "y" ] || [ "$$ans" = "Y" ]; then \
		echo "Stopping all Production Docker services..."; \
		docker-compose -p omnibooking-prod --env-file env/.env.prod -f docker-compose.prod.yml stop; \
	else \
		echo "Aborted..."; \
	fi

.PHONY: docker-down-prod
docker-down-prod:
	@read -p "Are you sure you want to stop and delete ALL Production containers and volumes? (y/n): " ans; \
	if [ "$$ans" = "y" ] || [ "$$ans" = "Y" ]; then \
		echo "Stopping all Production Docker services and removing volumes..."; \
		docker-compose -p omnibooking-prod --env-file env/.env.prod -f docker-compose.prod.yml down -v; \
		echo "Production environment completely cleaned..."; \
	else \
		echo "Aborted..."; \
	fi

.PHONY: docker-logs
docker-logs:
	@docker-compose -p omnibooking-prod --env-file env/.env.prod -f docker-compose.prod.yml logs -f

.PHONY: docker-logs-server
docker-logs-server:
	@docker logs -f omnibooking-server-prod

# Install dependencies for both projects
.PHONY: install
install:
	@echo "Installing Root dependencies..."
	@npm install
	@echo "Installing Server dependencies..."
	@cd Server && ./mvnw dependency:resolve
	@echo "Installing Client dependencies..."
	@npm install --prefix Client

# Build projects for production
.PHONY: build
build:
	@echo "Building Server artifacts..."
	@cd Server && ./mvnw clean package -DskipTests
	@echo "Building Client artifacts..."
	@npm run build:web --prefix Client
	@npm run build:partner --prefix Client
	@npm run build:owner --prefix Client

# Remove build artifacts and temporary files
.PHONY: clean
clean:
	@echo "Cleaning Server build directory..."
	@cd Server && ./mvnw clean
	@echo "Cleaning Client build directories..."
	@rm -rf Client/apps/web/.next Client/apps/partner/.next Client/apps/owner/.next Client/out Client/node_modules

# Clean logs
.PHONY: clear-logs
clear-logs:
	@rm -rf Server/logs/*
	@echo "Logs cleaned..."

# Packaging Commands
SERVER_EXCLUDES := --exclude="Server/src/main/resources/geo" --exclude="Server/src/main/resources/static" --exclude="Server/src/main/resources/mock-*.json"
CLIENT_SRCS := Client/apps/web/src Client/apps/partner/src Client/apps/owner/src
ARCHIVE_DIR := archives

.PHONY: zip-server
zip-server:
	@node scripts/zip.js $(ARCHIVE_DIR) Server Server/src $(SERVER_EXCLUDES)

.PHONY: zip-client
zip-client:
	@node scripts/zip.js $(ARCHIVE_DIR) Client $(CLIENT_SRCS)

.PHONY: zip-all
zip-all:
	@node scripts/zip.js $(ARCHIVE_DIR) OmniBooking Server/src $(CLIENT_SRCS) $(SERVER_EXCLUDES)

# Run CodeGraph to index and map code structure/flows
.PHONY: codegraph
codegraph:
	@echo "Updating CodeGraph index..."
	@if [ ! -d ".codegraph" ]; then \
		npx codegraph init -i; \
	else \
		npx codegraph index; \
	fi

# Display help information
.PHONY: help
help:
	@echo "OmniBooking Monorepo Management:"
	@echo "  Development Flow:"
	@echo "    make docker-infra  - Start DB and Redis in Docker"
	@echo "    make dev           - Run Server, Web and Partner clients in parallel"
	@echo "    make dev-server    - Run Spring Boot Server only"
	@echo "    make dev-client    - Run both Web and Partner clients in parallel"
	@echo "    make dev-web       - Run Web client only (Port 3000)"
	@echo "    make dev-partner   - Run Partner client only (Port 3002)"
	@echo "    make seed-db       - Generate mock-users.json and run Server with --seed argument"
	@echo ""
	@echo "  Docker Development (Local Infrastructure):"
	@echo "    make docker-infra  - Start DB, Redis, Kafka, ES, etc. in Docker"
	@echo "    make docker-stop   - Stop development infrastructure services"
	@echo "    make docker-down   - Stop and clean development infrastructure (remove volumes)"
	@echo "    make docker-restart - Rebuild and restart development infrastructure"
	@echo "  Docker Production Stack:"
	@echo "    make docker-build  - Build and start all services in Production Docker"
	@echo "    make docker-rebuild - Stop, rebuild, and restart Production Docker services (optimal)"
	@echo "    make docker-restart-prod - Rebuild and restart Production Docker services"
	@echo "    make docker-start-prod - Start Production Docker services"
	@echo "    make docker-stop-prod - Stop Production Docker services"
	@echo "    make docker-down-prod - Stop and clean Production stack"
	@echo "    make docker-logs   - Tail production Docker logs"
	@echo "    make docker-logs-server - Tail production Server (backend) logs"
	@echo ""
	@echo "  Maintenance:"
	@echo "    make install       - Install all dependencies"
	@echo "    make clean         - Remove build artifacts"
	@echo "    make monitoring    - Start Prometheus and Grafana"
	@echo "    make test-server   - Run server unit tests"
	@echo "    make test-load     - Run full k6 performance load test (50 VUs)"
	@echo "    make test-load-quick - Run quick k6 performance check (10 VUs, 15s)"
	@echo "    make test-load-high - Run high-intensity k6 stress test (200 VUs, 3m)"
	@echo "    make codegraph     - Index/update CodeGraph codebase flows"
	@echo "    make zip-server    - Compress Server source code into Server.zip"
	@echo "    make zip-client    - Compress Client source code into Client.zip"
	@echo "    make zip-all       - Compress all source code into OmniBooking.zip"
