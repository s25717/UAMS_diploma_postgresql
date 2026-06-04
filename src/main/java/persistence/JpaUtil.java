package persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public final class JpaUtil {
    private static final EntityManagerFactory EMF = createEntityManagerFactory();

    private JpaUtil() {
    }

    public static EntityManagerFactory entityManagerFactory() {
        return EMF;
    }

    public static void close() {
        EMF.close();
    }

    private static EntityManagerFactory createEntityManagerFactory() {
        DatabaseMigrator.migrate();
        return Persistence.createEntityManagerFactory("mas-pu", DatabaseConfig.jpaProperties());
    }
}
