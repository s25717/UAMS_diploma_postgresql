DROP TRIGGER IF EXISTS trg_weekly_schedule_teacher_subject ON weekly_schedule_entry;
DROP TRIGGER IF EXISTS trg_weekly_schedule_subject_group ON weekly_schedule_entry;
DROP TRIGGER IF EXISTS trg_weekly_schedule_group_semester ON weekly_schedule_entry;
DROP TRIGGER IF EXISTS trg_weekly_schedule_semester_subject ON weekly_schedule_entry;
DROP TRIGGER IF EXISTS trg_protect_used_curriculum_assignment ON semester_field_subject;
DROP TRIGGER IF EXISTS trg_class_meeting_availability ON class_meeting;

ALTER TABLE weekly_schedule_entry
    DROP CONSTRAINT IF EXISTS weekly_schedule_entry_group_id_fkey,
    DROP CONSTRAINT IF EXISTS weekly_schedule_entry_subject_id_fkey,
    DROP CONSTRAINT IF EXISTS weekly_schedule_entry_teacher_id_fkey,
    DROP CONSTRAINT IF EXISTS weekly_schedule_entry_room_id_fkey;

ALTER TABLE class_meeting
    DROP CONSTRAINT IF EXISTS class_meeting_schedule_entry_id_fkey;

DROP INDEX IF EXISTS idx_weekly_schedule_group;
DROP INDEX IF EXISTS idx_weekly_schedule_teacher;
DROP INDEX IF EXISTS idx_weekly_schedule_semester;

WITH ranked_weekly_entries AS (
    SELECT id,
           MIN(id) OVER (
               PARTITION BY group_id,
                            subject_id,
                            teacher_id,
                            class_type,
                            meeting_mode,
                            day_of_week,
                            start_time,
                            end_time,
                            room_id,
                            online_meeting_link
           ) AS keeper_id
    FROM weekly_schedule_entry
)
UPDATE class_meeting cm
SET schedule_entry_id = ranked.keeper_id
FROM ranked_weekly_entries ranked
WHERE cm.schedule_entry_id = ranked.id
  AND ranked.id <> ranked.keeper_id;

WITH ranked_weekly_entries AS (
    SELECT id,
           MIN(id) OVER (
               PARTITION BY group_id,
                            subject_id,
                            teacher_id,
                            class_type,
                            meeting_mode,
                            day_of_week,
                            start_time,
                            end_time,
                            room_id,
                            online_meeting_link
           ) AS keeper_id
    FROM weekly_schedule_entry
)
DELETE FROM weekly_schedule_entry w
USING ranked_weekly_entries ranked
WHERE w.id = ranked.id
  AND ranked.id <> ranked.keeper_id;

ALTER TABLE weekly_schedule_entry
    ADD COLUMN source_class_meeting_id bigint;

UPDATE weekly_schedule_entry w
SET source_class_meeting_id = (
    SELECT cm.id
    FROM class_meeting cm
    WHERE cm.schedule_entry_id = w.id
      AND cm.status IN ('DRAFT', 'SCHEDULED')
    ORDER BY cm.meeting_date, cm.start_time
    LIMIT 1
)
WHERE w.source_class_meeting_id IS NULL;

DO $$
DECLARE
    entry record;
    source_id bigint;
    base_date date;
    source_date date;
