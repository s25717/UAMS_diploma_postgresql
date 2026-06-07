# PostgreSQL Deployment And Proof Guide

This project now uses PostgreSQL as the only database engine. Hibernate validates the schema, while Flyway creates and evolves the physical PostgreSQL schema.

## Local PostgreSQL 16

If you already installed PostgreSQL 16 through pgAdmin, keep using it. The app reads these environment variables:

```powershell
$env:MAS_DB_URL="jdbc:postgresql://localhost:5434/UAMS"
$env:MAS_DB_USER="demo_user"
$env:MAS_DB_PASSWORD="your_password"
.\mvnw.cmd javafx:run
```

Or use the helper:

```powershell
.\tools\run-gui-postgres16.ps1
```

## Docker Deployment Option

The deployment folder contains a PostgreSQL 16 + pgAdmin stack:

```powershell
cd deployment
copy .env.example .env
# edit .env and set real passwords
docker compose up -d
```

Default endpoints:

| Component | URL / port |
|---|---|
| PostgreSQL | `localhost:5434` |
| Database | `UAMS` |
| User | `demo_user` |
| pgAdmin | `http://localhost:5050` |

The JavaFX app still runs on the host machine. The container is for the database and pgAdmin.

## Flyway Verification

In pgAdmin Query Tool:

```sql
select installed_rank, version, description, success, installed_on
from flyway_schema_history
order by installed_rank;
```

Expected migrations:

| Version | Purpose |
|---|---|
| V1 | explicit PostgreSQL schema |
| V2 | user activity log |
| V3 | stronger business rules and the original semester curriculum table |
| V7 | Semester-Field many-to-many context and `semester_field_subject` curriculum |
| V4 | cardinality, notification/report export rules |
| V5 | lifecycle, required details, non-blank checks |
| V6 | email notification delivery targets |

## Business-Rule Proof Script

Run this in pgAdmin after the app has applied Flyway migrations:

```sql
\i sql/postgresql_business_rule_proofs.sql
```

If pgAdmin does not support `\i`, open the file and execute its contents manually.

The script intentionally attempts invalid data and expects PostgreSQL to reject it. It proves:

| Rule area | PostgreSQL proof |
|---|---|
| Room booking overlaps | GiST exclusion constraint rejects overlapping active bookings |
| Teacher qualification | trigger rejects unqualified teacher/subject pairs |
| Curriculum/group subject | trigger rejects subject not assigned to group |
| Attendance membership | trigger rejects attendance for student outside meeting group |
| Group capacity | trigger rejects student beyond `student_group.max_size` |
| Meeting completeness | trigger rejects scheduled classroom meeting without matching booking |
| Online meeting link | check rejects scheduled online meeting without link |
| Date/day consistency | trigger rejects mismatched `meeting_date` and `day_of_week` |
| Completed attendance | trigger rejects completed meeting without full attendance |
| Notification task lifecycle | trigger rejects invalid task state transitions and payloads |

The script ends with `ROLLBACK`, so proof data is not kept.

## Performance Proof Script

Run:

```sql
\i sql/postgresql_performance_proofs.sql
```

This prints `EXPLAIN (ANALYZE, BUFFERS)` plans for the main query patterns:

| Query pattern | Main index/constraint evidence |
|---|---|
| Public schedule | `idx_class_meeting_status_date` |
| Teacher schedule/history | `idx_class_meeting_teacher_date` |
| Student attendance history | `idx_attendance_student_id`, attendance unique key |
| Room availability | `idx_room_booking_room_date`, GiST exclusion |
| Report filters | report/class-meeting filter indexes |
| Notification inbox | `idx_notification_recipient_status` |
| Scheduler due tasks | `idx_scheduled_notification_due` |
| Curriculum availability | unique `(semester_id, field_id, subject_id)` and context indexes on `semester_field_subject` |

For the thesis, include screenshots or copied plans from pgAdmin and explain why the chosen indexes match the query predicates.
