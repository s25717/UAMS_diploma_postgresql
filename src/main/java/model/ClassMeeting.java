package model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import model.enums.ClassMeetingStatus;
import model.enums.ClassType;
import model.enums.MeetingMode;
import model.value.MeetingTime;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
public class ClassMeeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Deprecated
    private String room;

    private String onlineMeetingLink;

    private String location;

    @NotNull
    @Column(nullable = false)
    private LocalDate meetingDate;

    @Valid
    @NotNull
    @Embedded
    private MeetingTime time;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassType classType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingMode meetingMode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassMeetingStatus status = ClassMeetingStatus.SCHEDULED;

    @Size(max = 1000)
    private String comment;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Subject subject;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Teacher teacher;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private StudentGroup group;

    @Valid
    @OneToMany(mappedBy = "classMeeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Attendance> attendances = new HashSet<>();

    @OneToOne(mappedBy = "classMeeting", fetch = FetchType.LAZY)
    private RoomBooking roomBooking;

    @ManyToOne(fetch = FetchType.LAZY)
    private WeeklyScheduleEntry scheduleEntry;

    protected ClassMeeting() {
    }

    public ClassMeeting(String room, String onlineLink, MeetingTime time, ClassType classType,
                        Subject subject, Teacher teacher, StudentGroup group) {
        this(LocalDate.now(), room, onlineLink, time, classType,
                onlineLink == null ? MeetingMode.CLASSROOM : MeetingMode.ONLINE, subject, teacher, group);
    }

    public ClassMeeting(LocalDate meetingDate, String location, String onlineMeetingLink, MeetingTime time,
                        ClassType classType, MeetingMode meetingMode,
                        Subject subject, Teacher teacher, StudentGroup group) {
        this.room = location;
        this.location = location;
        this.onlineMeetingLink = onlineMeetingLink;
        this.meetingDate = meetingDate;
        this.time = time;
        this.classType = classType;
        this.meetingMode = meetingMode;
        this.subject = subject;
        this.teacher = teacher;
        this.group = group;
        validateTeacherQualification();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoom() {
        return getLocation();
    }

    public void setRoom(String room) {
        this.room = room;
        this.location = room;
    }

    public String getOnlineLink() {
        return onlineMeetingLink;
    }

    public void setOnlineLink(String onlineLink) {
        this.onlineMeetingLink = onlineLink;
    }

    public String getOnlineMeetingLink() {
        return onlineMeetingLink;
    }

    public void setOnlineMeetingLink(String onlineMeetingLink) {
        this.onlineMeetingLink = onlineMeetingLink;
    }

    public String getLocation() {
        return location == null || location.isBlank() ? room : location;
    }

    public void setLocation(String location) {
        this.location = location;
        this.room = location;
    }

    public LocalDate getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(LocalDate meetingDate) {
        this.meetingDate = meetingDate;
    }

    public MeetingTime getTime() {
        return time;
    }

    public void setTime(MeetingTime time) {
        this.time = time;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void setClassType(ClassType classType) {
        this.classType = classType;
    }

    public ClassType getFormat() {
        return classType;
    }

    public MeetingMode getMeetingMode() {
        return meetingMode;
    }

    public void setMeetingMode(MeetingMode meetingMode) {
        this.meetingMode = meetingMode;
    }

    public ClassMeetingStatus getStatus() {
        return status;
    }

    public void setStatus(ClassMeetingStatus status) {
        this.status = status;
    }

    public boolean isDraft() {
        return status == ClassMeetingStatus.DRAFT;
    }

    public boolean isScheduled() {
        return status == ClassMeetingStatus.SCHEDULED;
    }

    public boolean isCancelled() {
        return status == ClassMeetingStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return status == ClassMeetingStatus.COMPLETED;
    }

    public void schedule() {
        if (status != ClassMeetingStatus.DRAFT) {
            throw new IllegalStateException("Only draft class meetings can be scheduled.");
        }
        status = ClassMeetingStatus.SCHEDULED;
    }

    public void cancel(String comment) {
        if (status == ClassMeetingStatus.COMPLETED) {
            throw new IllegalStateException("Completed class meetings cannot be cancelled.");
        }
        status = ClassMeetingStatus.CANCELLED;
        this.comment = comment;
    }

    public void complete() {
        if (status != ClassMeetingStatus.SCHEDULED) {
            throw new IllegalStateException("Only scheduled class meetings can be completed.");
        }
        status = ClassMeetingStatus.COMPLETED;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
        validateTeacherQualification();
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
        validateTeacherQualification();
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

    public RoomBooking getRoomBooking() {
        return roomBooking;
    }

    public void setRoomBooking(RoomBooking roomBooking) {
        this.roomBooking = roomBooking;
    }

    public WeeklyScheduleEntry getScheduleEntry() {
        return scheduleEntry;
    }

    public void setScheduleEntry(WeeklyScheduleEntry scheduleEntry) {
        this.scheduleEntry = scheduleEntry;
    }

    public void addAttendance(Attendance attendance) {
        if (status != ClassMeetingStatus.SCHEDULED) {
            throw new IllegalArgumentException("Attendance can only be marked for a scheduled class meeting.");
        }
        attendances.add(attendance);
        attendance.setClassMeeting(this);
    }

    public void removeAttendance(Attendance attendance) {
        attendances.remove(attendance);
        attendance.setClassMeeting(null);
    }

    private void validateTeacherQualification() {
        if (teacher != null && subject != null && !teacher.isQualifiedFor(subject)) {
            throw new IllegalArgumentException("Teacher is not qualified to teach this subject.");
        }
    }
}
