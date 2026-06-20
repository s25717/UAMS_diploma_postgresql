package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "field_of_study")
public class Field {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "field")
    private Set<SemesterField> semesterFields = new HashSet<>();

    @Transient
    private Set<SemesterFieldSubject> curriculumAssignments = new HashSet<>();

    protected Field() {
    }

    public Field(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Semester> getSemesters() {
        return semesterFields.stream()
                .map(SemesterField::getSemester)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public void setSemesters(Set<Semester> semesters) {
        new HashSet<>(semesterFields).forEach(context -> removeSemester(context.getSemester()));
        if (semesters != null) {
            semesters.forEach(this::addSemester);
        }
    }

    public SemesterField addSemester(Semester semester) {
        return semester.addField(this);
    }

    public void removeSemester(Semester semester) {
        semester.removeField(this);
    }

    public Set<SemesterField> getSemesterFields() {
        return semesterFields;
    }

    public void setSemesterFields(Set<SemesterField> semesterFields) {
        this.semesterFields = semesterFields;
    }

    public Set<SemesterFieldSubject> getCurriculumAssignments() {
        return semesterFields.stream()
                .flatMap(context -> context.getCurriculumAssignments().stream())
                .collect(Collectors.toCollection(HashSet::new));
    }

    public void setCurriculumAssignments(Set<SemesterFieldSubject> curriculumAssignments) {
        this.curriculumAssignments = curriculumAssignments;
    }
}
