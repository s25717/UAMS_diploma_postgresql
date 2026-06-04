ALTER TABLE attendance_report
    ADD COLUMN exported_at timestamp;

UPDATE scheduled_notification_task
SET recipient_id = (SELECT id FROM administrator ORDER BY id LIMIT 1)
WHERE recipient_id IS NULL
  AND EXISTS (SELECT 1 FROM administrator);

ALTER TABLE scheduled_notification_task
    ADD CONSTRAINT chk_scheduled_notification_task_recipient_required
    CHECK (recipient_id IS NOT NULL) NOT VALID;

ALTER TABLE notification
    ADD CONSTRAINT chk_notification_recipient_required
    CHECK (recipient_id IS NOT NULL) NOT VALID,
    ADD CONSTRAINT chk_notification_title_required
    CHECK (title IS NOT NULL AND btrim(title) <> '') NOT VALID;

CREATE OR REPLACE FUNCTION validate_person_email_count()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    checked_person_id bigint;
    email_count integer;
BEGIN
    IF TG_TABLE_NAME = 'person' THEN
        IF TG_OP = 'DELETE' THEN
            checked_person_id := OLD.id;
        ELSE
            checked_person_id := NEW.id;
        END IF;
    ELSE
        IF TG_OP = 'DELETE' THEN
            checked_person_id := OLD.person_id;
        ELSE
            checked_person_id := NEW.person_id;
        END IF;
    END IF;

    IF checked_person_id IS NULL OR NOT EXISTS (SELECT 1 FROM person WHERE id = checked_person_id) THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RETURN NEW;
    END IF;

    SELECT COUNT(*)
    INTO email_count
    FROM person_emails
    WHERE person_id = checked_person_id;

    IF email_count < 1 OR email_count > 3 THEN
        RAISE EXCEPTION 'Person % must have between 1 and 3 email addresses, but has %', checked_person_id, email_count;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION validate_teacher_subject_count()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    checked_teacher_id bigint;
    subject_count integer;
BEGIN
    IF TG_TABLE_NAME = 'teacher' THEN
        IF TG_OP = 'DELETE' THEN
            checked_teacher_id := OLD.id;
        ELSE
            checked_teacher_id := NEW.id;
        END IF;
    ELSE
        IF TG_OP = 'DELETE' THEN
            checked_teacher_id := OLD.teacher_id;
        ELSE
            checked_teacher_id := NEW.teacher_id;
        END IF;
    END IF;

    IF checked_teacher_id IS NULL OR NOT EXISTS (SELECT 1 FROM teacher WHERE id = checked_teacher_id) THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RETURN NEW;
    END IF;

    SELECT COUNT(*)
    INTO subject_count
    FROM teacher_subject
    WHERE teacher_id = checked_teacher_id;

    IF subject_count < 1 OR subject_count > 5 THEN
        RAISE EXCEPTION 'Teacher % must be qualified for 1 to 5 subjects, but has %', checked_teacher_id, subject_count;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION prevent_exported_report_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.exported_at IS NOT NULL THEN
        RAISE EXCEPTION 'Exported attendance report % is immutable. Generate a new report instead.', OLD.id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION prevent_exported_report_line_change()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    parent_exported_at timestamp;
    checked_report_id bigint;
BEGIN
    IF TG_OP = 'DELETE' THEN
        checked_report_id := OLD.report_id;
    ELSE
        checked_report_id := NEW.report_id;
    END IF;

    SELECT exported_at
    INTO parent_exported_at
    FROM attendance_report
    WHERE id = checked_report_id;

    IF parent_exported_at IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot change lines or semester links for exported attendance report %', checked_report_id;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION validate_active_subject_has_teacher()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    subject_to_check bigint;
    teacher_count integer;
    active_count integer;
BEGIN
    IF TG_TABLE_NAME = 'teacher_subject'
       AND TG_OP = 'UPDATE'
       AND OLD.subject_id IS DISTINCT FROM NEW.subject_id THEN
        subject_to_check := OLD.subject_id;

        SELECT COUNT(*)
        INTO active_count
        FROM (
            SELECT subject_id FROM semester_subject WHERE subject_id = subject_to_check
            UNION ALL
            SELECT subject_id FROM subject_group WHERE subject_id = subject_to_check
        ) active_subject;

        IF active_count > 0 THEN
            SELECT COUNT(*)
            INTO teacher_count
            FROM teacher_subject
            WHERE subject_id = subject_to_check;

            IF teacher_count < 1 THEN
                RAISE EXCEPTION 'Active subject % must have at least one qualified teacher', subject_to_check;
            END IF;
        END IF;
    END IF;

    IF TG_OP = 'DELETE' THEN
        subject_to_check := OLD.subject_id;
    ELSE
        subject_to_check := NEW.subject_id;
    END IF;

    IF subject_to_check IS NULL THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RETURN NEW;
    END IF;

    SELECT COUNT(*)
    INTO active_count
    FROM (
        SELECT subject_id FROM semester_subject WHERE subject_id = subject_to_check
        UNION ALL
        SELECT subject_id FROM subject_group WHERE subject_id = subject_to_check
    ) active_subject;

    IF active_count = 0 THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RETURN NEW;
    END IF;

    SELECT COUNT(*)
    INTO teacher_count
    FROM teacher_subject
    WHERE subject_id = subject_to_check;

    IF teacher_count < 1 THEN
        RAISE EXCEPTION 'Active subject % must have at least one qualified teacher', subject_to_check;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_person_email_count_from_person
AFTER INSERT ON person
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_person_email_count();

CREATE CONSTRAINT TRIGGER trg_person_email_count_from_emails
AFTER INSERT OR UPDATE OR DELETE ON person_emails
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_person_email_count();

CREATE CONSTRAINT TRIGGER trg_teacher_subject_count_from_teacher
AFTER INSERT ON teacher
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_teacher_subject_count();

CREATE CONSTRAINT TRIGGER trg_teacher_subject_count_from_teacher_subject
AFTER INSERT OR UPDATE OR DELETE ON teacher_subject
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_teacher_subject_count();

CREATE TRIGGER trg_prevent_exported_report_update
BEFORE UPDATE ON attendance_report
FOR EACH ROW
EXECUTE FUNCTION prevent_exported_report_update();

CREATE TRIGGER trg_prevent_exported_report_line_insert_update_delete
BEFORE INSERT OR UPDATE OR DELETE ON report_line
FOR EACH ROW
EXECUTE FUNCTION prevent_exported_report_line_change();

CREATE TRIGGER trg_prevent_exported_report_semester_insert_delete
BEFORE INSERT OR DELETE ON attendance_report_semesters
FOR EACH ROW
EXECUTE FUNCTION prevent_exported_report_line_change();

CREATE CONSTRAINT TRIGGER trg_active_semester_subject_has_teacher
AFTER INSERT OR UPDATE OF subject_id ON semester_subject
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_active_subject_has_teacher();

CREATE CONSTRAINT TRIGGER trg_active_group_subject_has_teacher
AFTER INSERT OR UPDATE OF subject_id ON subject_group
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_active_subject_has_teacher();

CREATE CONSTRAINT TRIGGER trg_teacher_subject_delete_preserves_active_subject_teacher
AFTER UPDATE OR DELETE ON teacher_subject
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_active_subject_has_teacher();
