# RVCE Resource Allocator (SCAS)

> A production-grade college resource allocation platform built with Spring Boot, React, Kubernetes, and AWS OIDC deployment.

## Overview

This repository implements a full-stack solution for RVCE room and exam resource allocation.
The system includes:
- Backend API in **Spring Boot 3.2 + Java 17**
- Frontend in **React + Vite + TypeScript + Tailwind**
- Database migrations with **Flyway** on **PostgreSQL**
- Caching and session support with **Redis**
- Secure authentication with **JWT RS256**, role-based access, and permission-based authorization
- Secrets management with **AWS Secrets Manager** and Kubernetes secrets
- CI/CD deployment using **GitHub Actions** with **OIDC / AWS EKS**

This README is written to help you remember the architecture, main features, and implementation details for interviews.

## What I implemented

### Core platform capabilities
- **User authentication & authorization** with JWT access tokens and Redis-backed refresh tokens
- **Role-based access control (RBAC)** for STUDENT, TEACHER, TTO, EXAM_CONTROLLER, SUPER_ADMIN
- **Permission-based method security** on backend services
- **Room availability search** with timetable and override exclusion logic
- **Teacher substitution engine** with clash detection and atomic apply/rollback semantics
- **Day override / cancellation** for teachers with room availability refresh and event publishing
- **Exam seating allocation / admin dashboards** for resource planning and student seat assignments
- **Frontend pages** for teacher room lookup, substitutions, exam control, admin workflows, and student views
- **Encryption at rest** for sensitive columns (USN, email, phone) using AES-256-GCM
- **Secure deployment pipeline** using GitHub Actions, ECR, EKS, and IAM OIDC

### Security and reliability
- JWT signed with **RS256 asymmetric key pair**
- **Public key endpoint** available at `/public-keys/jwt.pub`
- **AWS Secrets Manager** integration for DB credentials, encryption keys, and JWT keys
- **HTTPS / SSL** database connectivity via `sslmode=require`
- **Spring global exception handling** with structured JSON error responses
- **Security headers** and rate limiting in backend (planned or documented)
- **Redis cache invalidation** on timetable or override changes

### Deployment and infrastructure
- **Local development** via `docker compose up --build`
- **GitHub Actions** CI for backend and frontend builds
- **Deploy workflow** in `.github/workflows/deploy.yml`
- **AWS OIDC deployment** documented in `infra/aws/README-oidc.md`
- **Production staging flow** with smoke tests and rollback on failure

## Architecture at a glance

```
[React/Vite frontend] --> HTTPS --> [Spring Boot backend]
                                  |
                                  v
                          +---------------------+
                          | PostgreSQL database |
                          +---------------------+
                                  |
                                  v
                          +---------------------+
                          |      Redis cache    |
                          +---------------------+

Additional services:
- AWS Secrets Manager for secrets
- GitHub Actions OIDC for deployment
- ECR container registry + EKS Kubernetes cluster
```

### Technical stack
- Backend: `backend/`
  - Spring Boot 3.2, Java 17
  - Spring Security, Spring Data JPA, Spring Data Redis
  - Flyway migrations
  - MapStruct, Lombok, SpringDoc OpenAPI
  - PostgreSQL driver, AWS SDK Secrets Manager
- Frontend: `frontend/`
  - React 18, Vite, TypeScript
  - Tailwind CSS, React Router v6
  - React Query, Zustand, React Hook Form, Zod, Axios
  - MSW for local API mocking, Playwright for e2e
- Infra: `infra/`
  - AWS OIDC docs and IAM role guidance
  - Deployment readiness for EKS
- Project root:
  - Docker Compose for local dev
  - GitHub Actions deploy workflow

## Key implementation sections

### Backend packages and important files
- `backend/src/main/java/com/rvce/scas/controller`
- `backend/src/main/java/com/rvce/scas/service`
- `backend/src/main/java/com/rvce/scas/repository`
- `backend/src/main/java/com/rvce/scas/dto`
- `backend/src/main/java/com/rvce/scas/entity`
- `backend/src/main/java/com/rvce/scas/security`
- `backend/src/main/java/com/rvce/scas/config`
- `backend/src/main/resources/db/migration`

### Frontend structure
- `frontend/src/pages`
- `frontend/src/components`
- `frontend/src/services`
- `frontend/src/hooks`
- `frontend/src/store`
- `frontend/src/types`
- `frontend/src/utils`

### Important documentation
- `PROJECT_SETUP.md` — local startup, Docker Compose, Flyway, verification
- `docs/T-602-ENCRYPTION-AND-SECRETS-GUIDE.md` — encryption, secrets, JWT key management
- `infra/aws/README-oidc.md` — OIDC role creation and cluster mapping
- `README_COMPLETION.md` — phase completion and fix summary

## Feature summary

