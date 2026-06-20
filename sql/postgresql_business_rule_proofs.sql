-- PostgreSQL business-rule proof script for the UAMS diploma version.
-- Run after Flyway migrations V1..V9 have been applied.
-- The whole script runs inside one transaction and finishes with ROLLBACK.

BEGIN;
SET CONSTRAINTS ALL DEFERRED;

CREATE OR REPLACE FUNCTION pg_temp.iso_day_name(p_date date)
RETURNS text
LANGUAGE sql
AS $$
    SELECT CASE EXTRACT(ISODOW FROM p_date)::integer
        WHEN 1 THEN 'MONDAY'
        WHEN 2 THEN 'TUESDAY'
        WHEN 3 THEN 'WEDNESDAY'
        WHEN 4 THEN 'THURSDAY'
        WHEN 5 THEN 'FRIDAY'
        WHEN 6 THEN 'SATURDAY'
        WHEN 7 THEN 'SUNDAY'
    END
$$;

CREATE OR REPLACE FUNCTION pg_temp.expect_error(p_label text, p_sql text)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    expected_marker constant text := '__EXPECTED_DATABASE_ERROR_NOT_RAISED__';
BEGIN
    BEGIN
        EXECUTE p_sql;
        EXECUTE 'SET CONSTRAINTS ALL IMMEDIATE';
        RAISE EXCEPTION '%', expected_marker;
    EXCEPTION WHEN others THEN
        IF SQLERRM = expected_marker THEN
            RAISE EXCEPTION 'FAIL: % did not raise a database error', p_label;
        END IF;
        RAISE NOTICE 'OK: % rejected by PostgreSQL: %', p_label, SQLERRM;
        EXECUTE 'SET CONSTRAINTS ALL DEFERRED';
    END;
END;
$$;

DO $$
DECLARE
    field_id bigint;
    other_field_id bigint;
    semester_id bigint;
    semester_field_id bigint;
    other_semester_field_id bigint;
    group_id bigint;
    same_context_group_id bigint;
    other_group_id bigint;
    subject_id bigint;
    other_subject_id bigint;
    unassigned_subject_id bigint;
    teacher_id bigint;
    other_teacher_id bigint;
    student_1_id bigint;
    student_2_id bigint;
    outside_student_id bigint;
    room_id bigint;
    meeting_id bigint;
    task_id bigint;
    meeting_date date := DATE '2026-10-05';
    other_meeting_date date := DATE '2026-10-12';
