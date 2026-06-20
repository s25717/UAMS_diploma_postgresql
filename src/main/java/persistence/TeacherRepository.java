package persistence;

import jakarta.persistence.EntityManager;
import model.Teacher;

import java.util.List;
import java.util.Optional;

public class TeacherRepository extends GenericRepository<Teacher> {
    public TeacherRepository() {
        super(Teacher.class);
    }

    public Optional<Teacher> findByIdWithQualifiedSubjects(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct t
                    from Teacher t
                    left join fetch t.qualifiedSubjects
                    where t.id = :id
                    """, Teacher.class)
                    .setParameter("id", id)
                    .getResultStream()
                    .findFirst();
        }
    }

    public List<Teacher> findAllWithQualifiedSubjects() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct t
                    from Teacher t
                    left join fetch t.qualifiedSubjects
                    order by t.surname, t.name
                    """, Teacher.class)
                    .getResultList();
        }
    }
}
