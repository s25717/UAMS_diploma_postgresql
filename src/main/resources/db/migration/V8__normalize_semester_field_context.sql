DROP TRIGGER IF EXISTS trg_group_subject_curriculum ON subject_group;
DROP TRIGGER IF EXISTS trg_active_group_subject_has_teacher ON subject_group;
DROP TRIGGER IF EXISTS trg_group_academic_context_update ON student_group;
DROP TRIGGER IF EXISTS trg_weekly_schedule_group_semester ON weekly_schedule_entry;
DROP TRIGGER IF EXISTS trg_weekly_schedule_semester_subject ON weekly_schedule_entry;
DROP TRIGGER IF EXISTS trg_class_meeting_semester_subject ON class_meeting;
DROP TRIGGER IF EXISTS trg_active_semester_field_subject_has_teacher ON semester_field_subject;
DROP TRIGGER IF EXISTS trg_protect_used_curriculum_assignment ON semester_field_subject;
DROP TRIGGER IF EXISTS trg_semester_has_field_from_link ON semester_field;

ALTER TABLE weekly_schedule_entry
    DROP CONSTRAINT IF EXISTS fk_weekly_schedule_group_context,
    DROP CONSTRAINT IF EXISTS weekly_schedule_entry_semester_id_fkey;

ALTER TABLE student_group
    DROP CONSTRAINT IF EXISTS fk_student_group_semester_field,
    DROP CONSTRAINT IF EXISTS student_group_semester_id_fkey,
    DROP CONSTRAINT IF EXISTS uk_student_group_academic_context;

ALTER TABLE semester_field_subject
    DROP CONSTRAINT IF EXISTS fk_curriculum_semester_field,
    DROP CONSTRAINT IF EXISTS uk_semester_field_subject;

DROP INDEX IF EXISTS idx_student_group_field_semester;
DROP INDEX IF EXISTS idx_semester_field_field_id;
DROP INDEX IF EXISTS idx_curriculum_subject;
DROP INDEX IF EXISTS idx_curriculum_field_semester;
DROP INDEX IF EXISTS idx_weekly_schedule_academic_context;
DROP INDEX IF EXISTS idx_weekly_schedule_semester;
DROP INDEX IF EXISTS idx_weekly_schedule_group;
DROP INDEX IF EXISTS idx_subject_group_group_id;

ALTER TABLE semester_field
    DROP CONSTRAINT IF EXISTS semester_field_pkey;

ALTER TABLE semester_field
    ADD COLUMN id bigint;

CREATE SEQUENCE semester_field_id_seq OWNED BY semester_field.id;

UPDATE semester_field
SET id = nextval('semester_field_id_seq')
WHERE id IS NULL;

ALTER TABLE semester_field
    ALTER COLUMN id SET DEFAULT nextval('semester_field_id_seq'),
    ALTER COLUMN id SET NOT NULL,
    ADD CONSTRAINT semester_field_pkey PRIMARY KEY (id),
    ADD CONSTRAINT uk_semester_field_pair UNIQUE (semester_id, field_id);

ALTER TABLE student_group
    ADD COLUMN semester_field_id bigint;

UPDATE student_group g
SET semester_field_id = sf.id
FROM semester_field sf
WHERE sf.semester_id = g.semester_id
  AND sf.field_id = g.field_id;

ALTER TABLE student_group
    ALTER COLUMN semester_field_id SET NOT NULL,
    ADD CONSTRAINT student_group_semester_field_id_fkey
        FOREIGN KEY (semester_field_id)
        REFERENCES semester_field(id);

ALTER TABLE semester_field_subject
    ADD COLUMN semester_field_id bigint;

UPDATE semester_field_subject curriculum
SET semester_field_id = sf.id
FROM semester_field sf
WHERE sf.semester_id = curriculum.semester_id
  AND sf.field_id = curriculum.field_id;

ALTER TABLE semester_field_subject
    ALTER COLUMN semester_field_id SET NOT NULL,
    ADD CONSTRAINT semester_field_subject_semester_field_id_fkey
        FOREIGN KEY (semester_field_id)
        REFERENCES semester_field(id)
        ON DELETE CASCADE,
    ADD CONSTRAINT uk_semester_field_subject_context UNIQUE (semester_field_id, subject_id);

ALTER TABLE weekly_schedule_entry
    DROP COLUMN IF EXISTS semester_id,
    DROP COLUMN IF EXISTS field_id;

ALTER TABLE student_group
    DROP COLUMN IF EXISTS semester_id,
    DROP COLUMN IF EXISTS field_id;

ALTER TABLE semester_field_subject
    DROP COLUMN IF EXISTS semester_id,
    DROP COLUMN IF EXISTS field_id;

DROP TABLE IF EXISTS subject_group;