BEGIN
    INSERT INTO field_of_study (name)
    VALUES ('Proof Field')
    RETURNING id INTO field_id;

    INSERT INTO field_of_study (name)
    VALUES ('Proof Other Field')
    RETURNING id INTO other_field_id;

    INSERT INTO semester (number, start_date, end_date)
    VALUES (4, DATE '2026-10-01', DATE '2027-02-15')
    RETURNING id INTO semester_id;

    INSERT INTO semester_field (semester_id, field_id)
    VALUES (semester_id, field_id)
    RETURNING id INTO semester_field_id;

    INSERT INTO semester_field (semester_id, field_id)
    VALUES (semester_id, other_field_id)
    RETURNING id INTO other_semester_field_id;

    INSERT INTO student_group (code, max_size, semester_field_id)
    VALUES ('PROOF-G1', 2, semester_field_id)
    RETURNING id INTO group_id;

    INSERT INTO student_group (code, max_size, semester_field_id)
    VALUES ('PROOF-G3', 2, semester_field_id)
    RETURNING id INTO same_context_group_id;

    INSERT INTO student_group (code, max_size, semester_field_id)
    VALUES ('PROOF-G2', 2, other_semester_field_id)
    RETURNING id INTO other_group_id;

    INSERT INTO subject (name)
    VALUES ('Proof Databases')
    RETURNING id INTO subject_id;

    INSERT INTO subject (name)
    VALUES ('Proof Analytics')
    RETURNING id INTO other_subject_id;

    INSERT INTO subject (name)
    VALUES ('Proof Not Assigned To Group')
    RETURNING id INTO unassigned_subject_id;

    INSERT INTO person (name, surname, birth_date, primary_email, password_hash)
    VALUES ('Proof', 'Teacher', DATE '1980-01-01', 'proof.teacher@example.com', 'hash')
    RETURNING id INTO teacher_id;
    INSERT INTO teacher (id, teacher_employee_number) VALUES (teacher_id, 'proof-t1');
    INSERT INTO person_emails (person_id, email) VALUES (teacher_id, 'proof.teacher@example.com');

    INSERT INTO person (name, surname, birth_date, primary_email, password_hash)
    VALUES ('Proof', 'OtherTeacher', DATE '1981-01-01', 'proof.other.teacher@example.com', 'hash')
    RETURNING id INTO other_teacher_id;
    INSERT INTO teacher (id, teacher_employee_number) VALUES (other_teacher_id, 'proof-t2');
    INSERT INTO person_emails (person_id, email) VALUES (other_teacher_id, 'proof.other.teacher@example.com');

    INSERT INTO teacher_subject (teacher_id, subject_id) VALUES (teacher_id, subject_id);
    INSERT INTO teacher_subject (teacher_id, subject_id) VALUES (teacher_id, unassigned_subject_id);
    INSERT INTO teacher_subject (teacher_id, subject_id) VALUES (other_teacher_id, subject_id);
    INSERT INTO teacher_subject (teacher_id, subject_id) VALUES (other_teacher_id, other_subject_id);

    INSERT INTO semester_field_subject (semester_field_id, subject_id)
    VALUES (semester_field_id, subject_id);
    INSERT INTO semester_field_subject (semester_field_id, subject_id)
    VALUES (semester_field_id, other_subject_id);

    INSERT INTO person (name, surname, birth_date, primary_email, password_hash)
    VALUES ('Proof', 'StudentOne', DATE '2004-01-01', 'proof.student1@example.com', 'hash')
    RETURNING id INTO student_1_id;
    INSERT INTO student (id, student_number, group_id) VALUES (student_1_id, 's99001', group_id);
    INSERT INTO person_emails (person_id, email) VALUES (student_1_id, 'proof.student1@example.com');

    INSERT INTO person (name, surname, birth_date, primary_email, password_hash)
    VALUES ('Proof', 'StudentTwo', DATE '2004-02-01', 'proof.student2@example.com', 'hash')
    RETURNING id INTO student_2_id;
    INSERT INTO student (id, student_number, group_id) VALUES (student_2_id, 's99002', group_id);
    INSERT INTO person_emails (person_id, email) VALUES (student_2_id, 'proof.student2@example.com');

    INSERT INTO person (name, surname, birth_date, primary_email, password_hash)
    VALUES ('Proof', 'OutsideStudent', DATE '2004-03-01', 'proof.outside@example.com', 'hash')
    RETURNING id INTO outside_student_id;
    INSERT INTO student (id, student_number, group_id) VALUES (outside_student_id, 's99004', other_group_id);
    INSERT INTO person_emails (person_id, email) VALUES (outside_student_id, 'proof.outside@example.com');

    INSERT INTO room (room_number, capacity)
    VALUES ('PROOF-101', 2)
    RETURNING id INTO room_id;

    INSERT INTO class_meeting (
        location, meeting_date, day_of_week, start_time, end_time,
        class_type, meeting_mode, status, subject_id, teacher_id, group_id
    )
    VALUES (
        'PROOF-101', meeting_date, pg_temp.iso_day_name(meeting_date), TIME '10:00', TIME '11:30',
        'LABORATORY', 'CLASSROOM', 'SCHEDULED', subject_id, teacher_id, group_id
    )
    RETURNING id INTO meeting_id;

    INSERT INTO room_booking (date, start_time, end_time, booking_status, room_id, class_meeting_id)
    VALUES (meeting_date, TIME '10:00', TIME '11:30', 'CONFIRMED', room_id, meeting_id);

    INSERT INTO weekly_schedule_entry (source_class_meeting_id)
    VALUES (meeting_id);

    INSERT INTO scheduled_notification_task (task_type, status, scheduled_at, recipient_id, class_meeting_id)
    VALUES ('CLASS_MEETING_REMINDER', 'PENDING', now(), teacher_id, meeting_id)
    RETURNING id INTO task_id;

    EXECUTE 'SET CONSTRAINTS ALL IMMEDIATE';
    EXECUTE 'SET CONSTRAINTS ALL DEFERRED';

    PERFORM pg_temp.expect_error(
        'class meeting group cannot overlap another active meeting',
        format(
            'INSERT INTO class_meeting (
                 meeting_date, day_of_week, start_time, end_time,
                 class_type, meeting_mode, status, subject_id, teacher_id, group_id
             )
             VALUES (%L, %L, TIME %L, TIME %L, %L, %L, %L, %s, %s, %s)',
            meeting_date,
            pg_temp.iso_day_name(meeting_date),
            '10:30', '11:00',
            'LECTURE', 'ONLINE', 'DRAFT',
            subject_id, other_teacher_id, group_id
        )
    );

    PERFORM pg_temp.expect_error(
        'class meeting teacher cannot overlap another active meeting',
        format(
            'INSERT INTO class_meeting (
                 meeting_date, day_of_week, start_time, end_time,
                 class_type, meeting_mode, status, subject_id, teacher_id, group_id
             )
             VALUES (%L, %L, TIME %L, TIME %L, %L, %L, %L, %s, %s, %s)',
            meeting_date,
            pg_temp.iso_day_name(meeting_date),
            '10:30', '11:00',
            'LECTURE', 'ONLINE', 'DRAFT',
            subject_id, teacher_id, same_context_group_id
        )
    );

    PERFORM pg_temp.expect_error(
        'weekly schedule source cannot create recurring group overlap',
        format(
            'WITH source AS (
                 INSERT INTO class_meeting (
                     meeting_date, day_of_week, start_time, end_time,
                     class_type, meeting_mode, status, subject_id, teacher_id, group_id
                 )
                 VALUES (%L, %L, TIME %L, TIME %L, %L, %L, %L, %s, %s, %s)
                 RETURNING id
             )
             INSERT INTO weekly_schedule_entry (source_class_meeting_id)
             SELECT id FROM source',
            other_meeting_date,
            pg_temp.iso_day_name(other_meeting_date),
            '10:30', '11:45',
            'LECTURE', 'ONLINE', 'DRAFT',
            subject_id, other_teacher_id, group_id
        )
    );

    PERFORM pg_temp.expect_error(
        'class meeting subject must belong to the exact group semester-field curriculum',
        format(
            'INSERT INTO class_meeting (
                 meeting_date, day_of_week, start_time, end_time,
                 class_type, meeting_mode, status, subject_id, teacher_id, group_id
             )
             VALUES (%L, %L, TIME %L, TIME %L, %L, %L, %L, %s, %s, %s)',
            other_meeting_date + 2,
            pg_temp.iso_day_name(other_meeting_date + 2),
            '16:00', '17:30',
            'LECTURE', 'ONLINE', 'DRAFT',
            subject_id, teacher_id, other_group_id
        )
    );

    PERFORM pg_temp.expect_error(
        'room bookings prevent overlapping active time ranges',
        format(
            'INSERT INTO room_booking (date, start_time, end_time, booking_status, room_id)
             VALUES (%L, TIME %L, TIME %L, %L, %s)',
            meeting_date, '10:30', '11:45', 'CONFIRMED', room_id
        )
    );

    PERFORM pg_temp.expect_error(
        'teacher must be qualified for the class meeting subject',
        format(
            'INSERT INTO class_meeting (
                 meeting_date, day_of_week, start_time, end_time,
                 class_type, meeting_mode, status, subject_id, teacher_id, group_id
             )
             VALUES (%L, %L, TIME %L, TIME %L, %L, %L, %L, %s, %s, %s)',
            other_meeting_date,
            pg_temp.iso_day_name(other_meeting_date),
            '12:00', '13:30',
            'LECTURE', 'ONLINE', 'DRAFT',
            other_subject_id, teacher_id, group_id
        )
    );

    PERFORM pg_temp.expect_error(
        'class meeting subject must be in the selected group curriculum',
        format(
            'INSERT INTO class_meeting (
                 meeting_date, day_of_week, start_time, end_time,
                 class_type, meeting_mode, status, subject_id, teacher_id, group_id
             )
             VALUES (%L, %L, TIME %L, TIME %L, %L, %L, %L, %s, %s, %s)',
            other_meeting_date + 1,
            pg_temp.iso_day_name(other_meeting_date + 1),
            '14:00', '15:30',
            'LECTURE', 'ONLINE', 'DRAFT',
            unassigned_subject_id, teacher_id, group_id
        )
    );

    PERFORM pg_temp.expect_error(
        'attendance student must belong to the meeting group',
        format(
            'INSERT INTO attendance (status, student_id, class_meeting_id, registration_time)
             VALUES (%L, %s, %s, now())',
            'PRESENT', outside_student_id, meeting_id
        )
    );

    PERFORM pg_temp.expect_error(
        'student group maximum size is enforced in PostgreSQL',
        format(
            'WITH p AS (
                 INSERT INTO person (name, surname, birth_date, primary_email, password_hash)
                 VALUES (%L, %L, DATE %L, %L, %L)
                 RETURNING id
             ), e AS (
                 INSERT INTO person_emails (person_id, email)
                 SELECT id, %L FROM p
             )
             INSERT INTO student (id, student_number, group_id)
             SELECT id, %L, %s FROM p',
            'Proof', 'OverflowStudent', '2004-04-01',
            'proof.overflow@example.com', 'hash',
            'proof.overflow@example.com',
            's99003', group_id
        )
    );

    PERFORM pg_temp.expect_error(
        'scheduled classroom meeting requires matching active room booking',
        format(
            'INSERT INTO class_meeting (
                 location, meeting_date, day_of_week, start_time, end_time,
                 class_type, meeting_mode, status, subject_id, teacher_id, group_id
             )
             VALUES (%L, %L, %L, TIME %L, TIME %L, %L, %L, %L, %s, %s, %s)',
            'PROOF-101',
            other_meeting_date + 2,
            pg_temp.iso_day_name(other_meeting_date + 2),
            '16:00', '17:30',
            'LABORATORY', 'CLASSROOM', 'SCHEDULED',
            subject_id, teacher_id, group_id
        )
    );

    PERFORM pg_temp.expect_error(
        'scheduled online meeting requires non-blank link',
        format(
            'INSERT INTO class_meeting (
                 meeting_date, day_of_week, start_time, end_time,
                 class_type, meeting_mode, status, subject_id, teacher_id, group_id
             )
             VALUES (%L, %L, TIME %L, TIME %L, %L, %L, %L, %s, %s, %s)',
            other_meeting_date + 3,
            pg_temp.iso_day_name(other_meeting_date + 3),
            '18:00', '19:00',
            'LECTURE', 'ONLINE', 'SCHEDULED',
            subject_id, teacher_id, group_id
        )
    );

    PERFORM pg_temp.expect_error(
        'class meeting day_of_week must match meeting_date',
        format(
            'INSERT INTO class_meeting (
                 meeting_date, day_of_week, start_time, end_time,
                 class_type, meeting_mode, status, subject_id, teacher_id, group_id
             )
             VALUES (%L, %L, TIME %L, TIME %L, %L, %L, %L, %s, %s, %s)',
            other_meeting_date + 4,
            'MONDAY',
            '08:00', '09:00',
            'LECTURE', 'ONLINE', 'DRAFT',
            subject_id, teacher_id, group_id
        )
    );

    PERFORM pg_temp.expect_error(
        'completed meeting requires attendance for every group student',
        format(
            'WITH cm AS (
                 INSERT INTO class_meeting (
                     location, meeting_date, day_of_week, start_time, end_time,
                     class_type, meeting_mode, status, subject_id, teacher_id, group_id
                 )
                 VALUES (%L, %L, %L, TIME %L, TIME %L, %L, %L, %L, %s, %s, %s)
                 RETURNING id
             )
             INSERT INTO room_booking (date, start_time, end_time, booking_status, room_id, class_meeting_id)
             SELECT %L, TIME %L, TIME %L, %L, %s, id FROM cm',
            'PROOF-101',
            other_meeting_date + 5,
            pg_temp.iso_day_name(other_meeting_date + 5),
            '09:00', '10:00',
            'LABORATORY', 'CLASSROOM', 'COMPLETED',
            subject_id, teacher_id, group_id,
            other_meeting_date + 5, '09:00', '10:00', 'CONFIRMED', room_id
        )
    );

    PERFORM pg_temp.expect_error(
        'notification task cannot jump from pending directly to sent',
        format(
            'UPDATE scheduled_notification_task SET status = %L WHERE id = %s',
            'SENT', task_id
        )
    );

    PERFORM pg_temp.expect_error(
        'notification task payload must match task type',
        format(
            'INSERT INTO scheduled_notification_task (task_type, status, scheduled_at, recipient_id)
             VALUES (%L, %L, now(), %s)',
            'CLASS_MEETING_REMINDER', 'PENDING', teacher_id
        )
    );
END;
$$;

SELECT 'Business-rule proof script finished. All expected violations were rejected. Rolling back proof data.' AS result;

ROLLBACK;
