package application;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;
import javafx.scene.control.cell.PropertyValueFactory;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;

public class SampleController {

    // --- FXML controls ---
    @FXML private TextField idField, nameField, deptField, posField;
    @FXML private Button startCameraBtn, stopCameraBtn, saveFaceBtn, recognizeBtn, removePersonBtn;
    @FXML private ProgressIndicator pb;
    @FXML private ImageView cameraView;
    @FXML private ListView<String> logList;
    @FXML private ComboBox<Person> personSelect;
    @FXML private TableView<AttendanceRecord> attendanceTable;
    @FXML private TableColumn<AttendanceRecord,String> timeColumn, nameColumn, dateColumn;
    @FXML private TableColumn<AttendanceRecord, String> eventTypeColumn;
    @FXML private TableColumn<AttendanceRecord, String> activityColumnLog;
    @FXML private TableView<Schedule> scheduleTable;
    @FXML private TableColumn<Schedule, String> dayColumn;
    @FXML private TableColumn<Schedule, String> startTimeColumn;
    @FXML private TableColumn<Schedule, String> endTimeColumn;
    @FXML private TableColumn<Schedule, String> activityColumn;
    @FXML private TextField scheduleActivity;
    @FXML private Label attendanceLabel;
    @FXML private Label scheduleLabel;
    @FXML private ComboBox<String> scheduleDayCombo;
    @FXML private ComboBox<String> scheduleStartCombo;
    @FXML private ComboBox<String> scheduleEndCombo;

    // For selected template display and actions
    @FXML private ImageView selectedTemplateView;
    @FXML private Button retakeTemplateBtn;

    // Core services
    private final FaceDetector   faceDetect = new FaceDetector();
    private final FaceRecognizer faceRec    = new FaceRecognizer(70.0);
    private Database             db;
    private static final ObservableList<String> eventLog       = FXCollections.observableArrayList();
    private final ObservableList<AttendanceRecord> attendanceData = FXCollections.observableArrayList();
    private final ObservableList<Schedule> schedules = FXCollections.observableArrayList();

    // Store the current template data for delete/retake
    private byte[] currentTemplateData;

