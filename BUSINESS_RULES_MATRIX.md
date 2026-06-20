# Business Rules Implementation Matrix

This matrix maps the business rules backlog to the current implementation.

Status legend:

- **Implemented**: enforced by GUI/service and/or PostgreSQL.
- **Partially implemented**: present, but not fully enforced or not at the strongest layer.
- **Missing**: not implemented yet.
- **Design/documentation**: primarily a modeling or thesis explanation rule.

## Login, Access Control, And Users

| Rule | Status | Enforcement layer | Evidence / notes |
|---|---|---|---|
| BR-01: user must log in before personal functionality | Implemented | GUI/session | `AttendanceGuiApp.showLoginView`, `AuthenticationService.login`, role navigation is built only after login. |
| BR-02: role determines available functions | Implemented | GUI/service | `AttendanceGuiApp.buildNavigation`, `AuthenticationService.isStudent/isTeacher/isAdmin`. |
| BR-03: public schedule visible to logged-in users | Implemented | GUI | Public schedule is default page after login. |
| BR-04: class meeting details restricted | Implemented | Service/GUI | `ScheduleAccessService.assertCanOpenFullDetails`. |
| BR-05: email globally unique | Implemented | PostgreSQL/service | `person_emails.email` unique in `V1__create_university_schema.sql`; checked in `PersonRepository.emailExistsForAnotherPerson`. |
| BR-06: person must have 1 to 3 emails | Implemented | PostgreSQL/Java/service | `@Size(min = 1, max = 3)`, settings service checks, and deferred PostgreSQL triggers enforce collection cardinality. |
| BR-07: password change requires old password | Implemented | Service | `PersonService.changePassword`. |

## Academic Structure

| Rule | Status | Enforcement layer | Evidence / notes |
|---|---|---|---|
| BR-08: fields and semesters have a many-to-many relationship | Implemented | PostgreSQL/JPA/service | `semester_field` is an explicit entity with a surrogate `id`; the admin selects one or more fields for a semester. |
| BR-09: semester belongs to at least one field | Implemented | PostgreSQL/Java | `@Size(min = 1)` plus deferred PostgreSQL cardinality triggers on `semester` and `semester_field`. |
| BR-10: student group belongs to one semester-field context | Implemented | PostgreSQL/JPA | Required `student_group.semester_field_id` references `semester_field(id)`. |
| BR-11: student belongs to at most one group | Implemented | Schema/model | `student.group_id` is a single nullable FK. |
| BR-12: group maximum size | Implemented | PostgreSQL/service/model | `StudentGroup.maxSize`, service checks on create/update, and PostgreSQL triggers prevent groups exceeding max size. |
| BR-13: student can view only own group details | Implemented | GUI/repository | Student navigation only exposes own group via `createMyGroupView`. |

## Teacher Specialization

| Rule | Status | Enforcement layer | Evidence / notes |
|---|---|---|---|
| BR-14: teacher has at least one qualified subject | Implemented | PostgreSQL/Java | `@Size(min = 1, max = 5)` and deferred PostgreSQL triggers enforce teacher-subject cardinality. |
| BR-15: teacher has at most five qualified subjects | Implemented | PostgreSQL/Java | Same as BR-14. |
| BR-16: teacher can teach only qualified subjects | Implemented | PostgreSQL/service/model | Constructor validation and deferred PostgreSQL trigger `trg_class_meeting_teacher_subject`. |
| BR-17: subject has at least one qualified teacher | Implemented for active subjects | PostgreSQL/service | A catalog subject may be created first, but service and PostgreSQL prevent activating it in a semester/group unless at least one teacher is qualified. |
| BR-18: qualification vs actual teaching are separate associations | Implemented | Design/model | `teacher_subject` means qualification; `class_meeting.teacher_id` means actual teaching. |

## Weekly Schedule

