package model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.time.LocalDate;
import java.util.Set;

@Entity
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(1)
    @Max(8)
    @Column(nullable = false)
    private int number;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(min = 1)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "semester_field",
            joinColumns = @JoinColumn(name = "semester_id"),
            inverseJoinColumns = @JoinColumn(name = "field_id")
    )
    private Set<Field> fields = new HashSet<>();

    @Valid
    @OneToMany(mappedBy = "semester", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StudentGroup> groups = new HashSet<>();

    @OneToMany(mappedBy = "semester", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WeeklyScheduleEntry> weeklyScheduleEntries = new HashSet<>();

    @OneToMany(mappedBy = "semester", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SemesterFieldSubject> curriculumAssignments = new HashSet<>();

    protected Semester() {
    }

    public Semester(int number) {
        this.number = number;
        this.startDate = LocalDate.now().minusWeeks(4);
        this.endDate = LocalDate.now().plusWeeks(12);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Set<Field> getFields() {
        return fields;
    }

    public void setFields(Set<Field> fields) {
        this.fields = fields;
    }

    public Set<StudentGroup> getGroups() {
        return groups;
    }

    public void setGroups(Set<StudentGroup> groups) {
        this.groups = groups;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Set<WeeklyScheduleEntry> getWeeklyScheduleEntries() {
        return weeklyScheduleEntries;
    }

    public void setWeeklyScheduleEntries(Set<WeeklyScheduleEntry> weeklyScheduleEntries) {
        this.weeklyScheduleEntries = weeklyScheduleEntries;
    }

    public Set<SemesterFieldSubject> getCurriculumAssignments() {
        return curriculumAssignments;
    }

    public void setCurriculumAssignments(Set<SemesterFieldSubject> curriculumAssignments) {
        this.curriculumAssignments = curriculumAssignments;
    }

    public void addGroup(StudentGroup group) {
        groups.add(group);
        group.setSemester(this);
    }

    public void removeGroup(StudentGroup group) {
        groups.remove(group);
        group.setSemester(null);
    }

    public void addField(Field field) {
        fields.add(field);
        field.getSemesters().add(this);
    }

    public void removeField(Field field) {
        fields.remove(field);
        field.getSemesters().remove(this);
    }

    public SemesterFieldSubject assignSubject(Field field, Subject subject) {
        if (!fields.contains(field)) {
            throw new IllegalArgumentException("Field must belong to the semester before assigning its subjects.");
        }
        return curriculumAssignments.stream()
                .filter(assignment -> assignment.getField().equals(field)
                        && assignment.getSubject().equals(subject))
                .findFirst()
                .orElseGet(() -> {
                    SemesterFieldSubject assignment = new SemesterFieldSubject(this, field, subject);
                    curriculumAssignments.add(assignment);
                    field.getCurriculumAssignments().add(assignment);
                    subject.getCurriculumAssignments().add(assignment);
                    return assignment;
                });
    }

    public void removeSubject(Field field, Subject subject) {
        curriculumAssignments.removeIf(assignment -> {
            boolean matches = assignment.getField().equals(field)
                    && assignment.getSubject().equals(subject);
            if (matches) {
                field.getCurriculumAssignments().remove(assignment);
                subject.getCurriculumAssignments().remove(assignment);
            }
            return matches;
        });
    }
}
