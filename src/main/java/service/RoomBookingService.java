package service;

import model.Room;
import model.RoomBooking;
import model.enums.BookingStatus;
import model.value.MeetingSlot;
import persistence.JpaUtil;
import persistence.RoomBookingRepository;
import persistence.RoomRepository;

public class RoomBookingService {
    private final RoomRepository roomRepository = new RoomRepository();
    private final RoomBookingRepository roomBookingRepository = new RoomBookingRepository();

    public RoomBooking createBooking(Long roomId, MeetingSlot slot) {
        var em = JpaUtil.entityManagerFactory().createEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            Room room = em.createQuery("""
                    select distinct r
                    from Room r
                    left join fetch r.bookingsBySlot
                    where r.id = :roomId
                    """, Room.class)
                    .setParameter("roomId", roomId)
                    .getSingleResult();
            if (roomBookingRepository.existsOverlappingActiveBooking(roomId, slot)) {
                throw new IllegalArgumentException("Room is already booked for an overlapping time slot.");
            }
            RoomBooking booking = new RoomBooking(slot, BookingStatus.CONFIRMED, room, null);
            room.addBooking(booking);
            em.persist(booking);
            tx.commit();
            return booking;
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public void cancelBooking(Long bookingId) {
        var em = JpaUtil.entityManagerFactory().createEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            RoomBooking booking = em.createQuery("""
                    select rb
                    from RoomBooking rb
                    left join fetch rb.classMeeting
                    where rb.id = :id
                    """, RoomBooking.class)
                    .setParameter("id", bookingId)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
            if (booking == null) {
                throw new IllegalArgumentException("Room booking not found: " + bookingId);
            }
            var meeting = booking.getClassMeeting();
            if (meeting != null && (meeting.isScheduled() || meeting.isCompleted())) {
                throw new IllegalArgumentException("Cancel the class meeting before cancelling its active room booking.");
            }
            booking.setBookingStatus(BookingStatus.CANCELLED);
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

    public boolean isAvailable(Long roomId, MeetingSlot slot) {
        Room room = roomRepository.findByIdWithBookings(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        return !roomBookingRepository.existsOverlappingActiveBooking(roomId, slot);
    }
}
