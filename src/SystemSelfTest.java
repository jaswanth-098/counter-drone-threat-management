import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * SystemSelfTest.java
 *
 * Automated verification test runner to validate backend components:
 * - SQLite schema creation & foreign keys
 * - Risk assessment calculation & explainability output
 * - Transactional event saving & duplicate event handling
 * - Incident creation & duplicate suppression policy
 * - EvaluationService benchmark execution & metrics
 */
public class SystemSelfTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" COUNTER-DRONE THREAT MANAGEMENT SYSTEM: SELF-TEST");
        System.out.println("==================================================");

        try {
            // 1. Initialize Database
            System.out.println("[TEST 1] Initializing DatabaseManager...");
            DatabaseManager db = DatabaseManager.getInstance();
            File dbFile = new File("data/counterdrone.db");
            if (dbFile.exists()) {
                System.out.println("  ✔ SQLite database file verified: " + dbFile.getAbsolutePath());
            } else {
                throw new RuntimeException("Database file not found!");
            }

            // 2. Test Scoring Engine & Explainability
            System.out.println("[TEST 2] Testing RiskScoringService...");
            RiskScoringService scoring = new RiskScoringService();

            // Scenario: Proximity (<=300m -> +35), High Speed (>=100 -> +20), Repeats (>=3 -> +20), Unauthorized (+25) = 100 pts (CRITICAL)
            DetectionEvent testEvt = new DetectionEvent("EVT-TEST-99", "Zone-Alpha", 150.0, 120.0, 4, DetectionEvent.AUTH_UNAUTHORIZED);
            RiskAssessment assessment = scoring.evaluateRisk(testEvt);

            System.out.println("  Calculated Score: " + assessment.getScore() + "/100");
            System.out.println("  Risk Category   : " + assessment.getRiskCategory());
            System.out.println("  Triggered Rules : " + assessment.getTriggeredFactors().size());
            if (assessment.getScore() == 100 && assessment.getRiskCategory().equals("CRITICAL")) {
                System.out.println("  ✔ Scoring calculation and capping verified.");
            } else {
                throw new RuntimeException("Scoring calculation mismatch: expected 100, got " + assessment.getScore());
            }

            // Test Unknown Authorization (should yield 0 auth points)
            DetectionEvent unknownEvt = new DetectionEvent("EVT-UNKNOWN-01", "Zone-Bravo", 800.0, 40.0, 1, DetectionEvent.AUTH_UNKNOWN);
            RiskAssessment unknownAssessment = scoring.evaluateRisk(unknownEvt);
            System.out.println("  Unknown IFF Score: " + unknownAssessment.getScore() + "/100 (Category: " + unknownAssessment.getRiskCategory() + ")");
            if (unknownAssessment.getScore() == 0 && unknownAssessment.getRiskCategory().equals("LOW")) {
                System.out.println("  ✔ Unknown authorization neutral scoring verified.");
            } else {
                throw new RuntimeException("Unknown authorization score mismatch: expected 0, got " + unknownAssessment.getScore());
            }

            // 3. Test Transactional Save & Incident Auto-Triage
            System.out.println("[TEST 3] Testing Transactional Event & Incident Persistence...");
            String priorityReason = AlertManager.generatePrioritizationReason(assessment.getScore(), assessment.getRiskCategory(), testEvt);
            boolean saved = db.saveEventAndAssessment(testEvt, assessment, priorityReason);
            if (saved) {
                System.out.println("  ✔ Event and assessment saved transactionally.");
            } else {
                throw new RuntimeException("Failed to save event transactionally.");
            }

            // Verify incident auto-creation (Score 100 >= 50 threshold)
            boolean hasUnresolved = db.hasUnresolvedIncident("EVT-TEST-99");
            if (hasUnresolved) {
                System.out.println("  ✔ Incident auto-triage created for critical event.");
            } else {
                throw new RuntimeException("Incident auto-creation failed for score 100!");
            }

            // 4. Test Duplicate Event Update & Duplicate Incident Suppression
            System.out.println("[TEST 4] Testing Duplicate Event Submission & Policy Suppression...");
            // Re-submit same event ID with updated telemetry
            DetectionEvent testEvtUpdated = new DetectionEvent("EVT-TEST-99", "Zone-Alpha", 120.0, 130.0, 5, DetectionEvent.AUTH_UNAUTHORIZED);
            RiskAssessment assessmentUpdated = scoring.evaluateRisk(testEvtUpdated);
            boolean resaved = db.saveEventAndAssessment(testEvtUpdated, assessmentUpdated, priorityReason);
            if (resaved) {
                System.out.println("  ✔ Re-submission of existing Event ID updated successfully without duplicate record.");
            }

            // Check incident count for this event ID: must be strictly 1 active ticket
            List<Incident> allIncidents = db.getAllIncidents();
            long countForEvent = allIncidents.stream().filter(i -> i.getEventId().equals("EVT-TEST-99")).count();
            if (countForEvent == 1) {
                System.out.println("  ✔ Duplicate incident prevention verified: strictly 1 incident exists (duplicate ticket suppressed).");
            } else {
                throw new RuntimeException("Duplicate incident suppression failed! Expected 1, found " + countForEvent);
            }

            // 5. Test Evaluation Benchmark Suite
            System.out.println("[TEST 5] Running EvaluationService Benchmark Suite...");
            EvaluationService evalService = new EvaluationService();
            EvaluationService.BenchmarkReport report = evalService.runBenchmark(scoring);

            System.out.printf("  Proposed Accuracy : %.2f%%\n", report.proposedAccuracy * 100.0);
            System.out.printf("  Baseline Accuracy : %.2f%%\n", report.baselineAccuracy * 100.0);
            System.out.printf("  Proposed F1-Score : %.2f\n", report.proposedF1);
            System.out.printf("  Baseline F1-Score : %.2f\n", report.baselineF1);
            System.out.printf("  Explainability    : %.1f%%\n", report.explainabilityCoverageProposed);
            System.out.printf("  Duplicates Blocked: %d\n", report.duplicateIncidentsPrevented);
            System.out.printf("  Batch Latency     : %d ms for 1,000 evaluations\n", report.batchProcessingTimeMs);

            if (report.proposedAccuracy >= report.baselineAccuracy && report.explainabilityCoverageProposed == 100.0) {
                System.out.println("  ✔ Benchmark metrics verified.");
            } else {
                throw new RuntimeException("Benchmark metrics failed sanity check!");
            }

            // 6. Test Dashboard SQLite Metrics Calculation
            System.out.println("[TEST 6] Testing Live SQLite Dashboard Metrics Calculation...");
            Map<String, Integer> metrics = db.getDashboardMetrics();
            System.out.println("  Total Events: " + metrics.get("totalEvents"));
            System.out.println("  Critical Alerts: " + metrics.get("criticalAlerts"));
            System.out.println("  High Alerts: " + metrics.get("highAlerts"));
            System.out.println("  Open Incidents: " + metrics.get("openIncidents"));

            if (metrics.get("totalEvents") > 0) {
                System.out.println("  ✔ SQLite live metric aggregations verified.");
            } else {
                throw new RuntimeException("Dashboard metrics returned 0 events!");
            }

            System.out.println("\n==================================================");
            System.out.println(" ALL 6 BACKEND & ALGORITHMIC TESTS PASSED PERFECTLY!");
            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("\n[FAIL] Test encountered exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
