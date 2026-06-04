# MP4 Requirements Checklist

This project is a Java/JPA/Hibernate mini-project without Spring Framework or Spring Data JPA repositories.

## 1. Classes and attributes

### Entity class with primary key
All entity classes contain:
- `@Entity`
- `@Id`
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- full getters and setters for all fields

Entity classes:
- `Person` (abstract)
- `Student`
- `Teacher`
- `Administrator`
- `Notification` (abstract)
- `EmailNotification`
- `SystemNotification`
- `Field`
- `Semester`
- `StudentGroup`
- `Subject`
- `ClassMeeting`
- `Attendance`
- `AttendanceReport`
- `ReportLine`
- `Room`
- `RoomBooking`
- `ScheduledNotificationTask`
- `WeeklyScheduleEntry`

### Repository for every entity class
Repository classes are in `src/main/java/persistence`.
Every entity has a repository component with basic operations inherited from `GenericRepository`:
- `findAll()`
- `findById(Long id)`
- `save(T entity)`
- `update(T entity)`
- `delete(Long id)`

### Validation before writing to database
Implemented in `GenericRepository.validate(...)`, called before `save(...)` and `update(...)`.
Bean Validation is also enabled in `persistence.xml` using:
- `jakarta.persistence.validation.mode=AUTO`

Validation examples:
- `@NotBlank`: `Person.name`, `Person.surname`, `Subject.name`, `StudentGroup.code`
- `@Size`: `Person.name`, `Person.surname`, `Person.emails`
- `@Email`: values inside `Person.emails`
- `@Pattern`: `Student.studentNumber`
- `@Min`: `Notification.priority`, `SystemNotification.displaySeconds`
- `@Max`: `Semester.number`
- `@Past`: `BirthDate.value`
- `@DecimalMin` and `@DecimalMax`: `ReportLine.attendancePercentage`
- `@Valid`: embedded attributes and composed/dependent object collections

### Multi-value attribute
`Person.emails`:
- `@ElementCollection`
- `@CollectionTable`
- `@UniqueConstraint(columnNames = "email")`
- `@Size(min = 1, max = 3)`
- element validation: `Set<@Email @NotBlank String>`
- case-insensitive service validation with `emailExistsForAnotherPerson(String email, Long personId)`

### Complex attributes
Embeddable classes:
- `BirthDate` with `@Embeddable`
- `MeetingTime` with `@Embeddable`
- `MeetingSlot` with `@Embeddable`

Embedded usages:
- `Person.birthDate` uses `@Embedded` and `@Valid`
- `ClassMeeting.time` uses `@Embedded` and `@Valid`
- `RoomBooking.meetingSlot` uses `@Embedded` and `@Valid`

### Enum attributes
Enum examples:
- `Attendance.status` uses `@Enumerated(EnumType.STRING)` with `AttendanceStatus`
- `ClassMeeting.classType` uses `@Enumerated(EnumType.STRING)` with `ClassType`
- `AttendanceReport.reportType` uses `ReportType`
- `ClassMeeting.meetingMode` uses `MeetingMode`
- `ClassMeeting.status` uses `ClassMeetingStatus`
- `Notification.status` uses `NotificationStatus`
- `RoomBooking.bookingStatus` uses `BookingStatus`
- `ScheduledNotificationTask.status` and `taskType` use notification task enums
- `MeetingTime.dayOfWeek` uses `@Enumerated(EnumType.STRING)` with `DayOfWeek`

### Optional parameter / nullable value
`ClassMeeting.onlineMeetingLink` is optional:
- no `@NotBlank`
- demonstrated in `Main` by passing `null`

`ClassMeeting.location` and the legacy `room` display field are also nullable for draft and online meetings. Final scheduled classroom meetings are validated by `ClassMeetingService`.

## 2. Associations

### 1-* association with mappedBy
`StudentGroup` 1-* `Student`:
- `StudentGroup.students` uses `@OneToMany(mappedBy = "group")`
- `Student.group` uses `@ManyToOne(fetch = FetchType.LAZY)`
- collection type is `Set<Student>`

### *-* association without intermediate class
`Subject` *-* `StudentGroup`:
- `Subject.groups` uses `@ManyToMany`
- `StudentGroup.subjects` uses `@ManyToMany(mappedBy = "groups")`
- collection type is `Set`
- no artificial middle entity is used

`Teacher` *-* `Subject` qualification association:
- `Teacher.qualifiedSubjects` uses `@ManyToMany`
- `Subject.qualifiedTeachers` uses `@ManyToMany(mappedBy = "qualifiedSubjects")`
- `@Size(min = 1, max = 5)` restricts teacher specialization count
- class meeting creation validates that the teacher is qualified for the selected subject

### Qualified association
`Room [MeetingSlot] -> RoomBooking`:
- `Room.bookingsBySlot` uses `@OneToMany`, `Map<MeetingSlot, RoomBooking>`, and `@MapKey(name = "meetingSlot")`
- `RoomBooking` has a unique constraint on `room_id`, `date`, `start_time`, and `end_time`
- `Room.isAvailable(MeetingSlot)` and booking services prevent conflicts

