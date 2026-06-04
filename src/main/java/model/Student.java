package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import model.value.BirthDate;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Student extends Person {
    @NotBlank
    @Pattern(regexp = "s\\d{3,6}", message = "Student number must match format s123")
    @Column(nullable = false, unique = true)
    private String studentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    private StudentGroup group;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Attendance> attendances = new HashSet<>();

    protected Student() {
    }

    public Student(String name, String surname, BirthDate birthDate, Set<String> emails, String studentNumber) {
        super(name, surname, birthDate, emails);
        this.studentNumber = studentNumber;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public StudentGroup getGroup() {
        return group;
    }

    public void setGroup(StudentGroup group) {
        this.group = group;
    }

    public Set<Attendance> getAttendances() {
        return attendances;
    }

    public void setAttendances(Set<Attendance> attendances) {
        this.attendances = attendances;
    }
}
