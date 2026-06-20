package model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "semester_field",
        uniqueConstraints = @UniqueConstraint(columnNames = {"semester_id", "field_id"})
)
public class SemesterField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Semester semester;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Field field;

    @OneToMany(mappedBy = "semesterField")
    private Set<StudentGroup> groups = new HashSet<>();

    @OneToMany(mappedBy = "semesterField", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SemesterFieldSubject> curriculumAssignments = new HashSet<>();

    protected SemesterField() {
    }

    public SemesterField(Semester semester, Field field) {
        this.semester = semester;
        this.field = field;
    }

    public Long getId() {
        return id;
    }

    public Semester getSemester() {
        return semester;
    }

    public void setSemester(Semester semester) {
        this.semester = semester;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Set<StudentGroup> getGroups() {
        return groups;
    }

    public void setGroups(Set<StudentGroup> groups) {
        this.groups = groups;
    }

    public Set<SemesterFieldSubject> getCurriculumAssignments() {
        return curriculumAssignments;
    }

    public void setCurriculumAssignments(Set<SemesterFieldSubject> curriculumAssignments) {
        this.curriculumAssignments = curriculumAssignments;
    }
}
