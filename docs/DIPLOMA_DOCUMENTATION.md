# University Academic Management System - Diploma Documentation

## Introduction

The University Academic Management System is a desktop application for managing selected academic processes in a university environment. The system supports users, student groups, fields of study, semesters, curriculum subjects, weekly schedules, class meetings, attendance, rooms, reports, notifications, and user activity history.

The project is designed as a diploma-ready information system with a strong focus on database architecture. The application uses PostgreSQL as the only database engine and stores business-critical rules directly in the database through explicit DDL, constraints, indexes, triggers, and Flyway migrations. JavaFX provides the client interface, while the domain model is persisted with Jakarta Persistence and Hibernate.

The main goal of the project is not only to implement working functionality, but also to prove that the database design is intentional, normalized, enforceable, and suitable for future deployment.

## Context

Universities manage many related academic objects: students, teachers, administrators, groups, fields of study, semesters, subjects, class meetings, attendance records, reports, and communication events. These objects have strict consistency rules. For example, a teacher should not be assigned to a class meeting for a subject they are not qualified to teach, attendance should not be recorded for a student outside the meeting group, and room bookings should not overlap.

The project models this domain as a relational PostgreSQL database with a Java desktop application on top. The database is treated as a central source of truth, not only as passive storage. This is important because many rules are data-integrity rules and must remain valid regardless of whether data is inserted through the GUI, a service method, or a future integration.

## Functional Requirements

FR-01. The system shall allow users to log in using email and password.

FR-02. The system shall distinguish between student, teacher, and administrator roles.

FR-03. The system shall display role-specific navigation after login.

FR-04. The system shall allow users to view public schedule information.

FR-05. The system shall restrict detailed schedule and attendance views according to user role.

FR-06. The system shall allow administrators to manage people, fields of study, semesters, student groups, subjects, teachers, and rooms.

FR-07. The system shall support multiple email addresses per person, with one primary email.

FR-08. The system shall allow administrators to view and manage all emails assigned to a user.

FR-09. The system shall allow administrators to create notifications for selected user emails, a whole group, or all users.

FR-10. The system shall allow teachers and administrators to create and manage class meetings.

FR-11. The system shall support class meeting statuses: draft, scheduled, cancelled, and completed.

FR-12. The system shall allow weekly schedule templates to generate concrete class meetings.

FR-13. The system shall allow classroom and online meeting modes.

FR-14. The system shall support room booking for classroom meetings.

FR-15. The system shall prevent overlapping active room bookings.

FR-16. The system shall allow teachers and administrators to mark and correct attendance.

FR-17. The system shall prevent students from editing attendance.

FR-18. The system shall generate attendance reports with filters by semester, teacher, subject, group, class type, and date range.

FR-19. The system shall export report data to CSV.

FR-20. The system shall show historical data: class meetings, attendance, reports, room bookings, notifications, scheduled tasks, and user activity logs.

FR-21. The system shall store important user activity events, such as email and password changes.

FR-22. The system shall support a many-to-many relationship between semesters and fields of study.

FR-23. The system shall define curriculum subjects for an exact semester-field pair.

FR-24. The system shall enforce that schedules and meetings use subjects valid for the selected academic context.

## Non-Functional Requirements

NFR-01. The system shall use PostgreSQL as the only production database.

NFR-02. The database schema shall be created by explicit SQL migrations, not by Hibernate automatic schema generation.

NFR-03. Database migrations shall be versioned and repeatable through Flyway.

NFR-04. Important business rules shall be enforced in PostgreSQL, not only in Java.

NFR-05. The database shall include indexes based on actual query patterns.

NFR-06. The system shall preserve referential integrity through foreign keys.

NFR-07. The system shall use transactions for operations that modify multiple related entities.

NFR-08. The system shall be runnable locally using PostgreSQL 16.

NFR-09. The system shall provide deployment support through Docker Compose for PostgreSQL and pgAdmin.

NFR-10. The system shall be maintainable through a layered code structure: GUI, services, repositories, domain model, and persistence configuration.

