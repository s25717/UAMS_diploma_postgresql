package persistence;

import jakarta.persistence.EntityManager;
import model.Room;

import java.util.List;
import java.util.Optional;

public class RoomRepository extends GenericRepository<Room> {
    public RoomRepository() {
        super(Room.class);
    }

    public Optional<Room> findByIdWithBookings(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct r
                    from Room r
                    left join fetch r.bookingsBySlot
                    where r.id = :id
                    """, Room.class)
                    .setParameter("id", id)
                    .getResultStream()
                    .findFirst();
        }
    }

    public List<Room> findAllWithBookings() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct r
                    from Room r
                    left join fetch r.bookingsBySlot rb
                    left join fetch rb.classMeeting cm
                    left join fetch cm.subject
                    left join fetch cm.group
                    order by r.roomNumber
                    """, Room.class)
                    .getResultList();
        }
    }
}
