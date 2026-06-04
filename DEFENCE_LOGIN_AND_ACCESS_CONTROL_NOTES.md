# Defence Login And Access Control Notes

## Demo Accounts

- Student: `anna.nowak@student.pja.edu.pl` / `student`
- Teacher: `piotr.kowalski@pja.edu.pl` / `teacher`
- Administrator: `admin@pja.edu.pl` / `admin`

Passwords are stored as SHA-256 hashes through `PasswordService`. The visible passwords above are demo credentials for defence.

## Role Navigation

All logged-in users can see:

- Public Weekly Schedule
- Personal Settings
- My Notifications
- My History
- Logout

Student-only pages:

- My Schedule
- My Attendance
- My Group

Teacher-only pages:

- My Class Meetings
- Create Class Meeting

Administrator-only pages:

- Manage Class Meetings
- Manage Weekly Schedules
- Manage Users
- Manage Notifications
- Generate Reports
- Export Reports
- Manage Rooms

Unauthorized functions are not shown in the left navigation for that role.

## Blocked Access Examples

- A student can see every public schedule row, but can open full details only for meetings assigned to the student's group.
- A teacher can open full details only for meetings assigned to that teacher.
- If a user clicks a meeting that does not involve them, the GUI shows: `Only public schedule information is available for this class meeting.`
- Students never see attendance editing buttons.
- Teachers cannot mark attendance for cancelled meetings, meetings assigned to another teacher, or meetings that have not started yet.

## Public Schedule vs Personal Schedule

The Public Weekly Schedule shows all current-week class meetings for all logged-in users. It contains public information: subject, class type, group, teacher, teacher email, date, time, meeting mode, location/link, and status.

The student My Schedule page is personal. It is derived through `Student -> StudentGroup -> ClassMeeting` and shows only class meetings involving the student's group.

The teacher My Class Meetings page is personal. It is derived through `Teacher -> ClassMeeting` and shows only class meetings assigned to the logged-in teacher.

## Teacher Attendance Restrictions

Teacher attendance marking is validated in `ClassMeetingService.validateCanMarkAttendance(...)`.

The teacher must:

- be assigned to the class meeting
- wait until the class meeting start time has passed
- use a meeting that is not cancelled

Required messages:

- `You are not assigned to this class meeting.`
- `Attendance cannot be marked before the class meeting starts.`
- `Cannot mark attendance for a cancelled class meeting.`

## Administrator Reporting And Export

The administrator Generate Reports page supports optional filters for semesters, teacher, subject, group, class type, and date range.

If a filter is empty, it is ignored. A null class type means all class types are included.

After report generation, the GUI displays a conclusion message based on `AttendanceReport.overallPerformancePercentage`, for example:

`Selected parameters have an attendance performance of 81.30%.`

CSV export is available through Export CSV after a report is generated. The export includes metadata, selected filters, overall performance, conclusion, and report lines.

## Email Uniqueness

`Person.emails` is an `@ElementCollection` with a unique database constraint on `person_emails.email`.

`PersonRepository.emailExistsForAnotherPerson(email, personId)` checks duplicates case-insensitively and allows a person to keep their own email during update.

`PersonalSettingsService` enforces:

- each person must have 1 to 3 emails
- email format must be valid
- email must be globally unique

If an email is already used, the GUI displays:

`Email is already used by another person: <email>`
