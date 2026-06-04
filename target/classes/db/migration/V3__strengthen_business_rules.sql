ALTER TABLE student_group
    ADD COLUMN max_size integer NOT NULL DEFAULT 30,
    ADD CONSTRAINT chk_student_group_max_size CHECK (max_size >= 1);

ALTER TABLE attendance
    ADD COLUMN registration_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE notification
    ADD COLUMN created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE semester_subject (
    semester_id bigint NOT NULL REFERENCES semester(id) ON DELETE CASCADE,
    subject_id bigint NOT NULL REFERENCES subject(id) ON DELETE CASCADE,
    PRIMARY KEY (semester_id, subject_id)
);

INSERT INTO semester_subject (semester_id, subject_id)
SELECT DISTINCT g.semester_id, sg.subject_id
FROM subject_group sg
JOIN student_group g ON g.id = sg.group_id
ON CONFLICT DO NOTHING;

CREATE INDEX idx_semester_subject_subject_id ON semester_subject (subject_id);
CREATE INDEX idx_notification_created_at ON notification (created_at DESC);
CREATE INDEX idx_attendance_registration_time ON attendance (registration_time DESC);

CREATE OR REPLACE FUNCTION validate_student_group_max_size()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    current_size integer;
    allowed_size integer;
BEGIN
    IF NEW.group_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT COUNT(*), g.max_size
    INTO current_size, allowed_size
    FROM student s
    JOIN student_group g ON g.id = NEW.group_id
    WHERE s.group_id = NEW.group_id
    GROUP BY g.max_size;

    IF current_size > allowed_size THEN
        RAISE EXCEPTION 'Student group % exceeds maximum size %', NEW.group_id, allowed_size;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION validate_group_max_size_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    current_size integer;
BEGIN
    SELECT COUNT(*)
    INTO current_size
    FROM student
    WHERE group_id = NEW.id;

    IF current_size > NEW.max_size THEN
        RAISE EXCEPTION 'Student group % already has % students and cannot be limited to %', NEW.id, current_size, NEW.max_size;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION validate_class_meeting_semester_period()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    semester_start date;
    semester_end date;
BEGIN
    SELECT sem.start_date, sem.end_date
    INTO semester_start, semester_end
    FROM student_group g
    JOIN semester sem ON sem.id = g.semester_id
    WHERE g.id = NEW.group_id;

    IF semester_start IS NOT NULL AND NEW.meeting_date < semester_start THEN
        RAISE EXCEPTION 'Class meeting date % is before semester start %', NEW.meeting_date, semester_start;
    END IF;
    IF semester_end IS NOT NULL AND NEW.meeting_date > semester_end THEN
        RAISE EXCEPTION 'Class meeting date % is after semester end %', NEW.meeting_date, semester_end;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION validate_class_meeting_semester_subject()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM student_group g
        JOIN semester_subject ss ON ss.semester_id = g.semester_id
        WHERE g.id = NEW.group_id
          AND ss.subject_id = NEW.subject_id
    ) THEN
        RAISE EXCEPTION 'Subject % is not available in the selected group semester', NEW.subject_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION validate_weekly_schedule_group_semester()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM student_group g
        WHERE g.id = NEW.group_id
          AND g.semester_id = NEW.semester_id
    ) THEN
        RAISE EXCEPTION 'Weekly schedule semester must match the selected group semester';
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION validate_weekly_schedule_semester_subject()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM semester_subject ss
        WHERE ss.semester_id = NEW.semester_id
          AND ss.subject_id = NEW.subject_id
    ) THEN
        RAISE EXCEPTION 'Subject % is not available in semester %', NEW.subject_id, NEW.semester_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION validate_room_booking_capacity()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    room_capacity integer;
    group_size integer;
BEGIN
    IF NEW.class_meeting_id IS NULL OR NEW.booking_status = 'CANCELLED' THEN
        RETURN NEW;
    END IF;

    SELECT r.capacity, COUNT(s.id)
    INTO room_capacity, group_size
    FROM room r
    JOIN class_meeting cm ON cm.id = NEW.class_meeting_id
    LEFT JOIN student s ON s.group_id = cm.group_id
    WHERE r.id = NEW.room_id
    GROUP BY r.capacity;

    IF group_size > room_capacity THEN
        RAISE EXCEPTION 'Room capacity % is lower than group size %', room_capacity, group_size;
    END IF;

    RETURN NEW;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_student_group_max_size
AFTER INSERT OR UPDATE OF group_id ON student
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_student_group_max_size();

CREATE CONSTRAINT TRIGGER trg_group_max_size_update
AFTER INSERT OR UPDATE OF max_size ON student_group
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_group_max_size_update();

CREATE CONSTRAINT TRIGGER trg_class_meeting_semester_period
AFTER INSERT OR UPDATE OF meeting_date, group_id ON class_meeting
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_class_meeting_semester_period();

CREATE CONSTRAINT TRIGGER trg_class_meeting_semester_subject
AFTER INSERT OR UPDATE OF subject_id, group_id ON class_meeting
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_class_meeting_semester_subject();

CREATE CONSTRAINT TRIGGER trg_weekly_schedule_group_semester
AFTER INSERT OR UPDATE OF group_id, semester_id ON weekly_schedule_entry
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_weekly_schedule_group_semester();

CREATE CONSTRAINT TRIGGER trg_weekly_schedule_semester_subject
AFTER INSERT OR UPDATE OF subject_id, semester_id ON weekly_schedule_entry
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_weekly_schedule_semester_subject();

CREATE CONSTRAINT TRIGGER trg_room_booking_capacity
AFTER INSERT OR UPDATE OF room_id, class_meeting_id, booking_status ON room_booking
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_room_booking_capacity();
