package persistence;

import model.EmailNotification;

public class EmailNotificationRepository extends GenericRepository<EmailNotification> {
    public EmailNotificationRepository() {
        super(EmailNotification.class);
    }
}
