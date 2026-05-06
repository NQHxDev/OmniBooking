# 🏨 OmniBooking Monorepo

[![CI Pipeline](https://github.com/anhjkr/OmniBooking/actions/workflows/ci.yml/badge.svg)](https://github.com/anhjkr/OmniBooking/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Next.js Version](https://img.shields.io/badge/Next.js-15+-black.svg)](https://nextjs.org/)

**OmniBooking** is a high-performance, enterprise-grade booking management ecosystem. Designed with a modern micro-monorepo architecture, it provides a robust foundation for scalable reservation services, featuring advanced security patterns and high-concurrency optimizations.

---

## 🚀 Key Highlights

- **Professional DB Schema**: Implements **UUID v7** (time-ordered) for optimal B-Tree performance, **Soft Delete**, and **Optimistic Locking**.
- **Security-First**: Built-in **RBAC** (Role-Based Access Control) with Account/Profile separation and Argon2 hashing.
- **High Performance**: Integrated **Redis Stack** with **Bloom Filter** for ultra-fast existence checks and distributed caching.
- **Full Observability**: Centralized logging via **AOP**, **MDC** Request Tracing (X-Request-ID), and Spring Boot Actuator monitoring.
- **Enterprise Standards**: Standardized `ApiResponse`, global exception handling, and API versioning (`/api/v1`).

---

## 🛠 Tech Stack

| Layer        | Technologies                                                                 |
| :----------- | :--------------------------------------------------------------------------- |
| **Backend**  | Spring Boot 3.4, Spring Security, Spring Data JPA, Flyway, Hibernate         |
| **Frontend** | Next.js 15 (App Router), TypeScript, Tailwind CSS 4, Shadcn/ui, Lucide Icons |
| **Database** | PostgreSQL 16, Redis Stack (RedisBloom, RedisJSON)                           |
| **DevOps**   | Docker, Docker Compose, GitHub Actions, Husky, Make                          |

---

## 📦 Getting Started

### 1. Prerequisites

- **Docker & Docker Compose** (Highly Recommended)
- **Java 21** & **Node.js 23+** (For local development)

### 2. Fast Infrastructure Setup

The most professional way to start is using our specialized **Makefile**:

```bash
# 1. Setup your environment
cp .env.example .env

# 2. Launch Infrastructure (DB & Redis)
make infra

# 3. Run Application locally (with Hot-Reload)
make dev
```

### 3. Service Access Points

- **Frontend**: [http://localhost:3000](http://localhost:3000)
- **Backend API**: [http://localhost:8080/api/v1](http://localhost:8080/api/v1)
- **API Documentation**: [http://localhost:8080/api/v1/swagger-ui.html](http://localhost:8080/api/v1/swagger-ui.html)
- **System Health**: [http://localhost:8080/api/v1/actuator/health](http://localhost:8080/api/v1/actuator/health)

---

## 🏗 Architecture Documentation

For a deep dive into our design decisions, implementation patterns, and infrastructure strategy, please refer to our comprehensive:

👉 [**ARCHITECTURE.MD**](./ARCHITECTURE.md)

---

## 🛠 Development Workflow

- `make up` - Start everything in Docker.
- `make restart` - Force rebuild and restart all services.
- `make logs` - Follow logs from all containers.
- `make clean` - Clean up build artifacts.

---

© 2026 OmniBooking Team. Built with Passion & Precision.
