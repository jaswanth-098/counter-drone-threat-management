import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AlertItem.java
 *
 * Domain model representing an actionable security alert in the prioritized queue.
 * Combines spatial sector data, telemetry measurements (distance, speed, transponder),
 * composite risk score, threat category, and prioritization justification.
 */
public class AlertItem {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int alertId;
    private String eventId;
    private String zone;
    private int score;
    private String riskCategory;
    private String prioritizationReason;
    private String alertTimestamp;
    private boolean active;

    // Telemetry fields for queue visibility
    private double distanceMeters;
    private double speedKmh;
    private String authorizationStatus;

    public AlertItem() {
        this.alertTimestamp = LocalDateTime.now().format(FORMATTER);
        this.active = true;
        this.authorizationStatus = DetectionEvent.AUTH_UNKNOWN;
    }

    public AlertItem(int alertId,
                     String eventId,
                     String zone,
                     int score,
                     String riskCategory,
                     String prioritizationReason,
                     String alertTimestamp,
                     boolean active) {
        this.alertId = alertId;
        this.eventId = eventId;
        this.zone = zone;
        this.score = score;
        this.riskCategory = riskCategory;
        this.prioritizationReason = prioritizationReason;
        this.alertTimestamp = alertTimestamp;
        this.active = active;
        this.authorizationStatus = DetectionEvent.AUTH_UNKNOWN;
    }

    public AlertItem(int alertId,
                     String eventId,
                     String zone,
                     int score,
                     String riskCategory,
                     String prioritizationReason,
                     String alertTimestamp,
                     boolean active,
                     double distanceMeters,
                     double speedKmh,
                     String authorizationStatus) {
        this.alertId = alertId;
        this.eventId = eventId;
        this.zone = zone;
        this.score = score;
        this.riskCategory = riskCategory;
        this.prioritizationReason = prioritizationReason;
        this.alertTimestamp = alertTimestamp;
        this.active = active;
        this.distanceMeters = distanceMeters;
        this.speedKmh = speedKmh;
        this.authorizationStatus = authorizationStatus != null ? authorizationStatus : DetectionEvent.AUTH_UNKNOWN;
    }

    public AlertItem(String eventId,
                     String zone,
                     int score,
                     String riskCategory,
                     String prioritizationReason) {
        this.eventId = eventId;
        this.zone = zone;
        this.score = score;
        this.riskCategory = riskCategory;
        this.prioritizationReason = prioritizationReason;
        this.alertTimestamp = LocalDateTime.now().format(FORMATTER);
        this.active = true;
        this.authorizationStatus = DetectionEvent.AUTH_UNKNOWN;
    }

    // --- Getters and Setters ---

    public int getAlertId() {
        return alertId;
    }

    public void setAlertId(int alertId) {
        this.alertId = alertId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(String riskCategory) {
        this.riskCategory = riskCategory;
    }

    public String getPrioritizationReason() {
        return prioritizationReason;
    }

    public void setPrioritizationReason(String prioritizationReason) {
        this.prioritizationReason = prioritizationReason;
    }

    public String getAlertTimestamp() {
        return alertTimestamp;
    }

    public void setAlertTimestamp(String alertTimestamp) {
        this.alertTimestamp = alertTimestamp;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public double getSpeedKmh() {
        return speedKmh;
    }

    public void setSpeedKmh(double speedKmh) {
        this.speedKmh = speedKmh;
    }

    public String getAuthorizationStatus() {
        return authorizationStatus;
    }

    public void setAuthorizationStatus(String authorizationStatus) {
        this.authorizationStatus = authorizationStatus;
    }

    @Override
    public String toString() {
        return String.format("AlertItem[ID=%d, Event=%s, Score=%d, Category=%s, Dist=%.1fm, Spd=%.1fkm/h, Auth=%s]",
                alertId, eventId, score, riskCategory, distanceMeters, speedKmh, authorizationStatus);
    }
}
