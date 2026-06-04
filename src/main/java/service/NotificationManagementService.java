package service;

import model.Notification;
import model.Person;
import model.SystemNotification;
import model.enums.NotificationStatus;
import persistence.JpaTransactionManager;
import persistence.NotificationRepository;

import java.util.List;

public class NotificationManagementService extends EntityService<Notification> {
    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final JpaTransactionManager transactionManager = new JpaTransactionManager();

    public NotificationManagementService() {
        super(Notification.class);
    }

    public List<Notification> findAllWithRecipients() {
        return notificationRepository.findAllWithRecipients();
    }

    public List<Notification> findByRecipientId(Long personId) {
        return notificationRepository.findByRecipientId(personId);
    }

    public SystemNotification createSystemNotification(Long recipientId, String title, String message, NotificationStatus status) {
        if (recipientId == null || title == null || title.isBlank() || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Recipient, title, and message are required.");
        }
        return transactionManager.execute(em -> {
            SystemNotification notification = new SystemNotification(message, 1, 60);
            notification.setTitle(title);
            notification.setStatus(status == null ? NotificationStatus.PENDING : status);
            notification.setRecipient(em.getReference(Person.class, recipientId));
            em.persist(notification);
            return notification;
        });
    }

    public Notification updateEditableNotification(Notification notification, Person recipient, String title,
                                                   String message, NotificationStatus status) {
        if (notification == null) {
            throw new IllegalArgumentException("Select a notification first.");
        }
        if (notification.getStatus() == NotificationStatus.SENT) {
            throw new IllegalArgumentException("Sent notifications cannot be edited.");
        }
        if (!(notification.getStatus() == NotificationStatus.DRAFT
                || notification.getStatus() == NotificationStatus.PENDING
                || notification.getStatus() == NotificationStatus.FAILED)) {
            throw new IllegalArgumentException("Only draft, pending, or failed notifications can be edited.");
        }
        if (recipient == null || title == null || title.isBlank() || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Recipient, title, and message are required.");
        }
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setStatus(status == null ? NotificationStatus.PENDING : status);
        return update(notification);
    }

    public Notification cancelPendingNotification(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Select a notification first.");
        }
        if (!(notification.getStatus() == NotificationStatus.DRAFT || notification.getStatus() == NotificationStatus.PENDING)) {
            throw new IllegalArgumentException("Only draft or pending notifications can be cancelled.");
        }
        notification.setStatus(NotificationStatus.CANCELLED);
        return update(notification);
    }
}
