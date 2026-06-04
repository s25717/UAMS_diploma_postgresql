package service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import persistence.GenericRepository;
import persistence.JpaTransactionManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class EntityService<T> {
    private final Class<T> entityClass;
    private final GenericRepository<T> repository;
    private final JpaTransactionManager transactionManager = new JpaTransactionManager();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public EntityService(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.repository = new GenericRepository<>(entityClass);
    }

    public List<T> findAll() {
        return repository.findAll();
    }

    public Optional<T> findById(Long id) {
        return repository.findById(id);
    }

    public T add(T entity) {
        validate(entity);
        return transactionManager.execute(em -> {
            em.persist(entity);
            return entity;
        });
    }

    public T update(T entity) {
        validate(entity);
        return transactionManager.execute(em -> em.merge(entity));
    }

    public void delete(Long id) {
        transactionManager.executeVoid(em -> {
            T entity = em.find(entityClass, id);
            if (entity != null) {
                em.remove(entity);
            }
        });
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
