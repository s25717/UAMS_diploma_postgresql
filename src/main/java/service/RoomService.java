package service;

import model.Room;
import persistence.RoomRepository;

import java.util.List;

public class RoomService extends EntityService<Room> {
    private final RoomRepository roomRepository = new RoomRepository();

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
        room.setRoomNumber(roomNumber);
        room.setCapacity(capacity);
        return update(room);
    }
}
