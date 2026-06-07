package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "field_of_study")
public class Field {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "fields")
    private Set<Semester> semesters = new HashSet<>();

    @OneToMany(mappedBy = "field")
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
        return semesters;
    }

    public void setSemesters(Set<Semester> semesters) {
        this.semesters = semesters;
    }

    public void addSemester(Semester semester) {
        semesters.add(semester);
        semester.getFields().add(this);
    }

    public void removeSemester(Semester semester) {
        semesters.remove(semester);
        semester.getFields().remove(this);
    }

    public Set<SemesterFieldSubject> getCurriculumAssignments() {
        return curriculumAssignments;
    }

    public void setCurriculumAssignments(Set<SemesterFieldSubject> curriculumAssignments) {
        this.curriculumAssignments = curriculumAssignments;
    }
}
