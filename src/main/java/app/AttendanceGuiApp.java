package app;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import model.Administrator;
import model.Attendance;
import model.AttendanceReport;
import model.ClassMeeting;
import model.Field;
import model.Notification;
import model.Person;
import model.ReportLine;
import model.Room;
import model.RoomBooking;
import model.Semester;
import model.Student;
import model.StudentGroup;
import model.Subject;
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
import persistence.AttendanceRepository;
import persistence.ClassMeetingRepository;
import persistence.GenericRepository;
import persistence.JpaUtil;
import persistence.PersonRepository;
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
    private final AuthenticationService authenticationService = new AuthenticationService();
    private final AttendanceRegistrationService attendanceService = new AttendanceRegistrationService();
    private final AttendanceReportService reportService = new AttendanceReportService();
    private final ReportExportService reportExportService = new ReportExportService();
    private final ClassMeetingService classMeetingService = new ClassMeetingService();
    private final ClassMeetingRepository classMeetingRepository = new ClassMeetingRepository();
    private final AttendanceRepository attendanceRepository = new AttendanceRepository();
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
        content.setPadding(new Insets(36));
        primaryStage.setScene(new Scene(content, 620, 420));
    }

    private void showMainLayout() {
        mainLayout = new BorderPane();
        pageHistory.clear();
        currentPageTitle = null;
        currentPageContent = null;
        backButton = new Button("Back");
        backButton.setDisable(true);
        backButton.setOnAction(event -> goBack());
        userLabel = new Label();
        pageMessageLabel = new Label();
        navigation = new VBox(8);
        navigation.setPadding(new Insets(12));
        navigation.setPrefWidth(230);

        HBox header = new HBox(16, backButton, userLabel, pageMessageLabel);
        header.setPadding(new Insets(12));
        mainLayout.setTop(header);
        mainLayout.setLeft(navigation);
        refreshUserLabel();
        buildNavigation();
        setPage("Public Weekly Schedule", createPublicScheduleView());

        primaryStage.setScene(new Scene(mainLayout, 1180, 720));
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
        ObservableList<ClassMeeting> scheduleMeetings = FXCollections.observableArrayList(classMeetingRepository.findClassMeetingsForCurrentWeek());
        Label message = new Label("Double-click a meeting. Details open only for your own group, assigned teacher, or admin.");
        ListView<ClassMeeting> list = createMeetingList(scheduleMeetings);
        list.setOnMouseClicked(event -> {
            ClassMeeting selected = list.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) {
                tryOpenMeetingDetails(selected, message);
            }
        });

        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> scheduleMeetings.setAll(classMeetingRepository.findClassMeetingsForCurrentWeek()));
        BorderPane root = new BorderPane();
        root.setTop(padded(new VBox(8, message, refresh)));
        root.setCenter(list);
        BorderPane.setMargin(list, new Insets(0, 12, 12, 12));
        return root;
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
        TableColumn<Student, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPrimaryEmail()));
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
            message.setText(ex.getMessage());
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
            personalSettingsService.addEmail(user.getId(), emailField.getText());
            user.getEmails().add(emailField.getText());
            if (user.getPrimaryEmailValue() == null || user.getPrimaryEmailValue().isBlank()) {
                user.setPrimaryEmail(emailField.getText());
            }
            emails.setAll(user.getEmails());
            primaryEmailLabel.setText("Primary email: " + user.getPrimaryEmail());
            emailField.clear();
            return "Email added.";
        }));
        Button edit = new Button("Edit Selected Email");
        edit.setOnAction(event -> runWithMessage(message, () -> {
            String selected = emailList.getSelectionModel().getSelectedItem();
            personalSettingsService.editEmail(user.getId(), selected, emailField.getText());
            user.getEmails().remove(selected);
            user.getEmails().add(emailField.getText());
            if (selected != null && selected.equals(user.getPrimaryEmailValue())) {
                user.setPrimaryEmail(emailField.getText());
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

    private Node createAdminNotificationView() {
        ObservableList<Notification> notifications = FXCollections.observableArrayList(notificationManagementService.findAllWithRecipients());
        TableView<Notification> table = new TableView<>(notifications);
        TableColumn<Notification, String> recipientColumn = new TableColumn<>("Recipient");
        recipientColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getRecipient() == null ? "" : data.getValue().getRecipient().getName() + " " + data.getValue().getRecipient().getSurname()
        ));
        TableColumn<Notification, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(safe(data.getValue().getTitle())));
        TableColumn<Notification, String> messageColumn = new TableColumn<>("Message");
        messageColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getMessage()));
        messageColumn.setPrefWidth(300);
        TableColumn<Notification, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getStatus())));
        table.getColumns().add(recipientColumn);
        table.getColumns().add(titleColumn);
        table.getColumns().add(messageColumn);
        table.getColumns().add(statusColumn);

        ComboBox<Person> recipientBox = new ComboBox<>(FXCollections.observableArrayList(new PersonRepository().findAllWithEmails()));
        recipientBox.setConverter(converter(person -> person.getName() + " " + person.getSurname() + " | " + person.getPrimaryEmail()));
        TextField titleField = new TextField();
        titleField.setPromptText("title");
        TextArea messageArea = new TextArea();
        messageArea.setPromptText("notification message");
        messageArea.setPrefRowCount(3);
        ComboBox<String> channelBox = new ComboBox<>(FXCollections.observableArrayList("SYSTEM"));
        channelBox.getSelectionModel().select("SYSTEM");
        ComboBox<NotificationStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(NotificationStatus.values()));
        statusBox.getSelectionModel().select(NotificationStatus.PENDING);
        Label message = new Label();

        Button create = new Button("Create Notification");
        create.setOnAction(event -> runWithMessage(message, () -> {
            if (recipientBox.getValue() == null || titleField.getText().isBlank() || messageArea.getText().isBlank()) {
                throw new IllegalArgumentException("Recipient, title, and message are required.");
            }
            notificationManagementService.createSystemNotification(recipientBox.getValue().getId(),
                    titleField.getText(), messageArea.getText(), statusBox.getValue());
            notifications.setAll(notificationManagementService.findAllWithRecipients());
            titleField.clear();
            messageArea.clear();
            return "Notification created.";
        }));

        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected != null) {
                recipientBox.setValue(selected.getRecipient());
                titleField.setText(safe(selected.getTitle()));
                messageArea.setText(selected.getMessage());
                statusBox.setValue(selected.getStatus());
            }
        });

        Button saveChanges = new Button("Save Changes");
        saveChanges.setOnAction(event -> runWithMessage(message, () -> {
            Notification selected = table.getSelectionModel().getSelectedItem();
            notificationManagementService.updateEditableNotification(selected, recipientBox.getValue(),
                    titleField.getText(), messageArea.getText(), statusBox.getValue());
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
                recipientBox,
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

    private ListCell<Notification> notificationCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Notification notification, boolean empty) {
                super.updateItem(notification, empty);
                if (empty || notification == null) {
                    setText(null);
                    return;
                }
                String recipient = notification.getRecipient() == null ? "no recipient"
                        : notification.getRecipient().getName() + " " + notification.getRecipient().getSurname();
                setText(notification.getStatus() + " | " + recipient + " | " + notification.getMessage());
            }
        };
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
                        + " | " + entry.getSubject().getName()
                        + " | " + entry.getClassType()
                        + " | " + entry.getTeacher().getName() + " " + entry.getTeacher().getSurname()
                        + " | " + place);
            }
        };
    }

    private Node createMyHistoryView() {
        if (authenticationService.isStudent()) {
            return meetingListPage(classMeetingRepository.findClassMeetingHistoryForStudent(currentUser().getId()), "Student history is derived through Student -> StudentGroup -> ClassMeeting.");
        }
        if (authenticationService.isTeacher()) {
            return meetingListPage(classMeetingRepository.findClassMeetingHistoryForTeacher(currentUser().getId()), "Teacher history is derived through Teacher -> ClassMeeting.");
        }
        return meetingListPage(classMeetingRepository.findAllWithBasicData(), "Administrator can view all class meeting history.");
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
                new Label("Field of study: " + group.getSemester().getField().getName()),
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

    private Node createUserManagementPane() {
        ObservableList<Person> userItems = FXCollections.observableArrayList(new PersonRepository().findAllWithEmails());
        ObservableList<StudentGroup> groupItems = FXCollections.observableArrayList(new GenericRepository<>(StudentGroup.class).findAll());
        ObservableList<Subject> subjectItems = FXCollections.observableArrayList(new GenericRepository<>(Subject.class).findAll());

        ListView<Person> users = new ListView<>(userItems);
        users.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Person person, boolean empty) {
                super.updateItem(person, empty);
                setText(empty || person == null ? null : roleName(person) + " | " + person.getName() + " " + person.getSurname() + " | " + person.getPrimaryEmail());
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
        emailField.setPromptText("primary email");
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
            adminManagementService.updatePersonBasics(selected.getId(), nameField.getText(), surnameField.getText(), emailField.getText(), passwordField.getText());
            refreshUsers.run();
            return "User basic data updated.";
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
                new Label("Create users, create administrator accounts, and edit basic personal data."),
                form,
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
        ObservableList<Semester> semesters = FXCollections.observableArrayList(new GenericRepository<>(Semester.class).findAll());
        ObservableList<StudentGroup> groups = FXCollections.observableArrayList(new GenericRepository<>(StudentGroup.class).findAll());
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
                setText(empty || semester == null ? null : "Semester " + semester.getNumber());
            }
        });
        TextField semesterNumber = new TextField();
        semesterNumber.setPromptText("number");
        DatePicker semesterStart = new DatePicker(LocalDate.now().minusWeeks(4));
        DatePicker semesterEnd = new DatePicker(LocalDate.now().plusWeeks(12));
        ComboBox<Field> semesterField = new ComboBox<>(fields);
        semesterField.setConverter(converter(Field::getName));

        ListView<StudentGroup> groupList = new ListView<>(groups);
        groupList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(StudentGroup group, boolean empty) {
                super.updateItem(group, empty);
                setText(empty || group == null ? null : group.getCode());
            }
        });
        TextField groupCode = new TextField();
        groupCode.setPromptText("group code");
        ComboBox<Semester> groupSemester = new ComboBox<>(semesters);
        groupSemester.setConverter(converter(semester -> "Semester " + semester.getNumber()));

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
        ComboBox<StudentGroup> subjectGroup = new ComboBox<>(groups);
        subjectGroup.setConverter(converter(StudentGroup::getCode));

        Label message = new Label();
        Runnable refresh = () -> {
            fields.setAll(new GenericRepository<>(Field.class).findAll());
            semesters.setAll(new GenericRepository<>(Semester.class).findAll());
            groups.setAll(new GenericRepository<>(StudentGroup.class).findAll());
            subjects.setAll(new GenericRepository<>(Subject.class).findAll());
        };

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
                semesterField.setValue(selected.getField());
            }
        });
        groupList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected != null) {
                groupCode.setText(selected.getCode());
                groupSemester.setValue(selected.getSemester());
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
            Field field = semesterField.getValue();
            adminManagementService.createSemester(Integer.parseInt(semesterNumber.getText()), semesterStart.getValue(), semesterEnd.getValue(), field == null ? null : field.getId());
            refresh.run();
            return "Semester created.";
        }));
        Button updateSemester = new Button("Save Semester");
        updateSemester.setOnAction(event -> runWithMessage(message, () -> {
            Semester selected = semesterList.getSelectionModel().getSelectedItem();
            Field field = semesterField.getValue();
            adminManagementService.updateSemester(selected == null ? null : selected.getId(), Integer.parseInt(semesterNumber.getText()),
                    semesterStart.getValue(), semesterEnd.getValue(), field == null ? null : field.getId());
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
            adminManagementService.createGroup(groupCode.getText(), semester == null ? null : semester.getId());
            refresh.run();
            return "Group created.";
        }));
        Button updateGroup = new Button("Save Group");
        updateGroup.setOnAction(event -> runWithMessage(message, () -> {
            StudentGroup selected = groupList.getSelectionModel().getSelectedItem();
            Semester semester = groupSemester.getValue();
            adminManagementService.updateGroup(selected == null ? null : selected.getId(), groupCode.getText(), semester == null ? null : semester.getId());
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
        Button assignSubjectGroup = new Button("Assign Subject To Group");
        assignSubjectGroup.setOnAction(event -> runWithMessage(message, () -> {
            Subject subject = subjectList.getSelectionModel().getSelectedItem();
            StudentGroup group = subjectGroup.getValue();
            adminManagementService.assignSubjectToGroup(subject == null ? null : subject.getId(), group == null ? null : group.getId());
            refresh.run();
            return "Subject assigned to group.";
        }));
        Button removeSubjectGroup = new Button("Remove Subject From Group");
        removeSubjectGroup.setOnAction(event -> runWithMessage(message, () -> {
            Subject subject = subjectList.getSelectionModel().getSelectedItem();
            StudentGroup group = subjectGroup.getValue();
            adminManagementService.removeSubjectFromGroup(subject == null ? null : subject.getId(), group == null ? null : group.getId());
            refresh.run();
            return "Subject removed from group.";
        }));

        VBox fieldBox = new VBox(8, new Label("Fields of study"), fieldList, fieldName, new HBox(8, createField, updateField, deleteField));
        VBox semesterBox = new VBox(8, new Label("Semesters"), semesterList, semesterNumber,
                new HBox(8, new Label("From"), semesterStart, new Label("To"), semesterEnd), semesterField,
                new HBox(8, createSemester, updateSemester, deleteSemester));
        VBox groupBox = new VBox(8, new Label("Groups"), groupList, groupCode, groupSemester, new HBox(8, createGroup, updateGroup, deleteGroup));
        VBox subjectBox = new VBox(8, new Label("Subjects"), subjectList, subjectName,
                new HBox(8, createSubject, updateSubject, deleteSubject),
                new Label("Subject-group assignment"), subjectGroup, new HBox(8, assignSubjectGroup, removeSubjectGroup));

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
        ComboBox<StudentGroup> groupBox = new ComboBox<>(FXCollections.observableArrayList(new GenericRepository<>(StudentGroup.class).findAll()));
        ComboBox<Room> roomBox = new ComboBox<>(FXCollections.observableArrayList(roomService.findAll()));
        ComboBox<ClassType> classTypeBox = new ComboBox<>(FXCollections.observableArrayList(ClassType.values()));
        ComboBox<MeetingMode> modeBox = new ComboBox<>(FXCollections.observableArrayList(MeetingMode.values()));
        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        TextField startField = new TextField("10:00");
        TextField endField = new TextField("11:30");
        TextField locationField = new TextField();
        TextField linkField = new TextField();
        ComboBox<ClassMeetingStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(ClassMeetingStatus.values()));
        statusBox.getSelectionModel().select(adminMode ? ClassMeetingStatus.DRAFT : ClassMeetingStatus.SCHEDULED);

        teacherBox.setConverter(converter(teacher -> teacher.getName() + " " + teacher.getSurname()));
        subjectBox.setConverter(converter(Subject::getName));
        groupBox.setConverter(converter(StudentGroup::getCode));
        roomBox.setConverter(converter(Room::getRoomNumber));

        if (adminMode) {
            teacherBox.setItems(FXCollections.observableArrayList(new GenericRepository<>(Teacher.class).findAll()));
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
        ObservableList<WeeklyScheduleEntry> entries = FXCollections.observableArrayList(weeklyScheduleEntryRepository.findAllWithDetails());
        ListView<WeeklyScheduleEntry> entriesList = new ListView<>(entries);
        entriesList.setCellFactory(view -> weeklyScheduleEntryCell());

        ComboBox<StudentGroup> groupBox = new ComboBox<>(FXCollections.observableArrayList(new GenericRepository<>(StudentGroup.class).findAll()));
        ComboBox<Semester> semesterBox = new ComboBox<>(FXCollections.observableArrayList(new GenericRepository<>(Semester.class).findAll()));
        ComboBox<Subject> subjectBox = new ComboBox<>(FXCollections.observableArrayList(new GenericRepository<>(Subject.class).findAll()));
        ComboBox<Teacher> teacherBox = new ComboBox<>(FXCollections.observableArrayList(new GenericRepository<>(Teacher.class).findAll()));
        ComboBox<ClassType> classTypeBox = new ComboBox<>(FXCollections.observableArrayList(ClassType.values()));
        ComboBox<MeetingMode> modeBox = new ComboBox<>(FXCollections.observableArrayList(MeetingMode.values()));
        ComboBox<DayOfWeek> dayBox = new ComboBox<>(FXCollections.observableArrayList(DayOfWeek.values()));
        TextField startField = new TextField("10:00");
        TextField endField = new TextField("11:30");
        ComboBox<Room> roomBox = new ComboBox<>(FXCollections.observableArrayList(roomService.findAll()));
        TextField onlineLinkField = new TextField();
        Label message = new Label();
        Label generatedCount = new Label("Generated meetings count: 0");

        groupBox.setConverter(converter(StudentGroup::getCode));
        semesterBox.setConverter(converter(semester -> "Semester " + semester.getNumber()));
        subjectBox.setConverter(converter(Subject::getName));
        teacherBox.setConverter(converter(teacher -> teacher.getName() + " " + teacher.getSurname()));
        roomBox.setConverter(converter(Room::getRoomNumber));

        Button save = new Button("Save Weekly Schedule Entry");
        save.setOnAction(event -> runWithMessage(message, () -> {
            WeeklyScheduleEntry entry = buildWeeklyScheduleEntry(groupBox, semesterBox, subjectBox, teacherBox,
                    classTypeBox, modeBox, dayBox, startField, endField, roomBox, onlineLinkField);
            weeklyScheduleEntryRepository.saveWithManagedReferences(entry);
            entries.setAll(weeklyScheduleEntryRepository.findAllWithDetails());
            return "Weekly schedule entry saved.";
        }));

        Button generate = new Button("Generate Class Meetings");
        generate.setOnAction(event -> runWithMessage(message, () -> {
            WeeklyScheduleEntry selected = entriesList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                selected = buildWeeklyScheduleEntry(groupBox, semesterBox, subjectBox, teacherBox,
                        classTypeBox, modeBox, dayBox, startField, endField, roomBox, onlineLinkField);
                selected = weeklyScheduleEntryRepository.saveWithManagedReferences(selected);
                entries.setAll(weeklyScheduleEntryRepository.findAllWithDetails());
            }
            List<ClassMeeting> generated = scheduleGenerationService.generateClassMeetingsForSemester(selected);
            generatedCount.setText("Generated meetings count: " + generated.size());
            return "Generated " + generated.size() + " class meetings.";
        }));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.add(new Label("Student group"), 0, 0);
        form.add(groupBox, 1, 0);
        form.add(new Label("Semester"), 0, 1);
        form.add(semesterBox, 1, 1);
        form.add(new Label("Subject"), 0, 2);
        form.add(subjectBox, 1, 2);
        form.add(new Label("Teacher"), 0, 3);
        form.add(teacherBox, 1, 3);
        form.add(new Label("Class type"), 0, 4);
        form.add(classTypeBox, 1, 4);
        form.add(new Label("Meeting mode"), 0, 5);
        form.add(modeBox, 1, 5);
        form.add(new Label("Day of week"), 0, 6);
        form.add(dayBox, 1, 6);
        form.add(new Label("Start time"), 0, 7);
        form.add(startField, 1, 7);
        form.add(new Label("End time"), 0, 8);
        form.add(endField, 1, 8);
        form.add(new Label("Room"), 0, 9);
        form.add(roomBox, 1, 9);
        form.add(new Label("Online link"), 0, 10);
        form.add(onlineLinkField, 1, 10);
        form.add(new HBox(10, save, generate), 1, 11);
        form.add(message, 1, 12);

        VBox box = new VBox(10,
                new Label("Existing weekly schedule entries"),
                entriesList,
                generatedCount,
                new Label("Create weekly schedule entry"),
                form
        );
        box.setPadding(new Insets(12));
        return new ScrollPane(box);
    }

    private WeeklyScheduleEntry buildWeeklyScheduleEntry(ComboBox<StudentGroup> groupBox,
                                                         ComboBox<Semester> semesterBox,
                                                         ComboBox<Subject> subjectBox,
                                                         ComboBox<Teacher> teacherBox,
                                                         ComboBox<ClassType> classTypeBox,
                                                         ComboBox<MeetingMode> modeBox,
                                                         ComboBox<DayOfWeek> dayBox,
                                                         TextField startField,
                                                         TextField endField,
                                                         ComboBox<Room> roomBox,
                                                         TextField onlineLinkField) {
        StudentGroup group = groupBox.getValue();
        Semester semester = semesterBox.getValue();
        Subject subject = subjectBox.getValue();
        Teacher selectedTeacher = teacherBox.getValue();
        if (group == null || semester == null || subject == null || selectedTeacher == null
                || classTypeBox.getValue() == null || modeBox.getValue() == null || dayBox.getValue() == null) {
            throw new IllegalArgumentException("Group, semester, subject, teacher, type, mode, and day are required.");
        }
        Teacher teacher = new TeacherRepository().findByIdWithQualifiedSubjects(selectedTeacher.getId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found."));
        if (!teacher.isQualifiedFor(subject)) {
            throw new IllegalArgumentException("Teacher is not qualified to teach this subject.");
        }
        LocalTime start = LocalTime.parse(startField.getText());
        LocalTime end = LocalTime.parse(endField.getText());
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }
        Room room = roomBox.getValue();
        String onlineLink = onlineLinkField.getText();
        if (modeBox.getValue() == MeetingMode.CLASSROOM && room == null) {
            throw new IllegalArgumentException("Room is required for classroom weekly schedule.");
        }
        if (modeBox.getValue() == MeetingMode.ONLINE && (onlineLink == null || onlineLink.isBlank())) {
            throw new IllegalArgumentException("Online link is required for online weekly schedule.");
        }
        return new WeeklyScheduleEntry(group, subject, teacher, classTypeBox.getValue(), modeBox.getValue(),
                dayBox.getValue(), start, end, room, onlineLink, semester);
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
        teacherFilter.setItems(FXCollections.observableArrayList(new GenericRepository<>(Teacher.class).findAll()));
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
            reportMessageLabel.setText("Cannot generate report: " + ex.getMessage());
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
            reportMessageLabel.setText(ex.getMessage());
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
            label.setText(ex.getMessage());
        }
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
