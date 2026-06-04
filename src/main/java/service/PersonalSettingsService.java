package service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
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
            validateEmailValue(email);
            if (person.getEmails().size() >= 3) {
                throw new IllegalArgumentException("Each person can have at most 3 emails.");
            }
            ensureEmailIsAvailable(email, personId);
            person.getEmails().add(email);
            if (person.getPrimaryEmailValue() == null || person.getPrimaryEmailValue().isBlank()) {
                person.setPrimaryEmailValue(email);
            }
        });
    }

    public void editEmail(Long personId, String oldEmail, String newEmail) {
        mutateEmails(personId, person -> {
            validateEmailValue(newEmail);
            if (!person.getEmails().remove(oldEmail)) {
                throw new IllegalArgumentException("Email not found: " + oldEmail);
            }
            ensureEmailIsAvailable(newEmail, personId);
            person.getEmails().add(newEmail);
            if (oldEmail != null && oldEmail.equals(person.getPrimaryEmailValue())) {
                person.setPrimaryEmailValue(newEmail);
            }
        });
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
        });
    }

    public void setPrimaryEmail(Long personId, String email) {
        mutateEmails(personId, person -> {
            if (!person.getEmails().contains(email)) {
                throw new IllegalArgumentException("Primary email must be one of the person's emails.");
            }
            person.setPrimaryEmailValue(email);
        });
    }

    public List<Notification> getNotifications(Long personId) {
        return notificationRepository.findByRecipientId(personId);
    }

    private void mutateEmails(Long personId, java.util.function.Consumer<Person> mutation) {
        EntityManager em = JpaUtil.entityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person person = em.find(Person.class, personId);
            if (person == null) {
                throw new IllegalArgumentException("Person not found: " + personId);
            }
            mutation.accept(person);
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
