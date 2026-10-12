import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Incident.java
 *
 * Domain model representing an operational security incident triggered by
 * high-risk or critical counter-drone incursions.
 * Tracks lifecycle status, tactical team assignment, and audit notes.
 */
public class Incident {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String incidentId;
    private String eventId;
    private String status;
    private String assignedTo;
    private String notes;
    private String createdAt;
    private String updatedAt;

    public Incident() {
        String now = LocalDateTime.now().format(FORMATTER);
        this.status = STATUS_OPEN;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Incident(String incidentId,
                    String eventId,
                    String status,
                    String assignedTo,
                    String notes) {
        this.incidentId = incidentId != null ? incidentId.trim() : "";
        this.eventId = eventId != null ? eventId.trim() : "";
        this.status = status != null ? status.trim().toUpperCase() : STATUS_OPEN;
        this.assignedTo = assignedTo != null ? assignedTo.trim() : "Unassigned";
        this.notes = notes != null ? notes.trim() : "";
        String now = LocalDateTime.now().format(FORMATTER);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Incident(String incidentId,
                    String eventId,
                    String status,
                    String assignedTo,
                    String notes,
                    String createdAt,
                    String updatedAt) {
        this.incidentId = incidentId != null ? incidentId.trim() : "";
        this.eventId = eventId != null ? eventId.trim() : "";
        this.status = status != null ? status.trim().toUpperCase() : STATUS_OPEN;
        this.assignedTo = assignedTo != null ? assignedTo.trim() : "Unassigned";
        this.notes = notes != null ? notes.trim() : "";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- Getters and Setters ---

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId != null ? incidentId.trim() : "";
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId != null ? eventId.trim() : "";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status.trim().toUpperCase() : STATUS_OPEN;
        this.updatedAt = LocalDateTime.now().format(FORMATTER);
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo != null ? assignedTo.trim() : "Unassigned";
        this.updatedAt = LocalDateTime.now().format(FORMATTER);
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : "";
        this.updatedAt = LocalDateTime.now().format(FORMATTER);
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

    public boolean isUnresolved() {
        return STATUS_OPEN.equalsIgnoreCase(status) || STATUS_IN_PROGRESS.equalsIgnoreCase(status);
    }

    public boolean isResolved() {
        return STATUS_RESOLVED.equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return String.format("Incident[ID=%s, Event=%s, Status=%s, Assigned=%s]",
                incidentId, eventId, status, assignedTo);
    }
}
