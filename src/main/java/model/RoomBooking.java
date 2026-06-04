package model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import model.enums.BookingStatus;
import model.value.MeetingSlot;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "date", "start_time", "end_time"}))
public class RoomBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Valid
    @NotNull
    @Embedded
    private MeetingSlot meetingSlot;

    @NotNull
    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @OneToOne(fetch = FetchType.LAZY)
    private ClassMeeting classMeeting;

    protected RoomBooking() {
    }

    public RoomBooking(MeetingSlot meetingSlot, BookingStatus bookingStatus, Room room, ClassMeeting classMeeting) {
        this.meetingSlot = meetingSlot;
        this.bookingStatus = bookingStatus;
        this.room = room;
        this.classMeeting = classMeeting;
        if (classMeeting != null) {
            classMeeting.setRoomBooking(this);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MeetingSlot getMeetingSlot() {
        return meetingSlot;
    }

    public void setMeetingSlot(MeetingSlot meetingSlot) {
        this.meetingSlot = meetingSlot;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public ClassMeeting getClassMeeting() {
        return classMeeting;
    }

    public void setClassMeeting(ClassMeeting classMeeting) {
        this.classMeeting = classMeeting;
        if (classMeeting != null && classMeeting.getRoomBooking() != this) {
            classMeeting.setRoomBooking(this);
        }
    }
}
