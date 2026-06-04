package model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Field field;

    @Valid
    @OneToMany(mappedBy = "semester", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StudentGroup> groups = new HashSet<>();

    @OneToMany(mappedBy = "semester", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WeeklyScheduleEntry> weeklyScheduleEntries = new HashSet<>();

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

    public void addGroup(StudentGroup group) {
        groups.add(group);
        group.setSemester(this);
    }

    public void removeGroup(StudentGroup group) {
        groups.remove(group);
        group.setSemester(null);
    }
}