### Creating and deleting associations using database
Implemented in `Main` and `StudentGroupRepository.removeStudentFromGroup(...)`.
The method opens a transaction, fetches the group with students, removes the student association, commits, and rolls back on error.

### Fetching object with dependencies in one query
`ClassMeetingRepository.findByIdWithAttendance(Long id)`:
- uses JPQL `join fetch`
- fetches class meeting, attendance records, and students in one query

`StudentGroupRepository.findByIdWithStudents(Long id)`:
- uses JPQL `join fetch`
- fetches group and students in one query

### Fetching only related objects for a given id
`StudentGroupRepository.findStudentsByGroupId(Long groupId)`:
- fetches only students related to a given group id
- uses one JPQL query

`ClassMeetingRepository.findAttendanceByMeetingId(Long meetingId)`:
- fetches only attendance records related to a given class meeting id
- uses one JPQL query

### Association with attribute
`Attendance` is an association class between `Student` and `ClassMeeting`:
- `Attendance.student` is required: `@ManyToOne(optional = false)` and `@NotNull`
- `Attendance.classMeeting` is required: `@ManyToOne(optional = false)` and `@NotNull`
- `Attendance.status` is an attribute of the association
- uniqueness is enforced with `@Table(uniqueConstraints = ...)`
- `ClassMeeting.attendances` uses `cascade = CascadeType.ALL` and `orphanRemoval = true`
- deletion demonstrated in `ClassMeetingRepository.removeAttendanceFromMeeting(...)`

### Composition
`AttendanceReport` owns `ReportLine` objects:
- `AttendanceReport.reportLines` uses `@OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)`
- deleting or unlinking a `ReportLine` from the report removes it from the database
- `ReportLine.report` uses `@JoinColumn(name = "report_id", nullable = false, updatable = false)` so the owner cannot be changed after persistence
- report lines include total meetings plus present, late, excused, and absent counts

### Flexible reports and export
- `AttendanceReportFilter` supports optional semester, teacher, subject, group, class type, and date filters
- `AttendanceReportService.generateReport(...)` treats null or empty filters as unrestricted
- missing attendance rows count as `ABSENT` during report generation
- `ReportExportService.exportToCsv(...)` exports metadata and report lines

### Scheduler and notifications
- `ScheduledNotificationTask` stores pending notification work
- `SystemScheduler` processes tasks and creates concrete notification subtypes through `NotificationService`
- task types include reminders, low attendance warnings, attendance changes, and report-ready notifications
- notification recipients are `Person`, allowing students, teachers, and administrators to receive notifications

### Fixed weekly schedule
- `WeeklyScheduleEntry` represents the recurring semester template for a group
- `ScheduleGenerationService.generateClassMeetingsForSemester(...)` creates concrete `ClassMeeting` occurrences
- generated meetings reference their source `WeeklyScheduleEntry`
- teacher qualification and room availability are checked before generation

### Login and personal settings
- `Person.passwordHash` stores hashed passwords, not plain text
- `AuthenticationService.login(...)` authenticates by any registered email
- `PersonService.changePassword(...)` verifies the old password and validates the new password
- `PersonalSettingsService` supports add/edit/delete email operations while preserving 1 to 3 globally unique emails

### Public schedule and detail permissions
- `ClassMeetingRepository.findClassMeetingsForCurrentWeek()` fetches public schedule rows
- `ScheduleAccessService` enforces full-detail access by role: administrator all, teacher assigned meetings, student own group meetings

## 3. Inheritance

### Inheritance example 1: Person hierarchy
Classes:
- `Person`
- `Student`
- `Teacher`
- `Administrator`

Mapping strategy:
- `@Inheritance(strategy = InheritanceType.JOINED)`

Justification:
The person hierarchy has shared personal attributes and subtype-specific fields. The joined strategy avoids nullable subtype columns in one large table and keeps subtype data normalized.

### Inheritance example 2: Notification hierarchy
Classes:
- `Notification`
- `EmailNotification`
- `SystemNotification`

Mapping strategy:
- `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)`
- `@DiscriminatorColumn(name = "notification_type")`

Justification:
Notifications have a small number of subtype-specific attributes. The single-table strategy reduces joins and is efficient for polymorphic notification queries.

## 4. Transactions

All repository write operations use clean transactions in `GenericRepository.executeInTransaction(...)`:
- `EntityManager` is created for each operation
- transaction begins before write operation
- transaction commits on success
- transaction rolls back on exception
- `EntityManager` closes in `finally`

The sample data method in `Main` also uses the same clean transaction pattern with rollback and `EntityManager.close()`.

## 5. Query logging

`persistence.xml` enables SQL logging:
- `hibernate.show_sql=true`
- `hibernate.format_sql=true`
- `hibernate.highlight_sql=true`
- `hibernate.use_sql_comments=true`

This allows redundant or suboptimal ORM queries to be observed during defence.
