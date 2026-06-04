package persistence;

import model.SystemNotification;

public class SystemNotificationRepository extends GenericRepository<SystemNotification> {
    public SystemNotificationRepository() {
        super(SystemNotification.class);
    }
}
