# ADR 0002: Use A Layered JavaFX, Service, Repository, JPA Architecture

## Status

Accepted.

## Context

The application is a desktop prototype intended for diploma demonstration. It needs a clear structure that separates GUI code from business logic and persistence logic.

## Decision

Use a layered architecture:

- JavaFX GUI in the `app` package.
- Service classes for business operations.
- Repository classes for persistence queries.
- JPA/Hibernate entities in the `model` package.
- PostgreSQL schema and migrations in `src/main/resources/db/migration`.

## Pros

- Easy to explain during defence.
- GUI code does not directly manipulate database transactions.
- Services provide a natural place for application-level validation.
- Repositories isolate query logic.

## Cons

- Direct desktop-to-database connection is not ideal for large production systems.
- Manual repositories create more boilerplate.
- Without Spring or a server framework, dependency management is manual.

## Consequences

The architecture is well suited for a local diploma prototype. A future production version could keep the domain and database design but introduce a REST API or server layer between the JavaFX client and PostgreSQL.
