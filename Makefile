iffile := $(wildcard .env)
ifneq ($(iffile),)
  include .env
  export $(shell sed 's/=.*//' .env)
endif

.PHONY: dev dev-server dev-client docker-infra docker-up docker-down docker-stop logs install restart build clean help

.DEFAULT_GOAL := help

# Run both Server and Client in parallel (Local development)
dev:
	@echo "Starting Server and Client services..."
	@make -j 2 dev-server dev-client

# Run Backend only
dev-server:
	@echo "Starting Spring Boot Server..."
	@cd Server && ./mvnw spring-boot:run

# Run Frontend only
dev-client:
	@echo "Starting Next.js Client..."
	@npm run dev --prefix Client

# Docker Infrastructure Commands
docker-infra:
	@echo "Starting infrastructure..."
	@docker-compose up -d db redis kafka kafdrop elasticsearch kibana
	@echo "Infrastructure (DB, Redis, Kafka, ES) is Ready..."

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
	@npm run build --prefix Client

# Remove build artifacts and temporary files
clean:
	@echo "Cleaning Server build directory..."
	@cd Server && ./mvnw clean
	@echo "Cleaning Client build directory..."
	@rm -rf Client/.next Client/out Client/node_modules

# Display help information
help:
	@echo "OmniBooking Monorepo Management:"
	@echo "  Development Flow:"
	@echo "    make docker-infra  - Start DB and Redis in Docker"
	@echo "    make dev           - Run Server and Client locally (Fast hot-reload)"
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
