package service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import model.Attendance;
import model.AttendanceReport;
import model.Administrator;
import model.ClassMeeting;
import model.EmailNotification;
import model.Field;
import model.ReportLine;
import model.Room;
import model.RoomBooking;
import model.ScheduledNotificationTask;
import model.Semester;
import model.Student;
import model.StudentGroup;
import model.Subject;
import model.SystemNotification;
import model.Teacher;
import model.WeeklyScheduleEntry;
import model.enums.AttendanceStatus;
import model.enums.BookingStatus;
import model.enums.ClassMeetingStatus;
import model.enums.ClassType;
import model.enums.MeetingMode;
import model.enums.NotificationStatus;
import model.enums.NotificationTaskType;
import model.enums.ReportType;
import model.value.BirthDate;
import model.value.MeetingSlot;
import model.value.MeetingTime;
import persistence.JpaUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

public class SampleDataService {
    public void seedIfDatabaseIsEmpty() {
        EntityManager em = JpaUtil.entityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Long count = em.createQuery("select count(cm) from ClassMeeting cm", Long.class).getSingleResult();
            if (count > 0) {
                return;
            }

            tx.begin();
            PasswordService passwordService = new PasswordService();
            LocalDate today = LocalDate.now();
            LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1L);

            Field field = new Field("Computer Science");
            Semester semester = new Semester(4);
            semester.setStartDate(today.minusWeeks(8));
            semester.setEndDate(today.plusWeeks(8));
            StudentGroup group = new StudentGroup("24c");
            field.addSemester(semester);
            semester.addGroup(group);

            Student anna = new Student("Anna", "Nowak", new BirthDate(LocalDate.of(2002, 3, 15)), Set.of("anna.nowak@student.pja.edu.pl"), "s25717");
            Student jan = new Student("Jan", "Kowalski", new BirthDate(LocalDate.of(2001, 5, 10)), Set.of("jan.kowalski@student.pja.edu.pl"), "s25718");
            Student maria = new Student("Maria", "Wisniewska", new BirthDate(LocalDate.of(2002, 11, 4)), Set.of("maria.wisniewska@student.pja.edu.pl"), "s25719");
            Student adam = new Student("Adam", "Zielinski", new BirthDate(LocalDate.of(2000, 9, 22)), Set.of("adam.zielinski@student.pja.edu.pl"), "s25720");
            anna.setPasswordHash(passwordService.hash("student"));
            jan.setPasswordHash(passwordService.hash("student"));
            maria.setPasswordHash(passwordService.hash("student"));
            adam.setPasswordHash(passwordService.hash("student"));
            group.addStudent(anna);
            group.addStudent(jan);
            group.addStudent(maria);
            group.addStudent(adam);

            Teacher teacher = new Teacher("Piotr", "Kowalski", new BirthDate(LocalDate.of(1980, 8, 20)), Set.of("piotr.kowalski@pja.edu.pl"), "t100");
            teacher.setPasswordHash(passwordService.hash("teacher"));
            Administrator admin = new Administrator("Admin", "User", new BirthDate(LocalDate.of(1978, 1, 20)), Set.of("admin@pja.edu.pl"), "a100");
            admin.setPasswordHash(passwordService.hash("admin"));

            Subject mas = new Subject("MAS");
            mas.addGroup(group);
            teacher.addQualifiedSubject(mas);

            Room laboratoryRoom = new Room("201", 28);
            Room lectureRoom = new Room("Auditorium A", 120);

            WeeklyScheduleEntry scheduleEntry = new WeeklyScheduleEntry(
                    group,
                    mas,
                    teacher,
                    ClassType.TUTORIAL,
                    MeetingMode.CLASSROOM,
                    DayOfWeek.TUESDAY,
                    LocalTime.of(10, 0),
                    LocalTime.of(11, 30),
                    laboratoryRoom,
                    null,
                    semester
            );

