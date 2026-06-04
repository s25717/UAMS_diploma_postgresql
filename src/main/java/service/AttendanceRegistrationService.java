package service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import model.Attendance;
import model.ClassMeeting;
import model.Student;
import model.enums.AttendanceStatus;
import model.enums.ClassMeetingStatus;
import persistence.ClassMeetingRepository;
import persistence.JpaUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AttendanceRegistrationService {
    private final ClassMeetingRepository classMeetingRepository = new ClassMeetingRepository();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public List<ClassMeeting> getClassMeetings() {
        return classMeetingRepository.findAllWithBasicData();
    }

    public ClassMeeting getClassMeetingWithStudents(Long classMeetingId) {
        return classMeetingRepository.findByIdWithGroupAndStudents(classMeetingId)
                .orElseThrow(() -> new IllegalArgumentException("Class meeting not found: " + classMeetingId));
    }

    public void registerAttendance(Long classMeetingId, Map<Long, AttendanceStatus> statusesByStudentId) {
        registerAttendance(classMeetingId, statusesByStudentId, Map.of());
    }

    public void registerAttendance(Long classMeetingId, Map<Long, AttendanceStatus> statusesByStudentId,
                                   Map<Long, String> commentsByStudentId) {
        EntityManager em = JpaUtil.entityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            ClassMeeting meeting = em.createQuery("""
                    select distinct cm
                    from ClassMeeting cm
                    left join fetch cm.attendances a
                    left join fetch a.student
                    where cm.id = :id
                    """, ClassMeeting.class)
                    .setParameter("id", classMeetingId)
                    .getSingleResult();
            if (meeting.getStatus() == ClassMeetingStatus.CANCELLED) {
                throw new IllegalArgumentException("Cannot mark attendance for a cancelled class meeting.");
            }

            Map<Long, Attendance> existingByStudentId = meeting.getAttendances()
                    .stream()
                    .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

            for (Map.Entry<Long, AttendanceStatus> entry : statusesByStudentId.entrySet()) {
                Long studentId = entry.getKey();
                AttendanceStatus status = entry.getValue();
                if (status == null) {
                    throw new IllegalArgumentException("Attendance status is required.");
                }

                Attendance attendance = existingByStudentId.get(studentId);
                if (attendance == null) {
                    Student student = em.find(Student.class, studentId);
                    if (student == null) {
                        throw new IllegalArgumentException("Student not found: " + studentId);
                    }
                    attendance = new Attendance(status, student, meeting);
                    attendance.setComment(commentsByStudentId.get(studentId));
                    validate(attendance);
                    meeting.addAttendance(attendance);
                } else {
                    attendance.setStatus(status);
                    attendance.setComment(commentsByStudentId.get(studentId));
                    validate(attendance);
                }
            }

            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public Map<Long, AttendanceStatus> getSavedStatuses(Long classMeetingId) {
        EntityManager em = JpaUtil.entityManagerFactory().createEntityManager();
        try {
            List<Attendance> attendances = em.createQuery("""
                    select a
                    from Attendance a
                    join fetch a.student
                    where a.classMeeting.id = :classMeetingId
                    """, Attendance.class)
                    .setParameter("classMeetingId", classMeetingId)
                    .getResultList();

            Map<Long, AttendanceStatus> result = new LinkedHashMap<>();
            for (Attendance attendance : attendances) {
                result.put(attendance.getStudent().getId(), attendance.getStatus());
            }
            return result;
        } finally {
            em.close();
        }
    }

    public Map<Long, String> getSavedComments(Long classMeetingId) {
        EntityManager em = JpaUtil.entityManagerFactory().createEntityManager();
        try {
            List<Attendance> attendances = em.createQuery("""
                    select a
                    from Attendance a
                    join fetch a.student
                    where a.classMeeting.id = :classMeetingId
                    """, Attendance.class)
                    .setParameter("classMeetingId", classMeetingId)
                    .getResultList();

            Map<Long, String> result = new LinkedHashMap<>();
            for (Attendance attendance : attendances) {
                result.put(attendance.getStudent().getId(), attendance.getComment());
            }
            return result;
        } finally {
            em.close();
        }
    }

    private void validate(Attendance attendance) {
        Set<ConstraintViolation<Attendance>> violations = validator.validate(attendance);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException(message);
        }
    }
}
