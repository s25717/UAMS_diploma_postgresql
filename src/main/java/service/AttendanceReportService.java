package service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import model.Attendance;
import model.AttendanceReport;
import model.ClassMeeting;
import model.ReportLine;
import model.Semester;
import model.Student;
import model.enums.AttendanceStatus;
import model.enums.ReportType;
import persistence.AttendanceRepository;
import persistence.ClassMeetingRepository;
import persistence.JpaUtil;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AttendanceReportService {
    private final ClassMeetingRepository classMeetingRepository = new ClassMeetingRepository();
    private final AttendanceRepository attendanceRepository = new AttendanceRepository();

    public AttendanceReport generateReport(AttendanceReportFilter filter) {
        AttendanceReportFilter effectiveFilter = filter == null ? new AttendanceReportFilter() : filter;
        List<ClassMeeting> meetings = classMeetingRepository.findByReportFilter(effectiveFilter);
        Set<Long> meetingIds = meetings.stream()
                .map(ClassMeeting::getId)
                .collect(Collectors.toSet());
        List<Attendance> attendanceRecords = attendanceRepository.findByClassMeetingIds(meetingIds);
        Map<Long, Map<Long, AttendanceStatus>> attendanceByMeetingAndStudent = indexAttendance(attendanceRecords);

        Map<Long, ReportAccumulator> rowsByStudent = new LinkedHashMap<>();
        meetings.stream()
                .sorted(Comparator.comparing(ClassMeeting::getId))
                .forEach(meeting -> {
                    Map<Long, AttendanceStatus> statusesByStudent = attendanceByMeetingAndStudent
                            .getOrDefault(meeting.getId(), Map.of());
                    meeting.getGroup().getStudents().stream()
                            .sorted(Comparator.comparing(Student::getSurname).thenComparing(Student::getName))
                            .forEach(student -> rowsByStudent
                                    .computeIfAbsent(student.getId(), ignored -> new ReportAccumulator(student))
                                    .include(statusesByStudent.getOrDefault(student.getId(), AttendanceStatus.ABSENT)));
                });

        EntityManager em = JpaUtil.entityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            AttendanceReport report = new AttendanceReport(LocalDate.now(), resolveReportType(effectiveFilter));
            report.setDateFrom(effectiveFilter.getDateFrom());
            report.setDateTo(effectiveFilter.getDateTo());
            report.setClassType(effectiveFilter.getClassType());

            if (effectiveFilter.getSemesterIds() != null) {
                Set<Semester> semesters = effectiveFilter.getSemesterIds().stream()
                        .map(id -> em.find(Semester.class, id))
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toCollection(HashSet::new));
                report.setSemesters(semesters);
            }
            if (effectiveFilter.getTeacherId() != null) {
                report.setTeacher(em.find(model.Teacher.class, effectiveFilter.getTeacherId()));
            }
            if (effectiveFilter.getSubjectId() != null) {
                report.setSubject(em.find(model.Subject.class, effectiveFilter.getSubjectId()));
            }
            if (effectiveFilter.getGroupId() != null) {
                report.setGroup(em.find(model.StudentGroup.class, effectiveFilter.getGroupId()));
            }

            rowsByStudent.values().forEach(row -> report.addLine(row.toReportLine()));
            double overallPerformance = report.getReportLines()
                    .stream()
                    .mapToDouble(ReportLine::getAttendancePercentage)
                    .average()
                    .orElse(0.0);
            report.setOverallPerformancePercentage(overallPerformance);
            em.persist(report);
            tx.commit();
            return report;
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public String buildConclusionMessage(AttendanceReport report) {
        return report.generateConclusionMessage();
    }

    private Map<Long, Map<Long, AttendanceStatus>> indexAttendance(List<Attendance> attendanceRecords) {
        Map<Long, Map<Long, AttendanceStatus>> result = new HashMap<>();
        for (Attendance attendance : attendanceRecords) {
            result.computeIfAbsent(attendance.getClassMeeting().getId(), ignored -> new HashMap<>())
                    .put(attendance.getStudent().getId(), attendance.getStatus());
        }
        return result;
    }

    private ReportType resolveReportType(AttendanceReportFilter filter) {
        int selectedFilters = 0;
        ReportType singleType = null;
        if (filter.getSemesterIds() != null && !filter.getSemesterIds().isEmpty()) {
            selectedFilters++;
            singleType = ReportType.SEMESTER;
        }
        if (filter.getTeacherId() != null) {
            selectedFilters++;
            singleType = ReportType.TEACHER;
        }
        if (filter.getSubjectId() != null) {
            selectedFilters++;
            singleType = ReportType.SUBJECT;
        }
        if (filter.getGroupId() != null) {
            selectedFilters++;
            singleType = ReportType.GROUP;
        }
        if (filter.getClassType() != null) {
            selectedFilters++;
        }
        if (filter.getDateFrom() != null || filter.getDateTo() != null) {
            selectedFilters++;
        }
        return selectedFilters == 1 && singleType != null ? singleType : ReportType.COMBINED;
    }

    private static final class ReportAccumulator {
        private final Student student;
        private int totalMeetings;
        private int presentCount;
        private int absentCount;
        private int lateCount;
        private int excusedCount;

        private ReportAccumulator(Student student) {
            this.student = student;
        }

        private void include(AttendanceStatus status) {
            totalMeetings++;
            switch (status) {
                case PRESENT -> presentCount++;
                case LATE -> lateCount++;
                case EXCUSED -> excusedCount++;
                case ABSENT -> absentCount++;
            }
        }

        private ReportLine toReportLine() {
            return new ReportLine(student, totalMeetings, presentCount, lateCount, excusedCount, absentCount);
        }
    }
}
