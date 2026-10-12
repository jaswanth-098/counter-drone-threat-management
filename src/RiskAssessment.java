import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * RiskAssessment.java
 *
 * Domain model representing an explainable threat assessment.
 * Contains the final composite score, categorized threat tier, granular
 * point breakdown for each rule, reasons why rules did or did not fire,
 * itemized RuleEvaluationDetail structures, and an operator-oriented narrative summary.
 */
public class RiskAssessment {

    public static final String CATEGORY_LOW = "LOW";
    public static final String CATEGORY_MEDIUM = "MEDIUM";
    public static final String CATEGORY_HIGH = "HIGH";
    public static final String CATEGORY_CRITICAL = "CRITICAL";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Itemized breakdown for an individual scoring rule.
     */
    public static class RuleEvaluationDetail {
        private final String ruleName;
        private final boolean triggered;
        private final int points;
        private final String explanation;

        public RuleEvaluationDetail(String ruleName, boolean triggered, int points, String explanation) {
            this.ruleName = ruleName;
            this.triggered = triggered;
            this.points = points;
            this.explanation = explanation != null ? explanation : "";
        }

        public String getRuleName() {
            return ruleName;
        }

        public boolean isTriggered() {
            return triggered;
        }

        public int getPoints() {
            return points;
        }

        public String getExplanation() {
            return explanation;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s -> %s (+%d pts): %s",
                    triggered ? "TRIGGERED" : "WITHHELD", ruleName, triggered, points, explanation);
        }
    }

    private int id;
    private String eventId;
    private int score;
    private String riskCategory;
    private List<String> triggeredFactors;
    private List<String> explanations;
    private List<String> nonTriggeredExplanations;
    private List<RuleEvaluationDetail> ruleDetails;
    private String humanReadableSummary;
    private String assessmentTimestamp;

    public RiskAssessment() {
        this.triggeredFactors = new ArrayList<>();
        this.explanations = new ArrayList<>();
        this.nonTriggeredExplanations = new ArrayList<>();
        this.ruleDetails = new ArrayList<>();
        this.assessmentTimestamp = LocalDateTime.now().format(FORMATTER);
    }

    public RiskAssessment(String eventId,
                          int score,
                          String riskCategory,
                          List<String> triggeredFactors,
                          List<String> explanations,
                          List<String> nonTriggeredExplanations,
                          String humanReadableSummary) {
        this.eventId = eventId;
        this.score = score;
        this.riskCategory = riskCategory;
        this.triggeredFactors = triggeredFactors != null ? triggeredFactors : new ArrayList<>();
        this.explanations = explanations != null ? explanations : new ArrayList<>();
        this.nonTriggeredExplanations = nonTriggeredExplanations != null ? nonTriggeredExplanations : new ArrayList<>();
        this.ruleDetails = new ArrayList<>();
        this.humanReadableSummary = humanReadableSummary != null ? humanReadableSummary : "";
        this.assessmentTimestamp = LocalDateTime.now().format(FORMATTER);
    }

    public RiskAssessment(String eventId,
                          int score,
                          String riskCategory,
                          List<String> triggeredFactors,
                          List<String> explanations,
                          List<String> nonTriggeredExplanations,
                          List<RuleEvaluationDetail> ruleDetails,
                          String humanReadableSummary) {
        this.eventId = eventId;
        this.score = score;
        this.riskCategory = riskCategory;
        this.triggeredFactors = triggeredFactors != null ? triggeredFactors : new ArrayList<>();
        this.explanations = explanations != null ? explanations : new ArrayList<>();
        this.nonTriggeredExplanations = nonTriggeredExplanations != null ? nonTriggeredExplanations : new ArrayList<>();
        this.ruleDetails = ruleDetails != null ? ruleDetails : new ArrayList<>();
        this.humanReadableSummary = humanReadableSummary != null ? humanReadableSummary : "";
        this.assessmentTimestamp = LocalDateTime.now().format(FORMATTER);
    }

    public RiskAssessment(int id,
                          String eventId,
                          int score,
                          String riskCategory,
                          List<String> triggeredFactors,
                          List<String> explanations,
                          List<String> nonTriggeredExplanations,
                          String humanReadableSummary,
                          String assessmentTimestamp) {
        this.id = id;
        this.eventId = eventId;
        this.score = score;
        this.riskCategory = riskCategory;
        this.triggeredFactors = triggeredFactors != null ? triggeredFactors : new ArrayList<>();
        this.explanations = explanations != null ? explanations : new ArrayList<>();
        this.nonTriggeredExplanations = nonTriggeredExplanations != null ? nonTriggeredExplanations : new ArrayList<>();
        this.ruleDetails = new ArrayList<>();
        this.humanReadableSummary = humanReadableSummary != null ? humanReadableSummary : "";
        this.assessmentTimestamp = assessmentTimestamp;
    }

    // --- Getters and Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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

    public List<String> getTriggeredFactors() {
        return triggeredFactors;
    }

    public void setTriggeredFactors(List<String> triggeredFactors) {
        this.triggeredFactors = triggeredFactors;
    }

    public List<String> getExplanations() {
        return explanations;
    }

    public void setExplanations(List<String> explanations) {
        this.explanations = explanations;
    }

    public List<String> getNonTriggeredExplanations() {
        return nonTriggeredExplanations;
    }

    public void setNonTriggeredExplanations(List<String> nonTriggeredExplanations) {
        this.nonTriggeredExplanations = nonTriggeredExplanations;
    }

    public List<RuleEvaluationDetail> getRuleDetails() {
        return ruleDetails;
    }

    public void setRuleDetails(List<RuleEvaluationDetail> ruleDetails) {
        this.ruleDetails = ruleDetails;
    }

    public String getHumanReadableSummary() {
        return humanReadableSummary;
    }

    public void setHumanReadableSummary(String humanReadableSummary) {
        this.humanReadableSummary = humanReadableSummary;
    }

    public String getAssessmentTimestamp() {
        return assessmentTimestamp;
    }

    public void setAssessmentTimestamp(String assessmentTimestamp) {
        this.assessmentTimestamp = assessmentTimestamp;
    }

    public String getFormattedTriggeredFactors() {
        if (triggeredFactors == null || triggeredFactors.isEmpty()) {
            return "• None (Nominal flight parameters)";
        }
        StringBuilder sb = new StringBuilder();
        for (String factor : triggeredFactors) {
            sb.append("• ").append(factor).append("\n");
        }
        return sb.toString().trim();
    }

    public String getFormattedExplanations() {
        if (explanations == null || explanations.isEmpty()) {
            return "No risk elevation rules triggered.";
        }
        StringBuilder sb = new StringBuilder();
        for (String expl : explanations) {
            sb.append("✔ ").append(expl).append("\n");
        }
        return sb.toString().trim();
    }

    public String getFormattedNonTriggered() {
        if (nonTriggeredExplanations == null || nonTriggeredExplanations.isEmpty()) {
            return "All evaluation rules were triggered.";
        }
        StringBuilder sb = new StringBuilder();
        for (String n : nonTriggeredExplanations) {
            sb.append("✖ ").append(n).append("\n");
        }
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return String.format("RiskAssessment[Event=%s, Score=%d, Category=%s, Factors=%d]",
                eventId, score, riskCategory, triggeredFactors.size());
    }
}
