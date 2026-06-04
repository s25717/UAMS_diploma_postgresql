package persistence;

import model.Administrator;

public class AdministratorRepository extends GenericRepository<Administrator> {
    public AdministratorRepository() {
        super(Administrator.class);
    }
}