| Rule | Status | Enforcement layer | Evidence / notes |
|---|---|---|---|
| BR-19: group has weekly schedule in semester and field | Implemented | PostgreSQL/model/schema | `weekly_schedule_entry.source_class_meeting_id` points to a source class meeting; group, semester, and field are derived through `class_meeting.group_id -> student_group.semester_field_id`. |
| BR-20: WeeklyScheduleEntry is a recurring marker | Implemented | Model/design | `WeeklyScheduleEntry` stores only the source class meeting; subject, teacher, group, type, mode, day, time, room, and link are derived from that meeting. |
| BR-21: ClassMeeting is concrete occurrence | Implemented | Model/design | `ClassMeeting` stores concrete date and time. |
| BR-22: generated meetings inside semester dates | Implemented | Service | `ScheduleGenerationService` iterates from semester start to end. DB trigger still recommended for manual meetings. |
| BR-23: generated meeting matches weekly day | Implemented | Service | `ScheduleGenerationService` checks `date.getDayOfWeek() == entry.dayOfWeek`. |
| BR-24: generation validates teacher qualification | Implemented | Service/PostgreSQL | `ScheduleGenerationService` and PostgreSQL trigger validate the source class meeting. |
| BR-25: generation validates room availability | Implemented | Service/PostgreSQL | Service checks overlapping active bookings; PostgreSQL exclusion constraint prevents overlaps on insert. |

## Class Meetings

| Rule | Status | Enforcement layer | Evidence / notes |
|---|---|---|---|
| BR-26: meeting has subject, teacher, group | Implemented | PostgreSQL/JPA | NOT NULL FKs in `class_meeting`. |
| BR-27: subject valid for group academic context | Implemented | PostgreSQL/JPA/service | `semester_field_subject` is the explicit curriculum association; triggers require the subject in the group's exact semester-field context. |
| BR-28: teacher qualified for subject | Implemented | PostgreSQL/service/model | `trg_class_meeting_teacher_subject`, `ClassMeeting.validateTeacherQualification`. |
| BR-29: group must exist | Implemented | PostgreSQL | `class_meeting.group_id` FK. |
| BR-30: meeting time valid | Implemented | PostgreSQL | `chk_class_meeting_time_range`. |
| BR-31: meeting date inside semester period | Implemented | PostgreSQL/service | `trg_class_meeting_semester_period` and `ClassMeetingService` validate against group semester dates. |
| BR-32: meeting may be draft | Implemented | Enum/service | `ClassMeetingStatus.DRAFT`, service allows incomplete draft. |
| BR-33: scheduled meeting has complete details | Implemented | Service/PostgreSQL | Service validates scheduled meeting details; `chk_class_meeting_details_by_status` and deferred triggers require online links for online meetings and matching active room bookings for scheduled/completed classroom meetings. |
| BR-34: cancelled meeting is not deleted | Implemented | Service | `ClassMeetingService.cancelClassMeeting` changes status. |
| BR-35: attendance cannot be marked for cancelled/draft meeting | Implemented | Service/model | `AttendanceRegistrationService`, `ClassMeeting.addAttendance`; attendance registration is allowed for scheduled meetings and later edits on completed meetings. |
| BR-36: completed meetings remain in history | Implemented | Repository/GUI | history queries include completed meetings. |

## Room Booking

| Rule | Status | Enforcement layer | Evidence / notes |
|---|---|---|---|
| BR-37: classroom meeting requires room booking or location | Implemented | Service/PostgreSQL | Scheduled/completed classroom meetings require exactly one active `room_booking` matching meeting date/start/end; cancelling a meeting cancels its booking. |
| BR-38: online meeting requires online link | Implemented | Service/PostgreSQL | Service validation and DB mode/link check. |
| BR-39: prevent overlapping room bookings | Implemented | PostgreSQL | `ex_room_booking_no_overlap` GiST exclusion constraint. |
| BR-40: exact duplicate impossible | Implemented | PostgreSQL/JPA | unique `(room_id, date, start_time, end_time)`. |
| BR-41: meaningful qualified association | Implemented | Model/schema | `Room.bookingsBySlot` keyed by `MeetingSlot`; `RoomBooking` owns slot columns. |
| BR-42: room capacity fits group size | Implemented | PostgreSQL/service | Room booking trigger and service checks reject classroom bookings/meetings when capacity is below group size. |
| BR-43: cancelled booking does not block availability | Implemented | PostgreSQL/service | Exclusion constraint and service overlap checks ignore `CANCELLED` bookings. |

## Attendance