BEGIN
    FOR entry IN
        SELECT w.*,
               r.room_number,
               COALESCE(sem.start_date, CURRENT_DATE) AS semester_start_date
        FROM weekly_schedule_entry w
        JOIN student_group g ON g.id = w.group_id
        JOIN semester_field sf ON sf.id = g.semester_field_id
        JOIN semester sem ON sem.id = sf.semester_id
        LEFT JOIN room r ON r.id = w.room_id
        WHERE w.source_class_meeting_id IS NULL
    LOOP
        base_date := entry.semester_start_date;
        source_date :=
            base_date
            + (((CASE entry.day_of_week
                    WHEN 'MONDAY' THEN 1
                    WHEN 'TUESDAY' THEN 2
                    WHEN 'WEDNESDAY' THEN 3
                    WHEN 'THURSDAY' THEN 4
                    WHEN 'FRIDAY' THEN 5
                    WHEN 'SATURDAY' THEN 6
                    WHEN 'SUNDAY' THEN 7
                END - EXTRACT(ISODOW FROM base_date)::integer + 7) % 7)::integer);

        INSERT INTO class_meeting (
            room,
            location,
            online_meeting_link,
            meeting_date,
            day_of_week,
            start_time,
            end_time,
            class_type,
            meeting_mode,
            status,
            subject_id,
            teacher_id,
            group_id,
            schedule_entry_id,
            comment
        )
        VALUES (
            entry.room_number,
            entry.room_number,
            entry.online_meeting_link,
            source_date,
            entry.day_of_week,
            entry.start_time,
            entry.end_time,
            entry.class_type,
            entry.meeting_mode,
            'DRAFT',
            entry.subject_id,
            entry.teacher_id,
            entry.group_id,
            entry.id,
            'Draft source meeting created from legacy weekly schedule entry.'
        )
        RETURNING id INTO source_id;

        IF entry.room_id IS NOT NULL THEN
            INSERT INTO room_booking (
                date,
                start_time,
                end_time,
                booking_status,
                room_id,
                class_meeting_id
            )
            VALUES (
                source_date,
                entry.start_time,
                entry.end_time,
                'CONFIRMED',
                entry.room_id,
                source_id
            );
        END IF;

        UPDATE weekly_schedule_entry
        SET source_class_meeting_id = source_id
        WHERE id = entry.id;
    END LOOP;
END;
$$;

ALTER TABLE weekly_schedule_entry
    ALTER COLUMN source_class_meeting_id SET NOT NULL,
    ADD CONSTRAINT weekly_schedule_entry_source_class_meeting_id_key UNIQUE (source_class_meeting_id),
    ADD CONSTRAINT weekly_schedule_entry_source_class_meeting_id_fkey
        FOREIGN KEY (source_class_meeting_id)
        REFERENCES class_meeting(id)
        ON DELETE CASCADE;

UPDATE class_meeting cm
SET schedule_entry_id = w.id
FROM weekly_schedule_entry w
WHERE w.source_class_meeting_id = cm.id
  AND cm.schedule_entry_id IS NULL;

ALTER TABLE class_meeting
    ADD CONSTRAINT class_meeting_schedule_entry_id_fkey
        FOREIGN KEY (schedule_entry_id)
        REFERENCES weekly_schedule_entry(id)
        ON DELETE SET NULL;

ALTER TABLE weekly_schedule_entry
    DROP COLUMN IF EXISTS group_id,
    DROP COLUMN IF EXISTS subject_id,
    DROP COLUMN IF EXISTS teacher_id,
    DROP COLUMN IF EXISTS class_type,
    DROP COLUMN IF EXISTS meeting_mode,
    DROP COLUMN IF EXISTS day_of_week,
    DROP COLUMN IF EXISTS start_time,
    DROP COLUMN IF EXISTS end_time,
    DROP COLUMN IF EXISTS room_id,
    DROP COLUMN IF EXISTS online_meeting_link;

CREATE INDEX idx_weekly_schedule_source_class_meeting
    ON weekly_schedule_entry (source_class_meeting_id);

CREATE INDEX idx_class_meeting_group_week_slot
    ON class_meeting (group_id, day_of_week, start_time);

CREATE INDEX idx_class_meeting_teacher_week_slot
    ON class_meeting (teacher_id, day_of_week, start_time);

CREATE INDEX idx_class_meeting_type_mode
    ON class_meeting (class_type, meeting_mode);

CREATE OR REPLACE FUNCTION validate_weekly_schedule_source()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    src record;
    group_size integer;
