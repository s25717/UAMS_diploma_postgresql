# ADR 0003: Enforce Critical Business Rules In PostgreSQL

## Status

Accepted.

## Context

Many rules in the academic domain are data-integrity rules, not only GUI rules. Examples include preventing overlapping room bookings, requiring attendance students to belong to the meeting group, and ensuring teachers are qualified for subjects.

## Decision

Enforce critical business rules in PostgreSQL through:

- NOT NULL constraints.
- CHECK constraints.
- UNIQUE constraints.
- Foreign keys.
- Composite foreign keys.
- GiST exclusion constraints.
- PL/pgSQL trigger functions.
- Deferred constraint triggers.

## Pros

- Data remains valid even if inserted outside the GUI.
- Rules can be proven with SQL scripts.
- The database becomes a strong part of the diploma topic.
- PostgreSQL handles concurrency-sensitive rules such as overlapping bookings.

## Cons

- Trigger code is harder to debug than Java validation.
- Some rules are duplicated between Java services and PostgreSQL.
- Deferred constraints require careful migration ordering.

## Consequences

The project treats PostgreSQL as a rule-enforcing system component. Java validation improves user experience, while PostgreSQL provides final integrity guarantees.
