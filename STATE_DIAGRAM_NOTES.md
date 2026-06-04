# State Diagram Notes

## Files

Class meeting lifecycle:

- `STATE_CLASS_MEETING.puml`
- `STATE_CLASS_MEETING.svg`

Scheduled notification task lifecycle:

- `STATE_NOTIFICATION_TASK.puml`
- `STATE_NOTIFICATION_TASK.svg`

Notification lifecycle:

- `STATE_NOTIFICATION.puml`
- `STATE_NOTIFICATION.svg`

Room booking lifecycle:

- `STATE_ROOM_BOOKING.puml`
- `STATE_ROOM_BOOKING.svg`

Attendance report generation/export lifecycle:

- `STATE_ATTENDANCE_REPORT.puml`
- `STATE_ATTENDANCE_REPORT.svg`

## Recommended Defence Diagram

Use `STATE_CLASS_MEETING.svg` as the main state diagram.

It is the strongest one because `ClassMeeting` has a clear status enum:

- `DRAFT`
- `SCHEDULED`
- `CANCELLED`
- `COMPLETED`

It also supports important business rules:

- draft meetings can have incomplete details
- scheduled meetings are visible in schedules
- cancelled meetings are not deleted
- attendance cannot be marked for cancelled meetings
- completed meetings keep history and attendance records

## Additional Useful State Diagrams

`STATE_NOTIFICATION_TASK.svg` is useful if you want to show the `SystemScheduler` logic.

`STATE_ROOM_BOOKING.svg` is useful if you want to connect the state diagram with the qualified association:

`Room [MeetingSlot] -> RoomBooking`

`STATE_ATTENDANCE_REPORT.svg` is useful for explaining admin report generation and CSV export.

## Defence Sentences

Class meeting:

> A class meeting is not deleted when cancelled. It moves to the `CANCELLED` state, which preserves schedule history and prevents attendance marking.

Notification task:

> Scheduled notification tasks are persistent. The scheduler moves them from `PENDING` to `PROCESSING`, then either `SENT` or `FAILED`.

Room booking:

> A room booking can be created only if the room is available for the selected `MeetingSlot`.

Attendance report:

> A report is first generated from selected filters. Export is possible only when the generated report has report lines.
