package persistence;

import jakarta.persistence.EntityManager;
import model.Attendance;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AttendanceRepository extends GenericRepository<Attendance> {
    public AttendanceRepository() {
        super(Attendance.class);
    }

    public Optional<Attendance> findByStudentAndClassMeeting(Long studentId, Long classMeetingId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select a
                    from Attendance a
                    join fetch a.student s
                    join fetch a.classMeeting cm
                    where s.id = :studentId and cm.id = :classMeetingId
                    """, Attendance.class)
                    .setParameter("studentId", studentId)
                    .setParameter("classMeetingId", classMeetingId)
                    .getResultStream()
                    .findFirst();
        }
    }

    public boolean existsByStudentAndClassMeeting(Long studentId, Long classMeetingId) {
        try (EntityManager em = emf.createEntityManager()) {
            Long count = em.createQuery("""
                    select count(a)
                    from Attendance a
                    where a.student.id = :studentId and a.classMeeting.id = :classMeetingId
                    """, Long.class)
                    .setParameter("studentId", studentId)
                    .setParameter("classMeetingId", classMeetingId)
                    .getSingleResult();
            return count > 0;
        }
    }

    public List<Attendance> findByClassMeetingId(Long classMeetingId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select a
                    from Attendance a
                    join fetch a.student
                    where a.classMeeting.id = :classMeetingId
                    order by a.student.surname, a.student.name
                    """, Attendance.class)
                    .setParameter("classMeetingId", classMeetingId)
                    .getResultList();
        }
    }

    public List<Attendance> findByClassMeetingIds(Set<Long> meetingIds) {
        if (meetingIds == null || meetingIds.isEmpty()) {
            return List.of();
        }
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select a
                    from Attendance a
                    join fetch a.student
                    join fetch a.classMeeting
                    where a.classMeeting.id in :meetingIds
                    """, Attendance.class)
                    .setParameter("meetingIds", meetingIds)
                    .getResultList();
        }
    }

    public List<Attendance> findAttendanceHistoryForStudent(Long studentId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select a
                    from Attendance a
                    join fetch a.student
                    join fetch a.classMeeting cm
                    join fetch cm.subject
                    join fetch cm.teacher
                    left join fetch cm.roomBooking rb
                    left join fetch rb.room
                    where a.student.id = :studentId
                    order by cm.id
                    """, Attendance.class)
                    .setParameter("studentId", studentId)
                    .getResultList();
        }
    }

    public List<Attendance> findAllWithStudentAndClassMeeting() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select a
                    from Attendance a
                    join fetch a.student
                    join fetch a.classMeeting cm
                    join fetch cm.subject
                    join fetch cm.teacher
                    """, Attendance.class)
                    .getResultList();
        }
    }
}
