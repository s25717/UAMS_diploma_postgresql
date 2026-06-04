package app;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import model.Student;
import model.enums.AttendanceStatus;

public class AttendanceRow {
    private final Long studentId;
    private final StringProperty studentNumber;
    private final StringProperty fullName;
    private final ObjectProperty<AttendanceStatus> status;
    private final StringProperty comment;

    public AttendanceRow(Student student, AttendanceStatus status) {
        this(student, status, "");
    }

    public AttendanceRow(Student student, AttendanceStatus status, String comment) {
        this.studentId = student.getId();
        this.studentNumber = new SimpleStringProperty(student.getStudentNumber());
        this.fullName = new SimpleStringProperty(student.getName() + " " + student.getSurname());
        this.status = new SimpleObjectProperty<>(status);
        this.comment = new SimpleStringProperty(comment == null ? "" : comment);
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getStudentNumber() {
        return studentNumber.get();
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber.set(studentNumber);
    }

    public StringProperty studentNumberProperty() {
        return studentNumber;
    }

    public String getFullName() {
        return fullName.get();
    }

    public void setFullName(String fullName) {
        this.fullName.set(fullName);
    }

    public StringProperty fullNameProperty() {
        return fullName;
    }

    public AttendanceStatus getStatus() {
        return status.get();
    }

    public void setStatus(AttendanceStatus status) {
        this.status.set(status);
    }

    public ObjectProperty<AttendanceStatus> statusProperty() {
        return status;
    }

    public String getComment() {
        return comment.get();
    }

    public void setComment(String comment) {
        this.comment.set(comment);
    }

    public StringProperty commentProperty() {
        return comment;
    }
}
