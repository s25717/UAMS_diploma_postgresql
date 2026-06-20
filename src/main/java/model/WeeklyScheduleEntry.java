package model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
public class WeeklyScheduleEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_class_meeting_id", nullable = false, unique = true)
    private ClassMeeting sourceClassMeeting;

    protected WeeklyScheduleEntry() {
    }

    public WeeklyScheduleEntry(ClassMeeting sourceClassMeeting) {
        this.sourceClassMeeting = sourceClassMeeting;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClassMeeting getSourceClassMeeting() { return sourceClassMeeting; }
    public void setSourceClassMeeting(ClassMeeting sourceClassMeeting) { this.sourceClassMeeting = sourceClassMeeting; }
    public StudentGroup getGroup() { return sourceClassMeeting == null ? null : sourceClassMeeting.getGroup(); }
    public void setGroup(StudentGroup group) { validateDerived("group"); }
    public Subject getSubject() { return sourceClassMeeting == null ? null : sourceClassMeeting.getSubject(); }
    public void setSubject(Subject subject) { validateDerived("subject"); }
    public Teacher getTeacher() { return sourceClassMeeting == null ? null : sourceClassMeeting.getTeacher(); }
    public void setTeacher(Teacher teacher) { validateDerived("teacher"); }
    public model.enums.ClassType getClassType() { return sourceClassMeeting == null ? null : sourceClassMeeting.getClassType(); }
    public void setClassType(model.enums.ClassType classType) { validateDerived("class type"); }
    public model.enums.MeetingMode getMeetingMode() { return sourceClassMeeting == null ? null : sourceClassMeeting.getMeetingMode(); }
    public void setMeetingMode(model.enums.MeetingMode meetingMode) { validateDerived("meeting mode"); }
    public DayOfWeek getDayOfWeek() {
        if (sourceClassMeeting == null) {
            return null;
        }
        if (sourceClassMeeting.getTime() != null && sourceClassMeeting.getTime().getDayOfWeek() != null) {
            return sourceClassMeeting.getTime().getDayOfWeek();
        }
        return sourceClassMeeting.getMeetingDate() == null ? null : sourceClassMeeting.getMeetingDate().getDayOfWeek();
    }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { validateDerived("day of week"); }
    public LocalTime getStartTime() {
        return sourceClassMeeting == null || sourceClassMeeting.getTime() == null ? null : sourceClassMeeting.getTime().getStartTime();
    }
    public void setStartTime(LocalTime startTime) { validateDerived("start time"); }
    public LocalTime getEndTime() {
        return sourceClassMeeting == null || sourceClassMeeting.getTime() == null ? null : sourceClassMeeting.getTime().getEndTime();
    }
    public void setEndTime(LocalTime endTime) { validateDerived("end time"); }
    public Room getRoom() {
        return sourceClassMeeting == null || sourceClassMeeting.getRoomBooking() == null
                ? null
                : sourceClassMeeting.getRoomBooking().getRoom();
    }
    public void setRoom(Room room) { validateDerived("room"); }
    public String getOnlineMeetingLink() {
        return sourceClassMeeting == null ? null : sourceClassMeeting.getOnlineMeetingLink();
    }
    public void setOnlineMeetingLink(String onlineMeetingLink) { validateDerived("online meeting link"); }
    public Semester getSemester() {
        StudentGroup group = getGroup();
        return group == null ? null : group.getSemester();
    }
    public void setSemester(Semester semester) {
        validateDerived("semester");
    }
    public Field getField() {
        StudentGroup group = getGroup();
        return group == null ? null : group.getField();
    }
    public void setField(Field field) {
        validateDerived("field");
    }

    private void validateDerived(String attribute) {
        throw new UnsupportedOperationException("Weekly schedule " + attribute + " is derived from the source class meeting.");
    }
}
