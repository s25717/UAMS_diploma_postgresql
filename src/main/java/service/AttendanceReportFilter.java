package service;

import model.enums.ClassType;

import java.time.LocalDate;
import java.util.Set;

public class AttendanceReportFilter {
    private Set<Long> semesterIds;
    private Long teacherId;
    private Long subjectId;
    private Long groupId;
    private ClassType classType;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    public Set<Long> getSemesterIds() {
        return semesterIds;
    }

    public void setSemesterIds(Set<Long> semesterIds) {
        this.semesterIds = semesterIds;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void setClassType(ClassType classType) {
        this.classType = classType;
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
}