NFR-11. The system shall provide proof scripts for database rules and performance-related indexes.

NFR-12. The system shall be understandable for diploma defence through documentation, business-rule mapping, and diagram descriptions.

## Actors

### Student

A student can log in, view public schedule information, view their own group, view their own attendance, view allowed class meeting details, and read their notifications. A student cannot manage academic structure, mark attendance, generate reports, or create notifications.

### Teacher

A teacher can log in, view their assigned meetings, inspect allowed class meeting details, mark attendance for meetings assigned to them, view history related to their teaching, and receive notifications. A teacher can only teach subjects for which they are qualified.

### Administrator

An administrator has the broadest access. The administrator can manage academic data, users, emails, rooms, schedules, reports, notifications, and system history. The administrator can create targeted notifications for a single user email, multiple selected emails, a whole group, or all users.

### System Scheduler

The system scheduler is an internal actor responsible for processing scheduled notification tasks. It selects pending tasks whose scheduled time has passed, attempts processing, and updates task status, retry count, and failure reason.

### PostgreSQL Database

The database acts as a consistency-enforcing component. It stores data and enforces constraints, triggers, indexes, inheritance mappings, and business rules that must remain valid independently of the GUI.

## Use Case Diagram

No generated diagram is included in this document. The use case diagram should be created manually using the following description.

The diagram should contain four actors: Student, Teacher, Administrator, and System Scheduler. A fifth external system boundary may be shown as PostgreSQL Database if the diagram style allows supporting systems.

The system boundary should be named "University Academic Management System".

Student use cases:

- Log in.
- View public schedule.
- View personal schedule.
- View own group.
- View own attendance.
- View own notifications.
- Manage own email/password settings.

Teacher use cases:

- Log in.
- View assigned meetings.
- Open class meeting details.
- Mark attendance.
- Correct attendance.
- View teaching history.
- View notifications.

Administrator use cases:

- Log in.
- Manage users.
- Manage user emails.
- Manage fields, semesters, groups, and subjects.
- Manage curriculum subjects for semester-field pairs.
- Manage rooms.
- Manage class meetings.
- Generate weekly schedule.
- Generate attendance report.
- Export report.
- Create notification.
- Select notification recipients by email, group, or all users.
- View system history and activity logs.

System Scheduler use cases:

- Process pending notification tasks.
- Mark notification task as sent or failed.

Important include/extend relationships:

- "Generate weekly schedule" includes "Validate teacher qualification", "Validate curriculum subject", and "Validate room availability".
- "Mark attendance" includes "Validate meeting status" and "Validate student belongs to meeting group".
- "Create classroom meeting" includes "Book room".
- "Create notification" includes "Select recipient scope".
- "Export report" extends "Generate attendance report".

## Use Case Scenarios

### UC-01: User Login

Primary actor: Student, Teacher, or Administrator.

Preconditions:

- The user exists in the database.
- The user has at least one email address.
- The password hash is stored.

Main flow:

1. The user enters email and password.
2. The system searches for a person by email.
3. The system verifies the password.
4. The system determines the role from the specialized person table.
5. The system opens role-specific navigation.

Alternative flow:

- If credentials are invalid, the system rejects login and keeps the user on the login screen.

Postconditions:

- A session is created for the authenticated user.

### UC-02: Administrator Creates Notification

Primary actor: Administrator.

Preconditions:

- Administrator is logged in.
- At least one recipient exists.

Main flow:

1. Administrator opens notification management.
2. Administrator chooses recipient mode: selected user emails, group, or all users.
3. If a single user is selected, the system displays all emails assigned to that user.
4. Administrator selects one or more delivery emails or selects all.
5. Administrator enters notification content and priority.
6. The system creates notification rows with exact delivery targets.
7. PostgreSQL validates that the delivery email belongs to the notification recipient.

Postconditions:

- Notification records are stored and visible to recipients.

### UC-03: Teacher Marks Attendance

Primary actor: Teacher.

Preconditions:

- Teacher is logged in.
- The class meeting is assigned to the teacher.
- The class meeting is scheduled or completed.
- The meeting start time has passed.

