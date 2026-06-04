package service;

import model.AttendanceReport;
import model.ReportLine;
import model.Semester;
import persistence.JpaUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ReportExportService {
    public Path exportToCsv(AttendanceReport report, Path targetFile) {
        if (report == null) {
            throw new IllegalArgumentException("Generate a report before exporting.");
        }
        if (report.getReportLines() == null || report.getReportLines().isEmpty()) {
            throw new IllegalArgumentException("Cannot export an empty report.");
        }

        StringBuilder csv = new StringBuilder();
        appendMetadata(csv, "Generated On", report.getGeneratedOn());
        appendMetadata(csv, "Report Type", report.getReportType());
        appendMetadata(csv, "Semesters", report.getSemesters().stream()
                .sorted(Comparator.comparingInt(Semester::getNumber))
                .map(semester -> "Semester " + semester.getNumber())
                .collect(Collectors.joining("; ")));
        appendMetadata(csv, "Teacher", report.getTeacher() == null ? "" : report.getTeacher().getName() + " " + report.getTeacher().getSurname());
        appendMetadata(csv, "Subject", report.getSubject() == null ? "" : report.getSubject().getName());
        appendMetadata(csv, "Group", report.getGroup() == null ? "" : report.getGroup().getCode());
        appendMetadata(csv, "Class Type", report.getClassType());
        appendMetadata(csv, "Date From", report.getDateFrom());
        appendMetadata(csv, "Date To", report.getDateTo());
        appendMetadata(csv, "Overall Performance", String.format(java.util.Locale.US, "%.2f%%", report.getOverallPerformancePercentage()));
        appendMetadata(csv, "Conclusion", report.generateConclusionMessage());
        csv.append(System.lineSeparator());

        csv.append("Student Number,Student Name,Total Meetings,Present,Late,Excused,Absent,Attendance %")
                .append(System.lineSeparator());
        List<ReportLine> lines = report.getReportLines().stream()
                .sorted(Comparator.comparing(line -> line.getStudent().getSurname() + line.getStudent().getName()))
                .toList();
        for (ReportLine line : lines) {
            csv.append(escape(line.getStudent().getStudentNumber())).append(',')
                    .append(escape(line.getStudent().getName() + " " + line.getStudent().getSurname())).append(',')
                    .append(line.getTotalMeetings()).append(',')
                    .append(line.getPresentCount()).append(',')
                    .append(line.getLateCount()).append(',')
                    .append(line.getExcusedCount()).append(',')
                    .append(line.getAbsentCount()).append(',')
                    .append(String.format(java.util.Locale.US, "%.2f", line.getAttendancePercentage()))
                    .append(System.lineSeparator());
        }

        try {
            Path parent = targetFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(targetFile, csv.toString(), StandardCharsets.UTF_8);
            markExported(report);
            return targetFile;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot export report: " + ex.getMessage(), ex);
        }
    }

    private void markExported(AttendanceReport report) {
        if (report.getId() == null || report.getExportedAt() != null) {
            return;
        }
        var em = JpaUtil.entityManagerFactory().createEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            LocalDateTime exportedAt = LocalDateTime.now();
            int updated = em.createQuery("""
                    update AttendanceReport report
                    set report.exportedAt = :exportedAt
                    where report.id = :id
                    and report.exportedAt is null
                    """)
                    .setParameter("exportedAt", exportedAt)
                    .setParameter("id", report.getId())
                    .executeUpdate();
            tx.commit();
            if (updated > 0) {
                report.setExportedAt(exportedAt);
            }
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    private void appendMetadata(StringBuilder csv, String key, Object value) {
        csv.append(escape(key)).append(',').append(escape(value == null ? "" : String.valueOf(value)))
                .append(System.lineSeparator());
    }

    private String escape(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
