package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.sql.Timestamp;     // ← added for event_time

public class Database {
    private static final String URL  =
        "jdbc:mysql://localhost:3306/facial_attendance?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    private Connection conn;

    public Database() throws SQLException {
        conn = DriverManager.getConnection(URL, USER, PASS);
    }

    // 1) PERSONS table methods
    public void addPerson(String personCode, String fullName,
                          String department, String position) throws SQLException {
        String sql = "INSERT INTO persons "
                   + "(person_code, full_name, department, position) "
                   + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, personCode);
            ps.setString(2, fullName);
            ps.setString(3, department);
            ps.setString(4, position);
            ps.executeUpdate();
        }
    }

    public List<Person> getAllPersons() throws SQLException {
        String sql =
           "SELECT person_id, person_code, full_name, department, position "
         + "FROM persons";
        List<Person> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet  rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Person(
                    rs.getInt("person_id"),
                    rs.getString("person_code"),
                    rs.getString("full_name"),
                    rs.getString("department"),
                    rs.getString("position")
                ));
            }
        }
        return list;
    }

    // 2) FACE_TEMPLATES table methods
    public void addFaceTemplate(int personId, byte[] data) throws SQLException {
        String sql =
          "INSERT INTO face_templates (person_id, template_data) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setBytes(2, data);
            ps.executeUpdate();
        }
    }

    public List<byte[]> getTemplatesByPerson(int personId) throws SQLException {
        String sql =
          "SELECT template_data FROM face_templates WHERE person_id = ?";
        List<byte[]> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getBytes("template_data"));
                }
            }
        }
        return list;
    }

    // Replace all templates for a person with a new one
    public void replaceFaceTemplate(int personId, byte[] newTemplate) throws SQLException {
        // Delete all templates for this person
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM face_templates WHERE person_id = ?")) {
            ps.setInt(1, personId);
            ps.executeUpdate();
        }
        // Add the new template
        addFaceTemplate(personId, newTemplate);
    }

    // Delete a specific template for a person (by data)
    public void deleteFaceTemplate(int personId, byte[] templateData) throws SQLException {
        String sql = "DELETE FROM face_templates WHERE person_id = ? AND template_data = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setBytes(2, templateData);
            ps.executeUpdate();
        }
    }

    // 3) ATTENDANCE_LOGS table methods
    public void logAttendance(int personId, String type, String cameraId, double confidence, byte[] snapshot, String activity) throws SQLException {
        String sql =
          "INSERT INTO attendance_logs "
        + "(person_id, event_time, event_type, camera_id, confidence, snapshot, activity) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, type);
            ps.setString(4, cameraId);
            ps.setDouble(5, confidence);
            ps.setBytes(6, snapshot);
            ps.setString(7, activity);
            ps.executeUpdate();
        }
    }

    public List<AttendanceRecord> getAttendanceForPerson(int personId) throws SQLException {
        String sql =
          "SELECT al.event_time, p.full_name, al.event_type, al.activity "
        + "FROM attendance_logs al "
        + "JOIN persons p ON al.person_id = p.person_id "
        + "WHERE al.person_id = ? "
        + "ORDER BY al.event_time DESC";

        List<AttendanceRecord> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime ts = rs.getTimestamp("event_time").toLocalDateTime();
                    String full = rs.getString("full_name");
                    String type = rs.getString("event_type");
                    String activity = rs.getString("activity");
                    list.add(new AttendanceRecord(ts, full, type, activity));
                }
            }
        }
        return list;
    }

    public AttendanceRecord getLastAttendanceForToday(int personId, String activity) throws SQLException {
        String sql =
          "SELECT al.event_time, p.full_name, al.event_type, al.activity "
        + "FROM attendance_logs al "
        + "JOIN persons p ON al.person_id = p.person_id "
        + "WHERE al.person_id = ? AND al.activity = ? AND DATE(al.event_time) = CURDATE() "
        + "ORDER BY al.event_time DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setString(2, activity);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime ts = rs.getTimestamp("event_time").toLocalDateTime();
                    String full = rs.getString("full_name");
                    String type = rs.getString("event_type");
                    String act = rs.getString("activity");
                    return new AttendanceRecord(ts, full, type, act);
                }
            }
        }
        return null;
    }

    /** Deletes a person, all their face templates, and attendance logs. */
    public void deletePerson(int personId) throws SQLException {
        // 1) delete templates
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM face_templates WHERE person_id = ?")) {
            ps.setInt(1, personId);
            ps.executeUpdate();
        }
        // 2) delete logs (optional, but keeps DB clean)
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM attendance_logs WHERE person_id = ?")) {
            ps.setInt(1, personId);
            ps.executeUpdate();
        }
        // 3) delete the person record
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM persons WHERE person_id = ?")) {
            ps.setInt(1, personId);
            ps.executeUpdate();
        }
    }

    // Add a schedule for a person
    public void addScheduleForPerson(int personId, Schedule schedule) throws SQLException {
        String sql = "INSERT INTO schedules (person_id, day, start_time, end_time, activity) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setString(2, schedule.getDay());
            ps.setString(3, schedule.getStartTime());
            ps.setString(4, schedule.getEndTime());
            ps.setString(5, schedule.getActivity());
            ps.executeUpdate();
        }
    }

    // Get all schedules for a person
    public List<Schedule> getSchedulesForPerson(int personId) throws SQLException {
        String sql = "SELECT day, start_time, end_time, activity FROM schedules WHERE person_id = ?";
        List<Schedule> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Schedule(
                        rs.getString("day"),
                        rs.getString("start_time"),
                        rs.getString("end_time"),
                        rs.getString("activity")
                    ));
                }
            }
        }
        return list;
    }

    public void removeScheduleForPerson(int personId, Schedule schedule) throws SQLException {
        String sql = "DELETE FROM schedules WHERE person_id = ? AND day = ? AND start_time = ? AND end_time = ? AND activity = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setString(2, schedule.getDay());
            ps.setString(3, schedule.getStartTime());
            ps.setString(4, schedule.getEndTime());
            ps.setString(5, schedule.getActivity());
            ps.executeUpdate();
        }
    }

    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    public boolean isClosed() throws SQLException {
        return conn == null || conn.isClosed();
    }
}