    @FXML
    public void initialize() {
        faceDetect.setFrame(cameraView);

        try {
            db = new Database();
            putOnLog("DB connected");
        } catch (SQLException ex) {
            putOnLog("DB connection failed: " + ex.getMessage());
            return;
        }

        try {
            personSelect.getItems().setAll(db.getAllPersons());
        } catch (SQLException ex) {
            putOnLog("Failed to load persons: " + ex.getMessage());
        }
        personSelect.setConverter(new StringConverter<>() {
            @Override public String toString(Person p) { return p != null ? p.getFullName() : ""; }
            @Override public Person fromString(String s) { return null; }
        });

        personSelect.getSelectionModel().selectedItemProperty()
            .addListener((o, old, sel) -> {
                if (sel != null) {
                    idField.setText(sel.getPersonCode());
                    nameField.setText(sel.getFullName());
                    deptField.setText(sel.getDepartment());
                    posField.setText(sel.getPosition());

                    idField.setEditable(false);
                    nameField.setEditable(false);
                    deptField.setEditable(false);
                    posField.setEditable(false);

                    loadAttendance(sel.getPersonId());
                    loadTemplates(sel.getPersonId());
                    loadSchedules(sel.getPersonId());

                    faceRec.loadTrainedData();
                } else {
                    clearFields();
                }
            });

        timeColumn.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getEventTime().toLocalTime().toString()));
        nameColumn.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getFullName()));
        dateColumn.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getEventTime().toLocalDate().toString()));
        eventTypeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEventType()));
        activityColumnLog.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getActivity()));
        attendanceTable.setItems(attendanceData);

        dayColumn.setCellValueFactory(new PropertyValueFactory<>("day"));
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        activityColumn.setCellValueFactory(new PropertyValueFactory<>("activity"));
        scheduleTable.setItems(schedules);

        timeColumn.setReorderable(false);
        nameColumn.setReorderable(false);
        dateColumn.setReorderable(false);

        dayColumn.setReorderable(false);
        startTimeColumn.setReorderable(false);
        endTimeColumn.setReorderable(false);
        activityColumn.setReorderable(false);

        ObservableList<String> times = FXCollections.observableArrayList();
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 30) {
                String ampm = h < 12 ? "AM" : "PM";
                int hour12 = h % 12 == 0 ? 12 : h % 12;
                times.add(String.format("%02d:%02d %s", hour12, m, ampm));
            }
        }
        scheduleStartCombo.setItems(times);
        scheduleEndCombo.setItems(times);

        scheduleDayCombo.setItems(FXCollections.observableArrayList(
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        ));

        // Setup circular clip for selected template image
        if (selectedTemplateView != null) {
            Circle clip = new Circle(60, 60, 60);
            selectedTemplateView.setClip(clip);
        }
    }

    public void addSchedule(Schedule schedule) {
        schedules.add(schedule);
    }

    @FXML
    private void addSchedule() {
        Person selectedPerson = personSelect.getValue();
        String day = scheduleDayCombo.getValue();
        String start = scheduleStartCombo.getValue();
        String end = scheduleEndCombo.getValue();
        String activity = scheduleActivity.getText();
        if (selectedPerson != null && day != null && start != null && end != null && !activity.isEmpty()) {
            Schedule schedule = new Schedule(day, start, end, activity);
            try {
                db.addScheduleForPerson(selectedPerson.getPersonId(), schedule);
                scheduleTable.getItems().add(schedule);
                scheduleDayCombo.getSelectionModel().clearSelection();
                scheduleStartCombo.getSelectionModel().clearSelection();
                scheduleEndCombo.getSelectionModel().clearSelection();
                scheduleActivity.clear();
            } catch (SQLException ex) {
                putOnLog("Failed to add schedule: " + ex.getMessage());
            }
        }
    }

    private void putOnLog(String msg) {
        String entry = Instant.now() + ": " + msg;
        Platform.runLater(() -> {
            eventLog.add(entry);
            logList.setItems(eventLog);
            logList.scrollTo(eventLog.size()-1);
        });
    }

    private void loadAttendance(int pid) {
        try {
            attendanceData.setAll(db.getAttendanceForPerson(pid));
        } catch (SQLException ex) {
            putOnLog("Error loading attendance: " + ex.getMessage());
        }
    }

    private void loadTemplates(int pid) {
        try {
            List<byte[]> templates = db.getTemplatesByPerson(pid);
            if (!templates.isEmpty()) {
                currentTemplateData = templates.get(0);
                if (selectedTemplateView != null && currentTemplateData != null) {
                    Image img = new Image(new ByteArrayInputStream(currentTemplateData));
                    selectedTemplateView.setImage(img);
                } else {
                    putOnLog("selectedTemplateView is null!");
                }
                retakeTemplateBtn.setDisable(false);
            } else {
                if (selectedTemplateView != null) {
                    selectedTemplateView.setImage(null);
                }
                retakeTemplateBtn.setDisable(true);
            }
        } catch (SQLException ex) {
            putOnLog("Error loading templates: " + ex.getMessage());
        }
    }

    private void loadSchedules(int pid) {
        schedules.clear();
        try {
            schedules.addAll(db.getSchedulesForPerson(pid));
        } catch (SQLException ex) {
            putOnLog("Error loading schedules: " + ex.getMessage());
        }
    }

    private void clearFields() {
        Platform.runLater(() -> {
            idField.clear();
            nameField.clear();
            deptField.clear();
            posField.clear();
            personSelect.getSelectionModel().clearSelection();
            idField.setEditable(true);
            nameField.setEditable(true);
            deptField.setEditable(true);
            posField.setEditable(true);
            if (selectedTemplateView != null) selectedTemplateView.setImage(null);
            if (retakeTemplateBtn != null) retakeTemplateBtn.setDisable(true);
        });
        attendanceData.clear();
    }

    @FXML
    private void onRetakeTemplate() {
        Person sel = personSelect.getValue();
        if (sel == null) return;
        Mat roi = faceDetect.getFaceROI();
        if (roi != null) {
            try {
                byte[] tpl = encodeMat(roi);
                db.replaceFaceTemplate(sel.getPersonId(), tpl); // Implement this in Database.java
                loadTemplates(sel.getPersonId());
                putOnLog("Template retaken.");
                trainPersonModel(sel.getPersonId());
            } catch (Exception ex) {
                putOnLog("Retake error: " + ex.getMessage());
            }
        } else {
            putOnLog("No face detected to retake.");
        }
    }

    @FXML
    private void onDeleteTemplate() {
        Person sel = personSelect.getValue();
        if (sel == null || currentTemplateData == null) return;
        try {
            db.deleteFaceTemplate(sel.getPersonId(), currentTemplateData); // Implement this in Database.java
            loadTemplates(sel.getPersonId());
            putOnLog("Template deleted.");
        } catch (SQLException ex) {
            putOnLog("Delete error: " + ex.getMessage());
        }
    }

    @FXML
    private void onStartCamera(ActionEvent e) {
        if (faceDetect.start()) {
            putOnLog("Webcam started");
            startCameraBtn.setVisible(false);
            stopCameraBtn.setVisible(true);
            saveFaceBtn.setDisable(false);
            recognizeBtn.setDisable(false);
        } else {
            putOnLog("Failed to start webcam");
        }
    }

    @FXML
    private void onStopCamera(ActionEvent e) {
        faceDetect.stop();
        try { db.close(); } catch (Exception ignored) {}
        putOnLog("Webcam & DB closed");
        startCameraBtn.setVisible(true);
        stopCameraBtn.setVisible(false);
        saveFaceBtn.setDisable(true);
        recognizeBtn.setDisable(true);
        clearFields();
    }

    @FXML
    private void onSaveFace(ActionEvent e) {
        if (nameField.getText().trim().isEmpty()) return;
        pb.setVisible(true);
        new Thread(() -> {
            try {
                String code = idField.getText(), name = nameField.getText();
                String dept = deptField.getText(), pos = posField.getText();
                db.addPerson(code, name, dept, pos);

                int pid = db.getAllPersons().stream()
                           .filter(p->p.getPersonCode().equals(code))
                           .findFirst().get()
                           .getPersonId();

                File personDir = new File("resources/trained_faces/" + pid);
                if (!personDir.exists()) personDir.mkdirs();

                Mat roi = faceDetect.getFaceROI();
                if (roi != null) {
                    String filename = String.format("resources/trained_faces/%d/face_%d.png", pid, System.currentTimeMillis());
                    opencv_imgcodecs.imwrite(filename, roi);

                    byte[] tpl = encodeMat(roi);
                    db.addFaceTemplate(pid, tpl);
                    Platform.runLater(() ->
                        personSelect.getItems().add(new Person(pid,code,name,dept,pos))
                    );
                    putOnLog("Saved face for " + name);
                    trainPersonModel(pid);
                } else {
                    putOnLog("No face detected to save");
                }
            } catch (Exception ex) {
                putOnLog("Save‑face error: " + ex.getMessage());
            } finally {
                Platform.runLater(() -> {
                    pb.setVisible(false);
                    clearFields();
                });
            }
        }).start();
    }

    @FXML
    private void onRecognize(ActionEvent e) {
        Person sel = personSelect.getValue();
        if (sel == null) {
            putOnLog("No person selected to recognize."); 
            return;
        }
        Mat roi = faceDetect.getFaceROI();
        if (roi == null) {
            putOnLog("No face detected in frame."); 
            return;
        }

        try {
            File personDir = new File("resources/trained_faces/" + sel.getPersonId());
            if (!personDir.exists()) personDir.mkdirs();
            String loginImg = String.format("resources/trained_faces/%d/login_%d.png", sel.getPersonId(), System.currentTimeMillis());
            opencv_imgcodecs.imwrite(loginImg, roi);

            List<Person> allPersons = db.getAllPersons();
            List<Mat> mats = new ArrayList<>();
            List<Integer> labels = new ArrayList<>();
            for (Person p : allPersons) {
                File dir = new File("resources/trained_faces/" + p.getPersonId());
                File[] images = dir.listFiles((d, name) -> name.endsWith(".png"));
                if (images != null) {
                    for (File imgFile : images) {
                        Mat img = opencv_imgcodecs.imread(imgFile.getAbsolutePath(), opencv_imgcodecs.IMREAD_GRAYSCALE);
                        if (img != null && !img.empty()) {
                            mats.add(img);
                            labels.add(p.getPersonId());
                        }
                    }
                }
            }
            if (mats.isEmpty()) {
                putOnLog("No images found for any user.");
                return;
            }
            Mat[] matsArray = mats.toArray(new Mat[0]);
            int[] labelsArray = labels.stream().mapToInt(i -> i).toArray();
            faceRec.train(matsArray, labelsArray);
            faceRec.saveTrainedData();

            int predicted = faceRec.predict(roi);
            putOnLog("[LBPH] predicted=" + predicted + ", expected=" + sel.getPersonId());

            if (predicted == sel.getPersonId()) {
                String today = DayOfWeek.from(java.time.LocalDate.now()).name();
                LocalTime now = LocalTime.now();
                boolean inSchedule = false;
                String activity = "";
                for (Schedule s : db.getSchedulesForPerson(sel.getPersonId())) {
                    if (s.getDay().equalsIgnoreCase(today.substring(0,1) + today.substring(1).toLowerCase())) {
                        LocalTime start = parseTime(s.getStartTime());
                        LocalTime end = parseTime(s.getEndTime());
                        if (!now.isBefore(start) && !now.isAfter(end)) {
                            activity = s.getActivity();
                            inSchedule = true;
                            break;
                        }
                    }
                }
                if (!inSchedule) {
                    putOnLog("No active schedule for this user at this time.");
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "You do not have a schedule at this time.", ButtonType.OK);
                        alert.showAndWait();
                    });
                    return;
                }

                AttendanceRecord last = db.getLastAttendanceForToday(sel.getPersonId(), activity);

                String eventType;
                LocalDateTime nowDateTime = LocalDateTime.now();

                if (last == null) {
                    eventType = "Time In";
                } else if ("Time In".equals(last.getEventType())) {
                    if (java.time.Duration.between(last.getEventTime(), nowDateTime).toMinutes() < 10) {
                        putOnLog("You must wait at least 10 minutes before Time Out.");
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.WARNING, "You must wait at least 10 minutes before Time Out.", ButtonType.OK);
                            alert.showAndWait();
                        });
                        return;
                    }
                    eventType = "Time Out";
                } else {
                    eventType = "Time In";
                }

                if (last != null && last.getEventType().equals(eventType)) {
                    putOnLog("Already logged " + eventType + " for this activity today.");
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Already logged " + eventType + " for this activity today.", ButtonType.OK);
                        alert.showAndWait();
                    });
                    return;
                }

                db.logAttendance(sel.getPersonId(), eventType, "CAM1", 1.0, null, activity);
                putOnLog("Recognize ✔ matched " + sel.getFullName() + " (" + eventType + ")");
                loadAttendance(sel.getPersonId());
            } else {
                putOnLog("Recognize ✘ did NOT match");
            }
        } catch (Exception ex) {
            putOnLog("Recognition error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onRemovePerson(ActionEvent e) {
        Person sel = personSelect.getValue();
        if (sel == null) {
            putOnLog("No person selected to remove.");
            return;
        }
        try {
            db.deletePerson(sel.getPersonId());
            File personDir = new File("resources/trained_faces/" + sel.getPersonId());
            if (personDir.exists()) {
                for (File f : personDir.listFiles()) f.delete();
                personDir.delete();
            }
            personSelect.getItems().remove(sel);
            clearFields();
            putOnLog("Removed person: " + sel.getFullName());
        } catch (SQLException ex) {
            putOnLog("Failed to remove person: " + ex.getMessage());
        }
    }

    private byte[] encodeMat(Mat m) throws Exception {
        BytePointer buf = new BytePointer();
        opencv_imgcodecs.imencode(".png", m, buf);
        byte[] arr = new byte[(int)buf.limit()];
        buf.get(arr); buf.deallocate();
        return arr;
    }

    @FXML
    private void showAttendanceTable() {
        attendanceTable.setVisible(true);
        attendanceTable.setManaged(true);
        scheduleTable.setVisible(false);
        scheduleTable.setManaged(false);
        attendanceLabel.getStyleClass().setAll("switch-label-selected");
        scheduleLabel.getStyleClass().setAll("switch-label");
    }

    @FXML
    private void showScheduleTable() {
        attendanceTable.setVisible(false);
        attendanceTable.setManaged(false);
        scheduleTable.setVisible(true);
        scheduleTable.setManaged(true);
        attendanceLabel.getStyleClass().setAll("switch-label");
        scheduleLabel.getStyleClass().setAll("switch-label-selected");
    }

    private LocalTime parseTime(String timeStr) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");
        return LocalTime.parse(timeStr, fmt);
    }

    private void trainPersonModel(int personId) {
        File dir = new File("resources/trained_faces/" + personId);
        File[] images = dir.listFiles((d, name) -> name.endsWith(".png"));
        List<Mat> mats = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();
        if (images != null) {
            for (File imgFile : images) {
                Mat img = opencv_imgcodecs.imread(imgFile.getAbsolutePath(), opencv_imgcodecs.IMREAD_GRAYSCALE);
                if (img != null && !img.empty()) {
                    mats.add(img);
                    labels.add(personId);
                }
            }
        }
        if (!mats.isEmpty()) {
            Mat[] matsArray = mats.toArray(new Mat[0]);
            int[] labelsArray = labels.stream().mapToInt(i -> i).toArray();
            faceRec.train(matsArray, labelsArray);
            faceRec.saveTrainedData(); // No argument
        }
    }

}
