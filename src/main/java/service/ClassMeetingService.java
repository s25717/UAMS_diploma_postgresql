package service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import model.ClassMeeting;
import model.Room;
import model.RoomBooking;
import model.Subject;
import model.Teacher;
import model.enums.BookingStatus;
import model.enums.ClassMeetingStatus;
import model.enums.MeetingMode;
import model.value.MeetingSlot;
import persistence.JpaUtil;
import persistence.RoomBookingRepository;

public class ClassMeetingService {
    private final RoomBookingRepository roomBookingRepository = new RoomBookingRepository();

    public ClassMeeting createClassMeeting(ClassMeeting meeting, Long roomId, MeetingSlot slot) {
        EntityManager em = JpaUtil.entityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Teacher teacher = em.createQuery("""
                    select distinct t
                    from Teacher t
                    left join fetch t.qualifiedSubjects
                    where t.id = :id
                    """, Teacher.class)
                    .setParameter("id", meeting.getTeacher().getId())
                    .getSingleResult();
            Subject subject = em.find(Subject.class, meeting.getSubject().getId());
            if (!teacher.isQualifiedFor(subject)) {
                throw new IllegalArgumentException("Teacher is not qualified to teach this subject.");
            }

            Room room = null;
            if (meeting.getMeetingMode() == MeetingMode.CLASSROOM && meeting.getStatus() != ClassMeetingStatus.DRAFT) {
                if (roomId == null || slot == null) {
                    throw new IllegalArgumentException("Classroom meeting requires room and meeting slot.");
                }
                room = em.find(Room.class, roomId);
                if (room == null) {
                    throw new IllegalArgumentException("Room not found: " + roomId);
                }
                if (roomBookingRepository.existsForRoomAndSlot(roomId, slot)) {
                    throw new IllegalArgumentException("Room is already booked for this time slot.");
                }
            }
            if (meeting.getMeetingMode() == MeetingMode.ONLINE
                    && meeting.getStatus() != ClassMeetingStatus.DRAFT
                    && (meeting.getOnlineMeetingLink() == null || meeting.getOnlineMeetingLink().isBlank())) {
                throw new IllegalArgumentException("Online meeting link is required.");
            }

            meeting.setTeacher(teacher);
            meeting.setSubject(subject);
            ClassMeeting managedMeeting = em.merge(meeting);
            if (room != null) {
                RoomBooking booking = new RoomBooking(slot, BookingStatus.CONFIRMED, room, managedMeeting);
                em.persist(booking);
            }

            tx.commit();
            return managedMeeting;
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public void validateCanMarkAttendance(Long teacherId, ClassMeeting meeting) {
        if (!meeting.getTeacher().getId().equals(teacherId)) {
            throw new IllegalArgumentException("You are not assigned to this class meeting.");
        }
        if (meeting.getStatus() == ClassMeetingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot mark attendance for a cancelled class meeting.");
        }
        java.time.LocalDateTime meetingStart = java.time.LocalDateTime.of(
                meeting.getMeetingDate(),
                meeting.getTime().getStartTime()
        );
        if (java.time.LocalDateTime.now().isBefore(meetingStart)) {
            throw new IllegalArgumentException("Attendance cannot be marked before the class meeting starts.");
        }
    }

    public void cancelClassMeeting(Long meetingId, String comment) {
        var em = JpaUtil.entityManagerFactory().createEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            ClassMeeting meeting = em.find(ClassMeeting.class, meetingId);
            if (meeting == null) {
                throw new IllegalArgumentException("Class meeting not found: " + meetingId);
            }
            meeting.setStatus(ClassMeetingStatus.CANCELLED);
            meeting.setComment(comment);
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

    public void saveComment(Long meetingId, String comment) {
        var em = JpaUtil.entityManagerFactory().createEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            ClassMeeting meeting = em.find(ClassMeeting.class, meetingId);
            if (meeting == null) {
                throw new IllegalArgumentException("Class meeting not found: " + meetingId);
            }
            meeting.setComment(comment);
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

    public void saveLocationAndOnlineLink(Long meetingId, String location, String onlineMeetingLink) {
        var em = JpaUtil.entityManagerFactory().createEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            ClassMeeting meeting = em.find(ClassMeeting.class, meetingId);
            if (meeting == null) {
                throw new IllegalArgumentException("Class meeting not found: " + meetingId);
            }
            meeting.setLocation(location);
            meeting.setOnlineMeetingLink(onlineMeetingLink);
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
}
