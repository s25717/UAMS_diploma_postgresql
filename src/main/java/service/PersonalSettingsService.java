package service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import model.ActivityLog;
import model.Notification;
import model.Person;
import persistence.JpaUtil;
import persistence.NotificationRepository;
import persistence.PersonRepository;

import java.util.List;
import java.util.regex.Pattern;

public class PersonalSettingsService {
    private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final PersonRepository personRepository = new PersonRepository();
    private final NotificationRepository notificationRepository = new NotificationRepository();

    public void addEmail(Long personId, String email) {
        mutateEmails(personId, person -> {
            String normalizedEmail = email == null ? null : email.trim();
            validateEmailValue(normalizedEmail);
            if (person.getEmails().size() >= 3) {
                throw new IllegalArgumentException("Each person can have at most 3 emails.");
            }
            ensureEmailIsAvailable(normalizedEmail, personId);
            person.getEmails().add(normalizedEmail);
            if (person.getPrimaryEmailValue() == null || person.getPrimaryEmailValue().isBlank()) {
                person.setPrimaryEmailValue(normalizedEmail);
            }
        }, "EMAIL_ADDED", "Added email address " + email + ".");
    }

    public void editEmail(Long personId, String oldEmail, String newEmail) {
        mutateEmails(personId, person -> {
            String normalizedEmail = newEmail == null ? null : newEmail.trim();
            validateEmailValue(normalizedEmail);
            if (!person.getEmails().remove(oldEmail)) {
                throw new IllegalArgumentException("Email not found: " + oldEmail);
            }
            ensureEmailIsAvailable(normalizedEmail, personId);
            person.getEmails().add(normalizedEmail);
            if (oldEmail != null && oldEmail.equals(person.getPrimaryEmailValue())) {
                person.setPrimaryEmailValue(normalizedEmail);
            }
        }, "EMAIL_UPDATED", "Updated email address from " + oldEmail + " to " + newEmail + ".");
    }

    public void deleteEmail(Long personId, String email) {
        mutateEmails(personId, person -> {
            if (person.getEmails().size() <= 1) {
                throw new IllegalArgumentException("At least one email must remain.");
            }
            if (!person.getEmails().remove(email)) {
                throw new IllegalArgumentException("Email not found: " + email);
            }
            if (email != null && email.equals(person.getPrimaryEmailValue())) {
                person.setPrimaryEmailValue(person.getEmails().stream().sorted().findFirst().orElse(null));
            }
        }, "EMAIL_DELETED", "Deleted email address " + email + ".");
    }

    public void setPrimaryEmail(Long personId, String email) {
        mutateEmails(personId, person -> {
            if (!person.getEmails().contains(email)) {
                throw new IllegalArgumentException("Primary email must be one of the person's emails.");
            }
            person.setPrimaryEmailValue(email);
        }, "PRIMARY_EMAIL_CHANGED", "Set primary email to " + email + ".");
    }

    public List<Notification> getNotifications(Long personId) {
        return notificationRepository.findByRecipientId(personId);
    }

    private void mutateEmails(Long personId, java.util.function.Consumer<Person> mutation,
                              String actionType, String description) {
        EntityManager em = JpaUtil.entityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person person = em.find(Person.class, personId);
            if (person == null) {
                throw new IllegalArgumentException("Person not found: " + personId);
            }
            mutation.accept(person);
            em.persist(new ActivityLog(person, actionType, description));
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    private void ensureEmailIsAvailable(String email, Long personId) {
        if (personRepository.emailExistsForAnotherPerson(email, personId)) {
            throw new IllegalArgumentException("Email is already used by another person: " + email);
        }
    }

    private void validateEmailValue(String email) {
        if (email == null || email.isBlank() || !SIMPLE_EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email format is invalid.");
        }
    }
}
