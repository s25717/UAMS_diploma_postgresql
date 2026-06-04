package persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class GenericRepository<T> {
    protected final EntityManagerFactory emf;
    protected final Validator validator;
    protected final JpaTransactionManager transactionManager;
    private final Class<T> entityClass;

    public GenericRepository(Class<T> entityClass) {
        this.emf = JpaUtil.entityManagerFactory();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        this.transactionManager = new JpaTransactionManager();
        this.entityClass = entityClass;
    }

    public List<T> findAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("select e from " + entityClass.getSimpleName() + " e", entityClass)
                    .getResultList();
        }
    }

    public Optional<T> findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return Optional.ofNullable(em.find(entityClass, id));
        }
    }

    public T save(T entity) {
        validate(entity);
        return executeInTransaction(em -> {
            em.persist(entity);
            return entity;
        });
    }

    public T update(T entity) {
        validate(entity);
        return executeInTransaction(em -> em.merge(entity));
    }

    public void delete(Long id) {
        executeInTransaction(em -> {
            T entity = em.find(entityClass, id);
            if (entity != null) {
                em.remove(entity);
            }
            return null;
        });
    }

    protected <R> R executeInTransaction(Function<EntityManager, R> work) {
        return transactionManager.execute(work);
    }

    protected void validate(T entity) {
        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            StringBuilder message = new StringBuilder("Entity validation failed: ");
            for (ConstraintViolation<T> violation : violations) {
                message.append(violation.getPropertyPath())
                        .append(" ")
                        .append(violation.getMessage())
                        .append("; ");
            }
            throw new IllegalArgumentException(message.toString());
        }
    }
}
