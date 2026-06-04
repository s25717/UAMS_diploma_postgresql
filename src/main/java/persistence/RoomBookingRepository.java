package persistence;

import jakarta.persistence.EntityManager;
import model.RoomBooking;
import model.enums.BookingStatus;
import model.value.MeetingSlot;

import java.util.List;

public class RoomBookingRepository extends GenericRepository<RoomBooking> {
    public RoomBookingRepository() {
        super(RoomBooking.class);
    }

    public boolean existsForRoomAndSlot(Long roomId, MeetingSlot slot) {
        try (EntityManager em = emf.createEntityManager()) {
            Long count = em.createQuery("""
                    select count(rb)
                    from RoomBooking rb
                    where rb.room.id = :roomId
                    and rb.meetingSlot.date = :date
                    and rb.meetingSlot.startTime = :startTime
                    and rb.meetingSlot.endTime = :endTime
                    and rb.bookingStatus <> :cancelled
                    """, Long.class)
                    .setParameter("roomId", roomId)
                    .setParameter("date", slot.getDate())
                    .setParameter("startTime", slot.getStartTime())
                    .setParameter("endTime", slot.getEndTime())
                    .setParameter("cancelled", BookingStatus.CANCELLED)
                    .getSingleResult();
            return count > 0;
        }
    }

    public boolean existsOverlappingActiveBooking(Long roomId, MeetingSlot slot) {
        try (EntityManager em = emf.createEntityManager()) {
            Long count = em.createQuery("""
                    select count(rb)
                    from RoomBooking rb
                    where rb.room.id = :roomId
                    and rb.meetingSlot.date = :date
                    and rb.bookingStatus <> :cancelled
                    and rb.meetingSlot.startTime < :endTime
                    and rb.meetingSlot.endTime > :startTime
                    """, Long.class)
                    .setParameter("roomId", roomId)
                    .setParameter("date", slot.getDate())
                    .setParameter("startTime", slot.getStartTime())
                    .setParameter("endTime", slot.getEndTime())
                    .setParameter("cancelled", BookingStatus.CANCELLED)
                    .getSingleResult();
            return count > 0;
        }
    }

    public List<RoomBooking> findRoomBookingHistoryForTeacher(Long teacherId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select rb
                    from RoomBooking rb
                    join fetch rb.room
                    join fetch rb.classMeeting cm
                    join fetch cm.subject
                    where cm.teacher.id = :teacherId
                    order by rb.meetingSlot.date, rb.meetingSlot.startTime
                    """, RoomBooking.class)
                    .setParameter("teacherId", teacherId)
                    .getResultList();
        }
    }

    public List<RoomBooking> findAllWithDetails() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select rb
                    from RoomBooking rb
                    join fetch rb.room
                    left join fetch rb.classMeeting cm
                    left join fetch cm.subject
                    left join fetch cm.group
                    left join fetch cm.teacher
                    order by rb.meetingSlot.date desc, rb.meetingSlot.startTime desc
                    """, RoomBooking.class)
                    .getResultList();
        }
    }
}
