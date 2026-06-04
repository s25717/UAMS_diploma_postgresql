# Final Project Implementation Notes

## Actors

The current business logic uses these actors:

- `Student`
- `Teacher`
- `Administrator`
- `SystemScheduler`

Administrative reporting responsibilities belong to `Administrator`. `Dean Office Employee` is not modeled as a separate actor.

## Implemented selected use cases

The GUI implements the public weekly schedule, class meeting details, attendance registration, attendance comments, and administrator attendance reporting.

## GUI requirement 4.2.4

The JavaFX window contains two widgets displaying multiple objects connected by associations:

1. `ListView<ClassMeeting>` displays class meetings.
2. `TableView<AttendanceRow>` displays students belonging to the selected class meeting.

The students are not loaded by manual filtering. They are retrieved through the predefined association path:

`ClassMeeting -> StudentGroup -> Set<Student>`

The repository method used for this is:

`ClassMeetingRepository.findByIdWithGroupAndStudents(Long id)`

This method uses JPQL `join fetch` to load the class meeting, its group, and the students within one query.

## Updated business logic

- Emails are globally unique across all persons. `Person.emails` has a unique constraint on `person_emails.email`, and `PersonService` checks case-insensitively before saving or updating.
- Teacher specialization is modeled as `Teacher.qualifiedSubjects` and `Subject.qualifiedTeachers`. A teacher must have 1 to 5 qualified subjects.
- `ClassMeeting` validates that the assigned teacher is qualified for the selected subject.
- `ClassMeeting` stores meeting mode, status, date, location or online link, and a general comment.
- Cancelled class meetings are kept for history and cannot receive attendance.
- `Attendance` stores an optional individual comment for the student's attendance row.
- Administrator reports use `AttendanceReportFilter`; null or empty filters mean unrestricted criteria.
- `AttendanceReport` stores selected filters, report type, date range, class type, semesters, teacher, subject, and group.
- `AttendanceReport` stores `overallPerformancePercentage` and generates a conclusion message for the selected report scope.
- `ReportLine` stores total meetings and detailed PRESENT/LATE/EXCUSED/ABSENT counts. Attendance percentage is `(present + late) / totalMeetings * 100`.
- `ReportExportService` exports generated reports, overall performance, and conclusion messages to CSV, and prevents exporting empty or missing reports.
- `Room` and `RoomBooking` implement a qualified association by `MeetingSlot`, preventing duplicate room bookings for the same room/date/time slot.
- `ScheduledNotificationTask` and `SystemScheduler` provide business triggers for automatic notifications.
- Notifications and scheduled notification tasks are addressed to `Person`, so students, teachers, and administrators can receive messages.
- `AuthenticationService` supports login by email and password hash. `PersonService` supports password changes after old-password verification.
- `PersonalSettingsService` supports adding, editing, and deleting emails while keeping 1 to 3 globally unique emails.
- `WeeklyScheduleEntry` represents a fixed recurring group schedule during a semester, while `ClassMeeting` represents a generated concrete occurrence.
- History views are supported through repository paths, without adding direct `Person -> ClassMeeting` associations.

## Public weekly schedule and details

After login, the first functional page is the public weekly schedule. It lists current-week class meetings with subject, class type, group, teacher, teacher email, date, time, mode, location/link, and status.

The class meeting details behavior is implemented as a JavaFX details dialog. It displays the class meeting metadata, general comment, and attendance rows. Admin/teacher workflows can save the general class comment and save attendance statuses with individual comments. `ScheduleAccessService` contains the privacy rule for opening full details: administrators can open all meetings, teachers can open assigned meetings, and students can open meetings for their group.

## Admin report GUI

The `Admin Reports` tab lets an administrator select:

- semesters as a multi-select list
- teacher, subject, and group through combo boxes
- class type as `All`, `Lecture`, or `Tutorial`
- optional date range

The result table shows:

`Student | Total Meetings | Present | Late | Excused | Absent | Attendance %`

CSV export is available after a report has been generated.

After generation, the GUI also displays a conclusion such as:

`Selected parameters have an attendance performance of 81.30%.`

The value is the average of all `ReportLine.attendancePercentage` values.

## Login and personal settings

Human actors inherit from `Person` and use the shared personal data model. `AuthenticationService.login(email, password)` checks a registered email against `Person.passwordHash`.

Personal settings behavior is implemented in services:

