package model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Valid
    @Size(min = 1)
    @OneToMany(mappedBy = "semester", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SemesterField> semesterFields = new HashSet<>();

    @Transient
    private Set<StudentGroup> groups = new HashSet<>();

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
        return semesterFields.stream()
                .map(SemesterField::getField)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public void setFields(Set<Field> fields) {
        new HashSet<>(semesterFields).forEach(context -> removeField(context.getField()));
        if (fields != null) {
            fields.forEach(this::addField);
        }
    }

    public Set<StudentGroup> getGroups() {
        return semesterFields.stream()
                .flatMap(context -> context.getGroups().stream())
                .collect(Collectors.toCollection(HashSet::new));
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
        return Collections.emptySet();
    }

    public void setWeeklyScheduleEntries(Set<WeeklyScheduleEntry> weeklyScheduleEntries) {
    }

    public Set<SemesterFieldSubject> getCurriculumAssignments() {
        return semesterFields.stream()
                .flatMap(context -> context.getCurriculumAssignments().stream())
                .collect(Collectors.toCollection(HashSet::new));
    }

    public void setCurriculumAssignments(Set<SemesterFieldSubject> curriculumAssignments) {
    }

    public void addGroup(StudentGroup group) {
        groups.add(group);
        if (group.getSemesterField() == null) {
            SemesterField context = semesterFields.stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Semester must have a field before adding a group."));
            group.setSemesterField(context);
        }
    }

    public void removeGroup(StudentGroup group) {
        groups.remove(group);
        group.setSemesterField(null);
    }

    public SemesterField addField(Field field) {
        return semesterFields.stream()
                .filter(context -> context.getField().equals(field))
                .findFirst()
                .orElseGet(() -> {
                    SemesterField context = new SemesterField(this, field);
                    semesterFields.add(context);
                    field.getSemesterFields().add(context);
                    return context;
                });
    }

    public void removeField(Field field) {
        semesterFields.removeIf(context -> {
            boolean matches = context.getField().equals(field);
            if (matches) {
                field.getSemesterFields().remove(context);
            }
            return matches;
        });
    }

    public Set<SemesterField> getSemesterFields() {
        return semesterFields;
    }

    public void setSemesterFields(Set<SemesterField> semesterFields) {
        this.semesterFields = semesterFields;
    }

    public SemesterField getSemesterField(Field field) {
        return semesterFields.stream()
                .filter(context -> context.getField().equals(field))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Field does not belong to this semester."));
    }

    public SemesterFieldSubject assignSubject(Field field, Subject subject) {
        SemesterField context = getSemesterField(field);
        return context.getCurriculumAssignments().stream()
                .filter(assignment -> assignment.getSemesterField().equals(context)
                        && assignment.getSubject().equals(subject))
                .findFirst()
                .orElseGet(() -> {
                    SemesterFieldSubject assignment = new SemesterFieldSubject(context, subject);
                    context.getCurriculumAssignments().add(assignment);
                    field.getCurriculumAssignments().add(assignment);
                    subject.getCurriculumAssignments().add(assignment);
                    return assignment;
                });
    }

    public void removeSubject(Field field, Subject subject) {
        SemesterField context = getSemesterField(field);
        context.getCurriculumAssignments().removeIf(assignment -> {
            boolean matches = assignment.getSemesterField().equals(context)
                    && assignment.getSubject().equals(subject);
            if (matches) {
                field.getCurriculumAssignments().remove(assignment);
                subject.getCurriculumAssignments().remove(assignment);
            }
            return matches;
        });
    }
}
