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
                    join fetch e.sourceClassMeeting source
                    join fetch source.group g
                    join fetch g.semesterField sf
                    join fetch sf.semester
                    join fetch sf.field
                    join fetch source.subject
                    join fetch source.teacher t
                    left join fetch t.qualifiedSubjects
                    left join fetch source.roomBooking rb
                    left join fetch rb.room
                    where e.id = :id
                    """, WeeklyScheduleEntry.class)
                    .setParameter("id", entry.getId())
                    .getSingleResult();
            ClassMeeting source = managedEntry.getSourceClassMeeting();
            if (!source.getTeacher().isQualifiedFor(source.getSubject())) {
                throw new IllegalArgumentException("Teacher is not qualified to teach this subject.");
            }
            Long semesterSubjectMatches = em.createQuery("""
                    select count(c)
                    from SemesterFieldSubject c
                    where c.semesterField.id = :semesterFieldId
                    and c.subject.id = :subjectId
                    """, Long.class)
                    .setParameter("semesterFieldId", source.getGroup().getSemesterField().getId())
                    .setParameter("subjectId", source.getSubject().getId())
                    .getSingleResult();
            if (semesterSubjectMatches == 0) {
                throw new IllegalArgumentException("Subject is not available in the selected semester and field.");
            }
            if (source.getMeetingMode() == MeetingMode.CLASSROOM && managedEntry.getRoom() != null) {
                Long groupSize = em.createQuery("select count(s) from Student s where s.group.id = :groupId", Long.class)
                        .setParameter("groupId", source.getGroup().getId())
                        .getSingleResult();
                if (managedEntry.getRoom().getCapacity() < groupSize) {
                    throw new IllegalArgumentException("Room capacity is lower than the selected group size.");
                }
            }
            LocalDate date = source.getGroup().getSemester().getStartDate();
            LocalDate end = source.getGroup().getSemester().getEndDate();
            while (!date.isAfter(end)) {
                if (date.getDayOfWeek() == managedEntry.getDayOfWeek()) {
                    Long existingForDate = em.createQuery("""
                            select count(cm)
                            from ClassMeeting cm
                            where cm.scheduleEntry.id = :entryId
                            and cm.meetingDate = :date
                            """, Long.class)
                            .setParameter("entryId", managedEntry.getId())
                            .setParameter("date", date)
                            .getSingleResult();
                    if (existingForDate > 0) {
                        date = date.plusDays(1);
                        continue;
                    }
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
                            managedEntry.getRoom() == null ? source.getLocation() : managedEntry.getRoom().getRoomNumber(),
                            source.getOnlineMeetingLink(),
                            new MeetingTime(managedEntry.getDayOfWeek(), managedEntry.getStartTime(), managedEntry.getEndTime()),
                            source.getClassType(),
                            source.getMeetingMode(),
                            source.getSubject(),
                            source.getTeacher(),
                            source.getGroup()
                    );
                    meeting.setStatus(source.getStatus() == ClassMeetingStatus.DRAFT
                            ? ClassMeetingStatus.DRAFT
                            : ClassMeetingStatus.SCHEDULED);
                    meeting.setScheduleEntry(managedEntry);
                    em.persist(meeting);
                    if (meeting.getStatus() == ClassMeetingStatus.SCHEDULED
                            && managedEntry.getMeetingMode() == MeetingMode.CLASSROOM
                            && managedEntry.getRoom() != null) {
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
