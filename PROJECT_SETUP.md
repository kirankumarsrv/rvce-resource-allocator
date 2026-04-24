# SCAS Project Setup Guide

This guide is for new teammates who are starting work on this repository.

## 1) Prerequisites

Install these first:

1. Git
2. Docker Desktop (Windows/Mac) with Docker engine running
3. VS Code (recommended)

Recommended VS Code extensions:

1. Docker
2. Extension Pack for Java
3. ESLint

Optional local runtimes (only needed if running outside Docker):

1. Node.js 20+
2. JDK 17

## 2) Clone and Open

```bash
git clone <your-repo-url>
cd rvce-resource-allocator
```

Main orchestrator is root compose file:

- `docker-compose.yml`

## 3) First-Time Run (Clean)

From repo root:

```bash
docker compose down -v --remove-orphans
docker compose up --build
```

This starts:

1. Frontend: http://localhost:5173
2. Backend: http://localhost:8080
3. Postgres: localhost:5432
4. Redis: localhost:6379

## 4) Daily Workflow

Start stack:

```bash
docker compose up
```

Stop stack (keep DB data):

```bash
docker compose down
```

Rebuild after dependency/config changes:

```bash
docker compose up --build
```

Full reset (deletes DB data and reseeds from migrations):

```bash
docker compose down -v --remove-orphans
docker compose up --build
```

## 5) Migrations and Seed Data

Flyway migrations are in:

- `backend/src/main/resources/db/migration`

On backend startup, Flyway runs migrations in order (V1 to V6).

Seed data is in:

- `backend/src/main/resources/db/migration/V6__seed_data.sql`

Important:

1. `docker compose down` keeps Postgres volume and data.
2. `docker compose down -v` deletes Postgres volume, so migrations and seed run again from scratch on next `up`.

## 6) Verification Commands

Check migration history:

```bash
docker exec -it rvce-resource-allocator-postgres-1 psql -U scas -d scas_db -c "select installed_rank, version, description, success from flyway_schema_history order by installed_rank;"
```

Check tables:

```bash
docker exec -it rvce-resource-allocator-postgres-1 psql -U scas -d scas_db -c "\dt"
```

Check backend logs:

```bash
docker compose logs -f backend
```

## 7) Troubleshooting

### Docker pipe/engine error on Windows
If you see pipe or engine connection errors, Docker Desktop is not running.

Fix:

1. Start Docker Desktop
2. Wait until engine status is healthy
3. Re-run compose commands

### Backend exits during Flyway migration
Check backend logs for the first Flyway `ERROR` block:

```bash
docker compose logs -f backend
```

Then fix the failing SQL migration and rerun with clean reset:

```bash
docker compose down -v --remove-orphans
docker compose up --build
```

## 8) What Git Push/Pull Shares

Git shares:

1. Source code
2. Docker and compose files
3. Flyway migration scripts

Git does NOT share:

1. Your local containers
2. Your local Docker volumes
3. Your local seeded database state

Each teammate recreates DB state locally by running compose and migrations.
