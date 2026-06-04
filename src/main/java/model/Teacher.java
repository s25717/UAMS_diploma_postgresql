package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import model.value.BirthDate;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Teacher extends Person {
    @NotBlank
    @Column(name = "teacher_employee_number", nullable = false, unique = true)
    private String employeeNumber;

    @OneToMany(mappedBy = "teacher")
    private Set<ClassMeeting> classMeetings = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "teacher_subject",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @Size(min = 1, max = 5, message = "Teacher must be qualified for 1 to 5 subjects")
    private Set<Subject> qualifiedSubjects = new HashSet<>();

    protected Teacher() {
    }

    public Teacher(String name, String surname, BirthDate birthDate, Set<String> emails, String employeeNumber) {
        super(name, surname, birthDate, emails);
        this.employeeNumber = employeeNumber;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public Set<ClassMeeting> getClassMeetings() {
        return classMeetings;
    }

    public void setClassMeetings(Set<ClassMeeting> classMeetings) {
        this.classMeetings = classMeetings;
    }

    public Set<Subject> getQualifiedSubjects() {
        return qualifiedSubjects;
    }

    public void setQualifiedSubjects(Set<Subject> qualifiedSubjects) {
        this.qualifiedSubjects = qualifiedSubjects;
    }

    public void addQualifiedSubject(Subject subject) {
        qualifiedSubjects.add(subject);
        subject.getQualifiedTeachers().add(this);
    }

    public void removeQualifiedSubject(Subject subject) {
        qualifiedSubjects.remove(subject);
        subject.getQualifiedTeachers().remove(this);
    }

    public boolean isQualifiedFor(Subject subject) {
        return subject != null && qualifiedSubjects.stream()
                .anyMatch(qualifiedSubject -> qualifiedSubject.getId() != null && qualifiedSubject.getId().equals(subject.getId())
                        || qualifiedSubject == subject);
    }
}