| Rule | Status | Enforcement layer | Evidence / notes |
|---|---|---|---|
| BR-44: attendance student belongs to meeting group | Implemented | PostgreSQL | `trg_attendance_student_group`. |
| BR-45: attendance belongs to one student and meeting | Implemented | PostgreSQL/JPA | NOT NULL FKs. |
| BR-46: one attendance per student per meeting | Implemented | PostgreSQL/JPA | unique `(student_id, class_meeting_id)`. |
| BR-47: attendance status required | Implemented | PostgreSQL/JPA | NOT NULL and enum check. |
| BR-48: attendance comment optional | Implemented | Schema/model | nullable comment. |
| BR-49: attendance comment editable by teacher/admin | Implemented | GUI/service | Details dialog editable for assigned teacher/admin. |
| BR-50: student cannot edit attendance | Implemented | GUI | Student view uses non-editable table. |
| BR-51: teacher marks only assigned meetings | Implemented | Service/GUI | `ClassMeetingService.validateCanMarkAttendance`. |
| BR-52: teacher cannot mark before meeting starts | Implemented | Service | `validateCanMarkAttendance`. |
| BR-53: teacher cannot mark cancelled/draft meeting | Implemented | Service | `validateCanMarkAttendance` and registration service allow scheduled/completed meetings only. |
| BR-54: admin can mark/edit scheduled/completed meetings | Implemented | GUI/service | Admin save button visible; registration accepts scheduled/completed meetings and rejects draft/cancelled meetings. |
| BR-55: attendance registration time stored | Implemented | PostgreSQL/service/model | `Attendance.registrationTime` is saved and updated when attendance is registered. |
| BR-56: late counts as attended | Implemented | Report service/model | `ReportLine.recalculateAttendancePercentage`, report accumulator. |
| BR-57: excused shown separately, not attended | Implemented | Report service/model | Excused count separate; percentage uses present+late. |
| BR-58: absent not attended | Implemented | Report service/model | Absent count separate; percentage excludes absent. |

## Reports And Statistics

| Rule | Status | Enforcement layer | Evidence / notes |
|---|---|---|---|
| BR-59: only admin generates reports | Implemented | GUI | Generate Reports navigation only for admin. |
| BR-60: filters optional | Implemented | Service/repository | `AttendanceReportFilter`; dynamic JPQL in `ClassMeetingRepository.findByReportFilter`. |
| BR-61: filters combinable | Implemented | Service/repository | dynamic filter composition. |
| BR-62: class type unspecified includes all | Implemented | GUI/service | "All" resolves to null. |
| BR-63: meetings match selected filters | Implemented | Repository | `findByReportFilter`. |
| BR-64: calculate from meetings, not only attendance | Implemented | Service | Missing attendance records counted as absent in `AttendanceReportService`. |
| BR-65: ReportLine per student | Implemented | Model/service | `ReportAccumulator` creates one line per student. |
| BR-66: attendance percentage formula | Implemented | Model | `ReportLine.recalculateAttendancePercentage`. |
| BR-67: overall performance formula | Implemented | Service | average of report line percentages. |
| BR-68: conclusion message shown | Implemented | GUI/model | `generateConclusionMessage`, GUI report message label. |
| BR-69: empty report cannot export | Implemented | Service | `ReportExportService.exportToCsv`. |
| BR-70: report generated before export | Implemented | GUI/service | GUI checks `currentReport`; service rejects null. |
| BR-71: CSV includes metadata | Implemented | Service | `ReportExportService.appendMetadata`. |

## Notifications

| Rule | Status | Enforcement layer | Evidence / notes |
|---|---|---|---|
| BR-72: notification recipient is Person | Implemented | Model/schema | `Notification.recipient`, FK to `person`; email notifications additionally store the exact `delivery_email`. |
| BR-73: person views only own notifications | Implemented | GUI/repository | My Notifications uses current user id; admin view all separate. |
| BR-74: admin creates notifications | Implemented | GUI/service/PostgreSQL | Manage Notifications visible to admin; admin can target selected emails for a single user, an entire student group, or all users. |
| BR-75: admin cancels pending notifications | Implemented | Service | `cancelPendingNotification`. |
| BR-76: sent notifications not freely modified | Implemented | Service | `updateEditableNotification` rejects sent. |
| BR-77: notification required fields include createdAt | Implemented | Service/PostgreSQL/model | `Notification.createdAt` is persisted; service and PostgreSQL checks enforce title/message/recipient/status and, for email notifications, a delivery email owned by the recipient. |
| BR-78: scheduled task has scheduled time/status/recipient | Implemented | PostgreSQL/service/model | scheduledAt/status required; PostgreSQL check enforces recipients for new task rows; services assign recipients. |
| BR-79: scheduler processes pending tasks | Implemented | Service/repository | `SystemScheduler.processPendingNotificationTasks` processes only pending due tasks from `findPendingDueTasks`. |
| BR-80: failed task stores reason and retry count | Implemented | Service/model/schema | scheduler marks failed tasks, stores failure reason, and increments retry count. |

