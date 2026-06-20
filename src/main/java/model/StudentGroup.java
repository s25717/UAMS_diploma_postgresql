package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

@Entity
public class StudentGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String code;

    @Min(1)
    @Column(nullable = false)
    private int maxSize = 30;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private SemesterField semesterField;

    @OneToMany(mappedBy = "group")
    private Set<Student> students = new HashSet<>();

    @OneToMany(mappedBy = "group")
    private Set<ClassMeeting> classMeetings = new HashSet<>();

    @Transient
    private Set<WeeklyScheduleEntry> weeklyScheduleEntries = new HashSet<>();

    protected StudentGroup() {
    }

    public StudentGroup(String code) {
        this.code = code;
    }

    public StudentGroup(String code, int maxSize) {
        this.code = code;
        this.maxSize = maxSize;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public Semester getSemester() {
        return semesterField == null ? null : semesterField.getSemester();
    }

    public void setSemester(Semester semester) {
        if (semester == null) {
            this.semesterField = null;
            return;
        }
        Field currentField = getField();
        if (currentField == null) {
            throw new IllegalArgumentException("Set semesterField directly or set field before changing semester.");
        }
        this.semesterField = semester.getSemesterField(currentField);
    }

    public Field getField() {
        return semesterField == null ? null : semesterField.getField();
    }

    public void setField(Field field) {
        if (field == null) {
            this.semesterField = null;
            return;
        }
        Semester currentSemester = getSemester();
        if (currentSemester == null) {
            throw new IllegalArgumentException("Set semesterField directly or set semester before changing field.");
        }
        this.semesterField = currentSemester.getSemesterField(field);
    }

    public SemesterField getSemesterField() {
        return semesterField;
    }

    public void setSemesterField(SemesterField semesterField) {
        this.semesterField = semesterField;
    }

    public Set<Student> getStudents() {
        return students;
    }

    public void setStudents(Set<Student> students) {
        this.students = students;
    }

    public Set<ClassMeeting> getClassMeetings() {
        return classMeetings;
    }

    public void setClassMeetings(Set<ClassMeeting> classMeetings) {
        this.classMeetings = classMeetings;
    }

    public Set<WeeklyScheduleEntry> getWeeklyScheduleEntries() {
        return weeklyScheduleEntries;
    }

    public void setWeeklyScheduleEntries(Set<WeeklyScheduleEntry> weeklyScheduleEntries) {
        this.weeklyScheduleEntries = weeklyScheduleEntries;
    }

    public void addStudent(Student student) {
        if (students.size() >= maxSize && !students.contains(student)) {
            throw new IllegalArgumentException("Student group maximum size has been reached.");
        }
        students.add(student);
        student.setGroup(this);
    }

    public void removeStudent(Student student) {
        students.remove(student);
        student.setGroup(null);
    }
}
