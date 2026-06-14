# ADR 0005: Support Local Deployment With Docker Compose

## Status

Accepted.

## Context

The application needs a repeatable local deployment for demonstration, testing, and database inspection. The user may run PostgreSQL directly on Windows or through Docker.

## Decision

Provide `deployment/docker-compose.yml` with:

- PostgreSQL 16.
- pgAdmin.
- Persistent volumes.
- Configurable ports and credentials.
- Healthcheck for PostgreSQL.

## Pros

- Easier setup on machines without a local PostgreSQL service.
- pgAdmin is available for database inspection.
- Database and admin UI can be started together.
- Credentials and ports are documented in one place.

## Cons

- Requires Docker Desktop.
- Desktop JavaFX application still runs outside the containers.
- File permissions and port conflicts can require local troubleshooting.

## Consequences

The project supports both direct PostgreSQL installation and containerized database deployment. This is enough for diploma demonstration and local testing.