## History And Visibility

| Rule | Status | Enforcement layer | Evidence / notes |
|---|---|---|---|
| BR-81: student history through group/attendance | Implemented | Repository/GUI | `findClassMeetingHistoryForStudent`, `findAttendanceHistoryForStudent`. |
| BR-82: teacher history through assigned meetings/bookings | Implemented | Repository/GUI | teacher history shows assigned meetings and room bookings. |
| BR-83: admin can view all history | Implemented | GUI/repository | admin history tabs show class meetings, attendance, reports, room bookings, notifications, notification tasks, and activity logs. |
| BR-84: public schedule limited data | Implemented | GUI/service | public list plus restricted detail opening. |
| BR-85: students view only own attendance details | Implemented | GUI | meeting details filter rows to current student; My Attendance uses current user id. |
| User activity history: email/password changes | Implemented | PostgreSQL/service/GUI | `user_activity_log`, `ActivityLog`, Activity Logs tab. |

## Validation And Persistence

| Rule | Status | Enforcement layer | Evidence / notes |
|---|---|---|---|
| BR-86: mandatory fields cannot be null/blank | Implemented | Bean validation/PostgreSQL | Java validation covers entity inputs; V5 adds explicit PostgreSQL blank-string checks for core required text fields and notification messages. |
| BR-87: unique identifiers | Implemented | PostgreSQL/JPA | student number, employee numbers, room number, subject name, emails. Subject/admin code naming differs from current model. |
| BR-88: date/time ranges valid | Implemented | PostgreSQL/service | semester/report/meeting/booking time checks exist; class meeting date inside group semester is enforced. |
| BR-89: removal preserves consistency | Implemented | Service/PostgreSQL/cascade | report lines cascade; user/academic deletion is guarded; deferred triggers prevent removing active room-booking/attendance data when it would invalidate scheduled/completed meetings. |
| BR-90: transactions rollback on failure | Implemented | Service/repository | transaction manager and manual transaction blocks. |
| BR-91: associations stay consistent | Implemented | Model/service/PostgreSQL | helper methods maintain both sides in Java; PostgreSQL triggers verify class-meeting day/date consistency, matching room-booking slots, curriculum links, teacher qualifications, and attendance membership. |
| BR-92: lazy loading handled safely | Implemented | Repository | join-fetch repository methods for GUI screens. |

## State Rules

| Rule | Status | Enforcement layer | Evidence / notes |
|---|---|---|---|
| BR-93: class meeting status controls operations | Implemented | Service/GUI/PostgreSQL | Create form only starts draft/scheduled; services complete/cancel through lifecycle methods; PostgreSQL blocks invalid status transitions and terminal-state edits. |
| BR-94: booking status controls availability | Implemented | PostgreSQL/service | DB and service availability checks ignore cancelled bookings. |
| BR-95: notification task status controls processing | Implemented | Model/service/PostgreSQL | task methods and scheduler use explicit PENDING -> PROCESSING -> SENT/FAILED transitions; PostgreSQL enforces transitions, processed timestamps, failure reason, and task payload requirements. |
| BR-96: report snapshot should not change after export | Implemented | PostgreSQL/service/model | `AttendanceReport.exportedAt` is set on export; PostgreSQL triggers prevent later report/filter/line/semester changes. |

## Highest-Value Next Implementation Targets

1. Completed: `StudentGroup.maxSize` with service and PostgreSQL enforcement.
2. Completed: room-capacity validation for classroom meetings/bookings.
3. Completed: `Attendance.registrationTime`.
4. Completed: `Notification.createdAt`.
5. Completed: manual `ClassMeeting.meetingDate` inside the group's semester dates.
6. Completed: room availability service checks ignore `CANCELLED` bookings and check overlaps.
7. Completed: admin history includes reports, room bookings, notifications, scheduled tasks, attendance, meetings, and activity logs.
8. Completed: explicit `semester_field_subject` curriculum association for exact semester-field subject ownership.
9. Completed: class-meeting lifecycle/status rules with PostgreSQL transition triggers.
10. Completed: mandatory non-blank checks for core PostgreSQL text fields.
11. Completed: Semester-Field many-to-many model with group and weekly-schedule academic context constraints.
