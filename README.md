# RVCE Resource Allocator

A full-stack application for managing room bookings, timetable availability, exam seating, and academic resource allocation for RVCE.

## Overview

This project contains:
- A backend API built with Java and Spring Boot
- A frontend application built with React, Vite, and TypeScript
- PostgreSQL for persistent application data
- Redis for caching and session-related state
- Role-based access control and protected academic workflows

## Main features

- User authentication and authorization
- Role-based access for students, teachers, and admin roles
- Room availability checks and booking logic
- Teacher substitution and clash detection
- Day override and scheduling adjustments
- Exam hall and seating management
- Timetable-driven allocation workflows
- CSV/data import flows for academic records

## Architecture

```text
[Frontend] -> [Spring Boot API] -> [PostgreSQL]
                                -> [Redis]
```

## Tech stack

### Backend
- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Redis integration
- PostgreSQL

### Frontend
- React
- Vite
- TypeScript
- Tailwind CSS

### Project structure

- `backend/` — API, services, entities, repositories, configuration
- `frontend/` — React app and UI screens
- `infra/` — infrastructure and deployment-related manifests
- `docs/` — project documentation
- `scripts/` and root tools — helper scripts and utilities

## Local setup

```bash
cd /workspaces/rvce-resource-allocator
docker compose up --build
```

The app is expected to run with the backend and frontend services available through the local Docker setup.

## Useful project files

- `PROJECT_SETUP.md` — local setup and environment guidance
- `QUICK_START.md` — quick development instructions
- `README_COMPLETION.md` — summary of completed work
- `docs/` — implementation and design notes

## Notes

This project is centered around academic scheduling and allocation workflows, with a backend-first design and a separate frontend interface for operational use.

