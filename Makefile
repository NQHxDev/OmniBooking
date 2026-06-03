ifneq (,$(wildcard .env))
	include .env
	export
endif

.PHONY: dev dev-server dev-client dev-web dev-partner docker-infra docker-up docker-down docker-stop logs install restart build clean clear-logs help

.DEFAULT_GOAL := help

# Run Server and all Clients in parallel (Local development)
dev:
	@echo "Starting Server, Web Client, Partner Client, and Owner Client services..."
	@make -j 4 dev-server dev-web dev-partner dev-owner

# Run Backend only
dev-server:
	@echo "Starting Spring Boot Server..."
	@make clear-logs && cd Server && ./mvnw clean compile -DskipTests spring-boot:run

# Run all Clients in parallel
dev-client:
	@echo "Starting Next.js Web, Partner, and Owner Clients..."
	@make -j 3 dev-web dev-partner dev-owner

# Run Web Client only (Port 3000)
dev-web:
	@echo "Starting Next.js Web Client on port 3000..."
	@PORT=3000 npm run dev --workspace=apps/web --prefix Client

# Run Partner Client only (Port 3002)
dev-partner:
	@echo "Starting Next.js Partner Client on port 3002..."
	@PORT=3002 npm run dev --workspace=apps/partner --prefix Client

# Run Owner Client only (Port 3005)
dev-owner:
	@echo "Starting Next.js Owner Client on port 3005..."
	@PORT=3005 npm run dev --workspace=apps/owner --prefix Client

# Docker Infrastructure Commands
docker-infra:
	@echo "Starting infrastructure..."
	@docker-compose up -d db redis kafka kafdrop elasticsearch kibana prometheus grafana
	@echo "Waiting for Elasticsearch to be ready..."
	@until [ "$$(docker inspect --format='{{.State.Health.Status}}' omnibooking-elastic)" = "healthy" ]; do \
		printf '.'; \
		sleep 2; \
	done
	@echo "\nInfrastructure (DB, Redis, Kafka, ES, Prometheus, Grafana) is Ready..."

monitoring:
	@echo "Starting Prometheus and Grafana..."
	@docker-compose up -d prometheus grafana
	@echo "Prometheus is running at http://localhost:9090"
	@echo "Grafana is running at http://localhost:3001"

test-server:
	@echo "Running Server unit tests..."
	@cd Server && ./mvnw test

# Docker Full Stack Commands
docker-up:
	@echo "Starting all services in Docker (detached)..."
	@docker-compose up -d

docker-down:
	@echo "Stopping all Docker services..."
	@docker-compose down -v

docker-stop:
	@echo "Stopping all Docker services..."
	@docker-compose stop

logs:
	@docker-compose logs -f

# Install dependencies for both projects
install:
	@echo "Installing Root dependencies..."
	@npm install
	@echo "Installing Server dependencies..."
	@cd Server && ./mvnw dependency:resolve
	@echo "Installing Client dependencies..."
	@npm install --prefix Client

restart:
	@echo "Rebuilding and restarting all Docker services..."
	@docker-compose up -d --build

# Build projects for production
build:
	@echo "Building Server artifacts..."
	@cd Server && ./mvnw clean package -DskipTests
	@echo "Building Client artifacts..."
	@npm run build:web --prefix Client
	@npm run build:partner --prefix Client
	@npm run build:owner --prefix Client

# Remove build artifacts and temporary files
clean:
	@echo "Cleaning Server build directory..."
	@cd Server && ./mvnw clean
	@echo "Cleaning Client build directories..."
	@rm -rf Client/apps/web/.next Client/apps/partner/.next Client/apps/owner/.next Client/out Client/node_modules

# Clean logs
clear-logs:
	@rm -rf Server/logs/*
	@echo "Logs cleaned..."

# Display help information
help:
	@echo "OmniBooking Monorepo Management:"
	@echo "  Development Flow:"
	@echo "    make docker-infra  - Start DB and Redis in Docker"
	@echo "    make dev           - Run Server, Web and Partner clients in parallel"
	@echo "    make dev-server    - Run Spring Boot Server only"
	@echo "    make dev-client    - Run both Web and Partner clients in parallel"
	@echo "    make dev-web       - Run Web client only (Port 3000)"
	@echo "    make dev-partner   - Run Partner client only (Port 3002)"
	@echo ""
	@echo "  Docker Full Stack:"
	@echo "    make docker-up     - Start everything in Docker"
	@echo "    make docker-down   - Stop everything"
	@echo "    make docker-stop   - Stop Docker containers"
	@echo "    make restart       - Rebuild and restart Docker containers"
	@echo ""
	@echo "  Maintenance:"
	@echo "    make install       - Install all dependencies"
	@echo "    make clean         - Remove build artifacts"
	@echo "    make logs          - Tail Docker logs"
	@echo "    make monitoring    - Start Prometheus and Grafana"
	@echo "    make test-server   - Run server unit tests"
