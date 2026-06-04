package model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("EMAIL")
public class EmailNotification extends Notification {
    private boolean hasAttachment;

    protected EmailNotification() {
    }

    public EmailNotification(String message, int priority, boolean hasAttachment) {
        super(message, priority);
        this.hasAttachment = hasAttachment;
    }

    public boolean isHasAttachment() {
        return hasAttachment;
    }

    public void setHasAttachment(boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
    }

    @Override
    public int calculateDeliveryEffort() {
        return getPriority() + (hasAttachment ? 2 : 1);
    }
}
