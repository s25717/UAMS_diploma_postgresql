package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import model.enums.NotificationTaskStatus;
import model.enums.NotificationTaskType;

import java.time.LocalDateTime;

@Entity
public class ScheduledNotificationTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationTaskType taskType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationTaskStatus status = NotificationTaskStatus.PENDING;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    private LocalDateTime processedAt;

    @Min(0)
    private int retryCount;

    private String failureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    private Person recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    private ClassMeeting classMeeting;

    @ManyToOne(fetch = FetchType.LAZY)
    private AttendanceReport attendanceReport;

    protected ScheduledNotificationTask() {
    }

    public ScheduledNotificationTask(NotificationTaskType taskType, LocalDateTime scheduledAt) {
        this.taskType = taskType;
        this.scheduledAt = scheduledAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NotificationTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(NotificationTaskType taskType) {
        this.taskType = taskType;
    }

    public NotificationTaskStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationTaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Person getRecipient() {
        return recipient;
    }

    public void setRecipient(Person recipient) {
        this.recipient = recipient;
    }

    public ClassMeeting getClassMeeting() {
        return classMeeting;
    }

    public void setClassMeeting(ClassMeeting classMeeting) {
        this.classMeeting = classMeeting;
    }

    public AttendanceReport getAttendanceReport() {
        return attendanceReport;
    }

    public void setAttendanceReport(AttendanceReport attendanceReport) {
        this.attendanceReport = attendanceReport;
    }
}
