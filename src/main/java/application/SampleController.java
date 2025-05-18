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

import javafx.stage.Stage;

import application.AdvancedToast;

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
    @FXML private Button removeScheduleBtn;

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

    private Stage primaryStage;
    public void setPrimaryStage(Stage stage) { this.primaryStage = stage; }

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

        eventTypeColumn.setCellFactory(col -> new TableCell<AttendanceRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("Time In")) {
                        setStyle("-fx-text-fill: #43a047; -fx-font-weight: bold;"); // Green
                    } else if (item.equalsIgnoreCase("Time Out")) {
                        setStyle("-fx-text-fill: #e53935; -fx-font-weight: bold;"); // Red
                    } else {
                        setStyle("-fx-text-fill: #2c3e50;");
                    }
                }
            }
        });
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
                AdvancedToast.show(primaryStage, "Schedule added successfully!", AdvancedToast.ToastType.SUCCESS);
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
        // Reconnect to DB if needed
        if (db == null) {
            try {
                db = new Database();
                putOnLog("DB reconnected");
            } catch (SQLException ex) {
                putOnLog("DB reconnection failed: " + ex.getMessage());
                AdvancedToast.show(primaryStage, "DB reconnection failed: " + ex.getMessage(), AdvancedToast.ToastType.ERROR);
                return;
            }
        } else {
            try {
                if (db.isClosed()) {
                    db = new Database();
                    putOnLog("DB reconnected");
                }
            } catch (SQLException ex) {
                putOnLog("DB reconnection failed: " + ex.getMessage());
                AdvancedToast.show(primaryStage, "DB reconnection failed: " + ex.getMessage(), AdvancedToast.ToastType.ERROR);
                return;
            }
        }

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
                    Mat processed = preprocessFace(roi);
                    String filename = String.format("resources/trained_faces/%d/face_%d.png", pid, System.currentTimeMillis());
                    opencv_imgcodecs.imwrite(filename, processed);
                    byte[] tpl = encodeMat(processed);
                    db.addFaceTemplate(pid, tpl);
                    Platform.runLater(() ->
                        personSelect.getItems().add(new Person(pid,code,name,dept,pos))
                    );
                    putOnLog("Saved face for " + name);
                    trainPersonModel(pid);
                    AdvancedToast.show(primaryStage, "Face saved for " + name + "!", AdvancedToast.ToastType.SUCCESS);
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
            AdvancedToast.show(primaryStage, "No person selected to recognize.", AdvancedToast.ToastType.ERROR);
            return;
        }
        Mat roi = faceDetect.getFaceROI();
        if (roi == null || roi.empty()) {
            putOnLog("No face detected in frame.");
            AdvancedToast.show(primaryStage, "No face detected in frame.", AdvancedToast.ToastType.ERROR);
            return;
        }

        // --- Basic liveness detection: require face to move/change between frames ---
        long now = System.currentTimeMillis();
        boolean livenessPassed = false;
        if (lastFaceROI != null && (now - lastFaceTime) < LIVENESS_MAX_INTERVAL_MS) {
            double diff = faceDifference(lastFaceROI, roi);
            if (diff > LIVENESS_MIN_DIFF) {
                livenessPassed = true;
            }
        }
        lastFaceROI = roi.clone();
        lastFaceTime = now;
        if (!livenessPassed) {
            putOnLog("Liveness check: Please blink or move your face.");
            AdvancedToast.show(primaryStage, "Liveness check: Please blink or move your face.", AdvancedToast.ToastType.WARNING);
            return;
        }

        try {
            // Save the captured face for audit/debug
            File personDir = new File("resources/trained_faces/" + sel.getPersonId());
            if (!personDir.exists()) personDir.mkdirs();
            String loginImg = String.format("resources/trained_faces/%d/login_%d.png", sel.getPersonId(), System.currentTimeMillis());
            opencv_imgcodecs.imwrite(loginImg, roi);

            // Prepare training data for all persons
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
                AdvancedToast.show(primaryStage, "No face data available for recognition.", AdvancedToast.ToastType.ERROR);
                return;
            }

            // Predict and get confidence
            Mat processed = preprocessFace(roi);
            faceRec.loadTrainedData(); // Make sure model is loaded
            org.bytedeco.javacpp.IntPointer lbl = new org.bytedeco.javacpp.IntPointer(1);
            org.bytedeco.javacpp.DoublePointer conf = new org.bytedeco.javacpp.DoublePointer(1);
            faceRec.getRecognizer().predict(processed, lbl, conf);
            int predicted = lbl.get(0);
            double confidence = conf.get(0);

            putOnLog("[LBPH] predicted=" + predicted + ", expected=" + sel.getPersonId() + ", confidence=" + confidence);

            // Set a stricter threshold for acceptance (e.g., < 55.0)
            double strictThreshold = 70.0;
            if (predicted != sel.getPersonId()) {
                putOnLog("Face does not match the selected person.");
                AdvancedToast.show(primaryStage, "Face does not match the selected person.", AdvancedToast.ToastType.ERROR);
                return;
            }
            if (confidence > strictThreshold) {
                putOnLog("Face not recognized with high enough confidence (" + String.format("%.2f", confidence) + ").");
                AdvancedToast.show(primaryStage, "Face not recognized. Please try again.", AdvancedToast.ToastType.ERROR);
                return;
            }

            // Optionally: Add a check for face size/position to reject photos/screens (basic liveness)
            if (roi.cols() < 100 || roi.rows() < 100) {
                putOnLog("Face too small or not clear.");
                AdvancedToast.show(primaryStage, "Face too small or not clear. Move closer.", AdvancedToast.ToastType.WARNING);
                return;
            }

            // Proceed with attendance logic as before
            String today = DayOfWeek.from(java.time.LocalDate.now()).name();
            LocalTime nowTime = LocalTime.now();
            boolean inSchedule = false;
            String activity = "";
            for (Schedule s : db.getSchedulesForPerson(sel.getPersonId())) {
                if (s.getDay().equalsIgnoreCase(today.substring(0,1) + today.substring(1).toLowerCase())) {
                    LocalTime start = parseTime(s.getStartTime());
                    LocalTime end = parseTime(s.getEndTime());
                    if (!nowTime.isBefore(start) && !nowTime.isAfter(end)) {
                        activity = s.getActivity();
                        inSchedule = true;
                        break;
                    }
                }
            }
            if (!inSchedule) {
                putOnLog("No active schedule for this user at this time.");
                AdvancedToast.show(primaryStage, "No active schedule for this user at this time.", AdvancedToast.ToastType.WARNING);
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
                    AdvancedToast.show(primaryStage, "You must wait at least 10 minutes before Time Out.", AdvancedToast.ToastType.WARNING);
                    return;
                }
                eventType = "Time Out";
            } else {
                eventType = "Time In";
            }

            if (last != null && last.getEventType().equals(eventType)) {
                putOnLog("Already logged " + eventType + " for this activity today.");
                AdvancedToast.show(primaryStage, "Already logged " + eventType + " for this activity today.", AdvancedToast.ToastType.WARNING);
                return;
            }

            db.logAttendance(sel.getPersonId(), eventType, "CAM1", 1.0, null, activity);
            putOnLog("Recognize ✔ matched " + sel.getFullName() + " (" + eventType + ")");
            loadAttendance(sel.getPersonId());
            AdvancedToast.show(primaryStage, "Welcome, " + sel.getFullName() + "! " + eventType + " recorded.", AdvancedToast.ToastType.SUCCESS);

        } catch (Exception ex) {
            putOnLog("Recognition error: " + ex.getMessage());
            AdvancedToast.show(primaryStage, "Recognition error: " + ex.getMessage(), AdvancedToast.ToastType.ERROR);
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
                    Mat processed = preprocessFace(img); // <--- preprocess for training!
                    mats.add(processed);
                    labels.add(personId);
                }
            }
        }
        if (!mats.isEmpty()) {
            Mat[] matsArray = mats.toArray(new Mat[0]);
            int[] labelsArray = labels.stream().mapToInt(i -> i).toArray();
            faceRec.train(matsArray, labelsArray);
            faceRec.saveTrainedData();
        }
    }

    private Mat lastFaceROI = null;
    private long lastFaceTime = 0;
    private static final double LIVENESS_MIN_DIFF = 1200.0; // tune as needed
    private static final long LIVENESS_MAX_INTERVAL_MS = 3000; // 3 seconds

    /** Returns the sum of absolute differences between two Mats (grayscale, same size). */
    private double faceDifference(Mat a, Mat b) {
        if (a == null || b == null || a.size().width() != b.size().width() || a.size().height() != b.size().height())
            return Double.MAX_VALUE;
        Mat diff = new Mat();
        org.bytedeco.opencv.global.opencv_core.absdiff(a, b, diff);
        return org.bytedeco.opencv.global.opencv_core.sumElems(diff).get(0);
    }

    private static final int FACE_WIDTH = 100;
    private static final int FACE_HEIGHT = 100;

    private Mat preprocessFace(Mat face) {
        Mat gray = new Mat();
        if (face.channels() > 1)
            org.bytedeco.opencv.global.opencv_imgproc.cvtColor(face, gray, org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY);
        else
            gray = face.clone();
        Mat resized = new Mat();
        org.bytedeco.opencv.global.opencv_imgproc.resize(gray, resized, new org.bytedeco.opencv.opencv_core.Size(FACE_WIDTH, FACE_HEIGHT));
        return resized;
    }

    @FXML
    private void onRemoveSchedule(ActionEvent event) {
        Person sel = personSelect.getValue();
        Schedule selectedSchedule = scheduleTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AdvancedToast.show(primaryStage, "Select a person first.", AdvancedToast.ToastType.WARNING);
            return;
        }
        if (selectedSchedule == null) {
            AdvancedToast.show(primaryStage, "Select a schedule to remove.", AdvancedToast.ToastType.WARNING);
            return;
        }
        try {
            db.removeScheduleForPerson(sel.getPersonId(), selectedSchedule);
            schedules.remove(selectedSchedule); // Remove from observable list
            scheduleTable.refresh();
            AdvancedToast.show(primaryStage, "Schedule removed.", AdvancedToast.ToastType.SUCCESS);
        } catch (Exception ex) {
            AdvancedToast.show(primaryStage, "Failed to remove schedule: " + ex.getMessage(), AdvancedToast.ToastType.ERROR);
        }
    }

}
