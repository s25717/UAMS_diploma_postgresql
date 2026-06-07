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
        uniqueConstraints = @UniqueConstraint(columnNames = {"semester_id", "field_id", "subject_id"})
)
public class SemesterFieldSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Semester semester;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Field field;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Subject subject;

    protected SemesterFieldSubject() {
    }

    public SemesterFieldSubject(Semester semester, Field field, Subject subject) {
        this.semester = semester;
        this.field = field;
        this.subject = subject;
    }

    public Long getId() {
        return id;
    }

    public Semester getSemester() {
        return semester;
    }

    public Field getField() {
        return field;
    }

    public Subject getSubject() {
        return subject;
    }
}
