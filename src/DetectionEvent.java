import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DetectionEvent.java
 *
 * Domain model representing a radar/RF/optical drone detection event.
 * Captures spatial telemetry, kinematic velocity, sensor confirmation persistence,
 * and IFF (Identification Friend or Foe) transponder status.
 */
public class DetectionEvent {

    public static final String AUTH_AUTHORIZED = "AUTHORIZED";
    public static final String AUTH_UNAUTHORIZED = "UNAUTHORIZED";
    public static final String AUTH_UNKNOWN = "UNKNOWN";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String eventId;
    private String zone;
    private double distanceMeters;
    private double speedKmh;
    private int repeatCount;
    private String authorizationStatus;
    private String createdAt;
    private String updatedAt;

    /**
     * Default constructor for Jackson/framework compatibility or manual construction.
     */
    public DetectionEvent() {
        String now = LocalDateTime.now().format(FORMATTER);
        this.createdAt = now;
        this.updatedAt = now;
        this.authorizationStatus = AUTH_UNKNOWN;
        this.repeatCount = 1;
    }

    /**
     * Parameterized constructor for creating a new detection event with automatic timestamp.
     */
    public DetectionEvent(String eventId,
                          String zone,
                          double distanceMeters,
                          double speedKmh,
                          int repeatCount,
                          String authorizationStatus) {
        this.eventId = eventId != null ? eventId.trim() : "";
        this.zone = zone != null ? zone.trim() : "";
        this.distanceMeters = distanceMeters;
        this.speedKmh = speedKmh;
        this.repeatCount = repeatCount;
        this.authorizationStatus = authorizationStatus != null ? authorizationStatus.trim().toUpperCase() : AUTH_UNKNOWN;
        String now = LocalDateTime.now().format(FORMATTER);
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Full constructor including explicit database timestamps.
     */
    public DetectionEvent(String eventId,
                          String zone,
                          double distanceMeters,
                          double speedKmh,
                          int repeatCount,
                          String authorizationStatus,
                          String createdAt,
                          String updatedAt) {
        this.eventId = eventId != null ? eventId.trim() : "";
        this.zone = zone != null ? zone.trim() : "";
        this.distanceMeters = distanceMeters;
        this.speedKmh = speedKmh;
        this.repeatCount = repeatCount;
        this.authorizationStatus = authorizationStatus != null ? authorizationStatus.trim().toUpperCase() : AUTH_UNKNOWN;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- Getters and Setters ---

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId != null ? eventId.trim() : "";
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone != null ? zone.trim() : "";
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

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getAuthorizationStatus() {
        return authorizationStatus;
    }

    public void setAuthorizationStatus(String authorizationStatus) {
        this.authorizationStatus = authorizationStatus != null ? authorizationStatus.trim().toUpperCase() : AUTH_UNKNOWN;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Checks if the event authorization is explicitly unauthorized.
     */
    public boolean isUnauthorized() {
        return AUTH_UNAUTHORIZED.equalsIgnoreCase(authorizationStatus);
    }

    /**
     * Checks if the event authorization is unknown (unverified/missing transponder).
     */
    public boolean isUnknownAuthorization() {
        return AUTH_UNKNOWN.equalsIgnoreCase(authorizationStatus);
    }

    /**
     * Checks if the event authorization is verified authorized friendly.
     */
    public boolean isAuthorized() {
        return AUTH_AUTHORIZED.equalsIgnoreCase(authorizationStatus);
    }

    @Override
    public String toString() {
        return String.format("DetectionEvent[ID=%s, Zone=%s, Dist=%.1fm, Spd=%.1fkm/h, Repeats=%d, Auth=%s]",
                eventId, zone, distanceMeters, speedKmh, repeatCount, authorizationStatus);
    }
}
