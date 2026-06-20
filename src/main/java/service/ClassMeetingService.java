package service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import model.ClassMeeting;
import model.Room;
import model.RoomBooking;
import model.StudentGroup;
import model.Subject;
import model.Teacher;
import model.enums.BookingStatus;
import model.enums.ClassMeetingStatus;
import model.enums.MeetingMode;
import model.value.MeetingSlot;
import persistence.JpaUtil;
import persistence.RoomBookingRepository;

import java.time.LocalDateTime;

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
            StudentGroup group = em.find(StudentGroup.class, meeting.getGroup().getId());
            if (!teacher.isQualifiedFor(subject)) {
                throw new IllegalArgumentException("Teacher is not qualified to teach this subject.");
            }
            validateMeetingDayMatchesDate(meeting);
            validateCreationStatus(meeting);
            validateMeetingDateInsideSemester(meeting, group);
            validateSubjectAvailableForGroup(em, subject.getId(), group.getId());
            validateClassMeetingAvailability(em, meeting, teacher, group);

            Room room = null;
            if (meeting.getMeetingMode() == MeetingMode.CLASSROOM) {
                if (meeting.isScheduled() && (roomId == null || slot == null)) {
                    throw new IllegalArgumentException("Classroom meeting requires room and meeting slot.");
                }
                if (roomId != null || slot != null) {
                    if (roomId == null || slot == null) {
                        throw new IllegalArgumentException("Room booking requires both room and meeting slot.");
                    }
                    room = em.find(Room.class, roomId);
                    if (room == null) {
                        throw new IllegalArgumentException("Room not found: " + roomId);
                    }
                    validateRoomCapacity(em, room, group);
                    if (roomBookingRepository.existsOverlappingActiveBooking(roomId, slot)) {
                        throw new IllegalArgumentException("Room is already booked for an overlapping time slot.");
                    }
                    if (meeting.getLocation() == null || meeting.getLocation().isBlank()) {
                        meeting.setLocation(room.getRoomNumber());
                    }
                }
            }
            if (meeting.getMeetingMode() == MeetingMode.ONLINE
                    && meeting.isScheduled()
                    && (meeting.getOnlineMeetingLink() == null || meeting.getOnlineMeetingLink().isBlank())) {
                throw new IllegalArgumentException("Online meeting link is required.");
            }
            validateMeetingDetails(meeting);

            meeting.setTeacher(teacher);
            meeting.setSubject(subject);
            meeting.setGroup(group);
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
        if (!meeting.isScheduled() && !meeting.isCompleted()) {
            throw new IllegalArgumentException("Attendance can only be marked for scheduled or completed class meetings.");
        }
        LocalDateTime meetingStart = LocalDateTime.of(
                meeting.getMeetingDate(),
                meeting.getTime().getStartTime()
        );
        if (LocalDateTime.now().isBefore(meetingStart)) {
            throw new IllegalArgumentException("Attendance cannot be marked before the class meeting starts.");
        }
    }

    public void cancelClassMeeting(Long meetingId, String comment) {
        var em = JpaUtil.entityManagerFactory().createEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            ClassMeeting meeting = em.createQuery("""
                    select cm
                    from ClassMeeting cm
                    left join fetch cm.roomBooking rb
                    where cm.id = :id
                    """, ClassMeeting.class)
                    .setParameter("id", meetingId)
                    .getSingleResult();
            if (meeting == null) {
                throw new IllegalArgumentException("Class meeting not found: " + meetingId);
            }
            meeting.cancel(comment);
            if (meeting.getRoomBooking() != null) {
                meeting.getRoomBooking().setBookingStatus(BookingStatus.CANCELLED);
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
            validateMeetingDetails(meeting);
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

    private void validateCreationStatus(ClassMeeting meeting) {
        if (meeting.getStatus() == null) {
            meeting.setStatus(ClassMeetingStatus.SCHEDULED);
        }
        if (meeting.getStatus() == ClassMeetingStatus.CANCELLED || meeting.getStatus() == ClassMeetingStatus.COMPLETED) {
            throw new IllegalArgumentException("Create class meetings as draft or scheduled. Cancel or complete them through the lifecycle actions.");
        }
    }

    private void validateMeetingDetails(ClassMeeting meeting) {
        if (meeting.getStatus() == ClassMeetingStatus.DRAFT || meeting.getStatus() == ClassMeetingStatus.CANCELLED) {
            return;
        }
        if (meeting.getMeetingMode() == MeetingMode.ONLINE
                && (meeting.getOnlineMeetingLink() == null || meeting.getOnlineMeetingLink().isBlank())) {
            throw new IllegalArgumentException("Online meeting link is required.");
        }
        if (meeting.getMeetingMode() == MeetingMode.CLASSROOM
                && (meeting.getLocation() == null || meeting.getLocation().isBlank())) {
            throw new IllegalArgumentException("Classroom meeting requires a room or location.");
        }
    }

    private void validateMeetingDayMatchesDate(ClassMeeting meeting) {
        if (meeting.getMeetingDate() != null
                && meeting.getTime() != null
                && meeting.getTime().getDayOfWeek() != null
                && !meeting.getMeetingDate().getDayOfWeek().equals(meeting.getTime().getDayOfWeek())) {
            throw new IllegalArgumentException("Meeting day of week must match the selected meeting date.");
        }
    }

    private void validateMeetingDateInsideSemester(ClassMeeting meeting, StudentGroup group) {
        if (group.getSemester().getStartDate() != null && meeting.getMeetingDate().isBefore(group.getSemester().getStartDate())) {
            throw new IllegalArgumentException("Class meeting date is before the group's semester start date.");
        }
        if (group.getSemester().getEndDate() != null && meeting.getMeetingDate().isAfter(group.getSemester().getEndDate())) {
            throw new IllegalArgumentException("Class meeting date is after the group's semester end date.");
        }
    }

    private void validateSubjectAvailableForGroup(EntityManager em, Long subjectId, Long groupId) {
        Long semesterMatches = em.createQuery("""
                select count(c)
                from StudentGroup g, SemesterFieldSubject c
                where g.id = :groupId
                and c.semesterField = g.semesterField
                and c.subject.id = :subjectId
                """, Long.class)
                .setParameter("groupId", groupId)
                .setParameter("subjectId", subjectId)
                .getSingleResult();
        if (semesterMatches == 0) {
            throw new IllegalArgumentException("Subject is not available in the group's semester and field.");
        }
    }

    private void validateClassMeetingAvailability(EntityManager em, ClassMeeting meeting, Teacher teacher, StudentGroup group) {
        validateNoOverlappingMeeting(em, meeting, "cm.group.id", group.getId(), "Selected group");
        validateNoOverlappingMeeting(em, meeting, "cm.teacher.id", teacher.getId(), "Selected teacher");
    }

    private void validateNoOverlappingMeeting(EntityManager em, ClassMeeting meeting, String propertyPath, Long entityId, String label) {
        StringBuilder jpql = new StringBuilder("""
                select count(cm)
                from ClassMeeting cm
                where cm.status <> :cancelled
                and cm.meetingDate = :meetingDate
                and cm.time.startTime < :endTime
                and cm.time.endTime > :startTime
                and %s = :entityId
                """.formatted(propertyPath));
        if (meeting.getId() != null) {
            jpql.append(" and cm.id <> :meetingId");
        }
        var query = em.createQuery(jpql.toString(), Long.class)
                .setParameter("cancelled", ClassMeetingStatus.CANCELLED)
                .setParameter("meetingDate", meeting.getMeetingDate())
                .setParameter("startTime", meeting.getTime().getStartTime())
                .setParameter("endTime", meeting.getTime().getEndTime())
                .setParameter("entityId", entityId);
        if (meeting.getId() != null) {
            query.setParameter("meetingId", meeting.getId());
        }
        if (query.getSingleResult() > 0) {
            throw new IllegalArgumentException(label + " already has an overlapping class meeting.");
        }
    }

    private void validateRoomCapacity(EntityManager em, Room room, StudentGroup group) {
        Long groupSize = em.createQuery("select count(s) from Student s where s.group.id = :groupId", Long.class)
                .setParameter("groupId", group.getId())
                .getSingleResult();
        if (room.getCapacity() < groupSize) {
            throw new IllegalArgumentException("Room capacity is lower than the selected group size.");
        }
    }
}
