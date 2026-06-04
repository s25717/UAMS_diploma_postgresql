package service;

import model.Person;

public class SessionContext {
    private static final SessionContext INSTANCE = new SessionContext();

    private Person currentUser;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    public Person getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(Person currentUser) {
        this.currentUser = currentUser;
    }

    public void clear() {
        currentUser = null;
    }
}