- view and change own emails through `PersonalSettingsService`
- keep at least one email and at most three emails
- enforce global case-insensitive email uniqueness
- change password through `PersonService.changePassword(...)`
- view notifications through `NotificationRepository.findByRecipientId(...)`

The default demo password hash corresponds to `Password123!`.

## Fixed weekly schedule

`WeeklyScheduleEntry` stores the recurring weekly template for a group, subject, teacher, semester, day, time, mode, and optional room/link. `ScheduleGenerationService.generateClassMeetingsForSemester(...)` creates concrete `ClassMeeting` rows between semester start and end dates, validates teacher qualification, checks room conflicts, and links generated meetings back to the schedule entry.

## Use case diagram updates

Student:
- View class meeting history
- View attendance history
- View room booking history

Teacher:
- View conducted class meeting history
- View room booking history
- View assigned upcoming class meetings
- View assigned past class meetings
- Create class meeting
- Add or edit class meeting comment
- Register attendance
- Edit attendance
- Edit attendance comments
- Generate report

Administrator:
- Generate attendance report
- Export attendance report
- View all class meeting history
- View all room booking history
- Create class meeting
- Edit class meeting
- Cancel class meeting
- Create weekly schedule entry
- Generate class meetings from weekly schedule
- Manage teacher specializations
- Manage rooms
- Manage bookings
- Manage notifications

SystemScheduler:
- Process pending notification tasks
- Create class meeting reminder tasks
- Create low attendance warnings
- Create report ready notification
- Send automatic notifications

## Defence explanations

Teacher specialization: `Teacher` and `Subject` have a many-to-many association representing what a teacher is qualified to teach. This is separate from `ClassMeeting`, which represents a concrete lesson. The system validates that a teacher can only be assigned to a meeting for a qualified subject.

Flexible reports: `AttendanceReportFilter` uses optional filters. Null or empty values are ignored, so reports can be generated by semester, teacher, subject, group, class type, date range, or combined criteria. Null class type includes all class types.

Report performance: `AttendanceReport.overallPerformancePercentage` is the average attendance percentage across all report lines. The report conclusion is generated dynamically from the selected filter scope.

Qualified association: `Room` maps bookings by `MeetingSlot`. The unique database constraint and service checks guarantee that one room cannot be booked twice for the same exact date/start/end slot.

Notifications: `SystemScheduler` processes persistent `ScheduledNotificationTask` records and delegates concrete notification creation to `NotificationService`.

History views: student and teacher history is derived through existing associations such as `Student -> StudentGroup -> ClassMeeting`, `Student -> Attendance -> ClassMeeting`, and `Teacher -> ClassMeeting -> RoomBooking`.

Fixed weekly schedule: `WeeklyScheduleEntry` is the recurring plan for a group during a semester. `ClassMeeting` is the concrete scheduled occurrence, so individual meetings can later be cancelled, commented, completed, or modified without changing the whole template.

## Persistence

Data is persisted using JPA/Hibernate and the H2 relational database configured in `persistence.xml`.
The schema uses `hibernate.hbm2ddl.auto=create`, because the clarified final model changes report, room booking, class meeting, and notification schemas substantially. This keeps the demo database deterministic.

## Sample data

`SampleDataService.seedIfDatabaseIsEmpty()` creates sample university data only when the database has no class meetings. The sample data includes:

- Field of study
- Semester
- Student group
- Four students
- Teacher
- Subject
- Two class meetings
- Public weekly schedule data
- Teacher qualified subject assignment
- Rooms and room bookings
- Existing attendance record
- Attendance report with detailed report lines
- Notifications and a scheduled notification task

## Methods used for selected use case

- `AttendanceRegistrationService.getClassMeetings()`
- `AttendanceRegistrationService.getClassMeetingWithStudents(Long classMeetingId)`
- `AttendanceRegistrationService.getSavedStatuses(Long classMeetingId)`
- `AttendanceRegistrationService.registerAttendance(Long classMeetingId, Map<Long, AttendanceStatus> statusesByStudentId)`

## Transaction correctness

Attendance saving is implemented in one explicit transaction in `AttendanceRegistrationService.registerAttendance`.
The method uses the required structure:

- begin transaction
- execute business operation
- commit transaction
- rollback if exception occurs
- close EntityManager in finally block

## How to run

Use:

`.\mvnw.cmd javafx:run`

The previous console MP4 demonstration is still available through `Main`.
