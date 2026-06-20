package persistence;

import jakarta.persistence.EntityManager;
import model.ClassMeeting;
import model.StudentGroup;
import model.Subject;
import model.Teacher;
import model.WeeklyScheduleEntry;
import model.enums.ClassMeetingStatus;
import model.enums.ClassType;
import model.enums.MeetingMode;

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
                    join fetch e.sourceClassMeeting cm
                    join fetch cm.group g
                    join fetch g.semesterField sf
                    join fetch sf.semester sem
                    join fetch sf.field
                    join fetch cm.subject s
                    join fetch cm.teacher t
                    left join fetch t.emails
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    order by g.code, cm.time.dayOfWeek, cm.time.startTime
                    """, WeeklyScheduleEntry.class)
                    .getResultList();
        }
    }

    public WeeklyScheduleEntry saveWithManagedReferences(WeeklyScheduleEntry entry) {
        if (entry == null || entry.getSourceClassMeeting() == null || entry.getSourceClassMeeting().getId() == null) {
            throw new IllegalArgumentException("Select a source class meeting first.");
        }
        return createFromClassMeeting(entry.getSourceClassMeeting().getId());
    }

    public WeeklyScheduleEntry createFromClassMeeting(Long classMeetingId) {
        return executeInTransaction(em -> {
            ClassMeeting source = em.createQuery("""
                    select distinct cm
                    from ClassMeeting cm
                    join fetch cm.group g
                    join fetch g.semesterField sf
                    join fetch sf.semester
                    join fetch sf.field
                    join fetch cm.subject
                    join fetch cm.teacher t
                    left join fetch t.qualifiedSubjects
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    where cm.id = :id
                    """, ClassMeeting.class)
                    .setParameter("id", classMeetingId)
                    .getResultStream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Class meeting not found."));
            validateBusinessRules(em, source);
            WeeklyScheduleEntry managed = new WeeklyScheduleEntry(source);
            em.persist(managed);
            source.setScheduleEntry(managed);
            return managed;
        });
    }

    private void validateBusinessRules(EntityManager em, ClassMeeting source) {
        if (source.getStatus() == ClassMeetingStatus.CANCELLED || source.getStatus() == ClassMeetingStatus.COMPLETED) {
            throw new IllegalArgumentException("Only draft or scheduled class meetings can become weekly schedule entries.");
        }
        Long existingSource = em.createQuery("""
                select count(e)
                from WeeklyScheduleEntry e
                where e.sourceClassMeeting.id = :meetingId
                """, Long.class)
                .setParameter("meetingId", source.getId())
                .getSingleResult();
        if (existingSource > 0) {
            throw new IllegalArgumentException("This class meeting is already marked as a weekly schedule entry.");
        }
        Long semesterSubjectMatches = em.createQuery("""
                select count(c)
                from SemesterFieldSubject c
                where c.semesterField.id = :semesterFieldId
                and c.subject.id = :subjectId
                """, Long.class)
                .setParameter("semesterFieldId", source.getGroup().getSemesterField().getId())
                .setParameter("subjectId", source.getSubject().getId())
                .getSingleResult();
        if (semesterSubjectMatches == 0) {
            throw new IllegalArgumentException("Subject is not available in the selected semester and field.");
        }
        if (!source.getTeacher().isQualifiedFor(source.getSubject())) {
            throw new IllegalArgumentException("Teacher is not qualified to teach this subject.");
        }
        if (source.getRoomBooking() != null && source.getRoomBooking().getRoom() != null) {
            Integer capacity = em.createQuery("select r.capacity from Room r where r.id = :roomId", Integer.class)
                    .setParameter("roomId", source.getRoomBooking().getRoom().getId())
                    .getSingleResult();
            Long groupSize = em.createQuery("select count(s) from Student s where s.group.id = :groupId", Long.class)
                    .setParameter("groupId", source.getGroup().getId())
                    .getSingleResult();
            if (capacity < groupSize) {
                throw new IllegalArgumentException("Room capacity is lower than the selected group size.");
            }
        }
        validateNoTemplateConflicts(em, source);
    }

    private void validateNoTemplateConflicts(EntityManager em, ClassMeeting source) {
        Long groupConflicts = conflictCount(em, source, "existing.group.id = :entityId", source.getGroup().getId());
        if (groupConflicts > 0) {
            throw new IllegalArgumentException("Selected group already has a weekly schedule entry at this time.");
        }
        Long teacherConflicts = conflictCount(em, source, "existing.teacher.id = :entityId", source.getTeacher().getId());
        if (teacherConflicts > 0) {
            throw new IllegalArgumentException("Selected teacher already has a weekly schedule entry at this time.");
        }
        if (source.getRoomBooking() != null && source.getRoomBooking().getRoom() != null) {
            Long roomConflicts = em.createQuery("""
                    select count(e)
                    from WeeklyScheduleEntry e
                    join e.sourceClassMeeting existing
                    join existing.roomBooking existingBooking
                    where existing.id <> :sourceId
                    and existing.time.dayOfWeek = :day
                    and existing.time.startTime < :end
                    and existing.time.endTime > :start
                    and existingBooking.room.id = :roomId
                    """, Long.class)
                    .setParameter("sourceId", source.getId())
                    .setParameter("day", source.getTime().getDayOfWeek())
                    .setParameter("start", source.getTime().getStartTime())
                    .setParameter("end", source.getTime().getEndTime())
                    .setParameter("roomId", source.getRoomBooking().getRoom().getId())
                    .getSingleResult();
            if (roomConflicts > 0) {
                throw new IllegalArgumentException("Selected room already has a weekly schedule entry at this time.");
            }
        }
    }

    private Long conflictCount(EntityManager em, ClassMeeting source, String predicate, Long entityId) {
        return em.createQuery("""
                select count(e)
                from WeeklyScheduleEntry e
                join e.sourceClassMeeting existing
                where existing.id <> :sourceId
                and existing.time.dayOfWeek = :day
                and existing.time.startTime < :end
                and existing.time.endTime > :start
                and %s
                """.formatted(predicate), Long.class)
                .setParameter("sourceId", source.getId())
                .setParameter("day", source.getTime().getDayOfWeek())
                .setParameter("start", source.getTime().getStartTime())
                .setParameter("end", source.getTime().getEndTime())
                .setParameter("entityId", entityId)
                .getSingleResult();
    }

    public List<WeeklyScheduleEntry> findByTeacherIdWithDetails(Long teacherId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct e
                    from WeeklyScheduleEntry e
                    join fetch e.sourceClassMeeting cm
                    join fetch cm.group g
                    join fetch g.semesterField sf
                    join fetch sf.semester
                    join fetch sf.field
                    join fetch cm.subject
                    join fetch cm.teacher t
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    where t.id = :teacherId
                    order by cm.time.dayOfWeek, cm.time.startTime
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
                    join fetch e.sourceClassMeeting cm
                    join fetch cm.group g
                    join fetch g.semesterField sf
                    join fetch sf.semester
                    join fetch sf.field
                    join fetch cm.subject
                    join fetch cm.teacher t
                    left join fetch t.emails
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    where g.id = :groupId
                    order by cm.time.dayOfWeek, cm.time.startTime
                    """, WeeklyScheduleEntry.class)
                    .setParameter("groupId", groupId)
                    .getResultList();
        }
    }

    public List<WeeklyScheduleEntry> findWithFilters(Long groupId, Long teacherId, ClassType classType, MeetingMode meetingMode) {
        try (EntityManager em = emf.createEntityManager()) {
            StringBuilder jpql = new StringBuilder("""
                    select distinct e
                    from WeeklyScheduleEntry e
                    join fetch e.sourceClassMeeting cm
                    join fetch cm.group g
                    join fetch g.semesterField sf
                    join fetch sf.semester
                    join fetch sf.field
                    join fetch cm.subject
                    join fetch cm.teacher t
                    left join fetch t.emails
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    where 1 = 1
                    """);
            if (groupId != null) {
                jpql.append(" and g.id = :groupId");
            }
            if (teacherId != null) {
                jpql.append(" and t.id = :teacherId");
            }
            if (classType != null) {
                jpql.append(" and cm.classType = :classType");
            }
            if (meetingMode != null) {
                jpql.append(" and cm.meetingMode = :meetingMode");
            }
            jpql.append(" order by g.code, cm.time.dayOfWeek, cm.time.startTime");
            var query = em.createQuery(jpql.toString(), WeeklyScheduleEntry.class);
            if (groupId != null) {
                query.setParameter("groupId", groupId);
            }
            if (teacherId != null) {
                query.setParameter("teacherId", teacherId);
            }
            if (classType != null) {
                query.setParameter("classType", classType);
            }
            if (meetingMode != null) {
                query.setParameter("meetingMode", meetingMode);
            }
            return query.getResultList();
        }
    }
}
