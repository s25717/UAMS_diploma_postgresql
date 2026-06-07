package model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import model.enums.ClassType;
import model.enums.MeetingMode;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
public class WeeklyScheduleEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private StudentGroup group;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Subject subject;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Teacher teacher;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ClassType classType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private MeetingMode meetingMode;

    @NotNull
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    private Room room;

    private String onlineMeetingLink;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Semester semester;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Field field;

    protected WeeklyScheduleEntry() {
    }

    public WeeklyScheduleEntry(StudentGroup group, Subject subject, Teacher teacher, ClassType classType,
                               MeetingMode meetingMode, DayOfWeek dayOfWeek, LocalTime startTime,
                               LocalTime endTime, Room room, String onlineMeetingLink, Semester semester, Field field) {
        this.group = group;
        this.subject = subject;
        this.teacher = teacher;
        this.classType = classType;
        this.meetingMode = meetingMode;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.onlineMeetingLink = onlineMeetingLink;
        this.semester = semester;
        this.field = field;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StudentGroup getGroup() { return group; }
    public void setGroup(StudentGroup group) { this.group = group; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
    public ClassType getClassType() { return classType; }
    public void setClassType(ClassType classType) { this.classType = classType; }
    public MeetingMode getMeetingMode() { return meetingMode; }
    public void setMeetingMode(MeetingMode meetingMode) { this.meetingMode = meetingMode; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public String getOnlineMeetingLink() { return onlineMeetingLink; }
    public void setOnlineMeetingLink(String onlineMeetingLink) { this.onlineMeetingLink = onlineMeetingLink; }
    public Semester getSemester() { return semester; }
    public void setSemester(Semester semester) { this.semester = semester; }
    public Field getField() { return field; }
    public void setField(Field field) { this.field = field; }
}
