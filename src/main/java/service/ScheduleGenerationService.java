package service;

import model.ClassMeeting;
import model.RoomBooking;
import model.WeeklyScheduleEntry;
import model.enums.BookingStatus;
import model.enums.ClassMeetingStatus;
import model.enums.MeetingMode;
import model.value.MeetingSlot;
import model.value.MeetingTime;
import persistence.JpaUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ScheduleGenerationService {
    public List<ClassMeeting> generateClassMeetingsForSemester(WeeklyScheduleEntry entry) {
        List<ClassMeeting> generated = new ArrayList<>();
        var em = JpaUtil.entityManagerFactory().createEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            WeeklyScheduleEntry managedEntry = em.createQuery("""
                    select distinct e
                    from WeeklyScheduleEntry e
                    join fetch e.group
                    join fetch e.semester
                    join fetch e.subject
                    join fetch e.teacher t
                    left join fetch t.qualifiedSubjects
                    left join fetch e.room
                    where e.id = :id
                    """, WeeklyScheduleEntry.class)
                    .setParameter("id", entry.getId())
                    .getSingleResult();
            if (!managedEntry.getTeacher().isQualifiedFor(managedEntry.getSubject())) {
                throw new IllegalArgumentException("Teacher is not qualified to teach this subject.");
            }
            if (!managedEntry.getGroup().getSemester().getId().equals(managedEntry.getSemester().getId())) {
                throw new IllegalArgumentException("Weekly schedule semester must match the selected group semester.");
            }
            Long semesterSubjectMatches = em.createQuery("""
                    select count(sem)
                    from Semester sem
                    join sem.subjects subject
                    where sem.id = :semesterId
                    and subject.id = :subjectId
                    """, Long.class)
                    .setParameter("semesterId", managedEntry.getSemester().getId())
                    .setParameter("subjectId", managedEntry.getSubject().getId())
                    .getSingleResult();
            if (semesterSubjectMatches == 0) {
                throw new IllegalArgumentException("Subject is not available in the selected semester.");
            }
            if (managedEntry.getMeetingMode() == MeetingMode.CLASSROOM && managedEntry.getRoom() != null) {
                Long groupSize = em.createQuery("select count(s) from Student s where s.group.id = :groupId", Long.class)
                        .setParameter("groupId", managedEntry.getGroup().getId())
                        .getSingleResult();
                if (managedEntry.getRoom().getCapacity() < groupSize) {
                    throw new IllegalArgumentException("Room capacity is lower than the selected group size.");
                }
            }
            LocalDate date = managedEntry.getSemester().getStartDate();
            LocalDate end = managedEntry.getSemester().getEndDate();
            while (!date.isAfter(end)) {
                if (date.getDayOfWeek() == managedEntry.getDayOfWeek()) {
                    MeetingSlot slot = new MeetingSlot(date, managedEntry.getStartTime(), managedEntry.getEndTime());
                    if (managedEntry.getMeetingMode() == MeetingMode.CLASSROOM && managedEntry.getRoom() != null) {
                        Long roomId = managedEntry.getRoom().getId();
                        Long conflicts = em.createQuery("""
                                select count(rb)
                                from RoomBooking rb
                                where rb.room.id = :roomId
                                and rb.meetingSlot.date = :date
                                and rb.bookingStatus <> :cancelled
                                and rb.meetingSlot.startTime < :end
                                and rb.meetingSlot.endTime > :start
                                """, Long.class)
                                .setParameter("roomId", roomId)
                                .setParameter("date", slot.getDate())
                                .setParameter("start", slot.getStartTime())
                                .setParameter("end", slot.getEndTime())
                                .setParameter("cancelled", BookingStatus.CANCELLED)
                                .getSingleResult();
                        if (conflicts > 0) {
                            throw new IllegalArgumentException("Room is already booked for an overlapping time slot.");
                        }
                    }
                    ClassMeeting meeting = new ClassMeeting(
                            date,
                            managedEntry.getRoom() == null ? null : managedEntry.getRoom().getRoomNumber(),
                            managedEntry.getOnlineMeetingLink(),
                            new MeetingTime(managedEntry.getDayOfWeek(), managedEntry.getStartTime(), managedEntry.getEndTime()),
                            managedEntry.getClassType(),
                            managedEntry.getMeetingMode(),
                            managedEntry.getSubject(),
                            managedEntry.getTeacher(),
                            managedEntry.getGroup()
                    );
                    meeting.setStatus(ClassMeetingStatus.SCHEDULED);
                    meeting.setScheduleEntry(managedEntry);
                    em.persist(meeting);
                    if (managedEntry.getMeetingMode() == MeetingMode.CLASSROOM && managedEntry.getRoom() != null) {
                        em.persist(new RoomBooking(slot, BookingStatus.CONFIRMED, managedEntry.getRoom(), meeting));
                    }
                    generated.add(meeting);
                }
                date = date.plusDays(1);
            }
            tx.commit();
            return generated;
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }
}
