ALTER TABLE notification
    ADD COLUMN delivery_email varchar(255);

UPDATE notification n
SET delivery_email = COALESCE(NULLIF(btrim(p.primary_email), ''), pe.email)
FROM person p
LEFT JOIN LATERAL (
    SELECT email
    FROM person_emails
    WHERE person_id = p.id
    ORDER BY email
    LIMIT 1
) pe ON true
WHERE n.notification_type = 'EMAIL'
  AND n.recipient_id = p.id
  AND (n.delivery_email IS NULL OR btrim(n.delivery_email) = '');

ALTER TABLE notification
    ADD CONSTRAINT chk_email_notification_delivery_email_required
    CHECK (
        notification_type <> 'EMAIL'
        OR delivery_email IS NOT NULL AND btrim(delivery_email) <> ''
    ) NOT VALID;

CREATE INDEX idx_notification_delivery_email
    ON notification (delivery_email)
    WHERE notification_type = 'EMAIL';

CREATE OR REPLACE FUNCTION validate_email_notification_delivery_email()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.notification_type <> 'EMAIL' THEN
        RETURN NEW;
    END IF;

    IF NEW.recipient_id IS NULL THEN
        RAISE EXCEPTION 'Email notification % requires a recipient', NEW.id;
    END IF;

    IF NEW.delivery_email IS NULL OR btrim(NEW.delivery_email) = '' THEN
        RAISE EXCEPTION 'Email notification % requires a delivery email', NEW.id;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM person_emails pe
        WHERE pe.person_id = NEW.recipient_id
          AND lower(pe.email) = lower(NEW.delivery_email)
    ) THEN
        RAISE EXCEPTION 'Delivery email % does not belong to recipient %', NEW.delivery_email, NEW.recipient_id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_email_notification_delivery_email
AFTER INSERT OR UPDATE OF notification_type, recipient_id, delivery_email ON notification
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_email_notification_delivery_email();
