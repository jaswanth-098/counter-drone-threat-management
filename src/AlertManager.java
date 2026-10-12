import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * AlertManager.java
 *
 * Intelligent alert prioritization engine.
 * Applies multi-tier sorting, tactical filtering, and search capabilities.
 *
 * PRIORITIZATION POLICY:
 * 1. Primary Sort Key: Risk Score (Descending: 100 down to 0)
 * 2. Secondary Sort Key: Threat Category Tier (CRITICAL > HIGH > MEDIUM > LOW)
 * 3. Tertiary Sort Key: Alert Timestamp (Most recent first)
 *
 * DUPLICATE ALERT POLICY:
 * - Counter-drone tracking systems receive frequent pulse bursts for the same physical target.
 * - Repeated submissions with the same Event ID update the existing alert rather than spawning
 *   redundant entries, avoiding alarm fatigue in tactical operations centers.
 * - If an updated detection score increases the risk, the alert is automatically promoted
 *   to its new higher priority position in the queue.
 */
public class AlertManager {

    /**
     * Generates a descriptive tactical prioritization rationale for an alert item.
     *
     * @param score Composite risk score (0-100)
     * @param category Threat band (CRITICAL, HIGH, MEDIUM, LOW)
     * @param event The detection event metadata
     * @return Human-readable prioritization explanation
     */
    public static String generatePrioritizationReason(int score, String category, DetectionEvent event) {
        if (score >= 75) {
            if (event != null && event.isUnauthorized()) {
                return String.format("CRITICAL (Rank 1): Severe unauthorized incursion (Score %d). Proximity breach & high velocity require immediate interdiction protocol.", score);
            }
            return String.format("CRITICAL (Rank 1): Composite score of %d indicates critical perimeter vulnerability. Immediate operator alert dispatched.", score);
        } else if (score >= 50) {
            return String.format("HIGH (Rank 2): Elevated score (%d). Target breached primary defense zone or exhibits aggressive kinematic vector. Tactical triage assigned.", score);
        } else if (score >= 25) {
            return String.format("MEDIUM (Rank 3): Moderate score (%d). Single breach parameter detected. Sustained sensor tracking and optical confirmation advised.", score);
        } else {
            return String.format("LOW (Rank 4): Minimal threat score (%d). Nominal flight envelope or authorized friendly patrol. Logged for situational awareness.", score);
        }
    }

    /**
     * Filters and prioritizes a collection of alert items based on operator criteria.
     *
     * @param rawAlerts      Full list of active alerts
     * @param categoryFilter Filter by category ("ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW")
     * @param zoneFilter     Filter by zone ("ALL", or specific zone name)
     * @param searchEventId  Search query for event ID (case-insensitive substring)
     * @return Prioritized, filtered list of alerts
     */
    public static List<AlertItem> prioritizeAndFilter(List<AlertItem> rawAlerts,
                                                      String categoryFilter,
                                                      String zoneFilter,
                                                      String searchEventId) {
        if (rawAlerts == null) {
            return new ArrayList<>();
        }

        List<AlertItem> filtered = new ArrayList<>();

        for (AlertItem alert : rawAlerts) {
            if (alert == null || !alert.isActive()) {
                continue;
            }

            // Category filter
            if (categoryFilter != null && !categoryFilter.equalsIgnoreCase("ALL")) {
                if (!categoryFilter.equalsIgnoreCase(alert.getRiskCategory())) {
                    continue;
                }
            }

            // Zone filter
            if (zoneFilter != null && !zoneFilter.equalsIgnoreCase("ALL")) {
                if (!zoneFilter.equalsIgnoreCase(alert.getZone())) {
                    continue;
                }
            }

            // Search query filter (Event ID)
            if (searchEventId != null && !searchEventId.trim().isEmpty()) {
                String q = searchEventId.trim().toLowerCase();
                if (!alert.getEventId().toLowerCase().contains(q)) {
                    continue;
                }
            }

            filtered.add(alert);
        }

        // Multi-tier sorting
        Collections.sort(filtered, new Comparator<AlertItem>() {
            @Override
            public int compare(AlertItem a1, AlertItem a2) {
                // 1. Primary: Score descending
                int scoreComp = Integer.compare(a2.getScore(), a1.getScore());
                if (scoreComp != 0) {
                    return scoreComp;
                }

                // 2. Secondary: Category tier descending
                int cat1 = getCategoryRank(a1.getRiskCategory());
                int cat2 = getCategoryRank(a2.getRiskCategory());
                int catComp = Integer.compare(cat2, cat1);
                if (catComp != 0) {
                    return catComp;
                }

                // 3. Tertiary: Timestamp descending (lexicographical string comparison for ISO dates)
                String t1 = a1.getAlertTimestamp() != null ? a1.getAlertTimestamp() : "";
                String t2 = a2.getAlertTimestamp() != null ? a2.getAlertTimestamp() : "";
                return t2.compareTo(t1);
            }
        });

        return filtered;
    }

    /**
     * Helper mapping risk categories to numerical ranking weights.
     */
    private static int getCategoryRank(String category) {
        if (category == null) return 0;
        switch (category.toUpperCase()) {
            case RiskAssessment.CATEGORY_CRITICAL: return 4;
            case RiskAssessment.CATEGORY_HIGH:     return 3;
            case RiskAssessment.CATEGORY_MEDIUM:   return 2;
            case RiskAssessment.CATEGORY_LOW:      return 1;
            default: return 0;
        }
    }
}
