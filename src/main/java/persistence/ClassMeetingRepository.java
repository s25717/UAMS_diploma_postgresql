package persistence;

import jakarta.persistence.EntityManager;
import model.Attendance;
import model.ClassMeeting;
import model.enums.ClassMeetingStatus;
import service.AttendanceReportFilter;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ClassMeetingRepository extends GenericRepository<ClassMeeting> {
    public ClassMeetingRepository() {
        super(ClassMeeting.class);
    }

    public List<ClassMeeting> findAllWithBasicData() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct cm
                    from ClassMeeting cm
                    join fetch cm.subject
                    join fetch cm.teacher
                    join fetch cm.group
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    left join fetch cm.scheduleEntry
                    order by cm.id
                    """, ClassMeeting.class)
                    .getResultList();
        }
    }

    public List<ClassMeeting> findByTeacherId(Long teacherId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct cm
                    from ClassMeeting cm
                    join fetch cm.subject
                    join fetch cm.teacher t
                    join fetch cm.group
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    where t.id = :teacherId
                    order by cm.id
                    """, ClassMeeting.class)
                    .setParameter("teacherId", teacherId)
                    .getResultList();
        }
    }

    public Optional<ClassMeeting> findByIdWithAttendance(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct cm
                    from ClassMeeting cm
                    left join fetch cm.attendances a
                    left join fetch a.student
                    where cm.id = :id
                    """, ClassMeeting.class)
                    .setParameter("id", id)
                    .getResultStream()
                    .findFirst();
        }
    }

    public Optional<ClassMeeting> findByIdWithGroupAndStudents(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct cm
                    from ClassMeeting cm
                    join fetch cm.subject
                    join fetch cm.teacher
                    join fetch cm.group g
                    left join fetch g.students
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    where cm.id = :id
                    """, ClassMeeting.class)
                    .setParameter("id", id)
                    .getResultStream()
                    .findFirst();
        }
    }

    public List<Attendance> findAttendanceByMeetingId(Long meetingId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select a
                    from ClassMeeting cm
                    join cm.attendances a
                    join fetch a.student
                    where cm.id = :meetingId
                    """, Attendance.class)
                    .setParameter("meetingId", meetingId)
                    .getResultList();
        }
    }

    public List<ClassMeeting> findByReportFilter(AttendanceReportFilter filter) {
        try (EntityManager em = emf.createEntityManager()) {
            StringBuilder jpql = new StringBuilder("""
                    select distinct cm
                    from ClassMeeting cm
                    join fetch cm.subject s
                    join fetch cm.teacher t
                    join fetch cm.group g
                    join fetch g.semester sem
                    left join fetch g.students
                    left join fetch cm.roomBooking rb
                    where 1 = 1
                    """);
            Map<String, Object> parameters = new HashMap<>();

            if (filter.getSemesterIds() != null && !filter.getSemesterIds().isEmpty()) {
                jpql.append(" and sem.id in :semesterIds");
                parameters.put("semesterIds", filter.getSemesterIds());
            }
            if (filter.getTeacherId() != null) {
                jpql.append(" and t.id = :teacherId");
                parameters.put("teacherId", filter.getTeacherId());
            }
            if (filter.getSubjectId() != null) {
                jpql.append(" and s.id = :subjectId");
                parameters.put("subjectId", filter.getSubjectId());
            }
            if (filter.getGroupId() != null) {
                jpql.append(" and g.id = :groupId");
                parameters.put("groupId", filter.getGroupId());
            }
            if (filter.getClassType() != null) {
                jpql.append(" and cm.classType = :classType");
                parameters.put("classType", filter.getClassType());
            }
            if (filter.getDateFrom() != null) {
                jpql.append(" and cm.meetingDate >= :dateFrom");
                parameters.put("dateFrom", filter.getDateFrom());
            }
            if (filter.getDateTo() != null) {
                jpql.append(" and cm.meetingDate <= :dateTo");
                parameters.put("dateTo", filter.getDateTo());
            }
            jpql.append(" order by cm.id");

            var query = em.createQuery(jpql.toString(), ClassMeeting.class);
            parameters.forEach(query::setParameter);
            return query.getResultList();
        }
    }

    public List<ClassMeeting> findClassMeetingHistoryForStudent(Long studentId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct cm
                    from ClassMeeting cm
                    join fetch cm.subject
                    join fetch cm.teacher
                    join fetch cm.group g
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    left join g.students s
                    where s.id = :studentId
                    order by cm.id
                    """, ClassMeeting.class)
                    .setParameter("studentId", studentId)
                    .getResultList();
        }
    }

    public List<ClassMeeting> findClassMeetingHistoryForTeacher(Long teacherId) {
        return findByTeacherId(teacherId);
    }

    public List<ClassMeeting> findByDateRange(LocalDate from, LocalDate to) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct cm
                    from ClassMeeting cm
                    join fetch cm.subject
                    join fetch cm.teacher
                    join fetch cm.group
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    where cm.meetingDate between :from and :to
                    and cm.status in :statuses
                    order by cm.meetingDate, cm.time.startTime
                    """, ClassMeeting.class)
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .setParameter("statuses", EnumSet.of(
                            ClassMeetingStatus.SCHEDULED,
                            ClassMeetingStatus.CANCELLED,
                            ClassMeetingStatus.COMPLETED
                    ))
                    .getResultList();
        }
    }

    public List<ClassMeeting> findClassMeetingsForCurrentWeek() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        return findByDateRange(start, start.plusDays(6));
    }

    public List<ClassMeeting> findUpcomingClassMeetingsForStudent(Long studentId) {
        return findMeetingsForStudentByTime(studentId, true);
    }

    public List<ClassMeeting> findPastClassMeetingsForStudent(Long studentId) {
        return findMeetingsForStudentByTime(studentId, false);
    }

    public List<ClassMeeting> findUpcomingClassMeetingsForTeacher(Long teacherId) {
        return findMeetingsForTeacherByTime(teacherId, true);
    }

    public List<ClassMeeting> findPastClassMeetingsForTeacher(Long teacherId) {
        return findMeetingsForTeacherByTime(teacherId, false);
    }

    private List<ClassMeeting> findMeetingsForStudentByTime(Long studentId, boolean upcoming) {
        try (EntityManager em = emf.createEntityManager()) {
            String operator = upcoming ? ">=" : "<";
            Set<ClassMeetingStatus> statuses = upcoming
                    ? Set.of(ClassMeetingStatus.SCHEDULED)
                    : EnumSet.of(ClassMeetingStatus.SCHEDULED, ClassMeetingStatus.CANCELLED, ClassMeetingStatus.COMPLETED);
            return em.createQuery("""
                    select distinct cm
                    from ClassMeeting cm
                    join fetch cm.subject
                    join fetch cm.teacher
                    join fetch cm.group g
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    join g.students s
                    where s.id = :studentId
                    and cm.status in :statuses
                    and cm.meetingDate %s :today
                    order by cm.meetingDate, cm.time.startTime
                    """.formatted(operator), ClassMeeting.class)
                    .setParameter("studentId", studentId)
                    .setParameter("statuses", statuses)
                    .setParameter("today", LocalDate.now())
                    .getResultList();
        }
    }

    private List<ClassMeeting> findMeetingsForTeacherByTime(Long teacherId, boolean upcoming) {
        try (EntityManager em = emf.createEntityManager()) {
            String operator = upcoming ? ">=" : "<";
            Set<ClassMeetingStatus> statuses = upcoming
                    ? Set.of(ClassMeetingStatus.SCHEDULED)
                    : EnumSet.of(ClassMeetingStatus.SCHEDULED, ClassMeetingStatus.CANCELLED, ClassMeetingStatus.COMPLETED);
            return em.createQuery("""
                    select distinct cm
                    from ClassMeeting cm
                    join fetch cm.subject
                    join fetch cm.teacher t
                    join fetch cm.group
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    where t.id = :teacherId
                    and cm.status in :statuses
                    and cm.meetingDate %s :today
                    order by cm.meetingDate, cm.time.startTime
                    """.formatted(operator), ClassMeeting.class)
                    .setParameter("teacherId", teacherId)
                    .setParameter("statuses", statuses)
                    .setParameter("today", LocalDate.now())
                    .getResultList();
        }
    }

    public void removeAttendanceFromMeeting(Long meetingId, Long attendanceId) {
        executeInTransaction(em -> {
            ClassMeeting meeting = em.createQuery("""
                    select distinct cm
                    from ClassMeeting cm
                    left join fetch cm.attendances
                    where cm.id = :meetingId
                    """, ClassMeeting.class)
                    .setParameter("meetingId", meetingId)
                    .getSingleResult();

            Attendance attendance = em.find(Attendance.class, attendanceId);
            if (attendance != null) {
                meeting.removeAttendance(attendance);
            }
            return null;
        });
    }
}
