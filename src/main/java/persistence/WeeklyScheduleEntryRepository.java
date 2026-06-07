package persistence;

import jakarta.persistence.EntityManager;
import model.Room;
import model.Field;
import model.Semester;
import model.StudentGroup;
import model.Subject;
import model.Teacher;
import model.WeeklyScheduleEntry;

import java.util.List;

public class WeeklyScheduleEntryRepository extends GenericRepository<WeeklyScheduleEntry> {
    public WeeklyScheduleEntryRepository() {
        super(WeeklyScheduleEntry.class);
    }

    public List<WeeklyScheduleEntry> findAllWithDetails() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct e
                    from WeeklyScheduleEntry e
                    join fetch e.group g
                    join fetch e.semester sem
                    join fetch e.field
                    join fetch e.subject s
                    join fetch e.teacher t
                    left join fetch t.emails
                    left join fetch e.room
                    order by g.code, e.dayOfWeek, e.startTime
                    """, WeeklyScheduleEntry.class)
                    .getResultList();
        }
    }

    public WeeklyScheduleEntry saveWithManagedReferences(WeeklyScheduleEntry entry) {
        validate(entry);
        return executeInTransaction(em -> {
            WeeklyScheduleEntry managed = new WeeklyScheduleEntry(
                    em.getReference(StudentGroup.class, entry.getGroup().getId()),
                    em.getReference(Subject.class, entry.getSubject().getId()),
                    em.getReference(Teacher.class, entry.getTeacher().getId()),
                    entry.getClassType(),
                    entry.getMeetingMode(),
                    entry.getDayOfWeek(),
                    entry.getStartTime(),
                    entry.getEndTime(),
                    entry.getRoom() == null ? null : em.getReference(Room.class, entry.getRoom().getId()),
                    entry.getOnlineMeetingLink(),
                    em.getReference(Semester.class, entry.getSemester().getId()),
                    em.getReference(Field.class, entry.getField().getId())
            );
            validateBusinessRules(em, entry);
            em.persist(managed);
            return managed;
        });
    }

    private void validateBusinessRules(EntityManager em, WeeklyScheduleEntry entry) {
        Long groupSemesterMatches = em.createQuery("""
                select count(g)
                from StudentGroup g
                where g.id = :groupId
                and g.semester.id = :semesterId
                and g.field.id = :fieldId
                """, Long.class)
                .setParameter("groupId", entry.getGroup().getId())
                .setParameter("semesterId", entry.getSemester().getId())
                .setParameter("fieldId", entry.getField().getId())
                .getSingleResult();
        if (groupSemesterMatches == 0) {
            throw new IllegalArgumentException("Weekly schedule semester and field must match the selected group.");
        }
        Long semesterSubjectMatches = em.createQuery("""
                select count(c)
                from SemesterFieldSubject c
                where c.semester.id = :semesterId
                and c.field.id = :fieldId
                and c.subject.id = :subjectId
                """, Long.class)
                .setParameter("semesterId", entry.getSemester().getId())
                .setParameter("fieldId", entry.getField().getId())
                .setParameter("subjectId", entry.getSubject().getId())
                .getSingleResult();
        if (semesterSubjectMatches == 0) {
            throw new IllegalArgumentException("Subject is not available in the selected semester and field.");
        }
        Long groupSubjectMatches = em.createQuery("""
                select count(g)
                from StudentGroup g
                join g.subjects subject
                where g.id = :groupId
                and subject.id = :subjectId
                """, Long.class)
                .setParameter("groupId", entry.getGroup().getId())
                .setParameter("subjectId", entry.getSubject().getId())
                .getSingleResult();
        if (groupSubjectMatches == 0) {
            throw new IllegalArgumentException("Subject is not assigned to the selected group.");
        }
        if (entry.getRoom() != null) {
            Integer capacity = em.createQuery("select r.capacity from Room r where r.id = :roomId", Integer.class)
                    .setParameter("roomId", entry.getRoom().getId())
                    .getSingleResult();
            Long groupSize = em.createQuery("select count(s) from Student s where s.group.id = :groupId", Long.class)
                    .setParameter("groupId", entry.getGroup().getId())
                    .getSingleResult();
            if (capacity < groupSize) {
                throw new IllegalArgumentException("Room capacity is lower than the selected group size.");
            }
        }
    }

    public List<WeeklyScheduleEntry> findByTeacherIdWithDetails(Long teacherId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct e
                    from WeeklyScheduleEntry e
                    join fetch e.group
                    join fetch e.semester
                    join fetch e.field
                    join fetch e.subject
                    join fetch e.teacher t
                    left join fetch e.room
                    where t.id = :teacherId
                    order by e.dayOfWeek, e.startTime
                    """, WeeklyScheduleEntry.class)
                    .setParameter("teacherId", teacherId)
                    .getResultList();
        }
    }

    public List<WeeklyScheduleEntry> findByGroupIdWithDetails(Long groupId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct e
                    from WeeklyScheduleEntry e
                    join fetch e.group g
                    join fetch e.semester
                    join fetch e.field
                    join fetch e.subject
                    join fetch e.teacher t
                    left join fetch t.emails
                    left join fetch e.room
                    where g.id = :groupId
                    order by e.dayOfWeek, e.startTime
                    """, WeeklyScheduleEntry.class)
                    .setParameter("groupId", groupId)
                    .getResultList();
        }
    }
}
