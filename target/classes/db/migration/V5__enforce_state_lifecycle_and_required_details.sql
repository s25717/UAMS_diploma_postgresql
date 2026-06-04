ALTER TABLE class_meeting
    DROP CONSTRAINT IF EXISTS chk_class_meeting_location;

ALTER TABLE class_meeting
    ADD CONSTRAINT chk_class_meeting_details_by_status
    CHECK (
        status IN ('DRAFT', 'CANCELLED')
        OR (
            meeting_mode = 'ONLINE'
            AND online_meeting_link IS NOT NULL
            AND btrim(online_meeting_link) <> ''
        )
        OR meeting_mode = 'CLASSROOM'
    ) NOT VALID;

ALTER TABLE scheduled_notification_task
    ADD CONSTRAINT chk_notification_task_processed_state
    CHECK (
        (status IN ('PENDING', 'PROCESSING') AND processed_at IS NULL)
        OR (status IN ('SENT', 'FAILED', 'CANCELLED') AND processed_at IS NOT NULL)
    ) NOT VALID,
    ADD CONSTRAINT chk_notification_task_failed_reason
    CHECK (
        status <> 'FAILED'
        OR failure_reason IS NOT NULL AND btrim(failure_reason) <> ''
    ) NOT VALID,
    ADD CONSTRAINT chk_notification_task_payload
    CHECK (
        (
            task_type IN (
                'CLASS_MEETING_REMINDER',
                'LOW_ATTENDANCE_WARNING',
                'ATTENDANCE_REGISTERED',
                'ATTENDANCE_UPDATED'
            )
            AND class_meeting_id IS NOT NULL
        )
        OR (
            task_type = 'REPORT_READY'
            AND attendance_report_id IS NOT NULL
        )
    ) NOT VALID;

ALTER TABLE person
    ADD CONSTRAINT chk_person_name_not_blank CHECK (btrim(name) <> '') NOT VALID,
    ADD CONSTRAINT chk_person_surname_not_blank CHECK (btrim(surname) <> '') NOT VALID,
    ADD CONSTRAINT chk_person_password_hash_not_blank CHECK (btrim(password_hash) <> '') NOT VALID,
    ADD CONSTRAINT chk_person_primary_email_not_blank CHECK (primary_email IS NULL OR btrim(primary_email) <> '') NOT VALID;

ALTER TABLE person_emails
    ADD CONSTRAINT chk_person_email_not_blank CHECK (btrim(email) <> '') NOT VALID;

ALTER TABLE student
    ADD CONSTRAINT chk_student_number_not_blank CHECK (btrim(student_number) <> '') NOT VALID;

ALTER TABLE teacher
    ADD CONSTRAINT chk_teacher_employee_number_not_blank CHECK (btrim(teacher_employee_number) <> '') NOT VALID;

ALTER TABLE administrator
    ADD CONSTRAINT chk_admin_employee_number_not_blank CHECK (btrim(admin_employee_number) <> '') NOT VALID;

ALTER TABLE field_of_study
    ADD CONSTRAINT chk_field_name_not_blank CHECK (btrim(name) <> '') NOT VALID;

ALTER TABLE student_group
    ADD CONSTRAINT chk_student_group_code_not_blank CHECK (btrim(code) <> '') NOT VALID;

ALTER TABLE subject
    ADD CONSTRAINT chk_subject_name_not_blank CHECK (btrim(name) <> '') NOT VALID;

ALTER TABLE room
    ADD CONSTRAINT chk_room_number_not_blank CHECK (btrim(room_number) <> '') NOT VALID;

ALTER TABLE notification
    ADD CONSTRAINT chk_notification_message_not_blank CHECK (btrim(message) <> '') NOT VALID;

CREATE OR REPLACE FUNCTION validate_class_meeting_status_transition()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.status = 'CANCELLED' THEN
            RAISE EXCEPTION 'Class meeting cannot be inserted already cancelled. Insert draft or scheduled, then cancel through lifecycle.';
        END IF;
        RETURN NEW;
    END IF;

    IF OLD.status IS NOT DISTINCT FROM NEW.status THEN
        RETURN NEW;
    END IF;

    IF OLD.status IN ('CANCELLED', 'COMPLETED') THEN
        RAISE EXCEPTION 'Class meeting % is in terminal status % and cannot change status', OLD.id, OLD.status;
    END IF;

    IF OLD.status = 'DRAFT' AND NEW.status NOT IN ('SCHEDULED', 'CANCELLED') THEN
        RAISE EXCEPTION 'Draft class meeting % can only become scheduled or cancelled', OLD.id;
    END IF;

    IF OLD.status = 'SCHEDULED' AND NEW.status NOT IN ('COMPLETED', 'CANCELLED') THEN
        RAISE EXCEPTION 'Scheduled class meeting % can only become completed or cancelled', OLD.id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION validate_class_meeting_calendar_consistency()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    expected_iso_day integer;
