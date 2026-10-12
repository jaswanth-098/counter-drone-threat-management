import java.util.ArrayList;
import java.util.List;

/**
 * ValidationUtils.java
 *
 * Centralized validation utility for the Counter-Drone Threat Management System.
 * Ensures data integrity across event inputs, incident updates, and rule configurations.
 * Prevents invalid numeric ranges, blank identifiers, and malformed state transitions.
 */
public class ValidationUtils {

    /**
     * Encapsulates validation state and human-readable error messages.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("\n", errors);
        }
    }

    /**
     * Validates raw user input for a simulated detection event.
     *
     * @param eventId            Unique string identifier for the event
     * @param zone               Detection sector / perimeter zone
     * @param distanceStr        Raw string distance in meters
     * @param speedStr           Raw string velocity in km/h
     * @param repeatCountStr     Raw string repetition count
     * @param authorizationStatus Raw string authorization status ("AUTHORIZED", "UNAUTHORIZED", "UNKNOWN")
     * @return ValidationResult indicating success or list of errors
     */
    public static ValidationResult validateDetectionEvent(String eventId,
                                                           String zone,
                                                           String distanceStr,
                                                           String speedStr,
                                                           String repeatCountStr,
                                                           String authorizationStatus) {
        List<String> errors = new ArrayList<>();

        // 1. Event ID validation
        if (eventId == null || eventId.trim().isEmpty()) {
            errors.add("Event ID cannot be blank. Enter a unique identifier (e.g., 'EVT-1001').");
        } else if (!eventId.trim().matches("^[a-zA-Z0-9_-]{3,30}$")) {
            errors.add("Event ID must be 3-30 characters long and contain only alphanumeric, dash, or underscore.");
        }

        // 2. Detection Zone validation
        if (zone == null || zone.trim().isEmpty()) {
            errors.add("Detection Zone cannot be blank. Select or enter a valid security perimeter zone.");
        }

        // 3. Distance validation (meters: non-negative floating-point number)
        if (distanceStr == null || distanceStr.trim().isEmpty()) {
            errors.add("Distance cannot be blank.");
        } else {
            try {
                double dist = Double.parseDouble(distanceStr.trim());
                if (dist < 0.0) {
                    errors.add("Distance must be a non-negative number (0 meters or greater).");
                } else if (dist > 50000.0) {
                    errors.add("Distance exceeds maximum radar sensor detection radius (50,000 meters).");
                }
            } catch (NumberFormatException e) {
                errors.add("Distance must be a valid numeric value (e.g., '250.0').");
            }
        }

        // 4. Speed validation (km/h: non-negative floating-point number)
        if (speedStr == null || speedStr.trim().isEmpty()) {
            errors.add("Speed cannot be blank.");
        } else {
            try {
                double speed = Double.parseDouble(speedStr.trim());
                if (speed < 0.0) {
                    errors.add("Speed must be a non-negative number (0 km/h or greater).");
                } else if (speed > 1500.0) {
                    errors.add("Speed exceeds realistic small UAS/quadcopter kinematic envelope (max 1,500 km/h).");
                }
            } catch (NumberFormatException e) {
                errors.add("Speed must be a valid numeric value (e.g., '95.5').");
            }
        }

        // 5. Repetition count validation (integer >= 1)
        if (repeatCountStr == null || repeatCountStr.trim().isEmpty()) {
            errors.add("Repetition count cannot be blank.");
        } else {
            try {
                int count = Integer.parseInt(repeatCountStr.trim());
                if (count < 1) {
                    errors.add("Repetition count must be at least 1 (representing the initial detection burst).");
                } else if (count > 1000) {
                    errors.add("Repetition count cannot exceed 1,000 bursts.");
                }
            } catch (NumberFormatException e) {
                errors.add("Repetition count must be an integer value (e.g., '3').");
            }
        }

        // 6. Authorization status validation (strictly AUTHORIZED, UNAUTHORIZED, or UNKNOWN)
        if (authorizationStatus == null || authorizationStatus.trim().isEmpty()) {
            errors.add("Authorization status must be selected.");
        } else {
            String statusUpper = authorizationStatus.trim().toUpperCase();
            if (!statusUpper.equals("AUTHORIZED") &&
                !statusUpper.equals("UNAUTHORIZED") &&
                !statusUpper.equals("UNKNOWN")) {
                errors.add("Authorization status must be 'AUTHORIZED', 'UNAUTHORIZED', or 'UNKNOWN'.");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validates incident updates and manual creations.
     */
    public static ValidationResult validateIncident(String incidentId,
                                                    String eventId,
                                                    String status,
                                                    String assignedTo) {
        List<String> errors = new ArrayList<>();

        if (incidentId == null || incidentId.trim().isEmpty()) {
            errors.add("Incident ID cannot be blank.");
        }
        if (eventId == null || eventId.trim().isEmpty()) {
            errors.add("Associated Event ID cannot be blank.");
        }
        if (status == null || status.trim().isEmpty()) {
            errors.add("Incident Status must be specified.");
        } else {
            String s = status.trim().toUpperCase();
            if (!s.equals("OPEN") && !s.equals("IN_PROGRESS") && !s.equals("RESOLVED")) {
                errors.add("Incident Status must be 'OPEN', 'IN_PROGRESS', or 'RESOLVED'.");
            }
        }
        if (assignedTo == null || assignedTo.trim().isEmpty()) {
            errors.add("Assigned person or team cannot be blank. Enter responder name or team.");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validates rule configuration adjustments.
     */
    public static ValidationResult validateRuleConfiguration(double proxWeight,
                                                             double proxThreshold,
                                                             double speedWeight,
                                                             double speedThreshold,
                                                             double repWeight,
                                                             double repThreshold,
                                                             double unauthWeight) {
        List<String> errors = new ArrayList<>();

        if (proxWeight < 0 || proxWeight > 100) errors.add("Proximity weight must be between 0 and 100 points.");
        if (proxThreshold <= 0 || proxThreshold > 10000) errors.add("Proximity threshold must be between 1 and 10,000 meters.");

        if (speedWeight < 0 || speedWeight > 100) errors.add("Speed weight must be between 0 and 100 points.");
        if (speedThreshold <= 0 || speedThreshold > 1000) errors.add("Speed threshold must be between 1 and 1,000 km/h.");

        if (repWeight < 0 || repWeight > 100) errors.add("Repetition weight must be between 0 and 100 points.");
        if (repThreshold < 1 || repThreshold > 50) errors.add("Repetition count threshold must be between 1 and 50 bursts.");

        if (unauthWeight < 0 || unauthWeight > 100) errors.add("Unauthorized weight must be between 0 and 100 points.");

        return new ValidationResult(errors.isEmpty(), errors);
    }
}
