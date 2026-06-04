package persistence;

import jakarta.persistence.EntityManager;
import model.AttendanceReport;

import java.util.List;

public class AttendanceReportRepository extends GenericRepository<AttendanceReport> {
    public AttendanceReportRepository() {
        super(AttendanceReport.class);
    }

    public List<AttendanceReport> findAllWithDetails() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                    select distinct report
                    from AttendanceReport report
                    left join fetch report.semesters
                    left join fetch report.teacher
                    left join fetch report.subject
                    left join fetch report.group
                    left join fetch report.reportLines lines
                    left join fetch lines.student
                    order by report.generatedOn desc, report.id desc
                    """, AttendanceReport.class)
                    .getResultList();
        }
    }
}