BEGIN
    SELECT cm.id,
           cm.group_id,
           cm.subject_id,
           cm.teacher_id,
           cm.day_of_week,
           cm.start_time,
           cm.end_time,
           cm.status,
           rb.room_id,
           r.capacity AS room_capacity
    INTO src
    FROM class_meeting cm
    LEFT JOIN room_booking rb
      ON rb.class_meeting_id = cm.id
     AND rb.booking_status <> 'CANCELLED'
    LEFT JOIN room r ON r.id = rb.room_id
    WHERE cm.id = NEW.source_class_meeting_id
    LIMIT 1;

    IF src.status IN ('CANCELLED', 'COMPLETED') THEN
        RAISE EXCEPTION 'Only draft or scheduled class meetings can become weekly schedule entries';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM teacher_subject ts
        WHERE ts.teacher_id = src.teacher_id
          AND ts.subject_id = src.subject_id
    ) THEN
        RAISE EXCEPTION 'Source class meeting teacher % is not qualified for subject %',
            src.teacher_id, src.subject_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM student_group g
        JOIN semester_field_subject curriculum
          ON curriculum.semester_field_id = g.semester_field_id
        WHERE g.id = src.group_id
          AND curriculum.subject_id = src.subject_id
    ) THEN
        RAISE EXCEPTION 'Source class meeting subject % is not available in the group curriculum',
            src.subject_id;
    END IF;

    IF src.room_id IS NOT NULL THEN
        SELECT COUNT(*)
        INTO group_size
        FROM student
        WHERE group_id = src.group_id;

        IF src.room_capacity < group_size THEN
            RAISE EXCEPTION 'Source class meeting room capacity % is lower than group size %',
                src.room_capacity, group_size;
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM weekly_schedule_entry e
        JOIN class_meeting other ON other.id = e.source_class_meeting_id
        WHERE e.id <> NEW.id
          AND other.group_id = src.group_id
          AND other.day_of_week = src.day_of_week
          AND other.start_time < src.end_time
          AND other.end_time > src.start_time
    ) THEN
        RAISE EXCEPTION 'Group already has a weekly schedule entry at this time';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM weekly_schedule_entry e
        JOIN class_meeting other ON other.id = e.source_class_meeting_id
        WHERE e.id <> NEW.id
          AND other.teacher_id = src.teacher_id
          AND other.day_of_week = src.day_of_week
          AND other.start_time < src.end_time
          AND other.end_time > src.start_time
    ) THEN
        RAISE EXCEPTION 'Teacher already has a weekly schedule entry at this time';
    END IF;

    IF src.room_id IS NOT NULL AND EXISTS (
        SELECT 1
        FROM weekly_schedule_entry e
        JOIN class_meeting other ON other.id = e.source_class_meeting_id
        JOIN room_booking rb
          ON rb.class_meeting_id = other.id
         AND rb.booking_status <> 'CANCELLED'
        WHERE e.id <> NEW.id
          AND rb.room_id = src.room_id
          AND other.day_of_week = src.day_of_week
          AND other.start_time < src.end_time
          AND other.end_time > src.start_time
    ) THEN
        RAISE EXCEPTION 'Room already has a weekly schedule entry at this time';
    END IF;

    RETURN NEW;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_weekly_schedule_source
AFTER INSERT OR UPDATE OF source_class_meeting_id ON weekly_schedule_entry
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_weekly_schedule_source();

UPDATE weekly_schedule_entry
SET source_class_meeting_id = source_class_meeting_id;

CREATE OR REPLACE FUNCTION validate_class_meeting_availability()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.status = 'CANCELLED' THEN
        RETURN NEW;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM class_meeting other
        WHERE other.id <> NEW.id
          AND other.status <> 'CANCELLED'
          AND other.meeting_date = NEW.meeting_date
          AND other.start_time < NEW.end_time
          AND other.end_time > NEW.start_time
          AND other.group_id = NEW.group_id
    ) THEN
        RAISE EXCEPTION 'Group % already has an overlapping class meeting on %',
            NEW.group_id, NEW.meeting_date;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM class_meeting other
        WHERE other.id <> NEW.id
          AND other.status <> 'CANCELLED'
          AND other.meeting_date = NEW.meeting_date
          AND other.start_time < NEW.end_time
          AND other.end_time > NEW.start_time
          AND other.teacher_id = NEW.teacher_id
    ) THEN
        RAISE EXCEPTION 'Teacher % already has an overlapping class meeting on %',
            NEW.teacher_id, NEW.meeting_date;
    END IF;

    RETURN NEW;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_class_meeting_availability
AFTER INSERT OR UPDATE OF group_id, teacher_id, meeting_date, start_time, end_time, status ON class_meeting
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_class_meeting_availability();

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
        JOIN class_meeting source ON source.id = w.source_class_meeting_id
        JOIN student_group g ON g.id = source.group_id
        WHERE g.semester_field_id = OLD.semester_field_id
          AND source.subject_id = OLD.subject_id
    ) THEN
        RAISE EXCEPTION
            'Cannot remove curriculum subject % while it is used by meetings or weekly schedules',
            OLD.subject_id;
    END IF;
    RETURN OLD;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_protect_used_curriculum_assignment
AFTER DELETE ON semester_field_subject
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION protect_used_curriculum_assignment();