BEGIN
    expected_iso_day := CASE NEW.day_of_week
        WHEN 'MONDAY' THEN 1
        WHEN 'TUESDAY' THEN 2
        WHEN 'WEDNESDAY' THEN 3
        WHEN 'THURSDAY' THEN 4
        WHEN 'FRIDAY' THEN 5
        WHEN 'SATURDAY' THEN 6
        WHEN 'SUNDAY' THEN 7
        ELSE NULL
    END;

    IF expected_iso_day IS NULL
       OR EXTRACT(ISODOW FROM NEW.meeting_date)::integer <> expected_iso_day THEN
        RAISE EXCEPTION 'Class meeting % day_of_week % does not match meeting_date %', NEW.id, NEW.day_of_week, NEW.meeting_date;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION assert_class_meeting_details(checked_meeting_id bigint)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    meeting_record record;
    active_booking_count integer;
BEGIN
    IF checked_meeting_id IS NULL THEN
        RETURN;
    END IF;

    SELECT cm.id,
           cm.status,
           cm.meeting_mode,
           cm.online_meeting_link,
           cm.meeting_date,
           cm.start_time,
           cm.end_time
    INTO meeting_record
    FROM class_meeting cm
    WHERE cm.id = checked_meeting_id;

    IF NOT FOUND OR meeting_record.status IN ('DRAFT', 'CANCELLED') THEN
        RETURN;
    END IF;

    IF meeting_record.meeting_mode = 'ONLINE' THEN
        IF meeting_record.online_meeting_link IS NULL
           OR btrim(meeting_record.online_meeting_link) = '' THEN
            RAISE EXCEPTION 'Scheduled online class meeting % requires an online meeting link', checked_meeting_id;
        END IF;
        RETURN;
    END IF;

    SELECT COUNT(*)
    INTO active_booking_count
    FROM room_booking rb
    WHERE rb.class_meeting_id = checked_meeting_id
      AND rb.booking_status <> 'CANCELLED'
      AND rb.date = meeting_record.meeting_date
      AND rb.start_time = meeting_record.start_time
      AND rb.end_time = meeting_record.end_time;

    IF active_booking_count <> 1 THEN
        RAISE EXCEPTION 'Scheduled classroom class meeting % requires exactly one active room booking matching its date and time', checked_meeting_id;
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION validate_class_meeting_details_trigger()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_TABLE_NAME = 'class_meeting' THEN
        PERFORM assert_class_meeting_details(NEW.id);
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM assert_class_meeting_details(OLD.class_meeting_id);
    ELSIF TG_OP = 'UPDATE' THEN
        PERFORM assert_class_meeting_details(OLD.class_meeting_id);
        IF NEW.class_meeting_id IS DISTINCT FROM OLD.class_meeting_id THEN
            PERFORM assert_class_meeting_details(NEW.class_meeting_id);
        ELSE
            PERFORM assert_class_meeting_details(NEW.class_meeting_id);
        END IF;
    ELSE
        PERFORM assert_class_meeting_details(NEW.class_meeting_id);
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION assert_completed_meeting_attendance(checked_meeting_id bigint)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    meeting_status varchar(255);
    meeting_group_id bigint;
    expected_count integer;
    actual_count integer;
