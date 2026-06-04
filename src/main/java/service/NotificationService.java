package service;

import model.EmailNotification;
import model.Notification;
import model.ScheduledNotificationTask;
import model.SystemNotification;
import model.enums.NotificationTaskType;

public class NotificationService {
    private final NotificationManagementService notificationManagementService = new NotificationManagementService();

    public Notification createNotificationForTask(ScheduledNotificationTask task) {
        if (task.getRecipient() == null) {
            throw new IllegalArgumentException("Notification task recipient is required.");
        }
        String message = switch (task.getTaskType()) {
            case CLASS_MEETING_REMINDER -> "Upcoming class meeting reminder";
            case LOW_ATTENDANCE_WARNING -> "Low attendance warning";
            case ATTENDANCE_REGISTERED -> "Attendance registered";
            case ATTENDANCE_UPDATED -> "Attendance updated";
            case REPORT_READY -> "Attendance report is ready";
        };

        if (task.getTaskType() == NotificationTaskType.REPORT_READY) {
            SystemNotification notification = new SystemNotification(message, 2, 60);
            notification.setTitle(message);
            notification.setRecipient(task.getRecipient());
            return notificationManagementService.add(notification);
        }
        EmailNotification notification = new EmailNotification(message, 2, false);
        notification.setTitle(message);
        notification.setRecipient(task.getRecipient());
        return notificationManagementService.add(notification);
    }
}
