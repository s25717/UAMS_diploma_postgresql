# ADR 0004: Model Curriculum With Semester-Field-Subject

## Status

Accepted.

## Context

A semester does not belong to exactly one field of study. Different fields can use the same semester number and period, but their schedules and subject sets may differ. Therefore, attaching a subject only to a semester is not precise enough.

## Decision

Introduce:

- `semester_field` for the many-to-many relationship between semesters and fields.
- `semester_field_subject` for subjects assigned to an exact semester-field pair.
- `field_id` in `student_group` and `weekly_schedule_entry` to preserve academic context.

## Pros

- Correctly represents the academic domain.
- Allows different fields to share a semester while keeping different curricula.
- Supports schedule generation per semester-field context.
- Enables PostgreSQL constraints to validate curriculum membership.

## Cons

- Adds more joins.
- Requires composite foreign keys.
- Makes migrations and GUI forms more complex.

## Consequences

The database now has a precise curriculum model. Class meetings, weekly schedules, and group-subject assignments can be checked against the correct semester-field-subject context.
