package persistence;

import jakarta.persistence.EntityManager;
import model.ScheduledNotificationTask;
import model.enums.NotificationTaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public class ScheduledNotificationTaskRepository extends GenericRepository<ScheduledNotificationTask> {
    public ScheduledNotificationTaskRepository() {
        super(ScheduledNotificationTask.class);
    }

    public List<ScheduledNotificationTask> findPendingDueTasks(LocalDateTime now) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select t
                    from ScheduledNotificationTask t
                    left join fetch t.recipient
                    left join fetch t.classMeeting cm
                    left join fetch t.attendanceReport
                    where t.status = :status and t.scheduledAt <= :now
                    order by t.scheduledAt
                    """, ScheduledNotificationTask.class)
                    .setParameter("status", NotificationTaskStatus.PENDING)
                    .setParameter("now", now)
                    .getResultList();
        }
    }

    public List<ScheduledNotificationTask> findAllWithDetails() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select t
                    from ScheduledNotificationTask t
                    left join fetch t.recipient
                    left join fetch t.classMeeting cm
                    left join fetch cm.subject
                    left join fetch t.attendanceReport
                    order by t.scheduledAt desc, t.id desc
                    """, ScheduledNotificationTask.class)
                    .getResultList();
        }
    }
}
