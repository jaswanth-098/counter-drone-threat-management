import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DatabaseManager.java
 *
 * SQLite JDBC persistence engine for the Counter-Drone Threat Management System.
 * Manages database file lifecycle, schema migration, foreign key enforcement,
 * transactional atomic operations, and duplicate suppression policies.
 */
public class DatabaseManager {

    private static final String DB_DIR = "data";
    private static final String DB_FILE = "counterdrone.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + File.separator + DB_FILE;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static DatabaseManager instance;

    /**
     * Singleton accessor for centralized database connectivity.
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private DatabaseManager() {
        initializeDatabase();
    }

    /**
     * Obtains an active connection to the SQLite database with foreign keys enabled.
     */
    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC Driver not found in classpath. Ensure lib/sqlite-jdbc-*.jar is included.", e);
        }
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Verifies that the SQLite database is reachable and operational.
     */
    public boolean isDatabaseReady() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Initializes the SQLite database file and creates all required tables automatically.
     */
    public void initializeDatabase() {
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println("[WARN] Could not create database directory: " + dir.getAbsolutePath());
            }
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Detection Events Table
            stmt.execute("CREATE TABLE IF NOT EXISTS detection_events (" +
                    "event_id TEXT PRIMARY KEY, " +
                    "zone TEXT NOT NULL, " +
                    "distance_meters REAL NOT NULL, " +
                    "speed_kmh REAL NOT NULL, " +
                    "repeat_count INTEGER NOT NULL, " +
                    "authorization_status TEXT NOT NULL, " +
                    "created_at TEXT NOT NULL, " +
                    "updated_at TEXT NOT NULL" +
                    ");");

            // 2. Risk Assessments Table
            stmt.execute("CREATE TABLE IF NOT EXISTS risk_assessments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "event_id TEXT NOT NULL, " +
                    "score INTEGER NOT NULL, " +
                    "risk_category TEXT NOT NULL, " +
                    "triggered_factors TEXT NOT NULL, " +
                    "explanations TEXT NOT NULL, " +
                    "non_triggered_explanations TEXT, " +
                    "summary TEXT, " +
                    "assessment_timestamp TEXT NOT NULL, " +
                    "FOREIGN KEY (event_id) REFERENCES detection_events(event_id) ON DELETE CASCADE" +
                    ");");

            // 3. Alerts Table
            stmt.execute("CREATE TABLE IF NOT EXISTS alerts (" +
                    "alert_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "event_id TEXT NOT NULL, " +
                    "zone TEXT NOT NULL, " +
                    "priority_score INTEGER NOT NULL, " +
                    "risk_category TEXT NOT NULL, " +
                    "prioritization_reason TEXT NOT NULL, " +
                    "created_at TEXT NOT NULL, " +
                    "is_active INTEGER DEFAULT 1, " +
                    "FOREIGN KEY (event_id) REFERENCES detection_events(event_id) ON DELETE CASCADE" +
                    ");");

            // 4. Incidents Table
            stmt.execute("CREATE TABLE IF NOT EXISTS incidents (" +
                    "incident_id TEXT PRIMARY KEY, " +
                    "event_id TEXT NOT NULL, " +
                    "status TEXT NOT NULL CHECK(status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED')), " +
                    "assigned_to TEXT NOT NULL, " +
                    "notes TEXT, " +
                    "created_at TEXT NOT NULL, " +
                    "updated_at TEXT NOT NULL, " +
                    "FOREIGN KEY (event_id) REFERENCES detection_events(event_id)" +
                    ");");

            // 5. Rule Configurations Table
            stmt.execute("CREATE TABLE IF NOT EXISTS rule_configurations (" +
                    "config_key TEXT PRIMARY KEY, " +
                    "config_value REAL NOT NULL, " +
                    "description TEXT" +
                    ");");

            // 6. Evaluation Results Table
            stmt.execute("CREATE TABLE IF NOT EXISTS evaluation_results (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "run_timestamp TEXT NOT NULL, " +
                    "dataset_size INTEGER NOT NULL, " +
                    "accuracy REAL NOT NULL, " +
                    "precision_score REAL NOT NULL, " +
                    "recall_score REAL NOT NULL, " +
                    "f1_score REAL NOT NULL, " +
                    "summary_notes TEXT NOT NULL" +
                    ");");

            // Initialize default rule configuration if table is empty
            initializeDefaultRules(conn);

            // Populate initial demo seed events if detection_events is empty
            seedInitialDemoDataIfEmpty(conn);

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to initialize SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Seeds initial default rule parameters if not present.
     */
    private void initializeDefaultRules(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM rule_configurations";
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO rule_configurations (config_key, config_value, description) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    insertRuleParam(ps, "PROXIMITY_THRESHOLD", RuleConfiguration.DEFAULT_PROXIMITY_THRESHOLD, "Proximity horizon breach threshold (meters)");
                    insertRuleParam(ps, "PROXIMITY_WEIGHT", RuleConfiguration.DEFAULT_PROXIMITY_WEIGHT, "Score penalty for proximity breach");
                    insertRuleParam(ps, "SPEED_THRESHOLD", RuleConfiguration.DEFAULT_SPEED_THRESHOLD, "High velocity threshold (km/h)");
                    insertRuleParam(ps, "SPEED_WEIGHT", RuleConfiguration.DEFAULT_SPEED_WEIGHT, "Score penalty for high velocity");
                    insertRuleParam(ps, "REPETITION_THRESHOLD", RuleConfiguration.DEFAULT_REPETITION_THRESHOLD, "Repeated radar burst confirmation threshold");
                    insertRuleParam(ps, "REPETITION_WEIGHT", RuleConfiguration.DEFAULT_REPETITION_WEIGHT, "Score penalty for repeated incursion");
                    insertRuleParam(ps, "UNAUTHORIZED_WEIGHT", RuleConfiguration.DEFAULT_UNAUTHORIZED_WEIGHT, "Score penalty for unauthorized IFF transponder");
                }
            }
        }
    }

    private void insertRuleParam(PreparedStatement ps, String key, double value, String desc) throws SQLException {
        ps.setString(1, key);
        ps.setDouble(2, value);
        ps.setString(3, desc);
        ps.executeUpdate();
    }

    /**
     * Populates representative initial events to facilitate immediate faculty demonstration.
     */
    private void seedInitialDemoDataIfEmpty(Connection conn) {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM detection_events")) {
            if (rs.next() && rs.getInt(1) == 0) {
                RiskScoringService scoring = new RiskScoringService();

                // Demo Event 1: Critical Rogue Incursion
                DetectionEvent e1 = new DetectionEvent("EVT-1001", "Zone-Alpha (HQ Command)", 180.0, 135.0, 4, DetectionEvent.AUTH_UNAUTHORIZED);
                RiskAssessment a1 = scoring.evaluateRisk(e1);
                saveEventAndAssessmentInternal(conn, e1, a1, AlertManager.generatePrioritizationReason(a1.getScore(), a1.getRiskCategory(), e1));

                // Demo Event 2: High Threat Fast Contact
                DetectionEvent e2 = new DetectionEvent("EVT-1002", "Zone-Bravo (Runway North)", 250.0, 110.0, 1, DetectionEvent.AUTH_UNKNOWN);
                RiskAssessment a2 = scoring.evaluateRisk(e2);
                saveEventAndAssessmentInternal(conn, e2, a2, AlertManager.generatePrioritizationReason(a2.getScore(), a2.getRiskCategory(), e2));

                // Demo Event 3: Medium Loitering Intruder
                DetectionEvent e3 = new DetectionEvent("EVT-1003", "Zone-Charlie (Fuel Depot)", 220.0, 45.0, 1, DetectionEvent.AUTH_AUTHORIZED);
                RiskAssessment a3 = scoring.evaluateRisk(e3);
                saveEventAndAssessmentInternal(conn, e3, a3, AlertManager.generatePrioritizationReason(a3.getScore(), a3.getRiskCategory(), e3));

                // Demo Event 4: Low Threat Friendly Survey
                DetectionEvent e4 = new DetectionEvent("EVT-1004", "Zone-Delta (Perimeter West)", 850.0, 35.0, 1, DetectionEvent.AUTH_AUTHORIZED);
                RiskAssessment a4 = scoring.evaluateRisk(e4);
                saveEventAndAssessmentInternal(conn, e4, a4, AlertManager.generatePrioritizationReason(a4.getScore(), a4.getRiskCategory(), e4));
            }
        } catch (SQLException ex) {
            System.err.println("[WARN] Initial seed data setup error: " + ex.getMessage());
        }
    }

    /**
     * Saves detection event, risk assessment, alert, and auto-incident inside an atomic transaction.
     * Enforces the duplicate-event update policy and duplicate-incident prevention policy.
     *
     * @param event The detection event
     * @param assessment The calculated explainable risk assessment
     * @param prioritizationReason Rationale string for alert ordering
     * @return true if committed successfully, false if rolled back
     */
    public boolean saveEventAndAssessment(DetectionEvent event, RiskAssessment assessment, String prioritizationReason) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean result = saveEventAndAssessmentInternal(conn, event, assessment, prioritizationReason);
                conn.commit();
                return result;
            } catch (SQLException ex) {
                conn.rollback();
                System.err.println("[ERROR] Transaction rolled back during saveEventAndAssessment: " + ex.getMessage());
                ex.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Database connection failure: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean saveEventAndAssessmentInternal(Connection conn,
                                                   DetectionEvent event,
                                                   RiskAssessment assessment,
                                                   String prioritizationReason) throws SQLException {
        String now = LocalDateTime.now().format(FORMATTER);

        // 1. Check if Event ID already exists (Duplicate event handling)
        boolean eventExists = false;
        String checkSql = "SELECT created_at FROM detection_events WHERE event_id = ?";
        String originalCreatedAt = now;
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, event.getEventId());
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    eventExists = true;
                    originalCreatedAt = rs.getString("created_at");
                }
            }
        }

        if (eventExists) {
            // Update existing event record
            String updateSql = "UPDATE detection_events SET zone = ?, distance_meters = ?, speed_kmh = ?, " +
                    "repeat_count = ?, authorization_status = ?, updated_at = ? WHERE event_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, event.getZone());
                ps.setDouble(2, event.getDistanceMeters());
                ps.setDouble(3, event.getSpeedKmh());
                ps.setInt(4, event.getRepeatCount());
                ps.setString(5, event.getAuthorizationStatus());
                ps.setString(6, now);
                ps.setString(7, event.getEventId());
                ps.executeUpdate();
            }
        } else {
            // Insert brand new event
            String insertSql = "INSERT INTO detection_events (event_id, zone, distance_meters, speed_kmh, " +
                    "repeat_count, authorization_status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, event.getEventId());
                ps.setString(2, event.getZone());
                ps.setDouble(3, event.getDistanceMeters());
                ps.setDouble(4, event.getSpeedKmh());
                ps.setInt(5, event.getRepeatCount());
                ps.setString(6, event.getAuthorizationStatus());
                ps.setString(7, originalCreatedAt);
                ps.setString(8, now);
                ps.executeUpdate();
            }
        }

        // 2. Insert Risk Assessment record
        String assessSql = "INSERT INTO risk_assessments (event_id, score, risk_category, triggered_factors, " +
                "explanations, non_triggered_explanations, summary, assessment_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(assessSql)) {
            ps.setString(1, event.getEventId());
            ps.setInt(2, assessment.getScore());
            ps.setString(3, assessment.getRiskCategory());
            ps.setString(4, String.join(";;", assessment.getTriggeredFactors()));
            ps.setString(5, String.join(";;", assessment.getExplanations()));
            ps.setString(6, String.join(";;", assessment.getNonTriggeredExplanations()));
            ps.setString(7, assessment.getHumanReadableSummary());
            ps.setString(8, now);
            ps.executeUpdate();
        }

        // 3. Upsert Alert record (Duplicate alert policy: 1 active alert per event ID)
        String alertCheckSql = "SELECT alert_id FROM alerts WHERE event_id = ? AND is_active = 1";
        int existingAlertId = -1;
        try (PreparedStatement ps = conn.prepareStatement(alertCheckSql)) {
            ps.setString(1, event.getEventId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    existingAlertId = rs.getInt("alert_id");
                }
            }
        }

        if (existingAlertId != -1) {
            String updateAlertSql = "UPDATE alerts SET zone = ?, priority_score = ?, risk_category = ?, " +
                    "prioritization_reason = ?, created_at = ? WHERE alert_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateAlertSql)) {
                ps.setString(1, event.getZone());
                ps.setInt(2, assessment.getScore());
                ps.setString(3, assessment.getRiskCategory());
                ps.setString(4, prioritizationReason);
                ps.setString(5, now);
                ps.setInt(6, existingAlertId);
                ps.executeUpdate();
            }
        } else {
            String insertAlertSql = "INSERT INTO alerts (event_id, zone, priority_score, risk_category, " +
                    "prioritization_reason, created_at, is_active) VALUES (?, ?, ?, ?, ?, ?, 1)";
            try (PreparedStatement ps = conn.prepareStatement(insertAlertSql)) {
                ps.setString(1, event.getEventId());
                ps.setString(2, event.getZone());
                ps.setInt(3, assessment.getScore());
                ps.setString(4, assessment.getRiskCategory());
                ps.setString(5, prioritizationReason);
                ps.setString(6, now);
                ps.executeUpdate();
            }
        }

        // 4. Incident Management Auto-Triage Policy:
        // Automatically create an operational incident for HIGH or CRITICAL assessments (Score >= 50)
        if (assessment.getScore() >= 50) {
            handleIncidentTriagePolicy(conn, event, assessment, now);
        }

        return true;
    }

    /**
     * Enforces the incident triage and duplicate-incident prevention policy:
     * - Prevents duplicate unresolved incidents (OPEN or IN_PROGRESS) for the same event ID.
     * - Appends an audit note if re-detected while an unresolved incident is active.
     * - If prior incident was RESOLVED, permits creation of a new follow-up incident.
     */
    private void handleIncidentTriagePolicy(Connection conn,
                                            DetectionEvent event,
                                            RiskAssessment assessment,
                                            String now) throws SQLException {
        String queryUnresolved = "SELECT incident_id, notes FROM incidents WHERE event_id = ? AND status IN ('OPEN', 'IN_PROGRESS')";
        try (PreparedStatement ps = conn.prepareStatement(queryUnresolved)) {
            ps.setString(1, event.getEventId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Unresolved incident already exists! Suppress duplicate incident creation and append log note
                    String existingIncId = rs.getString("incident_id");
                    String currentNotes = rs.getString("notes");
                    String appendNote = currentNotes + " | [" + now + "] Re-detected: Score " + assessment.getScore() +
                            " (" + assessment.getRiskCategory() + "). Duplicate incident suppressed.";
                    String updateIncSql = "UPDATE incidents SET notes = ?, updated_at = ? WHERE incident_id = ?";
                    try (PreparedStatement updatePs = conn.prepareStatement(updateIncSql)) {
                        updatePs.setString(1, appendNote);
                        updatePs.setString(2, now);
                        updatePs.setString(3, existingIncId);
                        updatePs.executeUpdate();
                    }
                    return;
                }
            }
        }

        // No unresolved incident exists! Generate new incident
        String newIncId = "INC-" + System.currentTimeMillis() % 1000000;
        String assignedTeam = assessment.getScore() >= 75 ? "Air Defense Reaction Unit" : "Radar Watch Team";
        String notes = "Auto-triaged " + assessment.getRiskCategory() + " threat (Score: " + assessment.getScore() +
                ") in " + event.getZone() + ". Triggered factors: " + String.join(", ", assessment.getTriggeredFactors());

        String insertIncSql = "INSERT INTO incidents (incident_id, event_id, status, assigned_to, notes, created_at, updated_at) " +
                "VALUES (?, ?, 'OPEN', ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertIncSql)) {
            ps.setString(1, newIncId);
            ps.setString(2, event.getEventId());
            ps.setString(3, assignedTeam);
            ps.setString(4, notes);
            ps.setString(5, now);
            ps.setString(6, now);
            ps.executeUpdate();
        }
    }

    /**
     * Checks whether an unresolved incident (OPEN or IN_PROGRESS) exists for a given event ID.
     */
    public boolean hasUnresolvedIncident(String eventId) {
        String sql = "SELECT COUNT(*) FROM incidents WHERE event_id = ? AND status IN ('OPEN', 'IN_PROGRESS')";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Creates a manual incident with validation against duplicate unresolved incidents.
     */
    public boolean createIncident(Incident incident) throws SQLException {
        if (hasUnresolvedIncident(incident.getEventId())) {
            throw new SQLException("An unresolved incident (OPEN or IN_PROGRESS) already exists for Event ID: " +
                    incident.getEventId() + ". Duplicate incident suppressed under security triage policy.");
        }

        String sql = "INSERT INTO incidents (incident_id, event_id, status, assigned_to, notes, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, incident.getIncidentId());
            ps.setString(2, incident.getEventId());
            ps.setString(3, incident.getStatus());
            ps.setString(4, incident.getAssignedTo());
            ps.setString(5, incident.getNotes());
            ps.setString(6, incident.getCreatedAt());
            ps.setString(7, incident.getUpdatedAt());
            int affected = ps.executeUpdate();
            return affected > 0;
        }
    }

    /**
     * Updates an existing incident's status, assignment, and notes.
     */
    public boolean updateIncident(String incidentId, String status, String assignedTo, String notes) {
        String sql = "UPDATE incidents SET status = ?, assigned_to = ?, notes = ?, updated_at = ? WHERE incident_id = ?";
        String now = LocalDateTime.now().format(FORMATTER);
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, assignedTo);
            ps.setString(3, notes);
            ps.setString(4, now);
            ps.setString(5, incidentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to update incident: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves all recorded detection events ordered by latest update.
     */
    public List<DetectionEvent> getAllDetectionEvents() {
        List<DetectionEvent> list = new ArrayList<>();
        String sql = "SELECT * FROM detection_events ORDER BY updated_at DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                DetectionEvent ev = new DetectionEvent(
                        rs.getString("event_id"),
                        rs.getString("zone"),
                        rs.getDouble("distance_meters"),
                        rs.getDouble("speed_kmh"),
                        rs.getInt("repeat_count"),
                        rs.getString("authorization_status"),
                        rs.getString("created_at"),
                        rs.getString("updated_at")
                );
                list.add(ev);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves a single detection event by its unique ID.
     */
    public DetectionEvent getEventById(String eventId) {
        String sql = "SELECT * FROM detection_events WHERE event_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new DetectionEvent(
                            rs.getString("event_id"),
                            rs.getString("zone"),
                            rs.getDouble("distance_meters"),
                            rs.getDouble("speed_kmh"),
                            rs.getInt("repeat_count"),
                            rs.getString("authorization_status"),
                            rs.getString("created_at"),
                            rs.getString("updated_at")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves the latest risk assessment for a specific event ID.
     */
    public RiskAssessment getLatestAssessment(String eventId) {
        String sql = "SELECT * FROM risk_assessments WHERE event_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String tfStr = rs.getString("triggered_factors");
                    String expStr = rs.getString("explanations");
                    String nonTfStr = rs.getString("non_triggered_explanations");

                    List<String> tf = (tfStr != null && !tfStr.isEmpty()) ? Arrays.asList(tfStr.split(";;")) : new ArrayList<>();
                    List<String> exp = (expStr != null && !expStr.isEmpty()) ? Arrays.asList(expStr.split(";;")) : new ArrayList<>();
                    List<String> ntf = (nonTfStr != null && !nonTfStr.isEmpty()) ? Arrays.asList(nonTfStr.split(";;")) : new ArrayList<>();

                    return new RiskAssessment(
                            rs.getInt("id"),
                            rs.getString("event_id"),
                            rs.getInt("score"),
                            rs.getString("risk_category"),
                            tf,
                            exp,
                            ntf,
                            rs.getString("summary"),
                            rs.getString("assessment_timestamp")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves all active alerts from the database.
     */
    public List<AlertItem> getAllAlerts() {
        List<AlertItem> list = new ArrayList<>();
        String sql = "SELECT a.alert_id, a.event_id, a.zone, a.priority_score, a.risk_category, " +
                "a.prioritization_reason, a.created_at, a.is_active, " +
                "COALESCE(e.distance_meters, 0.0) as distance_meters, " +
                "COALESCE(e.speed_kmh, 0.0) as speed_kmh, " +
                "COALESCE(e.authorization_status, 'UNKNOWN') as authorization_status " +
                "FROM alerts a " +
                "LEFT JOIN detection_events e ON a.event_id = e.event_id " +
                "WHERE a.is_active = 1 " +
                "ORDER BY a.priority_score DESC, a.created_at DESC";
        try (Connection conn = getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                AlertItem item = new AlertItem(
                        rs.getInt("alert_id"),
                        rs.getString("event_id"),
                        rs.getString("zone"),
                        rs.getInt("priority_score"),
                        rs.getString("risk_category"),
                        rs.getString("prioritization_reason"),
                        rs.getString("created_at"),
                        rs.getInt("is_active") == 1,
                        rs.getDouble("distance_meters"),
                        rs.getDouble("speed_kmh"),
                        rs.getString("authorization_status")
                );
                list.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves all incidents from the database.
     */
    public List<Incident> getAllIncidents() {
        List<Incident> list = new ArrayList<>();
        String sql = "SELECT * FROM incidents ORDER BY updated_at DESC";
        try (Connection conn = getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Incident inc = new Incident(
                        rs.getString("incident_id"),
                        rs.getString("event_id"),
                        rs.getString("status"),
                        rs.getString("assigned_to"),
                        rs.getString("notes"),
                        rs.getString("created_at"),
                        rs.getString("updated_at")
                );
                list.add(inc);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Computes live summary metrics directly from SQLite database tables.
     */
    public Map<String, Integer> getDashboardMetrics() {
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("totalEvents", 0);
        metrics.put("criticalAlerts", 0);
        metrics.put("highAlerts", 0);
        metrics.put("mediumAlerts", 0);
        metrics.put("lowAlerts", 0);
        metrics.put("openIncidents", 0);
        metrics.put("inProgressIncidents", 0);
        metrics.put("resolvedIncidents", 0);

        try (Connection conn = getConnection()) {
            // Total events
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM detection_events")) {
                if (rs.next()) metrics.put("totalEvents", rs.getInt(1));
            }

            // Alerts by risk category
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT risk_category, COUNT(*) FROM alerts WHERE is_active = 1 GROUP BY risk_category")) {
                while (rs.next()) {
                    String cat = rs.getString(1).toUpperCase();
                    int count = rs.getInt(2);
                    switch (cat) {
                        case "CRITICAL": metrics.put("criticalAlerts", count); break;
                        case "HIGH":     metrics.put("highAlerts", count); break;
                        case "MEDIUM":   metrics.put("mediumAlerts", count); break;
                        case "LOW":      metrics.put("lowAlerts", count); break;
                    }
                }
            }

            // Incidents by status
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT status, COUNT(*) FROM incidents GROUP BY status")) {
                while (rs.next()) {
                    String st = rs.getString(1).toUpperCase();
                    int count = rs.getInt(2);
                    switch (st) {
                        case "OPEN":        metrics.put("openIncidents", count); break;
                        case "IN_PROGRESS": metrics.put("inProgressIncidents", count); break;
                        case "RESOLVED":    metrics.put("resolvedIncidents", count); break;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return metrics;
    }

    /**
     * Loads saved rule configurations from database, falling back to default values.
     */
    public RuleConfiguration loadRuleConfiguration() {
        RuleConfiguration config = new RuleConfiguration();
        String sql = "SELECT config_key, config_value FROM rule_configurations";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String key = rs.getString("config_key");
                double val = rs.getDouble("config_value");
                switch (key) {
                    case "PROXIMITY_THRESHOLD": config.setProximityThresholdMeters(val); break;
                    case "PROXIMITY_WEIGHT": config.setProximityWeight((int) val); break;
                    case "SPEED_THRESHOLD": config.setSpeedThresholdKmh(val); break;
                    case "SPEED_WEIGHT": config.setSpeedWeight((int) val); break;
                    case "REPETITION_THRESHOLD": config.setRepetitionThresholdCount((int) val); break;
                    case "REPETITION_WEIGHT": config.setRepetitionWeight((int) val); break;
                    case "UNAUTHORIZED_WEIGHT": config.setUnauthorizedWeight((int) val); break;
                }
            }
        } catch (SQLException e) {
            System.err.println("[WARN] Error loading rule config, using defaults: " + e.getMessage());
        }
        return config;
    }

    /**
     * Persists updated rule configuration into SQLite database.
     */
    public boolean saveRuleConfiguration(RuleConfiguration config) {
        String sql = "INSERT OR REPLACE INTO rule_configurations (config_key, config_value, description) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            saveRuleParam(ps, "PROXIMITY_THRESHOLD", config.getProximityThresholdMeters(), "Proximity threshold (meters)");
            saveRuleParam(ps, "PROXIMITY_WEIGHT", config.getProximityWeight(), "Proximity weight (points)");
            saveRuleParam(ps, "SPEED_THRESHOLD", config.getSpeedThresholdKmh(), "Speed threshold (km/h)");
            saveRuleParam(ps, "SPEED_WEIGHT", config.getSpeedWeight(), "Speed weight (points)");
            saveRuleParam(ps, "REPETITION_THRESHOLD", config.getRepetitionThresholdCount(), "Repetition threshold");
            saveRuleParam(ps, "REPETITION_WEIGHT", config.getRepetitionWeight(), "Repetition weight (points)");
            saveRuleParam(ps, "UNAUTHORIZED_WEIGHT", config.getUnauthorizedWeight(), "Unauthorized weight (points)");

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void saveRuleParam(PreparedStatement ps, String key, double value, String desc) throws SQLException {
        ps.setString(1, key);
        ps.setDouble(2, value);
        ps.setString(3, desc);
        ps.executeUpdate();
    }

    /**
     * Persists benchmark evaluation run results.
     */
    public boolean saveEvaluationResult(int datasetSize, double accuracy, double precision, double recall, double f1, String notes) {
        String sql = "INSERT INTO evaluation_results (run_timestamp, dataset_size, accuracy, precision_score, recall_score, f1_score, summary_notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String now = LocalDateTime.now().format(FORMATTER);
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, now);
            ps.setInt(2, datasetSize);
            ps.setDouble(3, accuracy);
            ps.setDouble(4, precision);
            ps.setDouble(5, recall);
            ps.setDouble(6, f1);
            ps.setString(7, notes);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
