package service;

import model.Administrator;
import model.ClassMeeting;
import model.Person;
import model.Student;
import model.Teacher;
import persistence.JpaUtil;

public class ScheduleAccessService {
    public boolean canOpenFullDetails(Person person, ClassMeeting meeting) {
        if (person instanceof Administrator) {
            return true;
        }
        if (person instanceof Teacher) {
            return meeting.getTeacher() != null && meeting.getTeacher().getId().equals(person.getId());
        }
        if (person instanceof Student) {
            return isStudentGroupMeeting(person.getId(), meeting.getId());
        }
        return false;
    }

    public void assertCanOpenFullDetails(Person person, ClassMeeting meeting) {
        if (!canOpenFullDetails(person, meeting)) {
            throw new IllegalArgumentException("Only public schedule information is available for this class meeting.");
        }
    }

    private boolean isStudentGroupMeeting(Long studentId, Long meetingId) {
        var em = JpaUtil.entityManagerFactory().createEntityManager();
        try {
            Long count = em.createQuery("""
                    select count(cm)
                    from ClassMeeting cm
                    join cm.group g
                    join g.students s
                    where cm.id = :meetingId
                    and s.id = :studentId
                    """, Long.class)
                    .setParameter("meetingId", meetingId)
                    .setParameter("studentId", studentId)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }
}