### Authentication & authorization
- `POST /auth/login` returns access token and refresh token
- `POST /auth/refresh` renews access token
- `POST /auth/logout` blacklists refresh tokens
- Custom `UserDetailsService` loads users and roles from DB
- Method-level authorization via `@PreAuthorize`
- AccessDeniedHandler returns JSON 403 responses

### Room availability
- Query endpoint likely similar to `GET /api/rooms/available`
- Filters by date, start/end time, capacity, building
- Excludes rooms with timetable slots or day overrides
- Caches availability in Redis with keys like `room:avail:{date}:{startTime}-{endTime}`

### Teacher substitution engine
- POST endpoint for substitution requests
- Supports `ONE_DAY` and `SEMESTER` scope
- Expands semester requests across the full date range
- Detects teacher clashes by overlapping time windows
- Applies substitution atomically only when no clashes exist
- Returns detailed clash list for manual review otherwise
- Writes audit log entries for every substitution

### Day override / cancellation
- Teachers can cancel specific slots or admins can override
- Inserts `day_overrides` records and invalidates availability cache
- Publishes cancellation events for affected stakeholders
- Supports reinstating overrides

### Exam seating and admin workflows
- Exam seating allocation engine built into backend and dashboard
- User data, rooms, timetable, exam seats, and halls managed in DB
- Frontend supports exam control, batch uploads, and seat allocation

### Encryption & secrets
- AES-256-GCM column encryption for sensitive fields
- JPA `AttributeConverter` transparently encrypts/decrypts DB data
- JWT private/public keys stored outside code via Secrets Manager
- DB connection configured with `sslmode=require`
- Secrets loaded at startup using Spring Cloud AWS Secrets Manager config

## CI/CD and deployment

### Local development
- `docker compose up --build` starts the full stack
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Postgres: `localhost:5432`
- Redis: `localhost:6379`

### GitHub Actions deployment
- `Backend CI` and `Frontend CI` run builds and tests
- `.github/workflows/deploy.yml` deploys after successful CI
- Uses `aws-actions/configure-aws-credentials@v4` with OIDC
- Updates Kubernetes deployments in staging and production
- Performs smoke tests and rollback on failures

### AWS OIDC deployment flow
- Create IAM role with GitHub OIDC trust
- Attach minimal AWS permissions for EKS deployment
- Add role ARN to GitHub Secrets as `AWS_ROLE_TO_ASSUME`
- Workflow uses `role-to-assume` and `aws eks update-kubeconfig`

## How to run locally

```bash
cd /workspaces/rvce-resource-allocator
docker compose down -v --remove-orphans
docker compose up --build
```

### Verify backend and frontend
```bash
curl -s http://localhost:8080/actuator/health
# expected: UP
```

### Run backend tests
```bash
cd backend
./gradlew test
```

### Run frontend build
```bash
cd frontend
npm ci
npm run build
```

## What to emphasize in an interview

- The project is designed as a **full-stack, enterprise-ready solution** with separate backend and frontend apps.
- I implemented **secure JWT auth with RS256**, refresh token lifecycle, and permission-based authorization.
- I built **teacher substitution and room availability services** with clash detection and cache invalidation.
- I used **Flyway migrations** for database schema and seed data, ensuring consistent deployments.
- I added **AES-256 encrypted PII storage** and integrated **AWS Secrets Manager**.
- I deployed using **GitHub Actions + AWS OIDC** into **EKS with rollback support**.
- I can explain the end-to-end flow from user login to room search, substitution, and audit logging.
- I can explain how the app uses **Redis caching**, **Spring Security**, **PostgreSQL**, and **React + Tailwind**.

## Notes for memory retention

- Backend packages are under `backend/src/main/java/com/rvce/scas`
- Frontend pages are under `frontend/src/pages`
- Migrations are in `backend/src/main/resources/db/migration`
- Deployment pipeline is in `.github/workflows/deploy.yml`
- OIDC docs are in `infra/aws/README-oidc.md`
- Encryption docs are in `docs/T-602-ENCRYPTION-AND-SECRETS-GUIDE.md`

## Useful repo files

- `PROJECT_SETUP.md` — local environment and compose startup
- `README_COMPLETION.md` — phase completion summary
- `QUICK_START.md` — quick run instructions and component mapping
- `docs/T-602-ENCRYPTION-AND-SECRETS-GUIDE.md` — security, JWT, and secrets details
- `infra/aws/README-oidc.md` — GitHub Actions OIDC deployment instructions

---

## Quick Reference: Important points to remember

- **Tech stack**: Spring Boot, React, PostgreSQL, Redis, AWS, Kubernetes
- **Security**: RS256 JWT, OAuth-like refresh flow, AES-256 column encryption
- **Deployment**: GitHub Actions, OIDC, EKS, ECR, kubeconfig updates, rollout status checks
- **Main flows**: room availability, teacher substitution, day override, exam seating
- **Key docs**: `PROJECT_SETUP.md`, `README_COMPLETION.md`, `docs/T-602-ENCRYPTION-AND-SECRETS-GUIDE.md`

