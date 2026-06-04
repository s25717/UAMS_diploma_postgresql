# MP4 MAS Fixed Version

This folder now contains a clean Maven/JPA implementation in the standard layout:

- `pom.xml`
- `src/main/java`
- `src/main/resources/META-INF/persistence.xml`

The old extracted source remains in `MP4_MAS_S25717/src` only as reference material. Maven ignores it because it is outside the standard `src/main` tree.

Covered MP4 requirements:

- JPA entities with generated primary keys.
- Repository classes with `findAll`, `findById`, `save`, `update`, and `delete`.
- Bean Validation annotations: `@NotBlank`, `@Size`, `@Email`, `@Pattern`, `@Min`, `@Max`, `@Past`.
- Multi-value attribute: `Person.emails` via `@ElementCollection`, with global uniqueness enforced by a database unique constraint and `PersonService`.
- Complex attributes: `BirthDate`, `MeetingTime`, and `MeetingSlot` via `@Embeddable` / `@Embedded`.
- Enum attributes: `AttendanceStatus`, `ClassType`, `MeetingMode`, `ClassMeetingStatus`, `ReportType`, `BookingStatus`, `NotificationStatus`, and notification task enums.
- Associations with `mappedBy`: group-student, teacher-meeting, meeting-attendance, etc.
- Many-to-many association: `Subject` and `StudentGroup`.
- Many-to-many qualification association: `Teacher.qualifiedSubjects` and `Subject.qualifiedTeachers`.
- Repository fetch examples using entity graph and JPQL `join fetch`.
- Association with attribute: `Attendance` between `Student` and `ClassMeeting`.
- Composition: `AttendanceReport` owns detailed `ReportLine` statistics with `orphanRemoval = true`.
- Qualified association: `Room [MeetingSlot] -> RoomBooking`, with one booking per room and exact slot.
- Flexible administrator reports with optional filters and CSV export.
- Report conclusion messages with overall attendance performance percentage.
- Main JavaFX page with a public weekly schedule and class meeting details dialog.
- Class meeting comments and per-student attendance comments.
- Login/password support through `AuthenticationService`, `passwordHash`, and personal settings email/notification services.
- Scheduled notification tasks processed by `SystemScheduler`.
- Fixed weekly schedules through `WeeklyScheduleEntry` and generated concrete `ClassMeeting` occurrences.
- Two inheritance mappings:
  - `Person` / `Student` / `Teacher` / `Administrator`: `JOINED`.
  - `Notification` / `EmailNotification` / `SystemNotification`: `SINGLE_TABLE`.

Main actors used by the implementation and documentation:

- `Student`
- `Teacher`
- `Administrator`
- `SystemScheduler`

## Build and run

Run the console smoke demo with:

```powershell
mvn clean compile exec:java
```

Run the JavaFX GUI with the standard JavaFX Maven plugin goal:

```powershell
mvn clean javafx:run
```

If global Maven is not installed, use the included wrapper:

```powershell
.\mvnw.cmd javafx:run
```

The Maven wrapper is included, so Maven does not need to be installed globally. In this environment, global `mvn` was not available, so verification used `.\mvnw.cmd`.

Note: `mvn clean javafx` is not a standard Maven lifecycle phase. Use `mvn clean javafx:run`.

## Troubleshooting

- If Hibernate reports schema or column errors after model changes, delete old local database files such as `mp4_mas_db.mv.db` and run again.
- Use JDK 21 or a project-compatible JDK. The project compiles with `maven.compiler.release=17`.
- In IntelliJ IDEA, reload the Maven project after changing `pom.xml`.
- If JavaFX does not start from IntelliJ, run `mvn clean javafx:run` in the terminal or set `app.AttendanceGuiApp` as the JavaFX entry point.

The project recreates the local H2 schema on startup with `hibernate.hbm2ddl.auto=create`. This avoids stale schema errors after the final model update and keeps the defence/demo database deterministic.