Main flow:

1. Teacher opens assigned meeting details.
2. System loads students from the meeting group.
3. Teacher selects attendance status for each student.
4. Teacher optionally enters comments.
5. System saves attendance records.
6. PostgreSQL verifies that every attendance student belongs to the meeting group.

Alternative flow:

- If the meeting is draft, cancelled, or scheduled in the future, the operation is rejected.

Postconditions:

- Attendance records exist for the meeting and can be used in reports.

### UC-04: Administrator Generates Weekly Schedule

Primary actor: Administrator.

Preconditions:

- Semester, field, group, subject, teacher, and room data exist.
- Subject belongs to the exact semester-field curriculum.
- Teacher is qualified for the subject.

Main flow:

1. Administrator defines a weekly schedule entry.
2. The system validates group academic context.
3. The system validates that the subject is available for the group semester-field pair.
4. The system validates teacher qualification.
5. The system generates concrete class meetings between semester start and end dates.
6. Classroom meetings reserve rooms when required.
7. PostgreSQL validates room-booking overlaps and academic-context constraints.

Postconditions:

- Class meetings and related room bookings are stored.

### UC-05: Administrator Generates Attendance Report

Primary actor: Administrator.

Preconditions:

- Class meetings and attendance data exist.

Main flow:

1. Administrator opens report generation.
2. Administrator optionally selects filters.
3. System loads matching class meetings.
4. System calculates report lines per student.
5. Missing attendance is counted according to report logic.
6. System calculates attendance percentages.
7. Administrator exports report to CSV if needed.

Postconditions:

- Report and report lines are stored.
- Exported reports become protected against later modification.

### UC-06: Room Booking Conflict Prevention

Primary actor: Administrator or Teacher.

Preconditions:

- A room exists.
- An active booking already exists for a time range.

Main flow:

1. User attempts to create another active room booking in the same room.
2. PostgreSQL compares the requested time range with existing active bookings.
3. If ranges overlap, the exclusion constraint rejects the insert or update.

Postconditions:

- The database contains no overlapping active room bookings.

## Repository With ADRs And Architectural Choices

The repository contains Architecture Decision Records in `docs/adr`. Each ADR records a design decision, its context, consequences, and pros and cons. This is useful for diploma defence because it shows the design process, not only the final code.

Current ADRs:

- `0001-use-postgresql-and-flyway.md`: PostgreSQL-only persistence and explicit Flyway migrations.
- `0002-layered-javafx-jpa-architecture.md`: layered desktop architecture with JavaFX, services, repositories, and JPA.
- `0003-enforce-business-rules-in-postgresql.md`: database-level business rules through constraints, triggers, and indexes.
- `0004-model-curriculum-with-semester-field-subject.md`: exact curriculum ownership by semester-field-subject.
- `0005-local-deployment-with-docker-compose.md`: local PostgreSQL and pgAdmin deployment strategy.

Summary of important architectural choices:

| Decision | Pros | Cons |
|---|---|---|
| PostgreSQL instead of H2 | Real production-grade database; supports constraints, triggers, GiST exclusion constraints, indexing, and proof scripts. | Requires local PostgreSQL setup and credentials. |
| Flyway migrations instead of Hibernate schema generation | Versioned schema, explicit DDL, defence-ready database design. | More SQL must be maintained manually. |
| JavaFX desktop client | Simple local prototype, fast development, no web server required. | Less suitable for multi-user web deployment without a future client/server split. |
| Manual repositories and services | Clear control over transactions and queries. | More boilerplate than using Spring Data. |
| Database-enforced business rules | Rules remain valid outside the GUI and can be proven with SQL scripts. | Trigger logic is harder to maintain than simple Java validation. |
| Explicit curriculum table `semester_field_subject` | Correctly models that subjects belong to a semester and field pair. | Adds joins and migration complexity. |

## Database Architecture Description And Diagram

No generated database diagram is included in this document. The ERD should be created manually from the following description.

The database is organized around these entity groups:

### Users And Inheritance

