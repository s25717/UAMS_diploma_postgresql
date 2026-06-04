package service;

import model.AttendanceReport;
import model.ScheduledNotificationTask;
import model.enums.NotificationTaskStatus;
import model.enums.NotificationTaskType;
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
                task.setStatus(NotificationTaskStatus.PROCESSING);
                taskService.update(task);
                notificationService.createNotificationForTask(task);
                task.setStatus(NotificationTaskStatus.SENT);
                task.setProcessedAt(LocalDateTime.now());
                taskService.update(task);
            } catch (RuntimeException ex) {
                task.setStatus(NotificationTaskStatus.FAILED);
                task.setRetryCount(task.getRetryCount() + 1);
                task.setFailureReason(ex.getMessage());
                taskService.update(task);
            }
        }
    }

    public void createClassMeetingReminderTasks() {
        classMeetingRepository.findAllWithBasicData().forEach(meeting -> {
            ScheduledNotificationTask task = new ScheduledNotificationTask(
                    NotificationTaskType.CLASS_MEETING_REMINDER,
                    LocalDateTime.now().plusMinutes(1)
            );
            task.setClassMeeting(meeting);
            taskService.add(task);
        });
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
        ScheduledNotificationTask task = new ScheduledNotificationTask(NotificationTaskType.REPORT_READY, LocalDateTime.now());
        task.setAttendanceReport(report);
        taskService.add(task);
    }
}
