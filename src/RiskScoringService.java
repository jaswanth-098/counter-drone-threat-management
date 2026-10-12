import java.util.ArrayList;
import java.util.List;

/**
 * RiskScoringService.java
 *
 * Core explainable heuristic scoring engine for counter-drone threat assessment.
 * Evaluates physical telemetry against configurable safety envelopes:
 * 1. Proximity Horizon Rule (distance to critical infrastructure)
 * 2. Kinematic Velocity Rule (high-speed approach vectors)
 * 3. Persistence / Repetition Rule (repeated radar cross-section detections)
 * 4. Authorization / IFF Rule (unauthorized vs unknown vs authorized)
 *
 * EXPLAINABILITY DESIGN:
 * Produces a full audit trail detailing:
 * - Exactly which rules triggered and why points were awarded
 * - Exactly which rules did not trigger and why points were withheld
 * - Transparent handling of "UNKNOWN" authorization (0 pts added, verification warning)
 * - Structured RuleEvaluationDetail list for real-time dashboard inspection
 *
 * PROTOTYPE DISCLAIMER:
 * Point weights and distance/speed thresholds represent heuristic prototype assumptions
 * for educational defense simulation, not certified operational military specifications.
 * This service does NOT employ a trained machine learning model.
 */
public class RiskScoringService {

    private RuleConfiguration configuration;

    public RiskScoringService() {
        this.configuration = new RuleConfiguration();
    }

    public RiskScoringService(RuleConfiguration configuration) {
        this.configuration = configuration != null ? configuration : new RuleConfiguration();
    }

    public RuleConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(RuleConfiguration configuration) {
        this.configuration = configuration != null ? configuration : new RuleConfiguration();
    }

    /**
     * Assesses risk for a given detection event using current rule configuration.
     *
     * @param event The detection event containing sensor measurements
     * @return RiskAssessment containing score, category, factor list, explanations, and rule details
     */
    public RiskAssessment evaluateRisk(DetectionEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Detection event cannot be null for risk evaluation.");
        }

        int score = 0;
        List<String> triggeredFactors = new ArrayList<>();
        List<String> explanations = new ArrayList<>();
        List<String> nonTriggered = new ArrayList<>();
        List<RiskAssessment.RuleEvaluationDetail> ruleDetails = new ArrayList<>();

        // -------------------------------------------------------------
        // RULE 1: Proximity Breach Rule (threshold <= proximityThresholdMeters)
        // -------------------------------------------------------------
        boolean proxTrig = event.getDistanceMeters() <= configuration.getProximityThresholdMeters();
        if (proxTrig) {
            int pts = configuration.getProximityWeight();
            score += pts;
            String msg = String.format("Proximity: Detected at %.1fm from protected facility (threshold <= %.0fm). Imminent perimeter breach (+%d pts).",
                    event.getDistanceMeters(), configuration.getProximityThresholdMeters(), pts);
            triggeredFactors.add(String.format("Critical Proximity Breach (Distance <= %.0fm)",
                    configuration.getProximityThresholdMeters()));
            explanations.add(msg);
            ruleDetails.add(new RiskAssessment.RuleEvaluationDetail("Proximity Breach Rule", true, pts, msg));
        } else {
            String msg = String.format("Proximity: Detected at %.1fm, safely beyond perimeter defense horizon (> %.0fm) (0 pts assigned).",
                    event.getDistanceMeters(), configuration.getProximityThresholdMeters());
            nonTriggered.add(msg);
            ruleDetails.add(new RiskAssessment.RuleEvaluationDetail("Proximity Breach Rule", false, 0, msg));
        }

        // -------------------------------------------------------------
        // RULE 2: Kinematic Velocity Rule (threshold >= speedThresholdKmh)
        // -------------------------------------------------------------
        boolean speedTrig = event.getSpeedKmh() >= configuration.getSpeedThresholdKmh();
        if (speedTrig) {
            int pts = configuration.getSpeedWeight();
            score += pts;
            String msg = String.format("Kinematics: Velocity of %.1f km/h exceeds normal commercial UAS envelope (threshold >= %.0f km/h) (+%d pts).",
                    event.getSpeedKmh(), configuration.getSpeedThresholdKmh(), pts);
            triggeredFactors.add(String.format("High Velocity Incursion (Speed >= %.0f km/h)",
                    configuration.getSpeedThresholdKmh()));
            explanations.add(msg);
            ruleDetails.add(new RiskAssessment.RuleEvaluationDetail("Kinematic Velocity Rule", true, pts, msg));
        } else {
            String msg = String.format("Kinematics: Velocity of %.1f km/h within sub-threshold loitering/cruising envelope (< %.0f km/h) (0 pts assigned).",
                    event.getSpeedKmh(), configuration.getSpeedThresholdKmh());
            nonTriggered.add(msg);
            ruleDetails.add(new RiskAssessment.RuleEvaluationDetail("Kinematic Velocity Rule", false, 0, msg));
        }

