package model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Min;

@Entity
@DiscriminatorValue("SYSTEM")
public class SystemNotification extends Notification {
    @Min(1)
    private int displaySeconds;

    protected SystemNotification() {
    }

    public SystemNotification(String message, int priority, int displaySeconds) {
        super(message, priority);
        this.displaySeconds = displaySeconds;
    }

    public int getDisplaySeconds() {
        return displaySeconds;
    }

    public void setDisplaySeconds(int displaySeconds) {
        this.displaySeconds = displaySeconds;
    }

    @Override
    public int calculateDeliveryEffort() {
        return getPriority() + displaySeconds / 30;
    }
}
