-- Final cleanup after the semester-field normalization.
-- These definitions make the active database rules match the final ERD:
-- StudentGroup -> SemesterField -> Semester/Field, with no direct subject_group
-- or student_group.semester_id dependency.

DROP TRIGGER IF EXISTS trg_class_meeting_subject_group ON class_meeting;

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
    JOIN semester_field sf ON sf.id = g.semester_field_id
    JOIN semester sem ON sem.id = sf.semester_id
    WHERE g.id = NEW.group_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Class meeting group % has no semester-field academic context', NEW.group_id;
    END IF;

    IF semester_start IS NOT NULL AND NEW.meeting_date < semester_start THEN
        RAISE EXCEPTION 'Class meeting date % is before semester start %', NEW.meeting_date, semester_start;
    END IF;
    IF semester_end IS NOT NULL AND NEW.meeting_date > semester_end THEN
        RAISE EXCEPTION 'Class meeting date % is after semester end %', NEW.meeting_date, semester_end;
    END IF;

    RETURN NEW;
END;
$$;

DROP FUNCTION IF EXISTS validate_subject_group_assignment();
DROP FUNCTION IF EXISTS validate_group_subject_curriculum();
DROP FUNCTION IF EXISTS validate_group_academic_context_update();
DROP FUNCTION IF EXISTS validate_weekly_schedule_group_semester();
