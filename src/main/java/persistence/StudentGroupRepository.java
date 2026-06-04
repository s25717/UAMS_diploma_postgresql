package persistence;

import jakarta.persistence.EntityManager;
import model.Student;
import model.StudentGroup;

import java.util.List;
import java.util.Optional;

public class StudentGroupRepository extends GenericRepository<StudentGroup> {
    public StudentGroupRepository() {
        super(StudentGroup.class);
    }

    public Optional<StudentGroup> findByIdWithStudents(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct g
                    from StudentGroup g
                    left join fetch g.students
                    where g.id = :id
                    """, StudentGroup.class)
                    .setParameter("id", id)
                    .getResultStream()
                    .findFirst();
        }
    }

    public List<StudentGroup> findAllWithStudents() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct g
                    from StudentGroup g
                    left join fetch g.students s
                    left join fetch s.emails
                    join fetch g.semester sem
                    join fetch sem.field
                    order by g.code
                    """, StudentGroup.class)
                    .getResultList();
        }
    }

    public Optional<StudentGroup> findByIdWithDetails(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct g
                    from StudentGroup g
                    join fetch g.semester sem
                    join fetch sem.field
                    left join fetch g.students
                    where g.id = :id
                    """, StudentGroup.class)
                    .setParameter("id", id)
                    .getResultStream()
                    .findFirst();
        }
    }

    public Optional<StudentGroup> findDetailsForStudent(Long studentId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct g
                    from StudentGroup g
                    join g.students owner
                    join fetch g.semester sem
                    join fetch sem.field
                    left join fetch g.students
                    where owner.id = :studentId
                    """, StudentGroup.class)
                    .setParameter("studentId", studentId)
                    .getResultStream()
                    .findFirst();
        }
    }

    public List<Student> findStudentsByGroupId(Long groupId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select s
                    from StudentGroup g
                    join g.students s
                    where g.id = :groupId
                    """, Student.class)
                    .setParameter("groupId", groupId)
                    .getResultList();
        }
    }

    public void removeStudentFromGroup(Long groupId, Long studentId) {
        executeInTransaction(em -> {
            StudentGroup group = em.createQuery("""
                    select distinct g
                    from StudentGroup g
                    left join fetch g.students
                    where g.id = :groupId
                    """, StudentGroup.class)
                    .setParameter("groupId", groupId)
                    .getSingleResult();

            Student student = em.find(Student.class, studentId);
            group.removeStudent(student);
            return null;
        });
    }
}
