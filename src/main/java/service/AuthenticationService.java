package service;

import model.Administrator;
import model.Person;
import model.Student;
import model.Teacher;
import persistence.PersonRepository;

import java.util.Optional;

public class AuthenticationService {
    private final PersonRepository personRepository = new PersonRepository();
    private final PasswordService passwordService = new PasswordService();
    private final SessionContext sessionContext = SessionContext.getInstance();

    public Optional<Person> login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        Optional<Person> person = personRepository.findByEmail(email);
        if (person.isEmpty() || !passwordService.matches(password, person.get().getPasswordHash())) {
            return Optional.empty();
        }
        sessionContext.setCurrentUser(person.get());
        return person;
    }

    public Person getCurrentUser() {
        return sessionContext.getCurrentUser();
    }

    public boolean isStudent() {
        return getCurrentUser() instanceof Student;
    }

    public boolean isTeacher() {
        return getCurrentUser() instanceof Teacher;
    }

    public boolean isAdmin() {
        return getCurrentUser() instanceof Administrator;
    }

    public void logout() {
        sessionContext.clear();
    }
}
