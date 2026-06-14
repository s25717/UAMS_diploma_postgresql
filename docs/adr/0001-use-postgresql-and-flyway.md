# ADR 0001: Use PostgreSQL And Flyway For Database Management

## Status

Accepted.

## Context

The project originally needed to become a serious diploma system with a database-design focus. The database could not remain dependent on an in-memory or teaching-oriented setup. The schema also needed to be explicit and defensible.

## Decision

Use PostgreSQL as the only database engine and Flyway as the schema migration tool. Disable Hibernate automatic schema creation and use Hibernate only to validate and access the schema.

## Pros

- PostgreSQL supports advanced constraints, triggers, indexes, and exclusion constraints.
- Flyway creates a clear versioned history of database evolution.
- Explicit SQL DDL is suitable for database architecture documentation.
- The schema can be inspected directly in pgAdmin or psql.

## Cons

- Local setup is more complex than an embedded database.
- SQL migrations require manual maintenance.
- Failed migrations must be repaired carefully during development.

## Consequences

The database schema is now part of the designed architecture. Database changes must be added as Flyway migrations, and Java entity changes must stay aligned with SQL DDL.
