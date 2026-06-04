package model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import model.enums.ClassType;
import model.enums.ReportType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
public class AttendanceReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate generatedOn;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    @Enumerated(EnumType.STRING)
    private ClassType classType;

    @ManyToMany
    @JoinTable(
            name = "attendance_report_semesters",
            joinColumns = @JoinColumn(name = "report_id"),
            inverseJoinColumns = @JoinColumn(name = "semester_id")
    )
    private Set<Semester> semesters = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    private StudentGroup group;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private double overallPerformancePercentage;

    @Valid
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ReportLine> reportLines = new HashSet<>();

    private LocalDateTime exportedAt;

    protected AttendanceReport() {
    }

    public AttendanceReport(LocalDate generatedOn, StudentGroup group, Subject subject) {
        this.generatedOn = generatedOn;
        this.reportType = ReportType.COMBINED;
        this.group = group;
        this.subject = subject;
    }

    public AttendanceReport(LocalDate generatedOn, ReportType reportType) {
        this.generatedOn = generatedOn;
        this.reportType = reportType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getGeneratedOn() {
        return generatedOn;
    }

    public void setGeneratedOn(LocalDate generatedOn) {
        this.generatedOn = generatedOn;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void setClassType(ClassType classType) {
        this.classType = classType;
    }

    public Set<Semester> getSemesters() {
        return semesters;
    }

    public void setSemesters(Set<Semester> semesters) {
        this.semesters = semesters;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public StudentGroup getGroup() {
        return group;
    }

    public void setGroup(StudentGroup group) {
        this.group = group;
    }

    public double getOverallPerformancePercentage() {
        return overallPerformancePercentage;
    }

    public void setOverallPerformancePercentage(double overallPerformancePercentage) {
        this.overallPerformancePercentage = overallPerformancePercentage;
    }

    public String generateConclusionMessage() {
        String performance = String.format(java.util.Locale.US, "%.2f", overallPerformancePercentage);
        boolean hasTeacher = teacher != null;
        boolean hasSubject = subject != null;
        boolean hasGroup = group != null;
        boolean hasOneSemester = semesters != null && semesters.size() == 1;

        if (hasTeacher && !hasSubject && !hasGroup) {
            return "Teacher " + teacher.getName() + " " + teacher.getSurname()
                    + " has an attendance performance of " + performance + "% across selected class meetings.";
        }
        if (hasSubject && !hasTeacher && !hasGroup) {
            return "Subject " + subject.getName() + " has an attendance performance of " + performance + "%.";
        }
        if (hasGroup && !hasTeacher && !hasSubject) {
            return "Group " + group.getCode() + " has an attendance performance of " + performance + "%.";
        }
        if (hasOneSemester && !hasTeacher && !hasSubject && !hasGroup) {
            Semester semester = semesters.iterator().next();
            return "Semester " + semester.getNumber() + " has an attendance performance of " + performance + "%.";
        }
        return "Selected parameters have an attendance performance of " + performance + "%.";
    }

    public Set<ReportLine> getReportLines() {
        return reportLines;
    }

    public void setReportLines(Set<ReportLine> reportLines) {
        this.reportLines = reportLines;
    }

    public void addLine(ReportLine line) {
        reportLines.add(line);
        line.setReport(this);
    }

    public void removeLine(ReportLine line) {
        reportLines.remove(line);
        line.setReport(null);
    }

    public LocalDateTime getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(LocalDateTime exportedAt) {
        this.exportedAt = exportedAt;
    }
}
