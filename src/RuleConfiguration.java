/**
 * RuleConfiguration.java
 *
 * Configurable parameters for the rule-based threat assessment engine.
 * Allows security administrators to calibrate proximity horizons, speed thresholds,
 * persistence counters, and point weights.
 *
 * NOTE: These weights and thresholds are prototype heuristic assumptions designed for
 * academic counter-drone simulation and baseline demonstration, not validated military
 * defense specifications.
 */
public class RuleConfiguration {

    // Default prototype parameters
    public static final double DEFAULT_PROXIMITY_THRESHOLD = 300.0; // meters
    public static final int DEFAULT_PROXIMITY_WEIGHT = 35;          // points

    public static final double DEFAULT_SPEED_THRESHOLD = 100.0;     // km/h
    public static final int DEFAULT_SPEED_WEIGHT = 20;              // points

    public static final int DEFAULT_REPETITION_THRESHOLD = 3;       // count
    public static final int DEFAULT_REPETITION_WEIGHT = 20;         // points

    public static final int DEFAULT_UNAUTHORIZED_WEIGHT = 25;       // points
    public static final int DEFAULT_UNKNOWN_WEIGHT = 0;             // points

    public static final int MAX_SCORE = 100;

    // Instance parameters
    private double proximityThresholdMeters;
    private int proximityWeight;

    private double speedThresholdKmh;
    private int speedWeight;

    private int repetitionThresholdCount;
    private int repetitionWeight;

    private int unauthorizedWeight;
    private int unknownWeight;

    public RuleConfiguration() {
        resetToDefaults();
    }

    public RuleConfiguration(double proximityThresholdMeters,
                             int proximityWeight,
                             double speedThresholdKmh,
                             int speedWeight,
                             int repetitionThresholdCount,
                             int repetitionWeight,
                             int unauthorizedWeight,
                             int unknownWeight) {
        this.proximityThresholdMeters = proximityThresholdMeters;
        this.proximityWeight = proximityWeight;
        this.speedThresholdKmh = speedThresholdKmh;
        this.speedWeight = speedWeight;
        this.repetitionThresholdCount = repetitionThresholdCount;
        this.repetitionWeight = repetitionWeight;
        this.unauthorizedWeight = unauthorizedWeight;
        this.unknownWeight = unknownWeight;
    }

    public final void resetToDefaults() {
        this.proximityThresholdMeters = DEFAULT_PROXIMITY_THRESHOLD;
        this.proximityWeight = DEFAULT_PROXIMITY_WEIGHT;
        this.speedThresholdKmh = DEFAULT_SPEED_THRESHOLD;
        this.speedWeight = DEFAULT_SPEED_WEIGHT;
        this.repetitionThresholdCount = DEFAULT_REPETITION_THRESHOLD;
        this.repetitionWeight = DEFAULT_REPETITION_WEIGHT;
        this.unauthorizedWeight = DEFAULT_UNAUTHORIZED_WEIGHT;
        this.unknownWeight = DEFAULT_UNKNOWN_WEIGHT;
    }

    // --- Getters and Setters ---

    public double getProximityThresholdMeters() {
        return proximityThresholdMeters;
    }

    public void setProximityThresholdMeters(double proximityThresholdMeters) {
        this.proximityThresholdMeters = proximityThresholdMeters;
    }

    public int getProximityWeight() {
        return proximityWeight;
    }

    public void setProximityWeight(int proximityWeight) {
        this.proximityWeight = proximityWeight;
    }

    public double getSpeedThresholdKmh() {
        return speedThresholdKmh;
    }

    public void setSpeedThresholdKmh(double speedThresholdKmh) {
        this.speedThresholdKmh = speedThresholdKmh;
    }

    public int getSpeedWeight() {
        return speedWeight;
    }

    public void setSpeedWeight(int speedWeight) {
        this.speedWeight = speedWeight;
    }

    public int getRepetitionThresholdCount() {
        return repetitionThresholdCount;
    }

    public void setRepetitionThresholdCount(int repetitionThresholdCount) {
        this.repetitionThresholdCount = repetitionThresholdCount;
    }

    public int getRepetitionWeight() {
        return repetitionWeight;
    }

    public void setRepetitionWeight(int repetitionWeight) {
        this.repetitionWeight = repetitionWeight;
    }

    public int getUnauthorizedWeight() {
        return unauthorizedWeight;
    }

    public void setUnauthorizedWeight(int unauthorizedWeight) {
        this.unauthorizedWeight = unauthorizedWeight;
    }

    public int getUnknownWeight() {
        return unknownWeight;
    }

    public void setUnknownWeight(int unknownWeight) {
        this.unknownWeight = unknownWeight;
    }

    /**
     * Categorizes a numeric score into standardized threat bands.
     */
    public static String categorizeScore(int score) {
        if (score >= 75) {
            return RiskAssessment.CATEGORY_CRITICAL;
        } else if (score >= 50) {
            return RiskAssessment.CATEGORY_HIGH;
        } else if (score >= 25) {
            return RiskAssessment.CATEGORY_MEDIUM;
        } else {
            return RiskAssessment.CATEGORY_LOW;
        }
    }
}
