package persistence;

import jakarta.persistence.EntityManager;
import model.Person;
import model.Student;
import model.Teacher;

import java.util.List;
import java.util.Optional;

public class PersonRepository extends GenericRepository<Person> {
    public PersonRepository() {
        super(Person.class);
    }

    public boolean emailExistsForAnotherPerson(String email, Long personId) {
        try (EntityManager em = emf.createEntityManager()) {
            Long count = em.createQuery("""
                    select count(p)
                    from Person p
                    join p.emails e
                    where lower(e) = lower(:email)
                    and (:personId is null or p.id <> :personId)
                    """, Long.class)
                    .setParameter("email", email)
                    .setParameter("personId", personId)
                    .getSingleResult();
            return count > 0;
        }
    }

    public Optional<Person> findByEmail(String email) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct p
                    from Person p
                    left join fetch p.emails
                    where exists (
                        select 1
                        from Person owner
                        join owner.emails e
                        where owner = p
                        and lower(e) = lower(:email)
                    )
                    """, Person.class)
                    .setParameter("email", email)
                    .getResultStream()
                    .findFirst();
        }
    }

    public List<Person> findAllWithEmails() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Person> people = em.createQuery("""
                    select distinct p
                    from Person p
                    left join fetch p.emails
                    order by p.surname, p.name
                    """, Person.class)
                    .getResultList();

            List<Long> teacherIds = people.stream()
                    .filter(Teacher.class::isInstance)
                    .map(Person::getId)
                    .toList();
            if (!teacherIds.isEmpty()) {
                em.createQuery("""
                        select distinct t
                        from Teacher t
                        left join fetch t.qualifiedSubjects
                        where t.id in :ids
                        """, Teacher.class)
                        .setParameter("ids", teacherIds)
                        .getResultList();
            }

            List<Long> studentIds = people.stream()
                    .filter(Student.class::isInstance)
                    .map(Person::getId)
                    .toList();
            if (!studentIds.isEmpty()) {
                em.createQuery("""
                        select distinct s
                        from Student s
                        left join fetch s.group g
                        left join fetch g.semesterField sf
                        left join fetch sf.semester
                        left join fetch sf.field
                        where s.id in :ids
                        """, Student.class)
                        .setParameter("ids", studentIds)
                        .getResultList();
            }

            return people;
        }
    }
}