BEGIN
    IF checked_meeting_id IS NULL THEN
        RETURN;
    END IF;

    SELECT status, group_id
    INTO meeting_status, meeting_group_id
    FROM class_meeting
    WHERE id = checked_meeting_id;

    IF NOT FOUND OR meeting_status <> 'COMPLETED' THEN
        RETURN;
    END IF;

    SELECT COUNT(*)
    INTO expected_count
    FROM student
    WHERE group_id = meeting_group_id;

    SELECT COUNT(DISTINCT student_id)
    INTO actual_count
    FROM attendance
    WHERE class_meeting_id = checked_meeting_id;

    IF actual_count <> expected_count THEN
        RAISE EXCEPTION 'Completed class meeting % must have attendance for every current group student: expected %, found %',
            checked_meeting_id, expected_count, actual_count;
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION validate_completed_meeting_attendance_trigger()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_TABLE_NAME = 'class_meeting' THEN
        PERFORM assert_completed_meeting_attendance(NEW.id);
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM assert_completed_meeting_attendance(OLD.class_meeting_id);
    ELSIF TG_OP = 'UPDATE' THEN
        PERFORM assert_completed_meeting_attendance(OLD.class_meeting_id);
        IF NEW.class_meeting_id IS DISTINCT FROM OLD.class_meeting_id THEN
            PERFORM assert_completed_meeting_attendance(NEW.class_meeting_id);
        ELSE
            PERFORM assert_completed_meeting_attendance(NEW.class_meeting_id);
        END IF;
    ELSE
        PERFORM assert_completed_meeting_attendance(NEW.class_meeting_id);
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION validate_notification_task_status_transition()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.status <> 'PENDING' THEN
            RAISE EXCEPTION 'Scheduled notification task must be inserted as PENDING, got %', NEW.status;
        END IF;
        RETURN NEW;
    END IF;

    IF OLD.status IS NOT DISTINCT FROM NEW.status THEN
        RETURN NEW;
    END IF;

    IF OLD.status = 'PENDING' AND NEW.status NOT IN ('PROCESSING', 'CANCELLED') THEN
        RAISE EXCEPTION 'Pending notification task % can only become processing or cancelled', OLD.id;
    END IF;

    IF OLD.status = 'PROCESSING' AND NEW.status NOT IN ('SENT', 'FAILED') THEN
        RAISE EXCEPTION 'Processing notification task % can only become sent or failed', OLD.id;
    END IF;

    IF OLD.status = 'FAILED' AND NEW.status NOT IN ('PENDING', 'CANCELLED') THEN
        RAISE EXCEPTION 'Failed notification task % can only be retried as pending or cancelled', OLD.id;
    END IF;

    IF OLD.status IN ('SENT', 'CANCELLED') THEN
        RAISE EXCEPTION 'Notification task % is terminal with status % and cannot change status', OLD.id, OLD.status;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_class_meeting_status_transition
BEFORE INSERT OR UPDATE OF status ON class_meeting
FOR EACH ROW
EXECUTE FUNCTION validate_class_meeting_status_transition();

CREATE CONSTRAINT TRIGGER trg_class_meeting_calendar_consistency
AFTER INSERT OR UPDATE OF meeting_date, day_of_week ON class_meeting
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_class_meeting_calendar_consistency();

CREATE CONSTRAINT TRIGGER trg_class_meeting_required_details
AFTER INSERT OR UPDATE OF status, meeting_mode, online_meeting_link, meeting_date, start_time, end_time ON class_meeting
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_class_meeting_details_trigger();

CREATE CONSTRAINT TRIGGER trg_room_booking_required_meeting_details
AFTER INSERT OR UPDATE OF class_meeting_id, booking_status, date, start_time, end_time ON room_booking
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_class_meeting_details_trigger();

CREATE CONSTRAINT TRIGGER trg_room_booking_delete_required_meeting_details
AFTER DELETE ON room_booking
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_class_meeting_details_trigger();

CREATE CONSTRAINT TRIGGER trg_completed_meeting_attendance_from_meeting
AFTER INSERT OR UPDATE OF status, group_id ON class_meeting
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_completed_meeting_attendance_trigger();

CREATE CONSTRAINT TRIGGER trg_completed_meeting_attendance_from_attendance
AFTER INSERT OR UPDATE OF class_meeting_id, student_id ON attendance
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_completed_meeting_attendance_trigger();

CREATE CONSTRAINT TRIGGER trg_completed_meeting_attendance_delete_from_attendance
AFTER DELETE ON attendance
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_completed_meeting_attendance_trigger();

CREATE TRIGGER trg_notification_task_status_transition
BEFORE INSERT OR UPDATE OF status ON scheduled_notification_task
FOR EACH ROW
EXECUTE FUNCTION validate_notification_task_status_transition();
