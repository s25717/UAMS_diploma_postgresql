-- PostgreSQL index/performance proof script for UAMS.
-- Run after the application has seeded or used the database.
-- Use the EXPLAIN output in the thesis to connect query patterns to indexes.

-- 1. Public/current schedule lookup.
-- Relevant indexes:
--   idx_class_meeting_status_date
--   idx_class_meeting_group_date
EXPLAIN (ANALYZE, BUFFERS)
SELECT cm.id,
       cm.meeting_date,
       cm.start_time,
       cm.end_time,
       cm.status,
       s.name AS subject_name,
       g.code AS group_code
FROM class_meeting cm
JOIN subject s ON s.id = cm.subject_id
JOIN student_group g ON g.id = cm.group_id
WHERE cm.status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED')
  AND cm.meeting_date BETWEEN CURRENT_DATE - 7 AND CURRENT_DATE + 30
ORDER BY cm.meeting_date, cm.start_time;

-- 2. Teacher schedule/history lookup.
-- Relevant index:
--   idx_class_meeting_teacher_date
EXPLAIN (ANALYZE, BUFFERS)
SELECT cm.id,
       cm.meeting_date,
       cm.start_time,
       cm.end_time,
       cm.status
FROM class_meeting cm
WHERE cm.teacher_id = (SELECT id FROM teacher ORDER BY id LIMIT 1)
ORDER BY cm.meeting_date, cm.start_time;

-- 3. Student attendance history lookup.
-- Relevant indexes:
--   idx_attendance_student_id
--   idx_attendance_class_meeting_id
--   uk_attendance_student_meeting
EXPLAIN (ANALYZE, BUFFERS)
SELECT a.id,
       a.status,
       a.registration_time,
       cm.meeting_date,
       cm.start_time,
       s.name AS subject_name
FROM attendance a
JOIN class_meeting cm ON cm.id = a.class_meeting_id
JOIN subject s ON s.id = cm.subject_id
WHERE a.student_id = (SELECT id FROM student ORDER BY id LIMIT 1)
ORDER BY cm.meeting_date DESC, cm.start_time DESC;

-- 4. Room availability overlap lookup.
-- Relevant index and constraint:
--   idx_room_booking_room_date
--   ex_room_booking_no_overlap
EXPLAIN (ANALYZE, BUFFERS)
SELECT rb.id
FROM room_booking rb
WHERE rb.room_id = (SELECT id FROM room ORDER BY id LIMIT 1)
  AND rb.date = CURRENT_DATE
  AND rb.booking_status <> 'CANCELLED'
  AND rb.start_time < TIME '11:30'
  AND rb.end_time > TIME '10:00';

-- 5. Report filter lookup.
-- Relevant indexes:
--   idx_attendance_report_filters
--   idx_class_meeting_subject_date
--   idx_class_meeting_group_date
--   idx_class_meeting_teacher_date
EXPLAIN (ANALYZE, BUFFERS)
SELECT cm.id,
       cm.meeting_date,
       cm.class_type,
       cm.teacher_id,
       cm.subject_id,
       cm.group_id
FROM class_meeting cm
WHERE cm.meeting_date BETWEEN CURRENT_DATE - 120 AND CURRENT_DATE + 30
  AND cm.subject_id = (SELECT id FROM subject ORDER BY id LIMIT 1)
ORDER BY cm.meeting_date;

-- 6. Notification inbox lookup.
-- Relevant index:
--   idx_notification_recipient_status
EXPLAIN (ANALYZE, BUFFERS)
SELECT n.id,
       n.title,
       n.status,
       n.created_at
FROM notification n
WHERE n.recipient_id = (SELECT id FROM person ORDER BY id LIMIT 1)
  AND n.status IN ('PENDING', 'SENT', 'FAILED')
ORDER BY n.created_at DESC;

-- 7. Scheduler due-task lookup.
-- Relevant index:
--   idx_scheduled_notification_due
EXPLAIN (ANALYZE, BUFFERS)
SELECT t.id,
       t.task_type,
       t.scheduled_at,
       t.retry_count
FROM scheduled_notification_task t
WHERE t.status = 'PENDING'
  AND t.scheduled_at <= now()
ORDER BY t.scheduled_at;

-- 8. Curriculum availability lookup.
-- Relevant indexes:
--   uk_semester_field_subject_context
--   idx_curriculum_subject
--   idx_curriculum_semester_field_id
EXPLAIN (ANALYZE, BUFFERS)
WITH selected_context AS (
    SELECT id
    FROM semester_field
    ORDER BY semester_id, field_id
    LIMIT 1
)
SELECT curriculum.semester_field_id,
       curriculum.subject_id
FROM semester_field_subject curriculum
JOIN selected_context context
  ON context.id = curriculum.semester_field_id
ORDER BY curriculum.subject_id;

-- 9. Weekly schedule filter lookup through source class meeting.
-- Relevant indexes:
--   idx_weekly_schedule_source_class_meeting
--   idx_class_meeting_group_week_slot
--   idx_class_meeting_teacher_week_slot
--   idx_class_meeting_type_mode
EXPLAIN (ANALYZE, BUFFERS)
SELECT w.id,
       cm.day_of_week,
       cm.start_time,
       cm.end_time,
       cm.class_type,
       cm.meeting_mode,
       cm.group_id,
       cm.teacher_id
FROM weekly_schedule_entry w
JOIN class_meeting cm ON cm.id = w.source_class_meeting_id
WHERE cm.group_id = (SELECT id FROM student_group ORDER BY id LIMIT 1)
  AND cm.class_type IN ('LECTURE', 'TUTORIAL', 'LABORATORY')
ORDER BY cm.day_of_week, cm.start_time;
