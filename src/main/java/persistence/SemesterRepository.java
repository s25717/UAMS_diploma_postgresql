package persistence;

import jakarta.persistence.EntityManager;
import model.Semester;

import java.util.List;

public class SemesterRepository extends GenericRepository<Semester> {
    public SemesterRepository() {
        super(Semester.class);
    }

    public List<Semester> findAllWithFields() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct sem
                    from Semester sem
                    left join fetch sem.semesterFields sf
                    left join fetch sf.field
                    order by sem.number, sem.id
                    """, Semester.class)
                    .getResultList();
        }
    }
}
