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

    public void startProcessing(LocalDateTime startedAt) {
        if (status != NotificationTaskStatus.PENDING) {
            throw new IllegalStateException("Only pending notification tasks can start processing.");
        }
        if (scheduledAt != null && startedAt != null && scheduledAt.isAfter(startedAt)) {
            throw new IllegalStateException("Notification task cannot be processed before its scheduled time.");
        }
        status = NotificationTaskStatus.PROCESSING;
        processedAt = null;
        failureReason = null;
    }

    public void markSent(LocalDateTime sentAt) {
        if (status != NotificationTaskStatus.PROCESSING) {
            throw new IllegalStateException("Only processing notification tasks can be marked as sent.");
        }
        status = NotificationTaskStatus.SENT;
        processedAt = requireTimestamp(sentAt);
        failureReason = null;
    }

    public void markFailed(LocalDateTime failedAt, String reason) {
        if (status != NotificationTaskStatus.PROCESSING) {
            throw new IllegalStateException("Only processing notification tasks can be marked as failed.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Failure reason is required for failed notification tasks.");
        }
        status = NotificationTaskStatus.FAILED;
        retryCount++;
        processedAt = requireTimestamp(failedAt);
        failureReason = reason;
    }

    public void cancel(LocalDateTime cancelledAt, String reason) {
        if (status != NotificationTaskStatus.PENDING && status != NotificationTaskStatus.FAILED) {
            throw new IllegalStateException("Only pending or failed notification tasks can be cancelled.");
        }
        status = NotificationTaskStatus.CANCELLED;
        processedAt = requireTimestamp(cancelledAt);
        failureReason = reason;
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

    private LocalDateTime requireTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Processing timestamp is required.");
        }
        return timestamp;
    }
}