CREATE INDEX idx_semester_field_field_id
    ON semester_field (field_id, semester_id);
CREATE INDEX idx_student_group_semester_field_id
    ON student_group (semester_field_id);
CREATE INDEX idx_curriculum_subject
    ON semester_field_subject (subject_id, semester_field_id);
CREATE INDEX idx_curriculum_semester_field_id
    ON semester_field_subject (semester_field_id);
CREATE INDEX idx_weekly_schedule_group
    ON weekly_schedule_entry (group_id, day_of_week, start_time);

CREATE OR REPLACE FUNCTION validate_class_meeting_semester_subject()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM student_group g
        JOIN semester_field_subject curriculum
          ON curriculum.semester_field_id = g.semester_field_id
        WHERE g.id = NEW.group_id
          AND curriculum.subject_id = NEW.subject_id
    ) THEN
        RAISE EXCEPTION
            'Subject % is not available for the selected group semester and field',
            NEW.subject_id;
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
        FROM student_group g
        JOIN semester_field_subject curriculum
          ON curriculum.semester_field_id = g.semester_field_id
        WHERE g.id = NEW.group_id
          AND curriculum.subject_id = NEW.subject_id
    ) THEN
        RAISE EXCEPTION
            'Subject % is not available for the selected group semester and field',
            NEW.subject_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION protect_used_curriculum_assignment()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM class_meeting cm
        JOIN student_group g ON g.id = cm.group_id
        WHERE g.semester_field_id = OLD.semester_field_id
          AND cm.subject_id = OLD.subject_id
    ) OR EXISTS (
        SELECT 1
        FROM weekly_schedule_entry w
        JOIN student_group g ON g.id = w.group_id
        WHERE g.semester_field_id = OLD.semester_field_id
          AND w.subject_id = OLD.subject_id
    ) THEN
        RAISE EXCEPTION
            'Cannot remove curriculum subject % while it is used by meetings or weekly schedules',
            OLD.subject_id;
    END IF;
    RETURN OLD;
END;
$$;

CREATE OR REPLACE FUNCTION validate_semester_has_field()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    checked_semester_id bigint;
BEGIN
    IF TG_TABLE_NAME = 'semester' THEN
        checked_semester_id := NEW.id;
    ELSIF TG_OP = 'DELETE' THEN
        checked_semester_id := OLD.semester_id;
    ELSE
        checked_semester_id := NEW.semester_id;
    END IF;

    IF EXISTS (SELECT 1 FROM semester WHERE id = checked_semester_id)
       AND NOT EXISTS (
           SELECT 1
           FROM semester_field
           WHERE semester_id = checked_semester_id
       ) THEN
        RAISE EXCEPTION
            'Semester % must belong to at least one field of study',
            checked_semester_id;
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
        FROM semester_field_subject
        WHERE subject_id = subject_to_check;

        IF active_count > 0 THEN
            SELECT COUNT(*)
            INTO teacher_count
            FROM teacher_subject
            WHERE subject_id = subject_to_check;

            IF teacher_count < 1 THEN
                RAISE EXCEPTION
                    'Active subject % must have at least one qualified teacher',
                    subject_to_check;
            END IF;
        END IF;
    END IF;

    IF TG_OP = 'DELETE' THEN
        subject_to_check := OLD.subject_id;
    ELSE
        subject_to_check := NEW.subject_id;
    END IF;

    SELECT COUNT(*)
    INTO active_count
    FROM semester_field_subject
    WHERE subject_id = subject_to_check;

    IF active_count > 0 THEN
        SELECT COUNT(*)
        INTO teacher_count
        FROM teacher_subject
        WHERE subject_id = subject_to_check;

        IF teacher_count < 1 THEN
            RAISE EXCEPTION
                'Active subject % must have at least one qualified teacher',
                subject_to_check;
        END IF;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_semester_has_field_from_link
AFTER UPDATE OR DELETE ON semester_field
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_semester_has_field();

CREATE CONSTRAINT TRIGGER trg_protect_used_curriculum_assignment
AFTER DELETE ON semester_field_subject
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION protect_used_curriculum_assignment();

CREATE CONSTRAINT TRIGGER trg_class_meeting_semester_subject
AFTER INSERT OR UPDATE OF subject_id, group_id ON class_meeting
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_class_meeting_semester_subject();

CREATE CONSTRAINT TRIGGER trg_weekly_schedule_semester_subject
AFTER INSERT OR UPDATE OF subject_id, group_id ON weekly_schedule_entry
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_weekly_schedule_semester_subject();

CREATE CONSTRAINT TRIGGER trg_active_semester_field_subject_has_teacher
AFTER INSERT OR UPDATE OF subject_id ON semester_field_subject
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_active_subject_has_teacher();
