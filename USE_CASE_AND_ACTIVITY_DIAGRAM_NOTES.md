# Use Case And Activity Diagram Notes

## Files

Use case diagram:

- `USE_CASE_DIAGRAM.puml`
- `USE_CASE_DIAGRAM.svg`

Activity diagrams:

- `ACTIVITY_LOGIN_AND_ACCESS_CONTROL.puml`
- `ACTIVITY_LOGIN_AND_ACCESS_CONTROL.svg`
- `ACTIVITY_OPEN_CLASS_MEETING_DETAILS.puml`
- `ACTIVITY_OPEN_CLASS_MEETING_DETAILS.svg`
- `ACTIVITY_MARK_ATTENDANCE.puml`
- `ACTIVITY_MARK_ATTENDANCE.svg`
- `ACTIVITY_GENERATE_AND_EXPORT_REPORT.puml`
- `ACTIVITY_GENERATE_AND_EXPORT_REPORT.svg`
- `ACTIVITY_WEEKLY_SCHEDULE_GENERATION.puml`
- `ACTIVITY_WEEKLY_SCHEDULE_GENERATION.svg`

## Use Case Diagram Defence Explanation

The use case diagram shows four actors:

- `Student`
- `Teacher`
- `Administrator`
- `SystemScheduler`

The human actors must log in before accessing personal functionality.

Common use cases are available to all logged-in users:

- view public weekly schedule
- open personal settings
- manage own emails
- change password
- view notifications
- view personal history

Role-specific functions are separated:

- Student can view own schedule, attendance, and group.
- Teacher can view assigned meetings, create class meetings, comment meetings, and mark attendance.
- Administrator can manage class meetings, weekly schedules, users, notifications, reports, rooms, and bookings.
- SystemScheduler processes automatic notification tasks.

Important use case relationship:

`Export attendance report` extends `Generate attendance report`, because export is possible only after a report exists.

## Activity Diagram Defence Explanation

### Login And Access Control

Shows that the application starts with login, validates email and password, stores the logged-in person in session context, and displays navigation based on role.

### Open Class Meeting Details

Shows public schedule access and private detail access:

- Student opens details only for own group meetings.
- Teacher opens details only for assigned meetings.
- Administrator opens all meetings.

### Mark Attendance

Shows the required restrictions:

- cancelled meeting cannot receive attendance
- teacher must be assigned to the meeting
- teacher cannot mark attendance before the meeting starts
- attendance statuses are required
- duplicate attendance is blocked

### Generate And Export Report

Shows flexible report filters, report line calculation, overall performance, conclusion message, and CSV export validation.

### Weekly Schedule Generation

Shows how an administrator creates a `WeeklyScheduleEntry`, validates teacher qualification and room availability, then generates concrete `ClassMeeting` objects for the semester.

## Recommended Thesis Set

For the final document, use:

1. `USE_CASE_DIAGRAM.svg`
2. `ACTIVITY_LOGIN_AND_ACCESS_CONTROL.svg`
3. `ACTIVITY_MARK_ATTENDANCE.svg`
4. `ACTIVITY_GENERATE_AND_EXPORT_REPORT.svg`
5. `ACTIVITY_WEEKLY_SCHEDULE_GENERATION.svg`

`ACTIVITY_OPEN_CLASS_MEETING_DETAILS.svg` is useful if you want to emphasize public/private schedule access.
