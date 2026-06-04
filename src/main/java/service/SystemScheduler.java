package service;

import model.AttendanceReport;
import model.Administrator;
import model.ClassMeeting;
import model.Person;
import model.Student;
import model.ScheduledNotificationTask;
import model.enums.ClassMeetingStatus;
import model.enums.NotificationTaskStatus;
import model.enums.NotificationTaskType;
import persistence.JpaUtil;
import persistence.AttendanceRepository;
import persistence.ClassMeetingRepository;
import persistence.ScheduledNotificationTaskRepository;

import java.time.LocalDateTime;

public class SystemScheduler {
    private final NotificationService notificationService;
    private final ScheduledNotificationTaskRepository taskRepository;
    private final EntityService<ScheduledNotificationTask> taskService = new EntityService<>(ScheduledNotificationTask.class);
    private final AttendanceRepository attendanceRepository;
    private final ClassMeetingRepository classMeetingRepository;

    public SystemScheduler() {
        this(new NotificationService(), new ScheduledNotificationTaskRepository(),
                new AttendanceRepository(), new ClassMeetingRepository());
    }

    public SystemScheduler(NotificationService notificationService,
                           ScheduledNotificationTaskRepository taskRepository,
                           AttendanceRepository attendanceRepository,
                           ClassMeetingRepository classMeetingRepository) {
        this.notificationService = notificationService;
        this.taskRepository = taskRepository;
        this.attendanceRepository = attendanceRepository;
        this.classMeetingRepository = classMeetingRepository;
    }

    public void processPendingNotificationTasks() {
        for (ScheduledNotificationTask task : taskRepository.findPendingDueTasks(LocalDateTime.now())) {
            try {
                task.startProcessing(LocalDateTime.now());
                taskService.update(task);
                notificationService.createNotificationForTask(task);
                task.markSent(LocalDateTime.now());
                taskService.update(task);
            } catch (RuntimeException ex) {
                if (task.getStatus() == NotificationTaskStatus.PROCESSING) {
                    String reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                    if (reason.length() > 255) {
                        reason = reason.substring(0, 255);
                    }
                    task.markFailed(LocalDateTime.now(), reason);
                    taskService.update(task);
                }
            }
        }
    }

    public void createClassMeetingReminderTasks() {
        var em = JpaUtil.entityManagerFactory().createEntityManager();
        try {
            em.createQuery("""
                    select distinct cm
                    from ClassMeeting cm
                    join fetch cm.teacher
                    join fetch cm.group g
                    left join fetch g.students
                    where cm.status = :status
                    """, ClassMeeting.class)
                    .setParameter("status", ClassMeetingStatus.SCHEDULED)
                    .getResultList()
                    .forEach(meeting -> {
                        createReminderTask(meeting, meeting.getTeacher());
                        for (Student student : meeting.getGroup().getStudents()) {
                            createReminderTask(meeting, student);
                        }
                    });
        } finally {
            em.close();
        }
    }

    public void createLowAttendanceWarnings() {
        attendanceRepository.findAllWithStudentAndClassMeeting().forEach(attendance -> {
            if (attendance.getStatus().name().equals("ABSENT")) {
                ScheduledNotificationTask task = new ScheduledNotificationTask(
                        NotificationTaskType.LOW_ATTENDANCE_WARNING,
                        LocalDateTime.now()
                );
                task.setRecipient(attendance.getStudent());
                task.setClassMeeting(attendance.getClassMeeting());
                taskService.add(task);
            }
        });
    }

    public void createReportReadyNotification(AttendanceReport report) {
        createReportReadyNotification(report, findFirstAdministrator());
    }

    public void createReportReadyNotification(AttendanceReport report, Person recipient) {
        if (recipient == null) {
            throw new IllegalArgumentException("Report-ready notification recipient is required.");
        }
        ScheduledNotificationTask task = new ScheduledNotificationTask(NotificationTaskType.REPORT_READY, LocalDateTime.now());
        task.setRecipient(recipient);
        task.setAttendanceReport(report);
        taskService.add(task);
    }

    private void createReminderTask(ClassMeeting meeting, Person recipient) {
        ScheduledNotificationTask task = new ScheduledNotificationTask(
                NotificationTaskType.CLASS_MEETING_REMINDER,
                LocalDateTime.now().plusMinutes(1)
        );
        task.setRecipient(recipient);
        task.setClassMeeting(meeting);
        taskService.add(task);
    }

    private Administrator findFirstAdministrator() {
        var em = JpaUtil.entityManagerFactory().createEntityManager();
        try {
            return em.createQuery("select a from Administrator a order by a.id", Administrator.class)
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } finally {
            em.close();
        }
    }
}
