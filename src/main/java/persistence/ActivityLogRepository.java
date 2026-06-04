package persistence;

import jakarta.persistence.EntityManager;
import model.ActivityLog;

import java.util.List;

public class ActivityLogRepository extends GenericRepository<ActivityLog> {
    public ActivityLogRepository() {
        super(ActivityLog.class);
    }

    public List<ActivityLog> findByActorId(Long actorId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select log
                    from ActivityLog log
                    join fetch log.actor actor
                    where actor.id = :actorId
                    order by log.occurredAt desc, log.id desc
                    """, ActivityLog.class)
                    .setParameter("actorId", actorId)
                    .getResultList();
        }
    }
}