            ClassMeeting currentTutorial = new ClassMeeting(
                    startOfWeek.plusDays(1),
                    "201",
                    null,
                    new MeetingTime(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 30)),
                    ClassType.TUTORIAL,
                    MeetingMode.CLASSROOM,
                    mas,
                    teacher,
                    group
            );
            currentTutorial.setScheduleEntry(scheduleEntry);
            currentTutorial.setComment("Current-week MAS tutorial for public schedule demo.");
            mas.getClassMeetings().add(currentTutorial);
            teacher.getClassMeetings().add(currentTutorial);
            group.getClassMeetings().add(currentTutorial);

            ClassMeeting futureLecture = new ClassMeeting(
                    today.plusWeeks(1),
                    null,
                    "https://meet.example.edu/mas-lecture",
                    new MeetingTime(today.plusWeeks(1).getDayOfWeek(), LocalTime.of(12, 0), LocalTime.of(13, 30)),
                    ClassType.LECTURE,
                    MeetingMode.ONLINE,
                    mas,
                    teacher,
                    group
            );
            futureLecture.setComment("Future online lecture. Teacher attendance marking should be blocked before start.");
            mas.getClassMeetings().add(futureLecture);
            teacher.getClassMeetings().add(futureLecture);
            group.getClassMeetings().add(futureLecture);

            ClassMeeting pastLaboratory = new ClassMeeting(
                    today.minusWeeks(1),
                    "201",
                    null,
                    new MeetingTime(today.minusWeeks(1).getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(9, 30)),
                    ClassType.LABORATORY,
                    MeetingMode.CLASSROOM,
                    mas,
                    teacher,
                    group
            );
            pastLaboratory.setStatus(ClassMeetingStatus.COMPLETED);
            pastLaboratory.setComment("Completed laboratory with existing attendance.");
            mas.getClassMeetings().add(pastLaboratory);
            teacher.getClassMeetings().add(pastLaboratory);
            group.getClassMeetings().add(pastLaboratory);

            laboratoryRoom.addBooking(new RoomBooking(
                    new MeetingSlot(currentTutorial.getMeetingDate(), LocalTime.of(10, 0), LocalTime.of(11, 30)),
                    BookingStatus.CONFIRMED,
                    laboratoryRoom,
                    currentTutorial
            ));
            laboratoryRoom.addBooking(new RoomBooking(
                    new MeetingSlot(pastLaboratory.getMeetingDate(), LocalTime.of(8, 0), LocalTime.of(9, 30)),
                    BookingStatus.CONFIRMED,
                    laboratoryRoom,
                    pastLaboratory
            ));

            Attendance existing = new Attendance(AttendanceStatus.PRESENT, anna, pastLaboratory);
            existing.setComment("Attended full class.");
            pastLaboratory.addAttendance(existing);
            Attendance existingLate = new Attendance(AttendanceStatus.LATE, jan, pastLaboratory);
            existingLate.setComment("Arrived 15 minutes late.");
            pastLaboratory.addAttendance(existingLate);

            AttendanceReport report = new AttendanceReport(LocalDate.now(), ReportType.COMBINED);
            report.setGroup(group);
            report.setSubject(mas);
            report.setTeacher(teacher);
            report.getSemesters().add(semester);
            report.addLine(new ReportLine(anna, 1, 1, 0, 0, 0));
            report.addLine(new ReportLine(jan, 1, 0, 0, 0, 1));

            em.persist(field);
            em.persist(anna);
            em.persist(jan);
            em.persist(maria);
            em.persist(adam);
            em.persist(teacher);
            em.persist(admin);
            em.persist(mas);
            em.persist(laboratoryRoom);
            em.persist(lectureRoom);
            em.persist(scheduleEntry);
            em.persist(report);
            EmailNotification studentNotification = new EmailNotification("Attendance was updated for MAS laboratory", 2, true);
            studentNotification.setRecipient(anna);
            studentNotification.setStatus(NotificationStatus.SENT);
            SystemNotification teacherNotification = new SystemNotification("Current-week MAS tutorial is scheduled in room 201", 2, 45);
            teacherNotification.setRecipient(teacher);
            SystemNotification adminNotification = new SystemNotification("Attendance report generated", 3, 60);
            adminNotification.setRecipient(admin);
            em.persist(studentNotification);
            em.persist(teacherNotification);
            em.persist(adminNotification);
            ScheduledNotificationTask task = new ScheduledNotificationTask(NotificationTaskType.REPORT_READY, LocalDateTime.now());
            task.setRecipient(admin);
            task.setAttendanceReport(report);
            em.persist(task);

            tx.commit();
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
