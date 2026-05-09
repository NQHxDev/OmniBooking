iffile := $(wildcard .env)
ifneq ($(iffile),)
  include .env
  export $(shell sed 's/=.*//' .env)
endif

.PHONY: install dev dev-server dev-client build clean up down docker-stop logs restart infra help

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

# Infrastructure Commands
infra:
	@echo "Starting infrastructure..."
	@docker-compose up -d db redis kafka kafdrop
	@echo "Infrastructure is Ready..."

# Docker Full Stack Commands
up:
	@echo "Starting all services in Docker (detached)..."
	@docker-compose up -d

down:
	@echo "Stopping all Docker services..."
	@docker-compose down -v

docker-stop:
	@echo "Stopping all Docker services..."
	@docker-compose stop

logs:
	@docker-compose logs -f

restart:
	@echo "Rebuilding and restarting all Docker services..."
	@docker-compose up -d --build

# Install dependencies for both projects
install:
	@echo "Installing Root dependencies..."
	@npm install
	@echo "Installing Server dependencies..."
	@cd Server && ./mvnw dependency:resolve
	@echo "Installing Client dependencies..."
	@npm install --prefix Client

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
	@echo "    make infra     - Start DB and Redis in Docker"
	@echo "    make dev       - Run Server and Client locally (Fast hot-reload)"
	@echo ""
	@echo "  Docker Full Stack:"
	@echo "    make up           - Start everything in Docker"
	@echo "    make down         - Stop everything"
	@echo "    make restart      - Rebuild and restart Docker containers"
	@echo ""
	@echo "  Maintenance:"
	@echo "    make install      - Install all dependencies"
	@echo "    make clean        - Remove build artifacts"
	@echo "    make logs         - Tail Docker logs"
