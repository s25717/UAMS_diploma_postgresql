package persistence;

import jakarta.persistence.EntityManager;
import model.Room;
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
                    em.getReference(Semester.class, entry.getSemester().getId())
            );
            em.persist(managed);
            return managed;
        });
    }

    public List<WeeklyScheduleEntry> findByTeacherIdWithDetails(Long teacherId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct e
                    from WeeklyScheduleEntry e
                    join fetch e.group
                    join fetch e.semester
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
