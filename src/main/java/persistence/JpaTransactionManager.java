package persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.function.Function;

public class JpaTransactionManager {
    private final EntityManagerFactory emf;

    public JpaTransactionManager() {
        this.emf = JpaUtil.entityManagerFactory();
    }

    public <R> R execute(Function<EntityManager, R> work) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            R result = work.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public void executeVoid(EntityWork work) {
        execute(em -> {
            work.accept(em);
            return null;
        });
    }

    @FunctionalInterface
    public interface EntityWork {
        void accept(EntityManager em);
    }
}