        // -------------------------------------------------------------
        // RULE 3: Persistence / Repetition Rule (threshold >= repetitionThresholdCount)
        // -------------------------------------------------------------
        boolean repTrig = event.getRepeatCount() >= configuration.getRepetitionThresholdCount();
        if (repTrig) {
            int pts = configuration.getRepetitionWeight();
            score += pts;
            String msg = String.format("Persistence: Sustained radar/RF tracking with %d confirmed bursts (threshold >= %d) (+%d pts).",
                    event.getRepeatCount(), configuration.getRepetitionThresholdCount(), pts);
            triggeredFactors.add(String.format("Persistent Incursion Pattern (Repeats >= %d)",
                    configuration.getRepetitionThresholdCount()));
            explanations.add(msg);
            ruleDetails.add(new RiskAssessment.RuleEvaluationDetail("Persistence / Repetition Rule", true, pts, msg));
        } else {
            String msg = String.format("Persistence: %d detection burst(s) recorded; below persistence confirmation threshold (>= %d) (0 pts assigned).",
                    event.getRepeatCount(), configuration.getRepetitionThresholdCount());
            nonTriggered.add(msg);
            ruleDetails.add(new RiskAssessment.RuleEvaluationDetail("Persistence / Repetition Rule", false, 0, msg));
        }

        // -------------------------------------------------------------
        // RULE 4: Authorization / IFF Status Rule
        // UNAUTHORIZED: +25 pts | UNKNOWN: 0 pts + warning | AUTHORIZED: 0 pts
        // -------------------------------------------------------------
        if (event.isUnauthorized()) {
            int pts = configuration.getUnauthorizedWeight();
            score += pts;
            String msg = String.format("Authorization: Confirmed hostile or unapproved drone flight plan within restricted airspace (+%d pts).", pts);
            triggeredFactors.add("Explicitly Unauthorized Airborne Asset");
            explanations.add(msg);
            ruleDetails.add(new RiskAssessment.RuleEvaluationDetail("Authorization / IFF Status", true, pts, msg));
        } else if (event.isUnknownAuthorization()) {
            String msg = "Authorization: ⚠️ VERIFICATION WARNING: Transponder IFF is UNKNOWN. Zero penalty awarded to prevent false alarm saturation. Visual/RF validation required (0 pts assigned).";
            nonTriggered.add(msg);
            ruleDetails.add(new RiskAssessment.RuleEvaluationDetail("Authorization / IFF Status", false, 0, msg));
        } else {
            String msg = "Authorization: Validated friendly transponder / pre-approved flight authorization verified (0 pts assigned).";
            nonTriggered.add(msg);
            ruleDetails.add(new RiskAssessment.RuleEvaluationDetail("Authorization / IFF Status", false, 0, msg));
        }

        // -------------------------------------------------------------
        // Score Clamping and Categorization
        // Clamped to [0, 100]
        // -------------------------------------------------------------
        int finalScore = Math.min(RuleConfiguration.MAX_SCORE, Math.max(0, score));
        String category = RuleConfiguration.categorizeScore(finalScore);
        String summary = generateNarrativeSummary(event, finalScore, category, triggeredFactors);

        return new RiskAssessment(
                event.getEventId(),
                finalScore,
                category,
                triggeredFactors,
                explanations,
                nonTriggered,
                ruleDetails,
                summary
        );
    }

    /**
     * Synthesizes an executive narrative summary explaining the operational threat posture.
     */
    private String generateNarrativeSummary(DetectionEvent event,
                                            int score,
                                            String category,
                                            List<String> triggeredFactors) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Threat Assessment for Event '%s' in Zone '%s': ", event.getEventId(), event.getZone()));
        sb.append(String.format("Calculated Risk Score is %d/100, placing this contact in the %s category. ", score, category));

        if (triggeredFactors.isEmpty()) {
            sb.append("No defensive breach thresholds were violated. Contact exhibits nominal characteristics consistent with routine air activity.");
        } else {
            sb.append("Assessment triggered ").append(triggeredFactors.size()).append(" security rules: ");
            for (int i = 0; i < triggeredFactors.size(); i++) {
                sb.append(triggeredFactors.get(i));
                if (i < triggeredFactors.size() - 1) sb.append("; ");
            }
            sb.append(". ");

            if (category.equals(RiskAssessment.CATEGORY_CRITICAL)) {
                sb.append("CRITICAL ACTION: Immediate electronic counter-measure or kinetic interdiction readiness protocol recommended.");
            } else if (category.equals(RiskAssessment.CATEGORY_HIGH)) {
                sb.append("HIGH ACTION: Escalate to air defense tactical coordinator and prepare tracking radar lock-on.");
            } else if (category.equals(RiskAssessment.CATEGORY_MEDIUM)) {
                sb.append("MEDIUM ACTION: Continue passive RF monitoring and verify optical sighting.");
            } else {
                sb.append("LOW ACTION: Maintain routine perimeter surveillance logs.");
            }
        }

        if (event.isUnknownAuthorization()) {
            sb.append(" [Note: IFF transponder status is UNKNOWN. Operator caution advised.]");
        }

        return sb.toString();
    }
}
