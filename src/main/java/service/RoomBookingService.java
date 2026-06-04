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
            if (!room.isAvailable(slot)) {
                throw new IllegalArgumentException("Room is already booked for this time slot.");
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
            RoomBooking booking = em.find(RoomBooking.class, bookingId);
            if (booking == null) {
                throw new IllegalArgumentException("Room booking not found: " + bookingId);
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
        return room.isAvailable(slot) && !roomBookingRepository.existsForRoomAndSlot(roomId, slot);
    }
}
