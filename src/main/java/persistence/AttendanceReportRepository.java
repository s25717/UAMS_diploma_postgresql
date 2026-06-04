package persistence;

import model.AttendanceReport;

public class AttendanceReportRepository extends GenericRepository<AttendanceReport> {
    public AttendanceReportRepository() {
        super(AttendanceReport.class);
    }
}