`person` stores common user data: name, surname, birth date, primary email, and password hash. Specialized roles are stored in `student`, `teacher`, and `administrator`. This implements joined inheritance in the database: each specialized role has the same primary key as `person`.

`person_emails` stores multiple email addresses for one person. Email addresses are globally unique. PostgreSQL triggers enforce the rule that each person must have between one and three emails.

### Academic Structure

`field_of_study` stores study fields.

`semester` stores semester number and optional start/end dates.

`semester_field` is the many-to-many bridge between semesters and fields. This is important because one semester can be used by multiple fields, and one field has many semesters.

`student_group` belongs to one exact academic context: semester plus field. A composite foreign key ensures that the selected semester-field pair exists in `semester_field`.

`subject` stores catalog subjects.

`semester_field_subject` stores curriculum subjects for an exact semester-field pair. This means a subject is not simply attached to a semester globally; it is attached to the precise combination of semester and field.

`subject_group` assigns subjects to student groups, but PostgreSQL verifies that such subjects are available in the group's curriculum context.

`teacher_subject` stores teacher qualifications. It is separate from real teaching assignments in class meetings.

### Scheduling And Attendance

`weekly_schedule_entry` stores recurring schedule templates. It includes group, semester, field, subject, teacher, meeting type, meeting mode, day of week, time range, and optional online link or room context.

`class_meeting` stores concrete occurrences of classes. It references subject, teacher, and group and includes meeting date, time, mode, status, and location/link fields.

`attendance` stores student attendance per class meeting. A unique constraint prevents duplicate attendance for the same student and meeting.

### Rooms

`room` stores room number and capacity.

`room_booking` stores room reservations for class meetings. A GiST exclusion constraint prevents overlapping active bookings in the same room.

### Reports

`attendance_report` stores report metadata and filters.

`attendance_report_semesters` stores the many-to-many relationship between reports and semesters.

`report_line` stores per-student calculated report rows.

### Notifications And Logs

`notification` stores both email and system notifications using a single-table inheritance strategy. Email notifications include an exact `delivery_email`.

`scheduled_notification_task` stores delayed notification jobs and processing status.

`user_activity_log` stores relevant user actions such as email and password changes.

Suggested ERD layout:

- Place users on the left: `person`, `person_emails`, `student`, `teacher`, `administrator`.
- Place academic structure in the center: `field_of_study`, `semester`, `semester_field`, `student_group`, `subject`, `semester_field_subject`, `subject_group`, `teacher_subject`.
- Place scheduling below academic structure: `weekly_schedule_entry`, `class_meeting`, `attendance`.
- Place rooms near scheduling: `room`, `room_booking`.
- Place reporting on the right: `attendance_report`, `attendance_report_semesters`, `report_line`.
- Place notifications and logs at the bottom/right: `notification`, `scheduled_notification_task`, `user_activity_log`.

Important relationships to show:

- `person` one-to-many `person_emails`.
- `person` one-to-one optional role specializations: `student`, `teacher`, `administrator`.
- `semester` many-to-many `field_of_study` through `semester_field`.
- `semester_field` one-to-many `semester_field_subject`.
- `student_group` many-to-one exact `semester_field`.
- `subject` many-to-many `student_group` through `subject_group`.
- `teacher` many-to-many `subject` through `teacher_subject`.
- `class_meeting` many-to-one `subject`, `teacher`, and `student_group`.
- `attendance` many-to-one `student` and `class_meeting`.
- `room_booking` many-to-one `room` and optional `class_meeting`.
- `notification` many-to-one `person`.
- `scheduled_notification_task` many-to-one `person` and optional `class_meeting`.

## Database Strategies

### Explicit DDL

The schema is defined through Flyway SQL migrations from `V1` to `V7`. Hibernate validates the schema but does not create it. This prevents hidden ORM defaults from defining the physical database design.

### Normalization

The model separates core concepts into dedicated tables. Examples:

- Person data is separated from role-specific student, teacher, and administrator data.
- Teacher qualification is separated from class meeting assignment.
- Weekly schedule templates are separated from concrete class meetings.
- Curriculum ownership is represented by `semester_field_subject`.
- Reports store metadata separately from report lines.

