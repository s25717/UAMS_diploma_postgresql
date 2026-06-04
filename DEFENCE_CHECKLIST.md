# Defence Checklist

## Demo Accounts

- Student: `anna.nowak@student.pja.edu.pl` / `student`
- Teacher: `piotr.kowalski@pja.edu.pl` / `teacher`
- Administrator: `admin@pja.edu.pl` / `admin`

## How To Run

Use:

```powershell
mvn clean javafx:run
```

If global Maven is unavailable:

```powershell
.\mvnw.cmd javafx:run
```

## MAS GUI Requirement 4.2.4

MAS 4.2.4 is satisfied by `GroupStudentsAssociationView`.

- Left side: `ListView<StudentGroup>`
- Right side: `TableView<Student>`
- Association: `StudentGroup 1 -> 0..* Student`
- Students are retrieved through `StudentGroup.getStudents()`
- The selection change does not manually filter all students and does not call `findStudentsByGroupId`

## Role-Based Access Examples

- Student sees public schedule, personal settings, notifications, history, my schedule, my attendance, and my group.
- Teacher sees public schedule, personal settings, notifications, history, group-student association demo, assigned class meetings, and class meeting creation.
- Administrator sees management screens for class meetings, weekly schedules, users, notifications, reports, export, rooms, and the association demo.
- If a student or teacher opens a public meeting that does not involve them, the GUI displays: `Only public schedule information is available for this class meeting.`

## Teacher Attendance Restrictions

Teacher attendance marking is allowed only when:

- the teacher is assigned to the meeting
- the meeting is not cancelled
- the meeting start time has already passed

Expected blocked messages:

- `You are not assigned to this class meeting.`
- `Attendance cannot be marked before the class meeting starts.`
- `Cannot mark attendance for a cancelled class meeting.`

## Admin Reports And Export

1. Log in as administrator.
2. Open Generate Reports.
3. Choose any combination of semester, teacher, subject, group, class type, and date range.
4. Click Generate Report.
5. Confirm the result table and conclusion message.
6. Click Export CSV.

CSV export includes metadata, selected filters, overall performance, conclusion, student number, student name, total meetings, present, late, excused, absent, and attendance percentage.

## Weekly Schedule Generation

1. Log in as administrator.
2. Open Manage Weekly Schedules.
3. Select group, semester, subject, teacher, class type, mode, day, time, and room/link.
4. Save the weekly schedule entry.
5. Select the entry and generate class meetings.
6. Generated meetings reference `WeeklyScheduleEntry` and become visible in public and personal schedules.

## Room Booking Qualified Association

The qualified association is:

`Room [MeetingSlot] -> RoomBooking`

`MeetingSlot` contains date, start time, and end time. A room can have many bookings, but only one booking for one exact slot. Availability is checked through `Room.isAvailable(MeetingSlot)` and the unique database constraint on room/date/start/end.

## Notification Scheduling

Notifications are addressed to `Person`, not only `Student`. `ScheduledNotificationTask` is processed by `SystemScheduler`, which creates concrete notifications through `NotificationService`.

Admin notification management can create notifications, edit DRAFT/PENDING/FAILED notifications, and cancel DRAFT/PENDING notifications. SENT notifications cannot be edited.

## Known Limitations

- `ClassMeeting.room` is retained only as a deprecated compatibility display field. New logic uses `location` for display and `RoomBooking` for structured room reservation.
- The standard JavaFX Maven command is `mvn clean javafx:run`; `mvn clean javafx` is not a valid Maven lifecycle phase.

## Final Verification Items

- Project compiles with Maven wrapper.
- Login GUI is first screen.
- Student, Teacher, and Administrator demo accounts authenticate.
- Role-based navigation is visible.
- `GroupStudentsAssociationView` demonstrates MAS 4.2.4.
- Manage Weekly Schedules is implemented, not a placeholder.
- Admin can create weekly schedule entries and generate class meetings.
- Admin notification edit blocks SENT notifications.
- Manage Rooms shows rooms and bookings.
- Room availability uses `MeetingSlot`.
- Primary email display is deterministic.
- CSV export handles target paths without parent directories.
- Weekly schedules are visible to normal users.
- No Spring is used.
- Important write operations use transactions and rollback.
