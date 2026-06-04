package service;

import model.Room;
import model.enums.BookingStatus;
import persistence.JpaTransactionManager;
import persistence.RoomRepository;

import java.util.List;

public class RoomService extends EntityService<Room> {
    private final RoomRepository roomRepository = new RoomRepository();
    private final JpaTransactionManager transactionManager = new JpaTransactionManager();

    public RoomService() {
        super(Room.class);
    }

    public List<Room> findAllWithBookings() {
        return roomRepository.findAllWithBookings();
    }

    public Room createRoom(String roomNumber, int capacity) {
        return add(new Room(roomNumber, capacity));
    }

    public Room updateRoom(Room room, String roomNumber, int capacity) {
        if (room == null) {
            throw new IllegalArgumentException("Select a room first.");
        }
        return transactionManager.execute(em -> {
            Long oversizedBookings = em.createQuery("""
                    select count(rb)
                    from RoomBooking rb
                    join rb.classMeeting cm
                    join cm.group g
                    where rb.room.id = :roomId
                    and rb.bookingStatus <> :cancelled
                    and (
                        select count(s)
                        from Student s
                        where s.group.id = g.id
                    ) > :capacity
                    """, Long.class)
                    .setParameter("roomId", room.getId())
                    .setParameter("cancelled", BookingStatus.CANCELLED)
                    .setParameter("capacity", (long) capacity)
                    .getSingleResult();
            if (oversizedBookings > 0) {
                throw new IllegalArgumentException("Room capacity is lower than at least one already booked group size.");
            }
            Room managed = em.find(Room.class, room.getId());
            if (managed == null) {
                throw new IllegalArgumentException("Room not found.");
            }
            managed.setRoomNumber(roomNumber);
            managed.setCapacity(capacity);
            validate(managed);
            return managed;
        });
    }
}
