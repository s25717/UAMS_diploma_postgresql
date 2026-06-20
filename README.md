# University Academic Management System

Diploma-ready JavaFX university management system using PostgreSQL, Flyway migrations, Hibernate/JPA, Bean Validation, and database-level business rules.

## What Is Included

- Java 17 Maven project with JavaFX GUI.
- PostgreSQL-only persistence, configured through environment variables.
- Flyway migrations `V1` through `V7`.
- Explicit PostgreSQL DDL, constraints, indexes, triggers, and proof scripts.
- JPA inheritance examples:
  - `Person -> Student / Teacher / Administrator` with `JOINED`.
  - `Notification -> EmailNotification / SystemNotification` with `SINGLE_TABLE`.
- Rendered diagram images in the repository root.
- Diploma documentation and ADRs in `docs/`.
- Business rule evidence in `BUSINESS_RULES_MATRIX.md`.

## Requirements

- JDK 17 or newer.
- PostgreSQL 16 recommended.
- JavaFX is downloaded by Maven.
- Maven is optional because the project includes `mvnw.cmd`.

## Database Setup

Create a PostgreSQL database and user. Example for local PostgreSQL on port `5434`:

```sql
CREATE DATABASE "UAMS";
CREATE USER demo_user WITH PASSWORD 'demo_password';
GRANT ALL PRIVILEGES ON DATABASE "UAMS" TO demo_user;
```

Set environment variables before running the app:

```powershell
$env:UAMS_DB_URL = "jdbc:postgresql://localhost:5434/UAMS"
$env:UAMS_DB_USER = "demo_user"
$env:UAMS_DB_PASSWORD = "demo_password"
```

Flyway applies all migrations automatically on startup. These are also the default connection settings in the app, so a normal local setup can run without extra environment variables.

## Run The Project

Compile and package:

```powershell
.\mvnw.cmd -q -DskipTests package
```

Run the JavaFX application:

```powershell
.\mvnw.cmd javafx:run
```

There is also a helper script:

```powershell
.\tools\run-gui-postgres16.ps1
```

## Proof Scripts

After the app has migrated the database, proof scripts can be run with `psql`:

```powershell
psql -h localhost -p 5434 -U demo_user -d UAMS -f sql/postgresql_business_rule_proofs.sql
psql -h localhost -p 5434 -U demo_user -d UAMS -f sql/postgresql_performance_proofs.sql
```

## Demo Accounts

The sample data service creates demo accounts when the database is empty:

- Student: `anna.nowak@student.pja.edu.pl` / `student`
- Teacher: `piotr.kowalski@pja.edu.pl` / `teacher`
- Admin: `admin@pja.edu.pl` / `admin`

## Current Status

Implemented:

- PostgreSQL migration from H2-style setup.
- Explicit DDL with Flyway.
- Core domain entities, inheritance, associations, and validation.
- Real database constraints, triggers, indexes, and proof scripts.
- Semester-field many-to-many model.
- Exact semester-field-subject curriculum association.
- Group and weekly schedule academic context enforcement.
- Room booking overlap prevention.
- Attendance registration and correction flow.
- Admin notification targeting by selected user emails, group, or all users.
- Admin/user email management.
- JavaFX screens for users, academic structure, schedules, reports, rooms, notifications, and histories.

Not finished yet:

- Full automated test suite.
- CI pipeline on GitHub.
- Production-grade packaging/installer.
- PNG images for every recently edited diagram.
- Final thesis text polish outside this repository.