### Referential Integrity

Foreign keys protect relationships between entities. Composite foreign keys are used where a simple single-column reference would be too weak, especially for group academic context and semester-field curriculum.

### Database-Level Business Rules

PostgreSQL enforces important rules through constraints and triggers:

- Room bookings cannot overlap for active bookings.
- Attendance student must belong to the class meeting group.
- Teacher must be qualified for the class meeting subject.
- Subject must belong to the group's semester-field curriculum.
- Weekly schedule entries must match group academic context.
- Person email count must stay between one and three.
- Teacher subject qualification count must stay between one and five.
- Completed meetings and exported reports have restricted mutation rules.

### Indexing Strategy

Indexes are based on expected application queries:

- Schedule lookups by status/date, teacher/date, group/date, and subject/date.
- Attendance lookup by student and class meeting.
- Room availability lookup by room/date/time.
- Notification inbox lookup by recipient/status.
- Scheduler lookup by status/scheduled time.
- Curriculum lookup by semester, field, and subject.
- Activity log lookup by actor and time.

The proof script `sql/postgresql_performance_proofs.sql` contains `EXPLAIN (ANALYZE, BUFFERS)` queries that can be used in the thesis to connect indexes to actual query patterns.

### Migration Strategy

Flyway tracks schema versions in `flyway_schema_history`. Each migration represents a logical schema improvement:

- `V1`: base schema.
- `V2`: user activity log.
- `V3`: strengthened business rules.
- `V4`: cardinality and report-export protections.
- `V5`: lifecycle and required-detail rules.
- `V6`: email notification delivery targets.
- `V7`: semester-field many-to-many and exact curriculum model.

### Proof Strategy

The repository includes two SQL proof scripts:

- `postgresql_business_rule_proofs.sql` intentionally attempts invalid operations and expects PostgreSQL to reject them.
- `postgresql_performance_proofs.sql` runs query plans for schedule, attendance, room, report, notification, scheduler, and curriculum use cases.

## GUI Design Description

The GUI is implemented as a JavaFX desktop client. It uses role-based navigation, where the available screens depend on the authenticated user's role.

The GUI should be described in diagrams or screenshots as follows:

- Login screen: email/password form and error feedback.
- Main layout: navigation area plus content area.
- Student view: public schedule, personal schedule, own group, own attendance, notifications, and settings.
- Teacher view: assigned meetings, attendance marking, history, notifications, and settings.
- Administrator view: management screens for users, emails, academic structure, schedules, rooms, reports, notifications, and system history.

Design principles:

- Keep operational screens dense and table-based because the system manages structured data.
- Use clear role separation to reduce accidental unauthorized actions.
- Use forms for creating and editing entities.
- Use tables for browsing schedules, attendance, reports, users, groups, and notifications.
- Display validation errors close to the action that caused them.

## Prototype

The current implementation is a working desktop prototype. It demonstrates the core diploma scope:

- PostgreSQL persistence.
- Flyway migrations.
- JavaFX GUI.
- Role-based access.
- Academic structure management.
- Semester-field many-to-many model.
- Curriculum subject assignment.
- Weekly schedule generation.
- Class meeting management.
- Room booking conflict prevention.
- Attendance marking.
- Attendance report generation and CSV export.
- Notification targeting.
- User activity logs.

The prototype is suitable for local demonstration with PostgreSQL 16. It is not yet packaged as a production installer and does not include a full automated test suite.

## System Architecture

The system uses a layered desktop architecture.

Suggested architecture diagram description:

- Top layer: JavaFX GUI (`app` package).
- Application/service layer: services such as authentication, attendance, reporting, rooms, notifications, scheduling, and user management.
- Persistence layer: repositories and transaction manager.
- Domain layer: JPA entities and value objects.
- Database layer: PostgreSQL schema, constraints, triggers, indexes, and Flyway migrations.

Data flow:

1. User interacts with JavaFX views.
2. GUI calls service methods.
3. Services validate business operations and start transactions when needed.
4. Repositories execute JPA/Hibernate queries.
5. Hibernate persists domain entities to PostgreSQL.
6. PostgreSQL performs final integrity checks through constraints and triggers.
7. Errors are returned to the service/GUI and shown to the user.

## Technology

| Area | Technology |
|---|---|
| Language | Java 17 |
| UI | JavaFX 17 |
| Build | Maven Wrapper |
| ORM | Jakarta Persistence and Hibernate 7 |
| Validation | Hibernate Validator / Bean Validation |
| Database | PostgreSQL 16 |
| Migrations | Flyway 12 |
| Logging | SLF4J Simple |
| Deployment support | Docker Compose |
| DB administration | pgAdmin or PostgreSQL command-line tools |

## Client Application

The client is a JavaFX desktop application started through Maven:

```powershell
.\mvnw.cmd javafx:run
```

The main JavaFX application class is `app.AttendanceGuiApp`. It builds the login flow, session handling, role-based navigation, forms, tables, and detail dialogs.

The client connects directly to PostgreSQL through Hibernate/JPA. This is acceptable for a local diploma prototype and simplifies deployment. For a larger production system, the next architecture step would be to introduce a server-side API between the client and database.

## Application Deployment

### Local Deployment

Local deployment requires:

- JDK 17 or newer.
- PostgreSQL 16.
- Database `UAMS`.
- User `demo_user`.
- Password `demo_password`.

Default local JDBC URL:

```text
jdbc:postgresql://localhost:5434/UAMS
```

The application reads optional environment variables:

```powershell
$env:UAMS_DB_URL = "jdbc:postgresql://localhost:5434/UAMS"
$env:UAMS_DB_USER = "demo_user"
$env:UAMS_DB_PASSWORD = "demo_password"
```

Run:

```powershell
.\mvnw.cmd clean javafx:run
```

### Docker-Based Database Deployment

The `deployment/docker-compose.yml` file defines:

- PostgreSQL 16 container.
- pgAdmin container.
- Persistent Docker volumes.
- Healthcheck for PostgreSQL.
- Port mapping from local port `5434` to container port `5432`.

Suggested deployment diagram description:

- Developer machine contains JavaFX application.
- JavaFX application connects over JDBC to PostgreSQL.
- PostgreSQL stores the application schema and data.
- pgAdmin connects to PostgreSQL for database inspection and management.
- Flyway runs from the application startup and applies migrations before Hibernate creates the entity manager.

## Application Testing

The current project includes manual and database-level testing assets.

### Build Verification

The project can be compiled and packaged with:

```powershell
.\mvnw.cmd -q -DskipTests package
```

### Migration Verification

Flyway validates and applies migrations on application startup. Migration success can be checked in the PostgreSQL table `flyway_schema_history`.

### Business Rule Testing

The script `sql/postgresql_business_rule_proofs.sql` verifies that PostgreSQL rejects invalid data. It covers examples such as:

- Invalid curriculum assignment.
- Overlapping room bookings.
- Unqualified teacher assignment.
- Subject outside group curriculum.
- Attendance for a student outside the meeting group.

### Performance/Index Testing

The script `sql/postgresql_performance_proofs.sql` runs `EXPLAIN (ANALYZE, BUFFERS)` for important query patterns:

- Public schedule lookup.
- Teacher schedule/history.
- Student attendance history.
- Room availability.
- Report filtering.
- Notification inbox.
- Scheduler due tasks.
- Curriculum availability.

### GUI Testing

Manual GUI testing should cover:

- Login for student, teacher, and administrator demo accounts.
- Role-specific navigation visibility.
- Creating and editing academic entities.
- Generating weekly schedule.
- Creating room bookings and checking overlap rejection.
- Marking attendance.
- Generating and exporting reports.
- Creating notifications for selected emails, groups, and all users.
- Viewing history and user activity logs.

### Current Testing Limitations

The project does not yet include a full automated unit/integration test suite or CI pipeline. For final diploma polish, automated tests should be added around services, repositories, and PostgreSQL integration rules.
