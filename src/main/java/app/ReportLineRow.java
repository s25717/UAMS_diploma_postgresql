package app;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import model.ReportLine;

public class ReportLineRow {
    private final StringProperty studentName;
    private final IntegerProperty totalMeetings;
    private final IntegerProperty presentCount;
    private final IntegerProperty lateCount;
    private final IntegerProperty excusedCount;
    private final IntegerProperty absentCount;
    private final DoubleProperty attendancePercentage;

    public ReportLineRow(ReportLine line) {
        this.studentName = new SimpleStringProperty(line.getStudent().getName() + " " + line.getStudent().getSurname());
        this.totalMeetings = new SimpleIntegerProperty(line.getTotalMeetings());
        this.presentCount = new SimpleIntegerProperty(line.getPresentCount());
        this.lateCount = new SimpleIntegerProperty(line.getLateCount());
        this.excusedCount = new SimpleIntegerProperty(line.getExcusedCount());
        this.absentCount = new SimpleIntegerProperty(line.getAbsentCount());
        this.attendancePercentage = new SimpleDoubleProperty(line.getAttendancePercentage());
    }

    public StringProperty studentNameProperty() {
        return studentName;
    }

    public IntegerProperty totalMeetingsProperty() {
        return totalMeetings;
    }

    public IntegerProperty presentCountProperty() {
        return presentCount;
    }

    public IntegerProperty lateCountProperty() {
        return lateCount;
    }

    public IntegerProperty excusedCountProperty() {
        return excusedCount;
    }

    public IntegerProperty absentCountProperty() {
        return absentCount;
    }

    public DoubleProperty attendancePercentageProperty() {
        return attendancePercentage;
    }
}
