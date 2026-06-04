package persistence;

import jakarta.persistence.EntityManager;
import model.Notification;

import java.util.List;

public class NotificationRepository extends GenericRepository<Notification> {
    public NotificationRepository() {
        super(Notification.class);
    }

    public List<Notification> findByRecipientId(Long personId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select n
                    from Notification n
                    left join fetch n.recipient
                    where n.recipient.id = :personId
                    order by n.id desc
                    """, Notification.class)
                    .setParameter("personId", personId)
                    .getResultList();
        }
    }

    public List<Notification> findAllWithRecipients() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select n
                    from Notification n
                    left join fetch n.recipient
                    order by n.id desc
                    """, Notification.class)
                    .getResultList();
        }
    }

}
