package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
public class ReportLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(0)
    private int totalMeetings;

    @Min(0)
    private int presentCount;

    @Min(0)
    private int absentCount;

    @Min(0)
    private int lateCount;

    @Min(0)
    private int excusedCount;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(nullable = false)
    private double attendancePercentage;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, updatable = false)
    private AttendanceReport report;

    protected ReportLine() {
    }

    public ReportLine(Student student, double attendancePercentage) {
        this.student = student;
        this.attendancePercentage = attendancePercentage;
    }

    public ReportLine(Student student, int totalMeetings, int presentCount, int lateCount,
                      int excusedCount, int absentCount) {
        this.student = student;
        this.totalMeetings = totalMeetings;
        this.presentCount = presentCount;
        this.lateCount = lateCount;
        this.excusedCount = excusedCount;
        this.absentCount = absentCount;
        recalculateAttendancePercentage();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getTotalMeetings() {
        return totalMeetings;
    }

    public void setTotalMeetings(int totalMeetings) {
        this.totalMeetings = totalMeetings;
        recalculateAttendancePercentage();
    }

    public int getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(int presentCount) {
        this.presentCount = presentCount;
        recalculateAttendancePercentage();
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(int absentCount) {
        this.absentCount = absentCount;
        recalculateAttendancePercentage();
    }

    public int getLateCount() {
        return lateCount;
    }

    public void setLateCount(int lateCount) {
        this.lateCount = lateCount;
        recalculateAttendancePercentage();
    }

    public int getExcusedCount() {
        return excusedCount;
    }

    public void setExcusedCount(int excusedCount) {
        this.excusedCount = excusedCount;
        recalculateAttendancePercentage();
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public AttendanceReport getReport() {
        return report;
    }

    public void setReport(AttendanceReport report) {
        this.report = report;
    }

    public void recalculateAttendancePercentage() {
        if (totalMeetings <= 0) {
            attendancePercentage = 0.0;
            return;
        }
        attendancePercentage = ((double) presentCount + lateCount) / totalMeetings * 100.0;
    }
}
