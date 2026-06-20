package app;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import model.Administrator;
import model.ActivityLog;
import model.Attendance;
import model.AttendanceReport;
import model.ClassMeeting;
import model.EmailNotification;
import model.Field;
import model.Notification;
import model.Person;
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
import model.enums.BookingStatus;
import model.enums.AttendanceStatus;
import model.enums.ClassMeetingStatus;
import model.enums.ClassType;
import model.enums.MeetingMode;
import model.enums.NotificationStatus;
import model.value.MeetingSlot;
import model.value.MeetingTime;
import persistence.ActivityLogRepository;
import persistence.AttendanceRepository;
import persistence.AttendanceReportRepository;
import persistence.ClassMeetingRepository;
import persistence.GenericRepository;
import persistence.JpaUtil;
import persistence.NotificationRepository;
import persistence.PersonRepository;
import persistence.RoomBookingRepository;
import persistence.ScheduledNotificationTaskRepository;
import persistence.SemesterRepository;
import persistence.StudentGroupRepository;
import persistence.TeacherRepository;
import persistence.WeeklyScheduleEntryRepository;
import service.AttendanceRegistrationService;
import service.AttendanceReportFilter;
import service.AttendanceReportService;
import service.AuthenticationService;
import service.AdminManagementService;
import service.ClassMeetingService;
import service.NotificationManagementService;
import service.PersonService;
import service.PersonalSettingsService;
import service.ReportExportService;
import service.RoomBookingService;
import service.RoomService;
import service.SampleDataService;
import service.ScheduleAccessService;
import service.ScheduleGenerationService;
import util.ExceptionMessages;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AttendanceGuiApp extends Application {
    private static final int SCHEDULE_START_HOUR = 7;
    private static final int SCHEDULE_END_HOUR = 22;
    private static final int SCHEDULE_SLOT_MINUTES = 30;
    private static final List<DayOfWeek> SCHEDULE_DAYS = List.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
    );

    private final AuthenticationService authenticationService = new AuthenticationService();
    private final AttendanceRegistrationService attendanceService = new AttendanceRegistrationService();
    private final AttendanceReportService reportService = new AttendanceReportService();
    private final ReportExportService reportExportService = new ReportExportService();
    private final ClassMeetingService classMeetingService = new ClassMeetingService();
    private final ClassMeetingRepository classMeetingRepository = new ClassMeetingRepository();
    private final AttendanceRepository attendanceRepository = new AttendanceRepository();
    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();
    private final AttendanceReportRepository attendanceReportRepository = new AttendanceReportRepository();
    private final RoomBookingRepository roomBookingRepository = new RoomBookingRepository();
    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final ScheduledNotificationTaskRepository scheduledNotificationTaskRepository = new ScheduledNotificationTaskRepository();
    private final NotificationManagementService notificationManagementService = new NotificationManagementService();
    private final PersonalSettingsService personalSettingsService = new PersonalSettingsService();
    private final ScheduleAccessService scheduleAccessService = new ScheduleAccessService();
    private final PersonService personService = new PersonService();
    private final AdminManagementService adminManagementService = new AdminManagementService();
    private final RoomService roomService = new RoomService();
    private final RoomBookingService roomBookingService = new RoomBookingService();
    private final ScheduleGenerationService scheduleGenerationService = new ScheduleGenerationService();
    private final WeeklyScheduleEntryRepository weeklyScheduleEntryRepository = new WeeklyScheduleEntryRepository();

    private Stage primaryStage;
    private BorderPane mainLayout;
    private VBox navigation;
    private Button backButton;
    private Label userLabel;
    private Label pageMessageLabel;
    private String currentPageTitle;
    private Node currentPageContent;
    private final Deque<PageSnapshot> pageHistory = new ArrayDeque<>();

    private final ObservableList<ReportLineRow> reportRows = FXCollections.observableArrayList();
    private ListView<Semester> semesterFilter;
    private ComboBox<Teacher> teacherFilter;
    private ComboBox<Subject> subjectFilter;
    private ComboBox<StudentGroup> groupFilter;
    private ComboBox<String> classTypeFilter;
    private DatePicker dateFromFilter;
    private DatePicker dateToFilter;
    private Label reportMessageLabel;
    private AttendanceReport currentReport;

    private record PageSnapshot(String title, Node content) {
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        new SampleDataService().seedIfDatabaseIsEmpty();
        showLoginView();
        stage.setTitle("University Academic Management System");
        stage.setMinWidth(1050);
        stage.setMinHeight(680);
        stage.show();
    }

    private void showLoginView() {
        TextField emailField = new TextField();
        emailField.setPromptText("email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("password");
        Label message = new Label();

        Button loginButton = new Button("Log in");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> authenticationService.login(emailField.getText(), passwordField.getText())
                .ifPresentOrElse(person -> showMainLayout(), () -> message.setText("Invalid email or password.")));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(24));
        form.add(new Label("Email"), 0, 0);
        form.add(emailField, 1, 0);
        form.add(new Label("Password"), 0, 1);
        form.add(passwordField, 1, 1);
        form.add(loginButton, 1, 2);
        form.add(message, 1, 3);

        Label accounts = new Label("""
                Demo accounts
                Student: anna.nowak@student.pja.edu.pl / student
                Teacher: piotr.kowalski@pja.edu.pl / teacher
                Admin: admin@pja.edu.pl / admin""");
        VBox content = new VBox(18, new Label("University Academic Management System"), form, accounts);
        content.getStyleClass().add("login-panel");
        content.setPadding(new Insets(36));
        primaryStage.setScene(styledScene(content, 620, 420));
    }

    private void showMainLayout() {
        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("app-shell");
        pageHistory.clear();
        currentPageTitle = null;
        currentPageContent = null;
        backButton = new Button("Back");
        backButton.setDisable(true);
        backButton.setOnAction(event -> goBack());
        userLabel = new Label();
        userLabel.getStyleClass().add("user-label");
        pageMessageLabel = new Label();
        pageMessageLabel.getStyleClass().add("page-title");
        navigation = new VBox(8);
        navigation.getStyleClass().add("navigation");
        navigation.setPadding(new Insets(12));
        navigation.setPrefWidth(230);

        HBox header = new HBox(16, backButton, userLabel, pageMessageLabel);
        header.getStyleClass().add("app-header");
        header.setPadding(new Insets(12));
        mainLayout.setTop(header);
        mainLayout.setLeft(navigation);
        refreshUserLabel();
        buildNavigation();
        setPage("Public Weekly Schedule", createPublicScheduleView());

        primaryStage.setScene(styledScene(mainLayout, 1180, 720));
    }

    private Scene styledScene(Node root, double width, double height) {
        Scene scene = new Scene((javafx.scene.Parent) root, width, height);
        scene.getStylesheets().add(getClass().getResource("/uams.css").toExternalForm());
        return scene;
    }

    private void refreshUserLabel() {
        Person user = currentUser();
        userLabel.setText("Logged in: " + user.getName() + " " + user.getSurname() + " (" + roleName(user) + ")");
    }

    private void buildNavigation() {
        navigation.getChildren().clear();
        navigation.getChildren().add(navButton("Public Weekly Schedule", () -> setPage("Public Weekly Schedule", createPublicScheduleView())));
        navigation.getChildren().add(navButton("Personal Settings", () -> setPage("Personal Settings", createPersonalSettingsView())));
        navigation.getChildren().add(navButton("My Notifications", () -> setPage("My Notifications", createMyNotificationsView())));
        navigation.getChildren().add(navButton("My History", () -> setPage("My History", createMyHistoryView())));

        if (authenticationService.isStudent()) {
            navigation.getChildren().add(new Separator());
            navigation.getChildren().add(navButton("My Schedule", () -> setPage("My Schedule", createStudentScheduleView())));
            navigation.getChildren().add(navButton("My Attendance", () -> setPage("My Attendance", createStudentAttendanceView())));
            navigation.getChildren().add(navButton("My Group", () -> setPage("My Group", createMyGroupView())));
        }
        if (authenticationService.isTeacher()) {
            navigation.getChildren().add(new Separator());
            navigation.getChildren().add(navButton("Group Students Association", () -> setPage("GroupStudentsAssociationView", createGroupStudentsAssociationView())));
            navigation.getChildren().add(navButton("My Class Meetings", () -> setPage("My Class Meetings", createTeacherMeetingsView())));
            navigation.getChildren().add(navButton("Weekly Schedules", () -> setPage("Weekly Schedules", createWeeklyScheduleView(false))));
            navigation.getChildren().add(navButton("Create Class Meeting", () -> setPage("Create Class Meeting", createClassMeetingForm(false))));
        }
        if (authenticationService.isAdmin()) {
            navigation.getChildren().add(new Separator());
            navigation.getChildren().add(navButton("Group Students Association", () -> setPage("GroupStudentsAssociationView", createGroupStudentsAssociationView())));
            navigation.getChildren().add(navButton("Manage Class Meetings", () -> setPage("Manage Class Meetings", createAdminMeetingsView())));
            navigation.getChildren().add(navButton("Manage Weekly Schedules", () -> setPage("Manage Weekly Schedules", createAdminWeeklyScheduleView())));
            navigation.getChildren().add(navButton("Manage Users", () -> setPage("Manage Users", createUsersView())));
            navigation.getChildren().add(navButton("Manage Notifications", () -> setPage("Manage Notifications", createAdminNotificationView())));
            navigation.getChildren().add(navButton("Generate Reports", () -> setPage("Generate Reports", createAdminReportView())));
            navigation.getChildren().add(navButton("Manage Rooms", () -> setPage("Manage Rooms", createRoomsView())));
        }

        Button logout = navButton("Logout", () -> {
            authenticationService.logout();
            pageHistory.clear();
            showLoginView();
        });
        navigation.getChildren().add(new Separator());
        navigation.getChildren().add(logout);
    }

    private Button navButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> action.run());
        return button;
    }

    private void setPage(String title, Node content) {
        setPage(title, content, true);
    }

    private void setPage(String title, Node content, boolean addToHistory) {
        if (addToHistory && currentPageContent != null) {
            pageHistory.push(new PageSnapshot(currentPageTitle, currentPageContent));
        }
        currentPageTitle = title;
        currentPageContent = content;
        pageMessageLabel.setText(title);
        mainLayout.setCenter(content);
        refreshBackButton();
    }

    private void goBack() {
        if (pageHistory.isEmpty()) {
            return;
        }
        PageSnapshot previousPage = pageHistory.pop();
        setPage(previousPage.title(), previousPage.content(), false);
    }

    private void refreshBackButton() {
        if (backButton != null) {
            backButton.setDisable(pageHistory.isEmpty());
        }
    }

    private BorderPane createPublicScheduleView() {
        BorderPane root = new BorderPane();
        Label message = new Label("Public view shows subject, group, class type, and room only. Click a meeting to open details if you are involved.");
        message.getStyleClass().add("muted-label");
        DatePicker weekPicker = new DatePicker(LocalDate.now());
        Label weekLabel = new Label();
        weekLabel.getStyleClass().add("section-title");

        Runnable refreshSchedule = () -> {
            LocalDate weekStart = weekStart(weekPicker.getValue() == null ? LocalDate.now() : weekPicker.getValue());
            weekPicker.setValue(weekStart);
            weekLabel.setText("Week " + weekStart + " - " + weekStart.plusDays(6));
            root.setCenter(createPublicScheduleGrid(weekStart, message));
        };

        Button previous = new Button("Previous Week");
        previous.setOnAction(event -> {
            weekPicker.setValue(weekStart(weekPicker.getValue()).minusWeeks(1));
            refreshSchedule.run();
        });
        Button today = new Button("This Week");
        today.setOnAction(event -> {
            weekPicker.setValue(LocalDate.now());
            refreshSchedule.run();
        });
        Button next = new Button("Next Week");
        next.setOnAction(event -> {
            weekPicker.setValue(weekStart(weekPicker.getValue()).plusWeeks(1));
            refreshSchedule.run();
        });
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> refreshSchedule.run());

        HBox controls = new HBox(10, previous, today, next, new Label("Show week of"), weekPicker, refresh);
        controls.setAlignment(Pos.CENTER_LEFT);
        VBox top = new VBox(8, weekLabel, message, controls);
        top.getStyleClass().add("page-toolbar");
        root.setTop(top);
        refreshSchedule.run();
        return root;
    }

    private ScrollPane createPublicScheduleGrid(LocalDate weekStart, Label message) {
        List<ClassMeeting> meetings = classMeetingRepository.findByDateRange(weekStart, weekStart.plusDays(6)).stream()
                .filter(meeting -> meeting.getStatus() == null || (meeting.getStatus() != ClassMeetingStatus.CANCELLED && meeting.getStatus() != ClassMeetingStatus.DRAFT))
                .toList();
        GridPane grid = new GridPane();
        grid.getStyleClass().add("schedule-grid");
        grid.getColumnConstraints().add(column(72, 82, 96));
        for (int i = 0; i < SCHEDULE_DAYS.size(); i++) {
            grid.getColumnConstraints().add(column(136, 160, Double.MAX_VALUE));
        }
        grid.getRowConstraints().add(row(48));

        addScheduleHeader(grid, 0, "Time");
        for (int i = 0; i < SCHEDULE_DAYS.size(); i++) {
            LocalDate date = weekStart.plusDays(i);
            addScheduleHeader(grid, i + 1, dayHeader(SCHEDULE_DAYS.get(i), date));
        }

        int slotCount = ((SCHEDULE_END_HOUR - SCHEDULE_START_HOUR) * 60) / SCHEDULE_SLOT_MINUTES;
        for (int slot = 0; slot < slotCount; slot++) {
            grid.getRowConstraints().add(row(34));
            LocalTime time = LocalTime.of(SCHEDULE_START_HOUR, 0).plusMinutes((long) slot * SCHEDULE_SLOT_MINUTES);
            addTimeCell(grid, slot + 1, time);
            for (int day = 1; day <= SCHEDULE_DAYS.size(); day++) {
                StackPane cell = new StackPane();
                cell.getStyleClass().add(time.getMinute() == 0 ? "schedule-cell-hour" : "schedule-cell-half");
                grid.add(cell, day, slot + 1);
            }
        }

        for (ClassMeeting meeting : meetings) {
            int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(weekStart, meeting.getMeetingDate());
            if (dayIndex < 0 || dayIndex >= SCHEDULE_DAYS.size() || meeting.getTime() == null) {
                continue;
            }
            LocalTime start = meeting.getTime().getStartTime();
            LocalTime end = meeting.getTime().getEndTime();
            if (start == null || end == null || !end.isAfter(start)) {
                continue;
            }
            int row = (int) Math.floor(java.time.Duration.between(LocalTime.of(SCHEDULE_START_HOUR, 0), start).toMinutes() / (double) SCHEDULE_SLOT_MINUTES) + 1;
            int span = (int) Math.ceil(java.time.Duration.between(start, end).toMinutes() / (double) SCHEDULE_SLOT_MINUTES);
            if (row < 1 || row >= slotCount + 1) {
                continue;
            }
            span = Math.max(1, Math.min(span, slotCount - row + 1));
            VBox card = createPublicScheduleCard(meeting, message);
            grid.add(card, dayIndex + 1, row, 1, span);
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.getStyleClass().add("schedule-scroll");
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        return scroll;
    }

    private ColumnConstraints column(double min, double pref, double max) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setMinWidth(min);
        constraints.setPrefWidth(pref);
        constraints.setMaxWidth(max);
        constraints.setHgrow(Priority.ALWAYS);
        return constraints;
    }

    private RowConstraints row(double height) {
        RowConstraints constraints = new RowConstraints();
        constraints.setMinHeight(height);
        constraints.setPrefHeight(height);
        return constraints;
    }

    private void addScheduleHeader(GridPane grid, int column, String text) {
        Label label = new Label(text);
        label.getStyleClass().add("schedule-header");
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        grid.add(label, column, 0);
    }

    private void addTimeCell(GridPane grid, int row, LocalTime time) {
        Label label = new Label(time.toString());
        label.getStyleClass().add("schedule-time");
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.setAlignment(Pos.TOP_CENTER);
        grid.add(label, 0, row);
    }

    private VBox createPublicScheduleCard(ClassMeeting meeting, Label message) {
        Label subject = new Label(safe(meeting.getSubject().getName()));
        subject.getStyleClass().add("schedule-card-subject");
        subject.setWrapText(true);
        Label meta = new Label(safe(meeting.getGroup().getCode()) + " • " + meeting.getClassType());
        meta.getStyleClass().add("schedule-card-meta");
        meta.setWrapText(true);
        Label room = new Label(publicRoomLabel(meeting));
        room.getStyleClass().add("schedule-card-room");
        room.setWrapText(true);
        VBox card = new VBox(3, subject, meta, room);
        card.getStyleClass().add("schedule-card");
        card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        card.setOnMouseClicked(event -> tryOpenMeetingDetails(meeting, message));
        return card;
    }

    private String publicRoomLabel(ClassMeeting meeting) {
        if (meeting.getRoomBooking() == null || meeting.getRoomBooking().getRoom() == null) {
            return "Room not specified";
        }
        String roomNumber = meeting.getRoomBooking().getRoom().getRoomNumber();
        if (roomNumber != null && !roomNumber.isBlank()) {
            return "Room " + roomNumber;
        }
        return "Room not specified";
    }

    private LocalDate weekStart(LocalDate date) {
        return date.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private String dayHeader(DayOfWeek day, LocalDate date) {
        return day.name().substring(0, 3) + System.lineSeparator() + date;
    }

    private Node createStudentScheduleView() {
        Long studentId = currentUser().getId();
        List<ClassMeeting> meetings = new ArrayList<>();
        meetings.addAll(classMeetingRepository.findUpcomingClassMeetingsForStudent(studentId));
        meetings.addAll(classMeetingRepository.findPastClassMeetingsForStudent(studentId));
        StudentGroup group = new StudentGroupRepository().findDetailsForStudent(studentId).orElse(null);
        ListView<WeeklyScheduleEntry> weeklyEntries = new ListView<>(FXCollections.observableArrayList(
                group == null ? List.of() : weeklyScheduleEntryRepository.findByGroupIdWithDetails(group.getId())
        ));
        weeklyEntries.setCellFactory(view -> weeklyScheduleEntryCell());
        VBox box = new VBox(10,
                new Label("Weekly schedule template"),
                weeklyEntries,
                new Label("Generated class meetings"),
                meetingListPage(meetings, "Student schedule uses Student -> StudentGroup -> ClassMeeting.")
        );
        box.setPadding(new Insets(12));
        return box;
    }

    private Node createTeacherMeetingsView() {
        Long teacherId = currentUser().getId();
        List<ClassMeeting> meetings = new ArrayList<>();
        meetings.addAll(classMeetingRepository.findUpcomingClassMeetingsForTeacher(teacherId));
        meetings.addAll(classMeetingRepository.findPastClassMeetingsForTeacher(teacherId));
        ListView<WeeklyScheduleEntry> weeklyEntries = new ListView<>(FXCollections.observableArrayList(
                weeklyScheduleEntryRepository.findByTeacherIdWithDetails(teacherId)
        ));
        weeklyEntries.setCellFactory(view -> weeklyScheduleEntryCell());
        VBox box = new VBox(10,
                new Label("Assigned weekly recurring entries"),
                weeklyEntries,
                new Label("Generated class meetings"),
                meetingListPage(meetings, "Double-click an assigned meeting to comment or mark attendance.")
        );
        box.setPadding(new Insets(12));
        return box;
    }

    private Node createAdminMeetingsView() {
        VBox content = new VBox(10,
                meetingListPage(classMeetingRepository.findAllWithBasicData(), "Admin can open every class meeting."),
                new Button("Create Class Meeting")
        );
        ((Button) content.getChildren().get(1)).setOnAction(event -> setPage("Create Class Meeting", createClassMeetingForm(true)));
        content.setPadding(new Insets(12));
        return content;
    }

    private Node meetingListPage(List<ClassMeeting> source, String text) {
        Label message = new Label(text);
        ListView<ClassMeeting> list = createMeetingList(FXCollections.observableArrayList(source));
        list.setOnMouseClicked(event -> {
            ClassMeeting selected = list.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) {
                tryOpenMeetingDetails(selected, message);
            }
        });
        VBox box = new VBox(10, message, list);
        box.setPadding(new Insets(12));
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    private Node createGroupStudentsAssociationView() {
        ObservableList<StudentGroup> groups = FXCollections.observableArrayList(new StudentGroupRepository().findAllWithStudents());
        ListView<StudentGroup> groupListView = new ListView<>(groups);
        groupListView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(StudentGroup group, boolean empty) {
                super.updateItem(group, empty);
                setText(empty || group == null ? null : group.getCode() + " | Semester " + group.getSemester().getNumber());
            }
        });

        TableView<Student> studentsTableView = new TableView<>();
        TableColumn<Student, String> numberColumn = new TableColumn<>("Student number");
        numberColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStudentNumber()));
        TableColumn<Student, String> firstNameColumn = new TableColumn<>("First name");
        firstNameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        TableColumn<Student, String> lastNameColumn = new TableColumn<>("Last name");
        lastNameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getSurname()));
        TableColumn<Student, String> emailColumn = new TableColumn<>("Emails");
        emailColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(emailsLabel(data.getValue())));
        studentsTableView.getColumns().add(numberColumn);
        studentsTableView.getColumns().add(firstNameColumn);
        studentsTableView.getColumns().add(lastNameColumn);
        studentsTableView.getColumns().add(emailColumn);

        groupListView.getSelectionModel().selectedItemProperty().addListener((observable, oldGroup, selectedGroup) -> {
            if (selectedGroup == null) {
                studentsTableView.setItems(FXCollections.observableArrayList());
            } else {
                studentsTableView.setItems(FXCollections.observableArrayList(selectedGroup.getStudents().stream()
                        .sorted(Comparator.comparing(Student::getSurname).thenComparing(Student::getName))
                        .toList()));
            }
        });
        if (!groups.isEmpty()) {
            groupListView.getSelectionModel().selectFirst();
        }

        BorderPane root = new BorderPane();
        root.setTop(padded(new Label("Students are displayed through the predefined association StudentGroup -> Students.")));
        root.setLeft(groupListView);
        root.setCenter(studentsTableView);
        BorderPane.setMargin(groupListView, new Insets(0, 8, 12, 12));
        BorderPane.setMargin(studentsTableView, new Insets(0, 12, 12, 0));
        groupListView.setPrefWidth(280);
        return root;
    }

    private ListView<ClassMeeting> createMeetingList(ObservableList<ClassMeeting> meetings) {
        ListView<ClassMeeting> list = new ListView<>(meetings);
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(ClassMeeting meeting, boolean empty) {
                super.updateItem(meeting, empty);
                setText(empty || meeting == null ? null : meetingSummary(meeting));
            }
        });
        return list;
    }

    private void tryOpenMeetingDetails(ClassMeeting meeting, Label message) {
        try {
            scheduleAccessService.assertCanOpenFullDetails(currentUser(), meeting);
            showClassMeetingDetails(meeting);
        } catch (RuntimeException ex) {
            message.setText(ExceptionMessages.userMessage(ex));
        }
    }

    private Node createPersonalSettingsView() {
        Person user = currentUser();
        ObservableList<String> emails = FXCollections.observableArrayList(user.getEmails());
        ListView<String> emailList = new ListView<>(emails);
        TextField emailField = new TextField();
        Label message = new Label();
        Label primaryEmailLabel = new Label("Primary email: " + user.getPrimaryEmail());

        Button add = new Button("Add Email");
        add.setOnAction(event -> runWithMessage(message, () -> {
            String newEmail = emailField.getText().trim();
            personalSettingsService.addEmail(user.getId(), newEmail);
            user.getEmails().add(newEmail);
            if (user.getPrimaryEmailValue() == null || user.getPrimaryEmailValue().isBlank()) {
                user.setPrimaryEmail(newEmail);
            }
            emails.setAll(user.getEmails());
            primaryEmailLabel.setText("Primary email: " + user.getPrimaryEmail());
            emailField.clear();
            return "Email added.";
        }));
        Button edit = new Button("Edit Selected Email");
        edit.setOnAction(event -> runWithMessage(message, () -> {
            String selected = emailList.getSelectionModel().getSelectedItem();
            String newEmail = emailField.getText().trim();
            personalSettingsService.editEmail(user.getId(), selected, newEmail);
            user.getEmails().remove(selected);
            user.getEmails().add(newEmail);
            if (selected != null && selected.equals(user.getPrimaryEmailValue())) {
                user.setPrimaryEmail(newEmail);
            }
            emails.setAll(user.getEmails());
            primaryEmailLabel.setText("Primary email: " + user.getPrimaryEmail());
            emailField.clear();
            return "Email updated.";
        }));
        Button delete = new Button("Delete Selected Email");
        delete.setOnAction(event -> runWithMessage(message, () -> {
            String selected = emailList.getSelectionModel().getSelectedItem();
            personalSettingsService.deleteEmail(user.getId(), selected);
            user.getEmails().remove(selected);
            if (selected != null && selected.equals(user.getPrimaryEmailValue())) {
                user.setPrimaryEmail(user.getEmails().stream().sorted().findFirst().orElse(null));
            }
            emails.setAll(user.getEmails());
            primaryEmailLabel.setText("Primary email: " + user.getPrimaryEmail());
            return "Email deleted.";
        }));
        Button makePrimary = new Button("Set Selected As Primary");
        makePrimary.setOnAction(event -> runWithMessage(message, () -> {
            String selected = emailList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                throw new IllegalArgumentException("Select an email first.");
            }
            personalSettingsService.setPrimaryEmail(user.getId(), selected);
            user.setPrimaryEmail(selected);
            primaryEmailLabel.setText("Primary email: " + user.getPrimaryEmail());
            return "Primary email updated.";
        }));

        PasswordField oldPassword = new PasswordField();
        oldPassword.setPromptText("old password");
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("new password");
        PasswordField repeatPassword = new PasswordField();
        repeatPassword.setPromptText("repeat new password");
        Button changePassword = new Button("Change Password");
        changePassword.setOnAction(event -> runWithMessage(message, () -> {
            if (!newPassword.getText().equals(repeatPassword.getText())) {
                throw new IllegalArgumentException("New password and repeated password must match.");
            }
            personService.changePassword(user.getId(), oldPassword.getText(), newPassword.getText());
            oldPassword.clear();
            newPassword.clear();
            repeatPassword.clear();
            return "Password changed.";
        }));

        VBox box = new VBox(10,
                new Label("Name: " + user.getName() + " " + user.getSurname()),
                new Label("Role: " + roleName(user)),
                primaryEmailLabel,
                new Label("Emails"), emailList,
                emailField,
                new HBox(10, add, edit, delete, makePrimary),
                new Separator(),
                new Label("Change password"),
                oldPassword, newPassword, repeatPassword, changePassword,
                message
        );
        box.setPadding(new Insets(12));
        return new ScrollPane(box);
    }

    private Node createMyNotificationsView() {
        ListView<Notification> list = new ListView<>(FXCollections.observableArrayList(notificationManagementService.findByRecipientId(currentUser().getId())));
        list.setCellFactory(view -> notificationCell());
        VBox box = new VBox(10, new Label("Notifications sent to " + currentUser().getName() + " " + currentUser().getSurname()), list);
        box.setPadding(new Insets(12));
        return box;
    }

    private List<String> sortedEmails(Person person) {
        if (person == null || person.getEmails() == null) {
            return List.of();
        }
        List<String> emails = new ArrayList<>(person.getEmails());
        emails.sort(String.CASE_INSENSITIVE_ORDER);
        return emails;
    }

    private String emailsLabel(Person person) {
        List<String> emails = sortedEmails(person);
        return emails.isEmpty() ? "no emails" : String.join(", ", emails);
    }

    private String personLabelWithAllEmails(Person person) {
        if (person == null) {
            return "";
        }
        return person.getName() + " " + person.getSurname() + " | emails: " + emailsLabel(person);
    }

    private Node createAdminNotificationView() {
        ObservableList<Notification> notifications = FXCollections.observableArrayList(notificationManagementService.findAllWithRecipients());
        TableView<Notification> table = new TableView<>(notifications);
        TableColumn<Notification, String> channelColumn = new TableColumn<>("Channel");
        channelColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(notificationChannel(data.getValue())));
        TableColumn<Notification, String> recipientColumn = new TableColumn<>("Recipient");
        recipientColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(notificationRecipientLabel(data.getValue())));
        TableColumn<Notification, String> emailColumn = new TableColumn<>("Channel target");
        emailColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(notificationChannelTarget(data.getValue())));
        TableColumn<Notification, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(safe(data.getValue().getTitle())));
        TableColumn<Notification, String> messageColumn = new TableColumn<>("Message");
        messageColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getMessage()));
        messageColumn.setPrefWidth(300);
        TableColumn<Notification, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getStatus())));
        table.getColumns().add(channelColumn);
        table.getColumns().add(recipientColumn);
        table.getColumns().add(emailColumn);
        table.getColumns().add(titleColumn);
        table.getColumns().add(messageColumn);
        table.getColumns().add(statusColumn);

        List<Person> people = new PersonRepository().findAllWithEmails();
        ObservableList<StudentGroup> groups = FXCollections.observableArrayList(new StudentGroupRepository().findAllWithStudents());
        String singleUserMode = "Single user";
        String groupMode = "Group";
        String allUsersMode = "All users";
        ComboBox<String> targetModeBox = new ComboBox<>(FXCollections.observableArrayList(singleUserMode, groupMode, allUsersMode));
        targetModeBox.getSelectionModel().select(singleUserMode);
        ComboBox<Person> recipientBox = new ComboBox<>(FXCollections.observableArrayList(people));
        recipientBox.setConverter(converter(this::personLabelWithAllEmails));
        ComboBox<StudentGroup> groupBox = new ComboBox<>(groups);
        groupBox.setConverter(converter(group -> group.getCode() + " | Semester " + group.getSemester().getNumber()
                + " | " + group.getField().getName()));
        if (!groups.isEmpty()) {
            groupBox.getSelectionModel().selectFirst();
        }
        ListView<NotificationEmailTarget> emailTargetList = new ListView<>();
        emailTargetList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        emailTargetList.setPrefHeight(130);
        emailTargetList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(NotificationEmailTarget target, boolean empty) {
                super.updateItem(target, empty);
                setText(empty || target == null ? null : target.label());
            }
        });
        TextField titleField = new TextField();
        titleField.setPromptText("title");
        TextArea messageArea = new TextArea();
        messageArea.setPromptText("notification message");
        messageArea.setPrefRowCount(3);
        ComboBox<String> channelBox = new ComboBox<>(FXCollections.observableArrayList("EMAIL", "SYSTEM"));
        channelBox.getSelectionModel().select("EMAIL");
        ComboBox<NotificationStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(NotificationStatus.values()));
        statusBox.getSelectionModel().select(NotificationStatus.PENDING);
        Label message = new Label();
        Label targetListLabel = new Label("Delivery emails");
        Runnable refreshTargets = () -> {
            boolean groupSelected = groupMode.equals(targetModeBox.getValue());
            boolean allSelected = allUsersMode.equals(targetModeBox.getValue());
            recipientBox.setManaged(!groupSelected && !allSelected);
            recipientBox.setVisible(!groupSelected && !allSelected);
            groupBox.setManaged(groupSelected);
            groupBox.setVisible(groupSelected);
            boolean emailChannel = "EMAIL".equals(channelBox.getValue());
            targetListLabel.setText(emailChannel ? "Delivery emails" : "In-app recipients");
            refreshNotificationTargets(channelBox.getValue(), targetModeBox.getValue(), recipientBox.getValue(), groupBox.getValue(), people, emailTargetList);
        };
        targetModeBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        recipientBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        groupBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        channelBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        if (!people.isEmpty()) {
            recipientBox.getSelectionModel().selectFirst();
        } else {
            refreshTargets.run();
        }
        Button selectAllEmails = new Button("Select All Targets");
        selectAllEmails.setOnAction(event -> emailTargetList.getSelectionModel().selectAll());
        Button clearEmails = new Button("Clear Selection");
        clearEmails.setOnAction(event -> emailTargetList.getSelectionModel().clearSelection());

        Button create = new Button("Create Notification");
        create.setOnAction(event -> runWithMessage(message, () -> {
            if ("SYSTEM".equals(channelBox.getValue())) {
                List<Long> recipientIds = emailTargetList.getSelectionModel()
                        .getSelectedItems()
                        .stream()
                        .map(target -> target.person().getId())
                        .toList();
                int created = notificationManagementService.createSystemNotifications(recipientIds,
                        titleField.getText(), messageArea.getText(), statusBox.getValue()).size();
                notifications.setAll(notificationManagementService.findAllWithRecipients());
                titleField.clear();
                messageArea.clear();
                return "Created " + created + " system notification(s).";
            }

            List<NotificationManagementService.EmailRecipient> recipients = emailTargetList.getSelectionModel()
                    .getSelectedItems()
                    .stream()
                    .map(target -> new NotificationManagementService.EmailRecipient(target.person().getId(), target.email()))
                    .toList();
            int created = notificationManagementService.createEmailNotifications(recipients,
                    titleField.getText(), messageArea.getText(), statusBox.getValue()).size();
            notifications.setAll(notificationManagementService.findAllWithRecipients());
            titleField.clear();
            messageArea.clear();
            return "Created " + created + " email notification(s).";
        }));

        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected != null) {
                targetModeBox.getSelectionModel().select(singleUserMode);
                recipientBox.setValue(findPersonOption(people, selected.getRecipient()));
                titleField.setText(safe(selected.getTitle()));
                messageArea.setText(selected.getMessage());
                statusBox.setValue(selected.getStatus());
                channelBox.getSelectionModel().select(selected instanceof SystemNotification ? "SYSTEM" : "EMAIL");
                refreshTargets.run();
                if (selected instanceof EmailNotification emailNotification) {
                    selectNotificationEmailTarget(emailTargetList, emailNotification.getDeliveryEmail());
                } else {
                    emailTargetList.getSelectionModel().selectFirst();
                }
            }
        });

        Button saveChanges = new Button("Save Changes");
        saveChanges.setOnAction(event -> runWithMessage(message, () -> {
            Notification selected = table.getSelectionModel().getSelectedItem();
            String deliveryEmail = null;
            if (selected instanceof EmailNotification) {
                deliveryEmail = selectedDeliveryEmailForSingleNotification(emailTargetList);
            }
            notificationManagementService.updateEditableNotification(selected, recipientBox.getValue(),
                    titleField.getText(), messageArea.getText(), statusBox.getValue(), deliveryEmail);
            notifications.setAll(notificationManagementService.findAllWithRecipients());
            return "Notification updated.";
        }));

        Button cancel = new Button("Cancel Selected");
        cancel.setOnAction(event -> runWithMessage(message, () -> {
            Notification selected = table.getSelectionModel().getSelectedItem();
            notificationManagementService.cancelPendingNotification(selected);
            notifications.setAll(notificationManagementService.findAllWithRecipients());
            return "Notification cancelled.";
        }));

        VBox form = new VBox(8,
                new HBox(10, new Label("Target"), targetModeBox),
                recipientBox,
                groupBox,
                targetListLabel,
                emailTargetList,
                new HBox(10, selectAllEmails, clearEmails),
                titleField,
                messageArea,
                new HBox(10, new Label("Channel"), channelBox, new Label("Status"), statusBox),
                new HBox(10, create, saveChanges, cancel),
                message
        );
        VBox box = new VBox(10, new Label("All notifications"), table, new Label("Create / edit selected notification"), form);
        box.setPadding(new Insets(12));
        return box;
    }

    private void refreshNotificationTargets(String channel, String mode, Person recipient, StudentGroup group,
                                            List<Person> allPeople,
                                            ListView<NotificationEmailTarget> emailTargetList) {
        List<NotificationEmailTarget> targets;
        boolean emailChannel = "EMAIL".equals(channel);
        if ("Group".equals(mode)) {
            targets = group == null ? List.of() : notificationTargets(new ArrayList<>(group.getStudents()), emailChannel);
        } else if ("All users".equals(mode)) {
            targets = notificationTargets(allPeople, emailChannel);
        } else {
            targets = recipient == null ? List.of() : notificationTargets(List.of(recipient), emailChannel);
        }
        emailTargetList.setItems(FXCollections.observableArrayList(targets));
        emailTargetList.getSelectionModel().clearSelection();
        if (emailChannel && "Single user".equals(mode) && recipient != null) {
            selectNotificationEmailTarget(emailTargetList, recipient.getPrimaryEmail());
        } else {
            emailTargetList.getSelectionModel().selectAll();
        }
    }

    private List<NotificationEmailTarget> notificationTargets(List<? extends Person> people, boolean emailChannel) {
        List<Person> sortedPeople = new ArrayList<>(people);
        sortedPeople.sort(Comparator.comparing(Person::getSurname).thenComparing(Person::getName));
        List<NotificationEmailTarget> targets = new ArrayList<>();
        for (Person person : sortedPeople) {
            if (!emailChannel) {
                targets.add(new NotificationEmailTarget(person, null));
                continue;
            }
            for (String email : sortedEmails(person)) {
                targets.add(new NotificationEmailTarget(person, email));
            }
        }
        return targets;
    }

    private void selectNotificationEmailTarget(ListView<NotificationEmailTarget> emailTargetList, String email) {
        emailTargetList.getSelectionModel().clearSelection();
        if (email == null || email.isBlank()) {
            return;
        }
        for (int i = 0; i < emailTargetList.getItems().size(); i++) {
            String targetEmail = emailTargetList.getItems().get(i).email();
            if (targetEmail != null && targetEmail.equalsIgnoreCase(email)) {
                emailTargetList.getSelectionModel().select(i);
                return;
            }
        }
    }

    private String selectedDeliveryEmailForSingleNotification(ListView<NotificationEmailTarget> emailTargetList) {
        List<NotificationEmailTarget> selectedTargets = new ArrayList<>(emailTargetList.getSelectionModel().getSelectedItems());
        if (selectedTargets.isEmpty()) {
            throw new IllegalArgumentException("Select one delivery email for the selected notification.");
        }
        if (selectedTargets.size() > 1) {
            throw new IllegalArgumentException("Select only one delivery email when editing one notification.");
        }
        return selectedTargets.get(0).email();
    }

    private Person findPersonOption(List<Person> people, Person selectedRecipient) {
        if (selectedRecipient == null) {
            return null;
        }
        return people.stream()
                .filter(person -> person.getId().equals(selectedRecipient.getId()))
                .findFirst()
                .orElse(selectedRecipient);
    }

    private String notificationRecipientLabel(Notification notification) {
        if (notification == null || notification.getRecipient() == null) {
            return "";
        }
        return personLabelWithAllEmails(notification.getRecipient());
    }

    private String notificationDeliveryEmail(Notification notification) {
        if (notification instanceof EmailNotification emailNotification) {
            return safe(emailNotification.getDeliveryEmail());
        }
        return "";
    }

    private String notificationChannel(Notification notification) {
        return notification instanceof SystemNotification ? "System" : "Email";
    }

    private String notificationChannelTarget(Notification notification) {
        if (notification instanceof EmailNotification emailNotification) {
            return safe(emailNotification.getDeliveryEmail());
        }
        return "In-app";
    }

    private ListCell<Notification> notificationCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Notification notification, boolean empty) {
                super.updateItem(notification, empty);
                if (empty || notification == null) {
                    setText(null);
                    return;
                }
                String recipient = notificationRecipientLabel(notification);
                String deliveryTarget = notificationChannelTarget(notification);
                String target = deliveryTarget.isBlank() ? recipient : recipient + " | " + deliveryTarget;
                setText(notification.getStatus() + " | " + target + " | " + notification.getMessage());
            }
        };
    }

    private record NotificationEmailTarget(Person person, String email) {
        private String label() {
            String target = email == null || email.isBlank() ? "In-app notification" : email;
            return person.getName() + " " + person.getSurname() + " | " + target;
        }
    }

    private ListCell<WeeklyScheduleEntry> weeklyScheduleEntryCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(WeeklyScheduleEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setText(null);
                    return;
                }
                String place = entry.getMeetingMode() == MeetingMode.CLASSROOM
                        ? (entry.getRoom() == null ? "" : "Room " + entry.getRoom().getRoomNumber())
                        : "Link " + safe(entry.getOnlineMeetingLink());
                setText(entry.getDayOfWeek() + " | " + entry.getStartTime() + "-" + entry.getEndTime()
                        + " | " + entry.getGroup().getCode()
                        + " | Semester " + entry.getSemester().getNumber()
                        + " | " + entry.getField().getName()
                        + " | " + entry.getSubject().getName()
                        + " | " + entry.getClassType()
                        + " | " + entry.getTeacher().getName() + " " + entry.getTeacher().getSurname()
                        + " | " + place);
            }
        };
    }

    private Node createMyHistoryView() {
        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Class Meetings", createClassMeetingHistoryView()));
        tabs.getTabs().add(new Tab("Activity Logs", createActivityLogHistoryView()));
        if (authenticationService.isTeacher()) {
            tabs.getTabs().add(new Tab("Room Bookings", createTeacherRoomBookingHistoryView()));
        }
        if (authenticationService.isAdmin()) {
            tabs.getTabs().add(new Tab("Attendance", createAdminAttendanceHistoryView()));
            tabs.getTabs().add(new Tab("Reports", createAdminReportHistoryView()));
            tabs.getTabs().add(new Tab("Room Bookings", createAdminRoomBookingHistoryView()));
            tabs.getTabs().add(new Tab("Notifications", createAdminNotificationHistoryView()));
            tabs.getTabs().add(new Tab("Notification Tasks", createAdminNotificationTaskHistoryView()));
        }
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        return tabs;
    }

    private Node createClassMeetingHistoryView() {
        if (authenticationService.isStudent()) {
            return meetingListPage(classMeetingRepository.findClassMeetingHistoryForStudent(currentUser().getId()), "Student history is derived through Student -> StudentGroup -> ClassMeeting.");
        }
        if (authenticationService.isTeacher()) {
            return meetingListPage(classMeetingRepository.findClassMeetingHistoryForTeacher(currentUser().getId()), "Teacher history is derived through Teacher -> ClassMeeting.");
        }
        return meetingListPage(classMeetingRepository.findAllWithBasicData(), "Administrator can view all class meeting history.");
    }

    private Node createActivityLogHistoryView() {
        ListView<ActivityLog> list = new ListView<>(FXCollections.observableArrayList(
                activityLogRepository.findByActorId(currentUser().getId())
        ));
        list.setCellFactory(view -> activityLogCell());
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> list.setItems(FXCollections.observableArrayList(
                activityLogRepository.findByActorId(currentUser().getId())
        )));
        VBox box = new VBox(10,
                new Label("Actions performed by " + currentUser().getName() + " " + currentUser().getSurname()),
                refresh,
                list
        );
        box.setPadding(new Insets(12));
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    private ListCell<ActivityLog> activityLogCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ActivityLog log, boolean empty) {
                super.updateItem(log, empty);
                if (empty || log == null) {
                    setText(null);
                    return;
                }
                setText(log.getOccurredAt() + " | " + log.getActionType() + " | " + log.getDescription());
            }
        };
    }

    private Node createAdminAttendanceHistoryView() {
        ListView<Attendance> list = new ListView<>(FXCollections.observableArrayList(attendanceRepository.findAllWithStudentAndClassMeeting()));
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Attendance attendance, boolean empty) {
                super.updateItem(attendance, empty);
                if (empty || attendance == null) {
                    setText(null);
                    return;
                }
                ClassMeeting meeting = attendance.getClassMeeting();
                setText(attendance.getRegistrationTime() + " | " + attendance.getStudent().getName() + " "
                        + attendance.getStudent().getSurname() + " | " + meeting.getSubject().getName()
                        + " | " + meeting.getMeetingDate() + " | " + attendance.getStatus());
            }
        });
        VBox box = new VBox(10, new Label("All attendance records"), list);
        box.setPadding(new Insets(12));
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    private Node createAdminReportHistoryView() {
        ListView<AttendanceReport> list = new ListView<>(FXCollections.observableArrayList(attendanceReportRepository.findAllWithDetails()));
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(AttendanceReport report, boolean empty) {
                super.updateItem(report, empty);
                if (empty || report == null) {
                    setText(null);
                    return;
                }
                setText(report.getGeneratedOn() + " | " + report.getReportType()
                        + " | lines " + report.getReportLines().size()
                        + " | overall " + String.format(java.util.Locale.US, "%.2f%%", report.getOverallPerformancePercentage())
                        + (report.getExportedAt() == null ? " | not exported" : " | exported " + report.getExportedAt()));
            }
        });
        VBox box = new VBox(10, new Label("Generated report snapshots"), list);
        box.setPadding(new Insets(12));
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    private Node createAdminRoomBookingHistoryView() {
        ListView<RoomBooking> list = new ListView<>(FXCollections.observableArrayList(roomBookingRepository.findAllWithDetails()));
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(RoomBooking booking, boolean empty) {
                super.updateItem(booking, empty);
                if (empty || booking == null) {
                    setText(null);
                    return;
                }
                String meeting = booking.getClassMeeting() == null ? "manual booking"
                        : booking.getClassMeeting().getSubject().getName() + " | group " + booking.getClassMeeting().getGroup().getCode();
                setText(booking.getBookingStatus() + " | room " + booking.getRoom().getRoomNumber()
                        + " | " + booking.getMeetingSlot().getDate()
                        + " " + booking.getMeetingSlot().getStartTime() + "-" + booking.getMeetingSlot().getEndTime()
                        + " | " + meeting);
            }
        });
        VBox box = new VBox(10, new Label("All room booking history"), list);
        box.setPadding(new Insets(12));
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    private Node createTeacherRoomBookingHistoryView() {
        ListView<RoomBooking> list = new ListView<>(FXCollections.observableArrayList(
                roomBookingRepository.findRoomBookingHistoryForTeacher(currentUser().getId())
        ));
        list.setCellFactory(view -> roomBookingHistoryCell());
        VBox box = new VBox(10, new Label("Room bookings for assigned class meetings"), list);
        box.setPadding(new Insets(12));
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    private ListCell<RoomBooking> roomBookingHistoryCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(RoomBooking booking, boolean empty) {
                super.updateItem(booking, empty);
                if (empty || booking == null) {
                    setText(null);
                    return;
                }
                String meeting = booking.getClassMeeting() == null ? "manual booking"
                        : booking.getClassMeeting().getSubject().getName() + " | group " + booking.getClassMeeting().getGroup().getCode();
                setText(booking.getBookingStatus() + " | room " + booking.getRoom().getRoomNumber()
                        + " | " + booking.getMeetingSlot().getDate()
                        + " " + booking.getMeetingSlot().getStartTime() + "-" + booking.getMeetingSlot().getEndTime()
                        + " | " + meeting);
            }
        };
    }

    private Node createAdminNotificationHistoryView() {
        ListView<Notification> list = new ListView<>(FXCollections.observableArrayList(notificationRepository.findAllWithRecipients()));
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Notification notification, boolean empty) {
                super.updateItem(notification, empty);
                if (empty || notification == null) {
                    setText(null);
                    return;
                }
                String recipient = notificationRecipientLabel(notification);
                String deliveryTarget = notificationChannelTarget(notification);
                String target = deliveryTarget.isBlank() ? recipient : recipient + " | " + deliveryTarget;
                setText(notification.getCreatedAt() + " | " + notification.getStatus() + " | " + target + " | " + notification.getTitle());
            }
        });
        VBox box = new VBox(10, new Label("All notifications"), list);
        box.setPadding(new Insets(12));
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    private Node createAdminNotificationTaskHistoryView() {
        ListView<ScheduledNotificationTask> list = new ListView<>(FXCollections.observableArrayList(scheduledNotificationTaskRepository.findAllWithDetails()));
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(ScheduledNotificationTask task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setText(null);
                    return;
                }
                String recipient = task.getRecipient() == null ? "no recipient"
                        : task.getRecipient().getName() + " " + task.getRecipient().getSurname();
                setText(task.getScheduledAt() + " | " + task.getTaskType() + " | " + task.getStatus()
                        + " | " + recipient + " | retries " + task.getRetryCount()
                        + (task.getFailureReason() == null ? "" : " | " + task.getFailureReason()));
            }
        });
        VBox box = new VBox(10, new Label("All scheduled notification tasks"), list);
        box.setPadding(new Insets(12));
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    private Node createStudentAttendanceView() {
        ListView<Attendance> list = new ListView<>(FXCollections.observableArrayList(attendanceRepository.findAttendanceHistoryForStudent(currentUser().getId())));
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Attendance attendance, boolean empty) {
                super.updateItem(attendance, empty);
                if (empty || attendance == null) {
                    setText(null);
                } else {
                    ClassMeeting meeting = attendance.getClassMeeting();
                    setText(meeting.getMeetingDate() + " | " + meeting.getSubject().getName() + " | " + attendance.getStatus()
                            + " | " + (attendance.getComment() == null ? "" : attendance.getComment()));
                }
            }
        });
        VBox box = new VBox(10, new Label("Personal attendance history"), list);
        box.setPadding(new Insets(12));
        return box;
    }

    private Node createMyGroupView() {
        StudentGroup group = new StudentGroupRepository().findDetailsForStudent(currentUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Student group not found."));
        ListView<Student> students = new ListView<>(FXCollections.observableArrayList(group.getStudents().stream()
                .sorted(Comparator.comparing(Student::getSurname).thenComparing(Student::getName))
                .toList()));
        students.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Student student, boolean empty) {
                super.updateItem(student, empty);
                setText(empty || student == null ? null : student.getStudentNumber() + " | " + student.getName() + " " + student.getSurname());
            }
        });
        ListView<WeeklyScheduleEntry> weeklyEntries = new ListView<>(FXCollections.observableArrayList(
                weeklyScheduleEntryRepository.findByGroupIdWithDetails(group.getId())
        ));
        weeklyEntries.setCellFactory(view -> weeklyScheduleEntryCell());
        VBox box = new VBox(10,
                new Label("Group: " + group.getCode()),
                new Label("Semester: " + group.getSemester().getNumber()),
                new Label("Field of study: " + group.getField().getName()),
                new Label("Students in my group"),
                students,
                new Label("Group weekly schedule"),
                weeklyEntries
        );
        box.setPadding(new Insets(12));
        return box;
    }

    private Node createUsersView() {
        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Users", createUserManagementPane()));
        tabs.getTabs().add(new Tab("Academic Structure", createAcademicStructurePane()));
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        return tabs;
    }

    private void refreshAdminUserEmailView(Person person, ObservableList<String> selectedUserEmails,
                                           Label selectedPrimaryEmailLabel) {
        if (person == null) {
            selectedUserEmails.clear();
            selectedPrimaryEmailLabel.setText("Primary email: ");
            return;
        }
        selectedUserEmails.setAll(sortedEmails(person));
        selectedPrimaryEmailLabel.setText("Primary email: " + person.getPrimaryEmail());
    }

    private Node createUserManagementPane() {
        ObservableList<Person> userItems = FXCollections.observableArrayList(new PersonRepository().findAllWithEmails());
        ObservableList<StudentGroup> groupItems = FXCollections.observableArrayList(new GenericRepository<>(StudentGroup.class).findAll());
        ObservableList<Subject> subjectItems = FXCollections.observableArrayList(new GenericRepository<>(Subject.class).findAll());

        ListView<Person> users = new ListView<>(userItems);
        users.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Person person, boolean empty) {
                super.updateItem(person, empty);
                setText(empty || person == null ? null : roleName(person) + " | " + personLabelWithAllEmails(person));
            }
        });

        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("Student", "Teacher", "Administrator"));
        roleBox.getSelectionModel().select("Student");
        TextField nameField = new TextField();
        nameField.setPromptText("first name");
        TextField surnameField = new TextField();
        surnameField.setPromptText("last name");
        DatePicker birthDatePicker = new DatePicker(LocalDate.of(2000, 1, 1));
        TextField emailField = new TextField();
        emailField.setPromptText("email for create/add/edit");
        ObservableList<String> selectedUserEmails = FXCollections.observableArrayList();
        ListView<String> selectedUserEmailList = new ListView<>(selectedUserEmails);
        selectedUserEmailList.setPrefHeight(96);
        selectedUserEmailList.getSelectionModel().selectedItemProperty().addListener((observable, oldEmail, selectedEmail) -> {
            if (selectedEmail != null) {
                emailField.setText(selectedEmail);
            }
        });
        Label selectedPrimaryEmailLabel = new Label("Primary email: ");
        TextField passwordField = new TextField("Password123!");
        TextField numberField = new TextField();
        numberField.setPromptText("student number / employee number");
        ComboBox<StudentGroup> groupBox = new ComboBox<>(groupItems);
        groupBox.setConverter(converter(StudentGroup::getCode));
        ListView<Subject> qualifiedSubjects = new ListView<>(subjectItems);
        qualifiedSubjects.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        qualifiedSubjects.setPrefHeight(130);
        qualifiedSubjects.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Subject subject, boolean empty) {
                super.updateItem(subject, empty);
                setText(empty || subject == null ? null : subject.getName());
            }
        });

        Label message = new Label();

        Runnable refreshUsers = () -> userItems.setAll(new PersonRepository().findAllWithEmails());
        Runnable refreshSelectedUser = () -> {
            Person selected = users.getSelectionModel().getSelectedItem();
            Long selectedId = selected == null ? null : selected.getId();
            refreshUsers.run();
            if (selectedId == null) {
                selectedUserEmails.clear();
                selectedPrimaryEmailLabel.setText("Primary email: ");
                return;
            }
            userItems.stream()
                    .filter(user -> user.getId().equals(selectedId))
                    .findFirst()
                    .ifPresent(refreshed -> {
                        users.getSelectionModel().select(refreshed);
                        refreshAdminUserEmailView(refreshed, selectedUserEmails, selectedPrimaryEmailLabel);
                    });
        };
        Runnable refreshReferenceLists = () -> {
            groupItems.setAll(new GenericRepository<>(StudentGroup.class).findAll());
            subjectItems.setAll(new GenericRepository<>(Subject.class).findAll());
        };

        users.getSelectionModel().selectedItemProperty().addListener((observable, oldUser, selected) -> {
            if (selected == null) {
                return;
            }
            roleBox.setValue(roleName(selected));
            nameField.setText(selected.getName());
            surnameField.setText(selected.getSurname());
            birthDatePicker.setValue(selected.getBirthDate() == null ? LocalDate.of(2000, 1, 1) : selected.getBirthDate().getValue());
            emailField.setText(selected.getPrimaryEmail());
            refreshAdminUserEmailView(selected, selectedUserEmails, selectedPrimaryEmailLabel);
            passwordField.clear();
            if (selected instanceof Student student) {
                numberField.setText(student.getStudentNumber());
                groupBox.setValue(student.getGroup());
            } else if (selected instanceof Teacher teacher) {
                numberField.setText(teacher.getEmployeeNumber());
                qualifiedSubjects.getSelectionModel().clearSelection();
                teacher.getQualifiedSubjects().forEach(subject -> qualifiedSubjects.getSelectionModel().select(subject));
            } else if (selected instanceof Administrator administrator) {
                numberField.setText(administrator.getEmployeeNumber());
            }
        });

        Button create = new Button("Create User");
        create.setOnAction(event -> runWithMessage(message, () -> {
            String role = roleBox.getValue();
            if ("Student".equals(role)) {
                StudentGroup group = groupBox.getValue();
                adminManagementService.createStudent(nameField.getText(), surnameField.getText(), birthDatePicker.getValue(),
                        emailField.getText(), passwordField.getText(), numberField.getText(), group == null ? null : group.getId());
            } else if ("Teacher".equals(role)) {
                Set<Long> subjectIds = qualifiedSubjects.getSelectionModel().getSelectedItems().stream()
                        .map(Subject::getId)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
                adminManagementService.createTeacher(nameField.getText(), surnameField.getText(), birthDatePicker.getValue(),
                        emailField.getText(), passwordField.getText(), numberField.getText(), subjectIds);
            } else {
                adminManagementService.createAdministrator(nameField.getText(), surnameField.getText(), birthDatePicker.getValue(),
                        emailField.getText(), passwordField.getText(), numberField.getText());
            }
            refreshUsers.run();
            return role + " created.";
        }));

        Button saveBasics = new Button("Save Basic Changes");
        saveBasics.setOnAction(event -> runWithMessage(message, () -> {
            Person selected = users.getSelectionModel().getSelectedItem();
            if (selected == null) {
                throw new IllegalArgumentException("Select a user first.");
            }
            adminManagementService.updatePersonBasics(selected.getId(), nameField.getText(), surnameField.getText(), passwordField.getText());
            refreshSelectedUser.run();
            return "User basic data updated.";
        }));

        Button addEmail = new Button("Add Email");
        addEmail.setOnAction(event -> runWithMessage(message, () -> {
            Person selected = users.getSelectionModel().getSelectedItem();
            if (selected == null) {
                throw new IllegalArgumentException("Select a user first.");
            }
            personalSettingsService.addEmail(selected.getId(), emailField.getText());
            refreshSelectedUser.run();
            return "Email added.";
        }));

        Button editEmail = new Button("Edit Selected Email");
        editEmail.setOnAction(event -> runWithMessage(message, () -> {
            Person selected = users.getSelectionModel().getSelectedItem();
            String oldEmail = selectedUserEmailList.getSelectionModel().getSelectedItem();
            if (selected == null || oldEmail == null) {
                throw new IllegalArgumentException("Select a user and an email first.");
            }
            personalSettingsService.editEmail(selected.getId(), oldEmail, emailField.getText());
            refreshSelectedUser.run();
            return "Email updated.";
        }));

        Button deleteEmail = new Button("Delete Selected Email");
        deleteEmail.setOnAction(event -> runWithMessage(message, () -> {
            Person selected = users.getSelectionModel().getSelectedItem();
            String email = selectedUserEmailList.getSelectionModel().getSelectedItem();
            if (selected == null || email == null) {
                throw new IllegalArgumentException("Select a user and an email first.");
            }
            personalSettingsService.deleteEmail(selected.getId(), email);
            refreshSelectedUser.run();
            return "Email deleted.";
        }));

        Button makePrimaryEmail = new Button("Make Primary");
        makePrimaryEmail.setOnAction(event -> runWithMessage(message, () -> {
            Person selected = users.getSelectionModel().getSelectedItem();
            String email = selectedUserEmailList.getSelectionModel().getSelectedItem();
            if (selected == null || email == null) {
                throw new IllegalArgumentException("Select a user and an email first.");
            }
            personalSettingsService.setPrimaryEmail(selected.getId(), email);
            refreshSelectedUser.run();
            return "Primary email changed.";
        }));

        Button delete = new Button("Delete User");
        delete.setOnAction(event -> runWithMessage(message, () -> {
            Person selected = users.getSelectionModel().getSelectedItem();
            if (selected == null) {
                throw new IllegalArgumentException("Select a user first.");
            }
            adminManagementService.deletePerson(selected.getId(), currentUser().getId());
            refreshUsers.run();
            return "User deleted.";
        }));

        Button refresh = new Button("Refresh Lists");
        refresh.setOnAction(event -> {
            refreshUsers.run();
            refreshReferenceLists.run();
            message.setText("Lists refreshed.");
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.add(new Label("Role"), 0, 0);
        form.add(roleBox, 1, 0);
        form.add(new Label("First name"), 0, 1);
        form.add(nameField, 1, 1);
        form.add(new Label("Last name"), 0, 2);
        form.add(surnameField, 1, 2);
        form.add(new Label("Birth date"), 0, 3);
        form.add(birthDatePicker, 1, 3);
        form.add(new Label("Email"), 0, 4);
        form.add(emailField, 1, 4);
        form.add(new Label("Password"), 0, 5);
        form.add(passwordField, 1, 5);
        form.add(new Label("Number"), 0, 6);
        form.add(numberField, 1, 6);
        form.add(new Label("Student group"), 0, 7);
        form.add(groupBox, 1, 7);
        form.add(new Label("Teacher qualified subjects"), 0, 8);
        form.add(qualifiedSubjects, 1, 8);

        VBox right = new VBox(10,
                new Label("Create users, create administrator accounts, edit basic personal data, and manage all user emails."),
                form,
                selectedPrimaryEmailLabel,
                new Label("All emails for selected user"),
                selectedUserEmailList,
                new HBox(10, addEmail, editEmail, deleteEmail, makePrimaryEmail),
                new HBox(10, create, saveBasics, delete, refresh),
                message
        );
        right.setPadding(new Insets(12));

        BorderPane box = new BorderPane();
        box.setLeft(users);
        box.setCenter(right);
        BorderPane.setMargin(users, new Insets(12));
        users.setPrefWidth(360);
        box.setPadding(new Insets(12));
        return box;
    }

    private Node createAcademicStructurePane() {
        ObservableList<Field> fields = FXCollections.observableArrayList(new GenericRepository<>(Field.class).findAll());
        ObservableList<Semester> semesters = FXCollections.observableArrayList(new SemesterRepository().findAllWithFields());
        ObservableList<StudentGroup> groups = FXCollections.observableArrayList(new StudentGroupRepository().findAllWithAcademicContext());
        ObservableList<Subject> subjects = FXCollections.observableArrayList(new GenericRepository<>(Subject.class).findAll());

        ListView<Field> fieldList = new ListView<>(fields);
        fieldList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Field field, boolean empty) {
                super.updateItem(field, empty);
                setText(empty || field == null ? null : field.getName());
            }
        });
        TextField fieldName = new TextField();
        fieldName.setPromptText("field of study name");

        ListView<Semester> semesterList = new ListView<>(semesters);
        semesterList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Semester semester, boolean empty) {
                super.updateItem(semester, empty);
                setText(empty || semester == null ? null : semesterAcademicLabel(semester));
            }
        });
        TextField semesterNumber = new TextField();
        semesterNumber.setPromptText("number");
        DatePicker semesterStart = new DatePicker(LocalDate.now().minusWeeks(4));
        DatePicker semesterEnd = new DatePicker(LocalDate.now().plusWeeks(12));
        ListView<Field> semesterFields = new ListView<>(fields);
        semesterFields.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        semesterFields.setPrefHeight(110);
        semesterFields.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Field field, boolean empty) {
                super.updateItem(field, empty);
                setText(empty || field == null ? null : field.getName());
            }
        });

        ListView<StudentGroup> groupList = new ListView<>(groups);
        groupList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(StudentGroup group, boolean empty) {
                super.updateItem(group, empty);
                setText(empty || group == null ? null : group.getCode()
                        + " | Semester " + group.getSemester().getNumber()
                        + " | " + group.getField().getName()
                        + " | max " + group.getMaxSize());
            }
        });
        TextField groupCode = new TextField();
        groupCode.setPromptText("group code");
        TextField groupMaxSize = new TextField("30");
        groupMaxSize.setPromptText("max size");
        ComboBox<Semester> groupSemester = new ComboBox<>(semesters);
        groupSemester.setConverter(converter(this::semesterAcademicLabel));
        ComboBox<Field> groupField = new ComboBox<>();
        groupField.setConverter(converter(Field::getName));

        ListView<Subject> subjectList = new ListView<>(subjects);
        subjectList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Subject subject, boolean empty) {
                super.updateItem(subject, empty);
                setText(empty || subject == null ? null : subject.getName());
            }
        });
        TextField subjectName = new TextField();
        subjectName.setPromptText("subject name");
        ComboBox<Semester> subjectSemester = new ComboBox<>(semesters);
        subjectSemester.setConverter(converter(this::semesterAcademicLabel));
        ComboBox<Field> subjectField = new ComboBox<>();
        subjectField.setConverter(converter(Field::getName));

        Label message = new Label();
        Runnable refresh = () -> {
            fields.setAll(new GenericRepository<>(Field.class).findAll());
            semesters.setAll(new SemesterRepository().findAllWithFields());
            groups.setAll(new StudentGroupRepository().findAllWithAcademicContext());
            subjects.setAll(new GenericRepository<>(Subject.class).findAll());
        };

        groupSemester.valueProperty().addListener((observable, oldValue, selected) -> {
            groupField.getItems().setAll(selected == null ? Set.of() : selected.getFields());
            if (selected == null || groupField.getValue() == null
                    || selected.getFields().stream().noneMatch(field -> field.getId().equals(groupField.getValue().getId()))) {
                groupField.setValue(null);
            }
        });
        subjectSemester.valueProperty().addListener((observable, oldValue, selected) -> {
            subjectField.getItems().setAll(selected == null ? Set.of() : selected.getFields());
            if (selected == null || subjectField.getValue() == null
                    || selected.getFields().stream().noneMatch(field -> field.getId().equals(subjectField.getValue().getId()))) {
                subjectField.setValue(null);
            }
        });

        fieldList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected != null) {
                fieldName.setText(selected.getName());
            }
        });
        semesterList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected != null) {
                semesterNumber.setText(String.valueOf(selected.getNumber()));
                semesterStart.setValue(selected.getStartDate());
                semesterEnd.setValue(selected.getEndDate());
                semesterFields.getSelectionModel().clearSelection();
                Set<Long> selectedFieldIds = selected.getFields().stream().map(Field::getId).collect(java.util.stream.Collectors.toSet());
                fields.stream()
                        .filter(field -> selectedFieldIds.contains(field.getId()))
                        .forEach(field -> semesterFields.getSelectionModel().select(field));
            }
        });
        groupList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected != null) {
                groupCode.setText(selected.getCode());
                groupMaxSize.setText(String.valueOf(selected.getMaxSize()));
                groupSemester.setValue(selected.getSemester());
                groupField.getItems().setAll(selected.getSemester().getFields());
                groupField.setValue(selected.getField());
            }
        });
        subjectList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected != null) {
                subjectName.setText(selected.getName());
            }
        });

        Button createField = new Button("Create Field");
        createField.setOnAction(event -> runWithMessage(message, () -> {
            adminManagementService.createField(fieldName.getText());
            refresh.run();
            return "Field created.";
        }));
        Button updateField = new Button("Save Field");
        updateField.setOnAction(event -> runWithMessage(message, () -> {
            Field selected = fieldList.getSelectionModel().getSelectedItem();
            adminManagementService.updateField(selected == null ? null : selected.getId(), fieldName.getText());
            refresh.run();
            return "Field updated.";
        }));
        Button deleteField = new Button("Delete Field");
        deleteField.setOnAction(event -> runWithMessage(message, () -> {
            Field selected = fieldList.getSelectionModel().getSelectedItem();
            adminManagementService.deleteField(selected == null ? null : selected.getId());
            refresh.run();
            return "Field deleted.";
        }));

        Button createSemester = new Button("Create Semester");
        createSemester.setOnAction(event -> runWithMessage(message, () -> {
            Set<Long> fieldIds = semesterFields.getSelectionModel().getSelectedItems().stream()
                    .map(Field::getId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            adminManagementService.createSemester(Integer.parseInt(semesterNumber.getText()), semesterStart.getValue(), semesterEnd.getValue(), fieldIds);
            refresh.run();
            return "Semester created.";
        }));
        Button updateSemester = new Button("Save Semester");
        updateSemester.setOnAction(event -> runWithMessage(message, () -> {
            Semester selected = semesterList.getSelectionModel().getSelectedItem();
            Set<Long> fieldIds = semesterFields.getSelectionModel().getSelectedItems().stream()
                    .map(Field::getId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            adminManagementService.updateSemester(selected == null ? null : selected.getId(), Integer.parseInt(semesterNumber.getText()),
                    semesterStart.getValue(), semesterEnd.getValue(), fieldIds);
            refresh.run();
            return "Semester updated.";
        }));
        Button deleteSemester = new Button("Delete Semester");
        deleteSemester.setOnAction(event -> runWithMessage(message, () -> {
            Semester selected = semesterList.getSelectionModel().getSelectedItem();
            adminManagementService.deleteSemester(selected == null ? null : selected.getId());
            refresh.run();
            return "Semester deleted.";
        }));

        Button createGroup = new Button("Create Group");
        createGroup.setOnAction(event -> runWithMessage(message, () -> {
            Semester semester = groupSemester.getValue();
            Field field = groupField.getValue();
            adminManagementService.createGroup(groupCode.getText(), semester == null ? null : semester.getId(),
                    field == null ? null : field.getId(), Integer.parseInt(groupMaxSize.getText()));
            refresh.run();
            return "Group created.";
        }));
        Button updateGroup = new Button("Save Group");
        updateGroup.setOnAction(event -> runWithMessage(message, () -> {
            StudentGroup selected = groupList.getSelectionModel().getSelectedItem();
            Semester semester = groupSemester.getValue();
            Field field = groupField.getValue();
            adminManagementService.updateGroup(selected == null ? null : selected.getId(), groupCode.getText(),
                    semester == null ? null : semester.getId(), field == null ? null : field.getId(),
                    Integer.parseInt(groupMaxSize.getText()));
            refresh.run();
            return "Group updated.";
        }));
        Button deleteGroup = new Button("Delete Group");
        deleteGroup.setOnAction(event -> runWithMessage(message, () -> {
            StudentGroup selected = groupList.getSelectionModel().getSelectedItem();
            adminManagementService.deleteGroup(selected == null ? null : selected.getId());
            refresh.run();
            return "Group deleted.";
        }));

        Button createSubject = new Button("Create Subject");
        createSubject.setOnAction(event -> runWithMessage(message, () -> {
            adminManagementService.createSubject(subjectName.getText());
            refresh.run();
            return "Subject created.";
        }));
        Button updateSubject = new Button("Save Subject");
        updateSubject.setOnAction(event -> runWithMessage(message, () -> {
            Subject selected = subjectList.getSelectionModel().getSelectedItem();
            adminManagementService.updateSubject(selected == null ? null : selected.getId(), subjectName.getText());
            refresh.run();
            return "Subject updated.";
        }));
        Button deleteSubject = new Button("Delete Subject");
        deleteSubject.setOnAction(event -> runWithMessage(message, () -> {
            Subject selected = subjectList.getSelectionModel().getSelectedItem();
            adminManagementService.deleteSubject(selected == null ? null : selected.getId());
            refresh.run();
            return "Subject deleted.";
        }));
        Button assignSubjectSemester = new Button("Assign To Curriculum");
        assignSubjectSemester.setOnAction(event -> runWithMessage(message, () -> {
            Subject subject = subjectList.getSelectionModel().getSelectedItem();
            Semester semester = subjectSemester.getValue();
            Field field = subjectField.getValue();
            adminManagementService.assignSubjectToSemesterField(subject == null ? null : subject.getId(),
                    semester == null ? null : semester.getId(), field == null ? null : field.getId());
            refresh.run();
            return "Subject assigned to semester and field curriculum.";
        }));
        Button removeSubjectSemester = new Button("Remove From Curriculum");
        removeSubjectSemester.setOnAction(event -> runWithMessage(message, () -> {
            Subject subject = subjectList.getSelectionModel().getSelectedItem();
            Semester semester = subjectSemester.getValue();
            Field field = subjectField.getValue();
            adminManagementService.removeSubjectFromSemesterField(subject == null ? null : subject.getId(),
                    semester == null ? null : semester.getId(), field == null ? null : field.getId());
            refresh.run();
            return "Subject removed from semester and field curriculum.";
        }));

        VBox fieldBox = new VBox(8, new Label("Fields of study"), fieldList, fieldName, new HBox(8, createField, updateField, deleteField));
        VBox semesterBox = new VBox(8, new Label("Semesters"), semesterList, semesterNumber,
                new HBox(8, new Label("From"), semesterStart, new Label("To"), semesterEnd),
                new Label("Fields in this semester"), semesterFields,
                new HBox(8, createSemester, updateSemester, deleteSemester));
        VBox groupBox = new VBox(8, new Label("Groups"), groupList, groupCode, groupMaxSize,
                groupSemester, groupField, new HBox(8, createGroup, updateGroup, deleteGroup));
        VBox subjectBox = new VBox(8, new Label("Subjects"), subjectList, subjectName,
                new HBox(8, createSubject, updateSubject, deleteSubject),
                new Label("Subject curriculum"), subjectSemester, subjectField,
                new HBox(8, assignSubjectSemester, removeSubjectSemester));

        HBox columns = new HBox(12, fieldBox, semesterBox, groupBox, subjectBox);
        columns.setPadding(new Insets(12));
        VBox box = new VBox(10, columns, message);
        ScrollPane scrollPane = new ScrollPane(box);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private Node createRoomsView() {
        ObservableList<Room> roomItems = FXCollections.observableArrayList(roomService.findAllWithBookings());
        ListView<Room> rooms = new ListView<>(roomItems);
        rooms.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                setText(empty || room == null ? null : room.getRoomNumber() + " | capacity " + room.getCapacity());
            }
        });

        TableView<RoomBooking> bookings = new TableView<>();
        TableColumn<RoomBooking, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getMeetingSlot().getDate())));
        TableColumn<RoomBooking, String> startColumn = new TableColumn<>("Start");
        startColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getMeetingSlot().getStartTime())));
        TableColumn<RoomBooking, String> endColumn = new TableColumn<>("End");
        endColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getMeetingSlot().getEndTime())));
        TableColumn<RoomBooking, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getBookingStatus())));
        TableColumn<RoomBooking, String> subjectColumn = new TableColumn<>("Subject");
        subjectColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getClassMeeting() == null ? "" : data.getValue().getClassMeeting().getSubject().getName()
        ));
        TableColumn<RoomBooking, String> groupColumn = new TableColumn<>("Group");
        groupColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getClassMeeting() == null ? "" : data.getValue().getClassMeeting().getGroup().getCode()
        ));
        bookings.getColumns().add(dateColumn);
        bookings.getColumns().add(startColumn);
        bookings.getColumns().add(endColumn);
        bookings.getColumns().add(statusColumn);
        bookings.getColumns().add(subjectColumn);
        bookings.getColumns().add(groupColumn);

        rooms.getSelectionModel().selectedItemProperty().addListener((observable, oldRoom, selectedRoom) -> {
            if (selectedRoom == null) {
                bookings.setItems(FXCollections.observableArrayList());
            } else {
                bookings.setItems(FXCollections.observableArrayList(selectedRoom.getBookingsBySlot().values().stream()
                        .sorted(Comparator.comparing((RoomBooking rb) -> rb.getMeetingSlot().getDate())
                                .thenComparing(rb -> rb.getMeetingSlot().getStartTime()))
                        .toList()));
            }
        });
        if (!roomItems.isEmpty()) {
            rooms.getSelectionModel().selectFirst();
        }

        TextField roomNumberField = new TextField();
        roomNumberField.setPromptText("room number");
        TextField capacityField = new TextField();
        capacityField.setPromptText("capacity");
        Label message = new Label();
        Button createRoom = new Button("Create Room");
        createRoom.setOnAction(event -> runWithMessage(message, () -> {
            roomService.createRoom(roomNumberField.getText(), Integer.parseInt(capacityField.getText()));
            roomItems.setAll(roomService.findAllWithBookings());
            return "Room created.";
        }));
        Button editRoom = new Button("Save Room Changes");
        editRoom.setOnAction(event -> runWithMessage(message, () -> {
            Room room = rooms.getSelectionModel().getSelectedItem();
            roomService.updateRoom(room, roomNumberField.getText(), Integer.parseInt(capacityField.getText()));
            roomItems.setAll(roomService.findAllWithBookings());
            return "Room updated.";
        }));
        rooms.getSelectionModel().selectedItemProperty().addListener((observable, oldRoom, selectedRoom) -> {
            if (selectedRoom != null) {
                roomNumberField.setText(selectedRoom.getRoomNumber());
                capacityField.setText(String.valueOf(selectedRoom.getCapacity()));
            }
        });

        DatePicker bookingDate = new DatePicker(LocalDate.now());
        TextField bookingStart = new TextField("10:00");
        TextField bookingEnd = new TextField("11:30");
        Button checkAvailability = new Button("Check Availability");
        checkAvailability.setOnAction(event -> runWithMessage(message, () -> {
            Room room = rooms.getSelectionModel().getSelectedItem();
            if (room == null) {
                throw new IllegalArgumentException("Select a room first.");
            }
            MeetingSlot slot = new MeetingSlot(bookingDate.getValue(), LocalTime.parse(bookingStart.getText()), LocalTime.parse(bookingEnd.getText()));
            return roomBookingService.isAvailable(room.getId(), slot) ? "Room is available." : "Room is already booked for this time slot.";
        }));
        Button createBooking = new Button("Create Booking");
        createBooking.setOnAction(event -> runWithMessage(message, () -> {
            Room room = rooms.getSelectionModel().getSelectedItem();
            if (room == null) {
                throw new IllegalArgumentException("Select a room first.");
            }
            MeetingSlot slot = new MeetingSlot(bookingDate.getValue(), LocalTime.parse(bookingStart.getText()), LocalTime.parse(bookingEnd.getText()));
            roomBookingService.createBooking(room.getId(), slot);
            roomItems.setAll(roomService.findAllWithBookings());
            return "Room booking created.";
        }));
        Button cancelBooking = new Button("Cancel Booking");
        cancelBooking.setOnAction(event -> runWithMessage(message, () -> {
            RoomBooking booking = bookings.getSelectionModel().getSelectedItem();
            if (booking == null) {
                throw new IllegalArgumentException("Select a booking first.");
            }
            roomBookingService.cancelBooking(booking.getId());
            roomItems.setAll(roomService.findAllWithBookings());
            return "Room booking cancelled.";
        }));

        VBox roomForm = new VBox(8, new Label("Room"), roomNumberField, capacityField, new HBox(10, createRoom, editRoom));
        VBox bookingForm = new VBox(8,
                new Label("Booking for selected room"),
                bookingDate,
                new HBox(10, new Label("Start"), bookingStart, new Label("End"), bookingEnd),
                new HBox(10, checkAvailability, createBooking, cancelBooking),
                message
        );
        VBox right = new VBox(10, new Label("Bookings"), bookings, roomForm, bookingForm);
        BorderPane root = new BorderPane();
        root.setLeft(rooms);
        root.setCenter(right);
        BorderPane.setMargin(rooms, new Insets(12));
        BorderPane.setMargin(right, new Insets(12));
        rooms.setPrefWidth(260);
        return root;
    }

    private Node createClassMeetingForm(boolean adminMode) {
        Label message = new Label();
        ComboBox<Teacher> teacherBox = new ComboBox<>();
        ComboBox<Subject> subjectBox = new ComboBox<>();
        ComboBox<StudentGroup> groupBox = new ComboBox<>(FXCollections.observableArrayList(
                new StudentGroupRepository().findAllWithAcademicContext()
        ));
        ComboBox<Room> roomBox = new ComboBox<>(FXCollections.observableArrayList(roomService.findAll()));
        ComboBox<ClassType> classTypeBox = new ComboBox<>(FXCollections.observableArrayList(ClassType.values()));
        ComboBox<MeetingMode> modeBox = new ComboBox<>(FXCollections.observableArrayList(MeetingMode.values()));
        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        TextField startField = new TextField("10:00");
        TextField endField = new TextField("11:30");
        TextField locationField = new TextField();
        TextField linkField = new TextField();
        ComboBox<ClassMeetingStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(
                ClassMeetingStatus.DRAFT,
                ClassMeetingStatus.SCHEDULED
        ));
        statusBox.getSelectionModel().select(adminMode ? ClassMeetingStatus.DRAFT : ClassMeetingStatus.SCHEDULED);

        teacherBox.setConverter(converter(teacher -> teacher.getName() + " " + teacher.getSurname()));
        subjectBox.setConverter(converter(Subject::getName));
        groupBox.setConverter(converter(StudentGroup::getCode));
        roomBox.setConverter(converter(Room::getRoomNumber));

        if (adminMode) {
            teacherBox.setItems(FXCollections.observableArrayList(new TeacherRepository().findAllWithQualifiedSubjects()));
            subjectBox.setItems(FXCollections.observableArrayList(new GenericRepository<>(Subject.class).findAll()));
        } else {
            Teacher teacher = new TeacherRepository().findByIdWithQualifiedSubjects(currentUser().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Teacher not found."));
            teacherBox.setItems(FXCollections.observableArrayList(teacher));
            teacherBox.getSelectionModel().select(teacher);
            subjectBox.setItems(FXCollections.observableArrayList(teacher.getQualifiedSubjects()));
        }

        Button create = new Button("Save Class Meeting");
        create.setOnAction(event -> runWithMessage(message, () -> {
            Teacher teacher = teacherBox.getValue();
            Subject subject = subjectBox.getValue();
            StudentGroup group = groupBox.getValue();
            if (teacher == null || subject == null || group == null || classTypeBox.getValue() == null || modeBox.getValue() == null) {
                throw new IllegalArgumentException("Subject, teacher, group, class type, and meeting mode are required.");
            }
            LocalTime start = LocalTime.parse(startField.getText());
            LocalTime end = LocalTime.parse(endField.getText());
            String location = modeBox.getValue() == MeetingMode.CLASSROOM
                    ? (roomBox.getValue() == null ? locationField.getText() : roomBox.getValue().getRoomNumber())
                    : locationField.getText();
            ClassMeeting meeting = new ClassMeeting(
                    datePicker.getValue(),
                    location,
                    linkField.getText(),
                    new MeetingTime(datePicker.getValue().getDayOfWeek(), start, end),
                    classTypeBox.getValue(),
                    modeBox.getValue(),
                    subject,
                    teacher,
                    group
            );
            meeting.setStatus(statusBox.getValue());
            Long roomId = roomBox.getValue() == null ? null : roomBox.getValue().getId();
            MeetingSlot slot = roomId == null ? null : new MeetingSlot(datePicker.getValue(), start, end);
            classMeetingService.createClassMeeting(meeting, roomId, slot);
            return "Class meeting saved.";
        }));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label("Teacher"), 0, 0);
        form.add(teacherBox, 1, 0);
        form.add(new Label("Subject"), 0, 1);
        form.add(subjectBox, 1, 1);
        form.add(new Label("Group"), 0, 2);
        form.add(groupBox, 1, 2);
        form.add(new Label("Class type"), 0, 3);
        form.add(classTypeBox, 1, 3);
        form.add(new Label("Meeting mode"), 0, 4);
        form.add(modeBox, 1, 4);
        form.add(new Label("Date"), 0, 5);
        form.add(datePicker, 1, 5);
        form.add(new Label("Start"), 0, 6);
        form.add(startField, 1, 6);
        form.add(new Label("End"), 0, 7);
        form.add(endField, 1, 7);
        form.add(new Label("Room"), 0, 8);
        form.add(roomBox, 1, 8);
        form.add(new Label("Location"), 0, 9);
        form.add(locationField, 1, 9);
        form.add(new Label("Online link"), 0, 10);
        form.add(linkField, 1, 10);
        form.add(new Label("Status"), 0, 11);
        form.add(statusBox, 1, 11);
        form.add(create, 1, 12);
        form.add(message, 1, 13);
        return padded(form);
    }

    private Node createAdminWeeklyScheduleView() {
        return createWeeklyScheduleView(true);
    }

    private Node createWeeklyScheduleView(boolean adminMode) {
        ObservableList<WeeklyScheduleEntry> entries = FXCollections.observableArrayList();
        ListView<WeeklyScheduleEntry> entriesList = new ListView<>(entries);
        entriesList.setCellFactory(view -> weeklyScheduleEntryCell());

        ComboBox<StudentGroup> groupFilter = new ComboBox<>(FXCollections.observableArrayList(
                new StudentGroupRepository().findAllWithAcademicContext()
        ));
        ComboBox<Teacher> teacherFilter = new ComboBox<>(FXCollections.observableArrayList(new TeacherRepository().findAllWithQualifiedSubjects()));
        ComboBox<ClassType> classTypeFilter = new ComboBox<>(FXCollections.observableArrayList(ClassType.values()));
        ComboBox<MeetingMode> modeFilter = new ComboBox<>(FXCollections.observableArrayList(MeetingMode.values()));
        groupFilter.setPromptText("All groups");
        teacherFilter.setPromptText("All teachers");
        classTypeFilter.setPromptText("All types");
        modeFilter.setPromptText("All modes");
        Label message = new Label();
        Label generatedCount = new Label("Generated meetings count: 0");

        groupFilter.setConverter(converter(group -> group.getCode() + " | Semester " + group.getSemester().getNumber()
                + " | " + group.getField().getName()));
        teacherFilter.setConverter(converter(teacher -> teacher.getName() + " " + teacher.getSurname()));

        Runnable refreshEntries = () -> entries.setAll(weeklyScheduleEntryRepository.findWithFilters(
                groupFilter.getValue() == null ? null : groupFilter.getValue().getId(),
                teacherFilter.getValue() == null ? (adminMode ? null : currentUser().getId()) : teacherFilter.getValue().getId(),
                classTypeFilter.getValue(),
                modeFilter.getValue()
        ));
        if (!adminMode) {
            teacherFilter.setValue((Teacher) currentUser());
            teacherFilter.setDisable(true);
        }
        refreshEntries.run();

        Button applyFilters = new Button("Apply Filters");
        applyFilters.setOnAction(event -> refreshEntries.run());
        Button clearFilters = new Button("Clear Filters");
        clearFilters.setOnAction(event -> {
            groupFilter.getSelectionModel().clearSelection();
            if (adminMode) {
                teacherFilter.getSelectionModel().clearSelection();
            }
            classTypeFilter.getSelectionModel().clearSelection();
            modeFilter.getSelectionModel().clearSelection();
            refreshEntries.run();
        });

        ObservableList<ClassMeeting> sourceMeetings = FXCollections.observableArrayList(
                adminMode
                        ? classMeetingRepository.findAllWithBasicData()
                        : classMeetingRepository.findByTeacherId(currentUser().getId())
        );
        ListView<ClassMeeting> sourceMeetingList = createMeetingList(sourceMeetings);
        sourceMeetingList.setPrefHeight(190);

        Button markWeekly = new Button("Mark Selected Meeting As Weekly");
        markWeekly.setOnAction(event -> runWithMessage(message, () -> {
            ClassMeeting selected = sourceMeetingList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                throw new IllegalArgumentException("Select a class meeting first.");
            }
            weeklyScheduleEntryRepository.createFromClassMeeting(selected.getId());
            refreshEntries.run();
            sourceMeetings.setAll(adminMode
                    ? classMeetingRepository.findAllWithBasicData()
                    : classMeetingRepository.findByTeacherId(currentUser().getId()));
            return "Class meeting marked as a weekly schedule entry.";
        }));

        Button generate = new Button("Generate Class Meetings");
        generate.setOnAction(event -> runWithMessage(message, () -> {
            WeeklyScheduleEntry selected = entriesList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                throw new IllegalArgumentException("Select a weekly schedule entry first.");
            }
            List<ClassMeeting> generated = scheduleGenerationService.generateClassMeetingsForSemester(selected);
            generatedCount.setText("Generated meetings count: " + generated.size());
            sourceMeetings.setAll(adminMode
                    ? classMeetingRepository.findAllWithBasicData()
                    : classMeetingRepository.findByTeacherId(currentUser().getId()));
            return "Generated " + generated.size() + " class meetings.";
        }));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.add(new Label("Group"), 0, 0);
        form.add(groupFilter, 1, 0);
        form.add(new Label("Teacher"), 2, 0);
        form.add(teacherFilter, 3, 0);
        form.add(new Label("Class type"), 0, 1);
        form.add(classTypeFilter, 1, 1);
        form.add(new Label("Mode"), 2, 1);
        form.add(modeFilter, 3, 1);
        form.add(new HBox(10, applyFilters, clearFilters), 1, 2, 3, 1);

        VBox box = new VBox(10,
                new Label("Existing weekly schedule entries"),
                entriesList,
                generatedCount,
                new HBox(10, generate),
                new Label("Filters"),
                form,
                new Label("Source class meetings"),
                sourceMeetingList,
                new HBox(10, markWeekly),
                message
        );
        box.setPadding(new Insets(12));
        return new ScrollPane(box);
    }

    private BorderPane createAdminReportView() {
        semesterFilter = new ListView<>();
        semesterFilter.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        semesterFilter.setPrefHeight(110);
        semesterFilter.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Semester semester, boolean empty) {
                super.updateItem(semester, empty);
                setText(empty || semester == null ? null : "Semester " + semester.getNumber());
            }
        });

        teacherFilter = new ComboBox<>();
        teacherFilter.setConverter(converter(teacher -> teacher.getName() + " " + teacher.getSurname()));
        subjectFilter = new ComboBox<>();
        subjectFilter.setConverter(converter(Subject::getName));
        groupFilter = new ComboBox<>();
        groupFilter.setConverter(converter(StudentGroup::getCode));
        classTypeFilter = new ComboBox<>(FXCollections.observableArrayList("All", "Lecture", "Tutorial", "Laboratory"));
        classTypeFilter.getSelectionModel().select("All");
        dateFromFilter = new DatePicker();
        dateToFilter = new DatePicker();
        reportMessageLabel = new Label();
        loadReportFilters();

        GridPane filters = new GridPane();
        filters.setHgap(10);
        filters.setVgap(10);
        filters.add(new Label("Semesters"), 0, 0);
        filters.add(semesterFilter, 1, 0, 3, 1);
        filters.add(new Label("Teacher"), 0, 1);
        filters.add(teacherFilter, 1, 1);
        filters.add(new Label("Subject"), 2, 1);
        filters.add(subjectFilter, 3, 1);
        filters.add(new Label("Group"), 0, 2);
        filters.add(groupFilter, 1, 2);
        filters.add(new Label("Class type"), 2, 2);
        filters.add(classTypeFilter, 3, 2);
        filters.add(new Label("Date from"), 0, 3);
        filters.add(dateFromFilter, 1, 3);
        filters.add(new Label("Date to"), 2, 3);
        filters.add(dateToFilter, 3, 3);

        Button generateButton = new Button("Generate Report");
        generateButton.setOnAction(event -> generateReport());
        Button exportButton = new Button("Export CSV");
        exportButton.setOnAction(event -> exportReport());
        Button clearButton = new Button("Clear Filters");
        clearButton.setOnAction(event -> clearReportFilters());

        VBox top = new VBox(12, filters, new HBox(10, generateButton, exportButton, clearButton), reportMessageLabel);
        top.setPadding(new Insets(12));
        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(createReportTable());
        return root;
    }

    private TableView<ReportLineRow> createReportTable() {
        TableView<ReportLineRow> table = new TableView<>(reportRows);
        TableColumn<ReportLineRow, String> studentColumn = new TableColumn<>("Student");
        studentColumn.setCellValueFactory(data -> data.getValue().studentNameProperty());
        studentColumn.setPrefWidth(240);
        TableColumn<ReportLineRow, Number> totalColumn = new TableColumn<>("Total Meetings");
        totalColumn.setCellValueFactory(data -> data.getValue().totalMeetingsProperty());
        TableColumn<ReportLineRow, Number> presentColumn = new TableColumn<>("Present");
        presentColumn.setCellValueFactory(data -> data.getValue().presentCountProperty());
        TableColumn<ReportLineRow, Number> lateColumn = new TableColumn<>("Late");
        lateColumn.setCellValueFactory(data -> data.getValue().lateCountProperty());
        TableColumn<ReportLineRow, Number> excusedColumn = new TableColumn<>("Excused");
        excusedColumn.setCellValueFactory(data -> data.getValue().excusedCountProperty());
        TableColumn<ReportLineRow, Number> absentColumn = new TableColumn<>("Absent");
        absentColumn.setCellValueFactory(data -> data.getValue().absentCountProperty());
        TableColumn<ReportLineRow, Number> percentageColumn = new TableColumn<>("Attendance %");
        percentageColumn.setCellValueFactory(data -> data.getValue().attendancePercentageProperty());
        table.getColumns().add(studentColumn);
        table.getColumns().add(totalColumn);
        table.getColumns().add(presentColumn);
        table.getColumns().add(lateColumn);
        table.getColumns().add(excusedColumn);
        table.getColumns().add(absentColumn);
        table.getColumns().add(percentageColumn);
        return table;
    }

    private void loadReportFilters() {
        semesterFilter.setItems(FXCollections.observableArrayList(new GenericRepository<>(Semester.class).findAll()));
        teacherFilter.setItems(FXCollections.observableArrayList(new TeacherRepository().findAllWithQualifiedSubjects()));
        subjectFilter.setItems(FXCollections.observableArrayList(new GenericRepository<>(Subject.class).findAll()));
        groupFilter.setItems(FXCollections.observableArrayList(new GenericRepository<>(StudentGroup.class).findAll()));
    }

    private void generateReport() {
        try {
            AttendanceReportFilter filter = new AttendanceReportFilter();
            Set<Long> semesterIds = semesterFilter.getSelectionModel().getSelectedItems().stream()
                    .map(Semester::getId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            filter.setSemesterIds(semesterIds);
            filter.setTeacherId(teacherFilter.getValue() == null ? null : teacherFilter.getValue().getId());
            filter.setSubjectId(subjectFilter.getValue() == null ? null : subjectFilter.getValue().getId());
            filter.setGroupId(groupFilter.getValue() == null ? null : groupFilter.getValue().getId());
            filter.setClassType(resolveClassType());
            filter.setDateFrom(dateFromFilter.getValue());
            filter.setDateTo(dateToFilter.getValue());

            currentReport = reportService.generateReport(filter);
            reportRows.setAll(currentReport.getReportLines().stream()
                    .sorted(Comparator.comparing(line -> line.getStudent().getSurname() + line.getStudent().getName()))
                    .map(ReportLineRow::new)
                    .toList());
            reportMessageLabel.setText(reportService.buildConclusionMessage(currentReport));
        } catch (RuntimeException ex) {
            reportMessageLabel.setText("Cannot generate report: " + ExceptionMessages.userMessage(ex));
        }
    }

    private ClassType resolveClassType() {
        return switch (classTypeFilter.getValue()) {
            case "Lecture" -> ClassType.LECTURE;
            case "Tutorial" -> ClassType.TUTORIAL;
            case "Laboratory" -> ClassType.LABORATORY;
            default -> null;
        };
    }

    private void exportReport() {
        try {
            if (currentReport == null) {
                reportMessageLabel.setText("Generate a report before exporting.");
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export attendance report");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            fileChooser.setInitialFileName("attendance-report.csv");
            java.io.File file = fileChooser.showSaveDialog(primaryStage);
            if (file == null) {
                return;
            }
            Path exported = reportExportService.exportToCsv(currentReport, file.toPath());
            reportMessageLabel.setText("Report exported to " + exported.getFileName() + ".");
        } catch (RuntimeException ex) {
            reportMessageLabel.setText(ExceptionMessages.userMessage(ex));
        }
    }

    private void clearReportFilters() {
        semesterFilter.getSelectionModel().clearSelection();
        teacherFilter.setValue(null);
        subjectFilter.setValue(null);
        groupFilter.setValue(null);
        classTypeFilter.getSelectionModel().select("All");
        dateFromFilter.setValue(null);
        dateToFilter.setValue(null);
        reportRows.clear();
        currentReport = null;
        reportMessageLabel.setText("Filters cleared.");
    }

    private void showClassMeetingDetails(ClassMeeting meeting) {
        ClassMeeting fullMeeting = attendanceService.getClassMeetingWithStudents(meeting.getId());
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Class meeting details");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        boolean admin = authenticationService.isAdmin();
        boolean teacher = authenticationService.isTeacher()
                && fullMeeting.getTeacher() != null
                && fullMeeting.getTeacher().getId().equals(currentUser().getId());
        boolean student = authenticationService.isStudent();

        TextArea commentArea = new TextArea(fullMeeting.getComment() == null ? "" : fullMeeting.getComment());
        commentArea.setEditable(admin || teacher);
        commentArea.setPrefRowCount(3);
        TextField locationField = new TextField(fullMeeting.getLocation());
        TextField linkField = new TextField(fullMeeting.getOnlineMeetingLink());
        locationField.setEditable(admin);
        linkField.setEditable(admin);

        TableView<AttendanceRow> table = createStudentsTable(admin || teacher);
        Map<Long, AttendanceStatus> statuses = attendanceService.getSavedStatuses(fullMeeting.getId());
        Map<Long, String> comments = attendanceService.getSavedComments(fullMeeting.getId());
        List<AttendanceRow> rows = fullMeeting.getGroup().getStudents().stream()
                .filter(s -> !student || s.getId().equals(currentUser().getId()))
                .sorted(Comparator.comparing(Student::getSurname).thenComparing(Student::getName))
                .map(s -> new AttendanceRow(s, statuses.get(s.getId()), comments.getOrDefault(s.getId(), "")))
                .toList();
        table.setItems(FXCollections.observableArrayList(rows));

        Label message = new Label();
        Button saveComment = new Button("Save Comment");
        saveComment.setVisible(admin || teacher);
        saveComment.setOnAction(event -> runWithMessage(message, () -> {
            classMeetingService.saveComment(fullMeeting.getId(), commentArea.getText());
            fullMeeting.setComment(commentArea.getText());
            return "Comment saved.";
        }));
        Button saveLocation = new Button("Save Location/Link");
        saveLocation.setVisible(admin);
        saveLocation.setOnAction(event -> runWithMessage(message, () -> {
            classMeetingService.saveLocationAndOnlineLink(fullMeeting.getId(), locationField.getText(), linkField.getText());
            return "Location/link saved.";
        }));
        Button cancel = new Button("Cancel Meeting");
        cancel.setVisible(admin);
        cancel.setOnAction(event -> runWithMessage(message, () -> {
            classMeetingService.cancelClassMeeting(fullMeeting.getId(), commentArea.getText());
            fullMeeting.setStatus(ClassMeetingStatus.CANCELLED);
            return "Class meeting cancelled.";
        }));
        Button saveAttendance = new Button("Save Attendance");
        saveAttendance.setVisible(admin || teacher);
        saveAttendance.setOnAction(event -> runWithMessage(message, () -> {
            if (teacher) {
                classMeetingService.validateCanMarkAttendance(currentUser().getId(), fullMeeting);
            }
            Map<Long, AttendanceStatus> updatedStatuses = new LinkedHashMap<>();
            Map<Long, String> updatedComments = new LinkedHashMap<>();
            for (AttendanceRow row : table.getItems()) {
                updatedStatuses.put(row.getStudentId(), row.getStatus());
                updatedComments.put(row.getStudentId(), row.getComment());
            }
            attendanceService.registerAttendance(fullMeeting.getId(), updatedStatuses, updatedComments);
            return "Attendance saved.";
        }));

        VBox content = new VBox(10,
                new Label(meetingSummary(fullMeeting)),
                new Label("General comment"), commentArea,
                new Label("Room/location"), locationField,
                new Label("Online link"), linkField,
                new Label(student ? "My attendance" : "Attendance"), table,
                new HBox(10, saveComment, saveLocation, cancel, saveAttendance),
                message
        );
        content.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(850);
        dialog.showAndWait();
    }

    private TableView<AttendanceRow> createStudentsTable(boolean editable) {
        TableView<AttendanceRow> table = new TableView<>();
        table.setEditable(editable);
        TableColumn<AttendanceRow, String> numberColumn = new TableColumn<>("Student number");
        numberColumn.setCellValueFactory(data -> data.getValue().studentNumberProperty());
        TableColumn<AttendanceRow, String> nameColumn = new TableColumn<>("Student");
        nameColumn.setCellValueFactory(data -> data.getValue().fullNameProperty());
        nameColumn.setPrefWidth(220);
        TableColumn<AttendanceRow, AttendanceStatus> statusColumn = new TableColumn<>("Attendance status");
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        if (editable) {
            statusColumn.setCellFactory(ComboBoxTableCell.forTableColumn(AttendanceStatus.values()));
            statusColumn.setOnEditCommit(event -> event.getRowValue().setStatus(event.getNewValue()));
        }
        TableColumn<AttendanceRow, String> commentColumn = new TableColumn<>("Comment");
        commentColumn.setCellValueFactory(data -> data.getValue().commentProperty());
        commentColumn.setPrefWidth(300);
        if (editable) {
            commentColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            commentColumn.setOnEditCommit(event -> event.getRowValue().setComment(event.getNewValue()));
        }
        table.getColumns().add(numberColumn);
        table.getColumns().add(nameColumn);
        table.getColumns().add(statusColumn);
        table.getColumns().add(commentColumn);
        return table;
    }

    private String meetingSummary(ClassMeeting meeting) {
        String modeLine = meeting.getMeetingMode() == MeetingMode.ONLINE
                ? "Mode: ONLINE | Link: " + safe(meeting.getOnlineMeetingLink())
                : "Mode: CLASSROOM | Room: " + safe(meeting.getLocation());
        return meeting.getSubject().getName() + " | " + meeting.getClassType() + " | Group " + meeting.getGroup().getCode()
                + System.lineSeparator()
                + "Teacher: " + meeting.getTeacher().getName() + " " + meeting.getTeacher().getSurname()
                + " | " + meeting.getTeacher().getPrimaryEmail()
                + System.lineSeparator()
                + "Date: " + meeting.getMeetingDate() + " | Time: "
                + meeting.getTime().getStartTime() + "-" + meeting.getTime().getEndTime()
                + System.lineSeparator()
                + modeLine
                + System.lineSeparator()
                + "Status: " + meeting.getStatus();
    }

    private Node placeholder(String text) {
        return padded(new VBox(10, new Label(text)));
    }

    private Node padded(Node node) {
        BorderPane wrapper = new BorderPane(node);
        wrapper.setPadding(new Insets(12));
        return wrapper;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "" : value;
    }

    private Person currentUser() {
        return authenticationService.getCurrentUser();
    }

    private String roleName(Person person) {
        if (person instanceof Administrator) {
            return "Administrator";
        }
        if (person instanceof Teacher) {
            return "Teacher";
        }
        if (person instanceof Student) {
            return "Student";
        }
        return "Person";
    }

    private void runWithMessage(Label label, MessageAction action) {
        try {
            label.setText(action.run());
        } catch (RuntimeException ex) {
            label.setText(ExceptionMessages.userMessage(ex));
        }
    }

    private String semesterAcademicLabel(Semester semester) {
        String fieldNames = semester.getFields().stream()
                .map(Field::getName)
                .sorted()
                .collect(java.util.stream.Collectors.joining(", "));
        return "Semester " + semester.getNumber() + (fieldNames.isBlank() ? "" : " | " + fieldNames);
    }

    private <T> StringConverter<T> converter(java.util.function.Function<T, String> label) {
        return new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : label.apply(item);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }

    @Override
    public void stop() {
        JpaUtil.close();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @FunctionalInterface
    private interface MessageAction {
        String run();
    }
}
