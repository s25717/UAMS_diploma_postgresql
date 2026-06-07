package service;

import model.EmailNotification;
import model.Notification;
import model.Person;
import model.SystemNotification;
import model.enums.NotificationStatus;
import persistence.JpaTransactionManager;
import persistence.NotificationRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    public List<EmailNotification> createEmailNotifications(List<EmailRecipient> recipients, String title,
                                                            String message, NotificationStatus status) {
        if (recipients == null || recipients.isEmpty() || title == null || title.isBlank()
                || message == null || message.isBlank()) {
            throw new IllegalArgumentException("At least one email recipient, title, and message are required.");
        }
        Map<String, EmailRecipient> uniqueRecipients = new LinkedHashMap<>();
        for (EmailRecipient recipient : recipients) {
            if (recipient == null || recipient.personId() == null || recipient.email() == null
                    || recipient.email().isBlank()) {
                throw new IllegalArgumentException("Each selected target must have a user and email address.");
            }
            String normalizedEmail = recipient.email().trim();
            uniqueRecipients.put(recipient.personId() + "|" + normalizedEmail.toLowerCase(Locale.ROOT),
                    new EmailRecipient(recipient.personId(), normalizedEmail));
        }

        return transactionManager.execute(em -> {
            List<EmailNotification> created = new ArrayList<>();
            for (EmailRecipient target : uniqueRecipients.values()) {
                Person recipient = em.createQuery("""
                        select distinct p
                        from Person p
                        left join fetch p.emails
                        where p.id = :id
                        """, Person.class)
                        .setParameter("id", target.personId())
                        .getResultStream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Recipient not found: " + target.personId()));
                String deliveryEmail = requireOwnedEmail(recipient, target.email());
                EmailNotification notification = new EmailNotification(message.trim(), 1, false, deliveryEmail);
                notification.setTitle(title.trim());
                notification.setStatus(status == null ? NotificationStatus.PENDING : status);
                notification.setRecipient(recipient);
                em.persist(notification);
                created.add(notification);
            }
            return created;
        });
    }

    public Notification updateEditableNotification(Notification notification, Person recipient, String title,
                                                    String message, NotificationStatus status) {
        return updateEditableNotification(notification, recipient, title, message, status, null);
    }

    public Notification updateEditableNotification(Notification notification, Person recipient, String title,
                                                    String message, NotificationStatus status, String deliveryEmail) {
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
        if (notification instanceof EmailNotification emailNotification) {
            String selectedDeliveryEmail = deliveryEmail == null || deliveryEmail.isBlank()
                    ? emailNotification.getDeliveryEmail()
                    : deliveryEmail;
            emailNotification.setDeliveryEmail(requireOwnedEmail(recipient, selectedDeliveryEmail));
        }
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

    private String requireOwnedEmail(Person recipient, String email) {
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient is required.");
        }
        String selectedEmail = email == null ? "" : email.trim();
        if (selectedEmail.isBlank()) {
            selectedEmail = recipient.getPrimaryEmail();
        }
        for (String ownedEmail : recipient.getEmails()) {
            if (ownedEmail.equalsIgnoreCase(selectedEmail)) {
                return ownedEmail;
            }
        }
        throw new IllegalArgumentException("Selected email does not belong to recipient: " + selectedEmail);
    }

    public record EmailRecipient(Long personId, String email) {
    }
}
