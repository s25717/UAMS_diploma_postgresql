package service;

import model.Person;
import persistence.JpaUtil;
import persistence.PersonRepository;

public class PersonService {
    private final PersonRepository personRepository = new PersonRepository();
    private final PasswordService passwordService = new PasswordService();

    public <T extends Person> T save(T person) {
        validateGlobalEmailUniqueness(person);
        if ("change-me".equals(person.getPasswordHash())) {
            person.setPasswordHash(passwordService.hash("Password123!"));
        }
        return new EntityService<>((Class<T>) person.getClass()).add(person);
    }

    public <T extends Person> T update(T person) {
        validateGlobalEmailUniqueness(person);
        return new EntityService<>((Class<T>) person.getClass()).update(person);
    }

    private void validateGlobalEmailUniqueness(Person person) {
        if (person.getEmails() == null || person.getEmails().isEmpty()) {
            throw new IllegalArgumentException("Each person must have at least one email.");
        }
        if (person.getEmails().size() > 3) {
            throw new IllegalArgumentException("Each person can have at most 3 emails.");
        }
        if (person.getPrimaryEmailValue() == null || person.getPrimaryEmailValue().isBlank()) {
            person.setPrimaryEmailValue(person.getEmails().stream().sorted().findFirst().orElse(null));
        }
        if (person.getPrimaryEmailValue() != null && !person.getEmails().contains(person.getPrimaryEmailValue())) {
            throw new IllegalArgumentException("Primary email must be one of the person's emails.");
        }
        for (String email : person.getEmails()) {
            if (personRepository.emailExistsForAnotherPerson(email, person.getId())) {
                throw new IllegalArgumentException("Email is already used by another person: " + email);
            }
        }
    }

    public void changePassword(Long personId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8 || !newPassword.matches(".*\\d.*")) {
            throw new IllegalArgumentException("New password must have at least 8 characters and contain a digit.");
        }
        var em = JpaUtil.entityManagerFactory().createEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            Person person = em.find(Person.class, personId);
            if (person == null) {
                throw new IllegalArgumentException("Person not found: " + personId);
            }
            if (!passwordService.matches(oldPassword, person.getPasswordHash())) {
                throw new IllegalArgumentException("Old password is incorrect.");
            }
            person.setPasswordHash(passwordService.hash(newPassword));
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

    public String hashPassword(String password) {
        return passwordService.hash(password);
    }
}
