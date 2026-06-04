package model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import model.value.MeetingSlot;

import java.util.HashMap;
import java.util.Map;

@Entity
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String roomNumber;

    @Min(1)
    private int capacity;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "meetingSlot")
    private Map<MeetingSlot, RoomBooking> bookingsBySlot = new HashMap<>();

    protected Room() {
    }

    public Room(String roomNumber, int capacity) {
        this.roomNumber = roomNumber;
        this.capacity = capacity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Map<MeetingSlot, RoomBooking> getBookingsBySlot() {
        return bookingsBySlot;
    }

    public void setBookingsBySlot(Map<MeetingSlot, RoomBooking> bookingsBySlot) {
        this.bookingsBySlot = bookingsBySlot;
    }

    public RoomBooking getBookingForSlot(MeetingSlot slot) {
        return bookingsBySlot.get(slot);
    }

    public boolean isAvailable(MeetingSlot slot) {
        return !bookingsBySlot.containsKey(slot);
    }

    public void addBooking(RoomBooking booking) {
        if (!isAvailable(booking.getMeetingSlot())) {
            throw new IllegalArgumentException("Room is already booked for this time slot.");
        }
        bookingsBySlot.put(booking.getMeetingSlot(), booking);
        booking.setRoom(this);
    }

    public void removeBooking(RoomBooking booking) {
        bookingsBySlot.remove(booking.getMeetingSlot());
        booking.setRoom(null);
    }
}
