package model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "subject_group",
            joinColumns = @JoinColumn(name = "subject_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<StudentGroup> groups = new HashSet<>();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ClassMeeting> classMeetings = new HashSet<>();

    @ManyToMany(mappedBy = "qualifiedSubjects", fetch = FetchType.LAZY)
    private Set<Teacher> qualifiedTeachers = new HashSet<>();

    @OneToMany(mappedBy = "subject")
    private Set<SemesterFieldSubject> curriculumAssignments = new HashSet<>();

    protected Subject() {
    }

    public Subject(String name) {
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

    public Set<StudentGroup> getGroups() {
        return groups;
    }

    public void setGroups(Set<StudentGroup> groups) {
        this.groups = groups;
    }

    public Set<ClassMeeting> getClassMeetings() {
        return classMeetings;
    }

    public void setClassMeetings(Set<ClassMeeting> classMeetings) {
        this.classMeetings = classMeetings;
    }

    public Set<Teacher> getQualifiedTeachers() {
        return qualifiedTeachers;
    }

    public void setQualifiedTeachers(Set<Teacher> qualifiedTeachers) {
        this.qualifiedTeachers = qualifiedTeachers;
    }

    public Set<SemesterFieldSubject> getCurriculumAssignments() {
        return curriculumAssignments;
    }

    public void setCurriculumAssignments(Set<SemesterFieldSubject> curriculumAssignments) {
        this.curriculumAssignments = curriculumAssignments;
    }

    public void addGroup(StudentGroup group) {
        groups.add(group);
        group.getSubjects().add(this);
    }

    public void removeGroup(StudentGroup group) {
        groups.remove(group);
        group.getSubjects().remove(this);
    }
}
