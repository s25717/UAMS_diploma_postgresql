import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import model.Attendance;
import model.AttendanceReport;
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
import model.enums.AttendanceStatus;
import model.enums.BookingStatus;
import model.enums.ClassType;
import model.enums.NotificationTaskType;
import model.enums.ReportType;
import model.value.BirthDate;
import model.value.MeetingSlot;
import model.value.MeetingTime;
import persistence.ClassMeetingRepository;
import persistence.GenericRepository;
import persistence.JpaUtil;
import persistence.StudentGroupRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        try {
            DemoIds ids = createSampleDataInCleanTransaction();

            GenericRepository<Student> studentRepository = new GenericRepository<>(Student.class);
            StudentGroupRepository groupRepository = new StudentGroupRepository();
            ClassMeetingRepository meetingRepository = new ClassMeetingRepository();

            System.out.println("All students persisted: " + studentRepository.findAll().size());
            System.out.println("Group with students fetched using one join-fetch query: "
                    + groupRepository.findByIdWithStudents(ids.groupId()).orElseThrow().getStudents().size());
            System.out.println("Only students for group fetched using one query: "
                    + groupRepository.findStudentsByGroupId(ids.groupId()).size());
            System.out.println("Meeting with attendance fetched using one join-fetch query: "
                    + meetingRepository.findByIdWithAttendance(ids.meetingId()).orElseThrow().getAttendances().size());
            System.out.println("Only attendance rows for meeting fetched using one query: "
                    + meetingRepository.findAttendanceByMeetingId(ids.meetingId()).size());
            System.out.println("Public weekly schedule rows fetched with status filtering: "
                    + meetingRepository.findClassMeetingsForCurrentWeek().size());

            groupRepository.removeStudentFromGroup(ids.groupId(), ids.studentToRemoveId());
            System.out.println("Association deleted in database. Students after removal: "
                    + groupRepository.findStudentsByGroupId(ids.groupId()).size());

            meetingRepository.removeAttendanceFromMeeting(ids.meetingId(), ids.attendanceToRemoveId());
            System.out.println("Association class row removed by orphanRemoval. Attendance rows after removal: "
                    + meetingRepository.findAttendanceByMeetingId(ids.meetingId()).size());
        } finally {
            JpaUtil.close();
        }
    }

    private static DemoIds createSampleDataInCleanTransaction() {
        EntityManager em = JpaUtil.entityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            clearDatabase(em);

            Field field = new Field("Informatics");
            Semester semester = new Semester(3);
            StudentGroup group = new StudentGroup("G3A");
            field.addSemester(semester);
            semester.addGroup(group);

            Student jan = new Student(
                    "Jan",
                    "Kowalski",
                    new BirthDate(LocalDate.of(2000, 5, 10)),
                    Set.of("jan.kowalski@student.pja.edu.pl"),
                    "s100"
            );
            Student anna = new Student(
                    "Anna",
                    "Nowak",
                    new BirthDate(LocalDate.of(2001, 3, 15)),
                    Set.of("anna.nowak@student.pja.edu.pl", "anna.private@example.com"),
                    "s101"
            );
            group.addStudent(jan);
            group.addStudent(anna);

            Teacher teacher = new Teacher(
                    "Piotr",
                    "Zielinski",
                    new BirthDate(LocalDate.of(1980, 8, 20)),
                    Set.of("piotr.zielinski@pja.edu.pl"),
                    "t100"
            );

            Subject databases = new Subject("Databases");
            databases.addGroup(group);
            teacher.addQualifiedSubject(databases);

            ClassMeeting meeting = new ClassMeeting(
                    "A101",
                    null,
                    new MeetingTime(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 30)),
                    ClassType.LABORATORY,
                    databases,
                    teacher,
                    group
            );
            databases.getClassMeetings().add(meeting);
            teacher.getClassMeetings().add(meeting);
            group.getClassMeetings().add(meeting);

            Room room = new Room("A101", 30);
            RoomBooking roomBooking = new RoomBooking(
                    new MeetingSlot(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 30)),
                    BookingStatus.CONFIRMED,
                    room,
                    meeting
            );
            room.addBooking(roomBooking);

            Attendance janAttendance = new Attendance(AttendanceStatus.PRESENT, jan, meeting);
            Attendance annaAttendance = new Attendance(AttendanceStatus.LATE, anna, meeting);
            meeting.addAttendance(janAttendance);
            meeting.addAttendance(annaAttendance);

            AttendanceReport report = new AttendanceReport(LocalDate.now(), ReportType.COMBINED);
            report.setGroup(group);
            report.setSubject(databases);
            report.setTeacher(teacher);
            report.getSemesters().add(semester);
            report.addLine(new ReportLine(jan, 1, 1, 0, 0, 0));
            report.addLine(new ReportLine(anna, 1, 0, 1, 0, 0));

            em.persist(field);
            em.persist(room);
            em.persist(jan);
            em.persist(anna);
            em.persist(teacher);
            em.persist(databases);
            em.persist(report);
            em.persist(new EmailNotification("Class room changed", 2, true));
            em.persist(new SystemNotification("Attendance report generated", 3, 60));
            em.persist(new ScheduledNotificationTask(NotificationTaskType.REPORT_READY, java.time.LocalDateTime.now()));

            tx.commit();
            return new DemoIds(group.getId(), meeting.getId(), jan.getId(), annaAttendance.getId());
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    private static void clearDatabase(EntityManager em) {
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        for (String tableName : List.of(
                "Attendance",
                "ReportLine",
                "AttendanceReport",
                "ClassMeeting",
                "WeeklyScheduleEntry",
                "subject_group",
                "teacher_subject",
                "attendance_report_semesters",
                "person_emails",
                "RoomBooking",
                "ScheduledNotificationTask",
                "Student",
                "Teacher",
                "Administrator",
                "Person",
                "Subject",
                "StudentGroup",
                "Semester",
                "Field",
                "Notification",
                "Room"
        )) {
            em.createNativeQuery("TRUNCATE TABLE " + tableName + " RESTART IDENTITY").executeUpdate();
        }
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    }

    private record DemoIds(Long groupId, Long meetingId, Long studentToRemoveId, Long attendanceToRemoveId) {
    }
}
