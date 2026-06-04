# Class Diagram Defence Notes

This project has two class diagrams:

- `ANALYTICAL_CLASS_DIAGRAM.puml`
- `DESIGN_CLASS_DIAGRAM.puml`

For defence slides or thesis screenshots, use the compact versions first:

- `ANALYTICAL_CLASS_DIAGRAM_COMPACT.puml`
- `ANALYTICAL_REPORTING_ROOMS_NOTIFICATIONS.puml`
- `DESIGN_CLASS_DIAGRAM_COMPACT.puml`

The original full diagrams are useful as backup/reference, but they may be too large for PlantUML preview panes or printed pages.

## Analytical Class Diagram

The analytical diagram shows the business model of the University Academic Management System.

It focuses on domain concepts, associations, multiplicities, inheritance, and MAS requirements.

Main points to explain:

### Person Inheritance

`Person` is an abstract superclass.

The implemented human actors inherit from it:

- `Student`
- `Teacher`
- `Administrator`

This matches the login and role-based navigation logic.

### Multi-Value Attribute

`Person.emails` is a multi-value attribute.

In the ERD it is represented as `person_emails`.

In the class diagram it is represented as:

`Person 1 -- 1..3 PersonEmail`

The system also stores `primaryEmail` to display a deterministic contact email.

### MAS Requirement 4.2.4

The mandatory association with target multiplicity many is:

`StudentGroup 1 -> 0..* Student`

This is demonstrated in the GUI by `GroupStudentsAssociationView`.

The screen has:

- `ListView` of groups
- `TableView` of students
- students loaded through `StudentGroup.getStudents()`

This proves that the predefined association is used, not manual filtering.

### Teacher Specialization

The many-to-many association between `Teacher` and `Subject` represents qualification.

A teacher can be qualified for 1 to 5 subjects.

This is different from `ClassMeeting`:

- `Teacher -- Subject` means what the teacher can teach.
- `Teacher -- ClassMeeting` means who conducts a concrete lesson.

### Weekly Schedule And Class Meeting

`WeeklyScheduleEntry` is a recurring template.

`ClassMeeting` is a concrete generated occurrence.

This separation is important because a single generated meeting can later be cancelled, completed, commented, or used for attendance.

### Attendance

`Attendance` is an association class between:

- `Student`
- `ClassMeeting`

It stores additional information:

- status
- optional comment

There is one attendance record per student per class meeting.

### Reports

`AttendanceReport` stores report metadata and selected filters.

`ReportLine` stores detailed student statistics:

- total meetings
- present
- late
- excused
- absent
- attendance percentage

The report also stores `overallPerformancePercentage`.

### Qualified Association

The qualified association is:

`Room [MeetingSlot] -> RoomBooking`

`MeetingSlot` consists of:

- date
- start time
- end time

This means one room can have many bookings, but for one exact meeting slot it can have at most one booking.

### Notifications

Notifications are sent to `Person`, not only to `Student`.

This allows:

- students to receive attendance or reminder messages
- teachers to receive class meeting changes
- administrators to receive system/report messages

`ScheduledNotificationTask` gives notifications a business trigger and is processed by `SystemScheduler`.

## Design Class Diagram

The design diagram shows how the system is implemented in Java.

It includes:

- JavaFX GUI class
- services
- repositories
- JPA utility
- domain model classes

### GUI Layer

The main GUI class is:

`AttendanceGuiApp`

It builds:

- login view
- main layout
- public weekly schedule
- personal settings
- group-students association screen
- admin weekly schedule screen
- admin notification screen
- room management screen
- report screen

Some screens are represented as design-level view classes in the diagram. In the current implementation they are built by methods inside `AttendanceGuiApp`.

### Service Layer

Services hold business logic:

- `AuthenticationService` handles login/logout and role checks.
- `PersonalSettingsService` handles emails and password changes.
- `ScheduleAccessService` checks whether a user may open class meeting details.
- `ClassMeetingService` validates class meeting operations.
- `AttendanceRegistrationService` saves attendance.
- `AttendanceReportService` generates reports.
- `ReportExportService` exports reports to CSV.
- `ScheduleGenerationService` creates class meetings from weekly schedule entries.
- `RoomBookingService` checks and creates room bookings.
- `SystemScheduler` processes scheduled notification tasks.

### Persistence Layer

Repositories handle JPA/Hibernate access manually.

There is no Spring.

`GenericRepository<T>` provides common CRUD behavior.

Specific repositories add fetch queries needed by the GUI, for example:

- `StudentGroupRepository.findAllWithStudents()`
- `ClassMeetingRepository.findClassMeetingsForCurrentWeek()`
- `WeeklyScheduleEntryRepository.findAllWithDetails()`
- `RoomRepository.findAllWithBookings()`
- `NotificationRepository.findAllWithRecipients()`

### Important Defence Sentence

The analytical diagram explains the business domain and MAS relationships, while the design diagram explains how the same domain is implemented using JavaFX views, service classes, repositories, and JPA/Hibernate persistence.

## How To Render

Open either `.puml` file in:

- IntelliJ PlantUML plugin
- Visual Studio Code PlantUML extension
- any local PlantUML renderer
- an online PlantUML renderer

Export each diagram as PNG or PDF for the final thesis/defence materials.

Recommended final set:

1. Use `ANALYTICAL_CLASS_DIAGRAM_COMPACT.svg` as the main analytical diagram.
2. Use `ANALYTICAL_REPORTING_ROOMS_NOTIFICATIONS.svg` as a second analytical detail diagram.
3. Use `DESIGN_CLASS_DIAGRAM_COMPACT.svg` as the design diagram.
