package model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@DiscriminatorValue("EMAIL")
public class EmailNotification extends Notification {
    private boolean hasAttachment;

    @Email
    @NotBlank
    @Column(name = "delivery_email")
    private String deliveryEmail;

    protected EmailNotification() {
    }

    public EmailNotification(String message, int priority, boolean hasAttachment) {
        super(message, priority);
        this.hasAttachment = hasAttachment;
    }

    public EmailNotification(String message, int priority, boolean hasAttachment, String deliveryEmail) {
        this(message, priority, hasAttachment);
        this.deliveryEmail = deliveryEmail;
    }

    public boolean isHasAttachment() {
        return hasAttachment;
    }

    public void setHasAttachment(boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
    }

    public String getDeliveryEmail() {
        return deliveryEmail;
    }

    public void setDeliveryEmail(String deliveryEmail) {
        this.deliveryEmail = deliveryEmail;
    }

    @Override
    public int calculateDeliveryEffort() {
        return getPriority() + (hasAttachment ? 2 : 1);
    }
}
