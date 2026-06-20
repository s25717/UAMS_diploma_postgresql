package model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "semester_field_subject",
        uniqueConstraints = @UniqueConstraint(columnNames = {"semester_field_id", "subject_id"})
)
public class SemesterFieldSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private SemesterField semesterField;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Subject subject;

    protected SemesterFieldSubject() {
    }

    public SemesterFieldSubject(SemesterField semesterField, Subject subject) {
        this.semesterField = semesterField;
        this.subject = subject;
    }

    public SemesterFieldSubject(Semester semester, Field field, Subject subject) {
        this(semester.getSemesterField(field), subject);
    }

    public Long getId() {
        return id;
    }

    public Semester getSemester() {
        return semesterField == null ? null : semesterField.getSemester();
    }

    public Field getField() {
        return semesterField == null ? null : semesterField.getField();
    }

    public SemesterField getSemesterField() {
        return semesterField;
    }

    public Subject getSubject() {
        return subject;
    }
}
