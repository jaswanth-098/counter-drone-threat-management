import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * EvaluationUI.java
 *
 * Professional academic evaluation dashboard window.
 * Displays comparative empirical benchmarks, classification performance metrics,
 * confusion matrices, duplicate-suppression counts, explainability coverage,
 * and high-throughput execution latency.
 */
public class EvaluationUI extends JDialog {

    private final DatabaseManager databaseManager;
    private final RiskScoringService scoringService;
    private final EvaluationService evaluationService;

    private EvaluationService.BenchmarkReport currentReport;

    // UI metric labels
    private JLabel lblPropAccuracy;
    private JLabel lblPropPrecision;
    private JLabel lblPropRecall;
    private JLabel lblPropF1;
    private JLabel lblBaseAccuracy;
    private JLabel lblBasePrecision;
    private JLabel lblBaseRecall;
    private JLabel lblBaseF1;
    private JLabel lblExplainCoverage;
    private JLabel lblDupePrevented;
    private JLabel lblBatchLatency;

    // Confusion Matrix Panels
    private JPanel propConfusionGrid;
    private JPanel baseConfusionGrid;

    // Test cases table
    private JTable caseTable;
    private DefaultTableModel caseTableModel;

    public EvaluationUI(Frame parent, DatabaseManager databaseManager, RiskScoringService scoringService) {
        super(parent, "Evaluation & Baseline Comparative Benchmark Console", true);
        this.databaseManager = databaseManager;
        this.scoringService = scoringService;
        this.evaluationService = new EvaluationService();

        setSize(1120, 720);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(DashboardComponents.BG_DARK);
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(DashboardComponents.HEADER_BG);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DashboardComponents.BORDER_COLOR),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)
        ));

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setBackground(DashboardComponents.HEADER_BG);

        JLabel titleLbl = new JLabel("EMPIRICAL EVALUATION & BASELINE COMPARISON");
        titleLbl.setFont(DashboardComponents.FONT_HEADER);
        titleLbl.setForeground(DashboardComponents.ACCENT_BLUE);

        JLabel subLbl = new JLabel("Reproducible evaluation on synthetic counter-drone dataset (Proposed Multi-Factor Heuristic vs. Naive Baseline).");
        subLbl.setFont(DashboardComponents.FONT_SMALL);
        subLbl.setForeground(DashboardComponents.TEXT_SECONDARY);

        titleGrp.add(titleLbl);
        titleGrp.add(Box.createRigidArea(new Dimension(0, 4)));
        titleGrp.add(subLbl);
        headerPanel.add(titleGrp, BorderLayout.WEST);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setBackground(DashboardComponents.HEADER_BG);

        JButton btnRun = DashboardComponents.createPrimaryButton("⚡ Run Evaluation Benchmark");
        btnRun.addActionListener(e -> executeBenchmark());
        btnBar.add(btnRun);

        JButton btnSaveDb = DashboardComponents.createSecondaryButton("💾 Save Results to SQLite");
        btnSaveDb.addActionListener(e -> saveResultsToDatabase());
        btnBar.add(btnSaveDb);

        headerPanel.add(btnBar, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Center Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(DashboardComponents.BG_DARK);
        tabbedPane.setForeground(DashboardComponents.TEXT_PRIMARY);
        tabbedPane.setFont(DashboardComponents.FONT_BODY_BOLD);

        // Tab 1: Executive Metrics & Confusion Matrices
        tabbedPane.addTab("Benchmark Summary & Matrices", createSummaryTab());

        // Tab 2: Granular Test Cases Table
        tabbedPane.addTab("Synthetic Dataset Inspection (15 Scenarios)", createCasesTableTab());

        // Tab 3: Academic Methodology & Limitations
        tabbedPane.addTab("Academic Methodology & Limitations", createMethodologyTab());

        add(tabbedPane, BorderLayout.CENTER);

        // Run initial benchmark on open
        executeBenchmark();
    }

    private JPanel createSummaryTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(DashboardComponents.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Top: KPI Cards Grid
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 12));
        kpiRow.setBackground(DashboardComponents.BG_DARK);

        kpiRow.add(createKpiCard("PROPOSED ACCURACY", lblPropAccuracy = new JLabel("0.0%"), "Heuristic Rule Engine", DashboardComponents.ACCENT_BLUE));
        kpiRow.add(createKpiCard("BASELINE ACCURACY", lblBaseAccuracy = new JLabel("0.0%"), "Naive Threshold Classifier", DashboardComponents.COLOR_MEDIUM));
        kpiRow.add(createKpiCard("EXPLAINABILITY COVERAGE", lblExplainCoverage = new JLabel("100.0%"), "Audit trail availability", DashboardComponents.COLOR_LOW));
        kpiRow.add(createKpiCard("DUPLICATE SUPPRESSION", lblDupePrevented = new JLabel("0"), "Redundant tickets blocked", DashboardComponents.ACCENT_PURPLE));
        panel.add(kpiRow, BorderLayout.NORTH);

        // Center: Side-by-Side Detailed Metrics & Confusion Matrices
        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 14, 14));
        centerGrid.setBackground(DashboardComponents.BG_DARK);

        // Left Panel: Proposed Engine Metrics & Matrix
        JPanel leftProposed = createEngineDetailCard("PROPOSED EXPLAINABLE ENGINE", true);
        centerGrid.add(leftProposed);

        // Right Panel: Baseline Classifier Metrics & Matrix
        JPanel rightBaseline = createEngineDetailCard("TRADITIONAL NAIVE BASELINE", false);
        centerGrid.add(rightBaseline);

        panel.add(centerGrid, BorderLayout.CENTER);

        // Bottom: Latency & Operational Throughput Bar
        JPanel footerBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        footerBar.setBackground(DashboardComponents.CARD_BG);
        footerBar.setBorder(BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true));

        JLabel lblBenchTitle = new JLabel("BATCH THROUGHPUT BENCHMARK (1,000 SYNTHETIC EVENTS):");
        lblBenchTitle.setFont(DashboardComponents.FONT_BODY_BOLD);
        lblBenchTitle.setForeground(DashboardComponents.ACCENT_BLUE);
        footerBar.add(lblBenchTitle);

        lblBatchLatency = new JLabel("Execution Time: -- ms (~ -- evaluations/sec)");
        lblBatchLatency.setFont(DashboardComponents.FONT_BODY);
        lblBatchLatency.setForeground(DashboardComponents.TEXT_PRIMARY);
        footerBar.add(lblBatchLatency);

        panel.add(footerBar, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createKpiCard(String title, JLabel valueLbl, String sub, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(DashboardComponents.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 10));
        t.setForeground(DashboardComponents.TEXT_SECONDARY);

        valueLbl.setFont(DashboardComponents.FONT_METRIC);
        valueLbl.setForeground(accent);

        JLabel s = new JLabel(sub);
        s.setFont(DashboardComponents.FONT_SMALL);
        s.setForeground(DashboardComponents.TEXT_MUTED);

        card.add(t);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(valueLbl);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(s);
        return card;
    }

    private JPanel createEngineDetailCard(String title, boolean isProposed) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(DashboardComponents.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isProposed ? DashboardComponents.ACCENT_BLUE : DashboardComponents.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JLabel header = new JLabel(title);
        header.setFont(DashboardComponents.FONT_HEADER);
        header.setForeground(isProposed ? DashboardComponents.ACCENT_BLUE : DashboardComponents.COLOR_MEDIUM);
        card.add(header, BorderLayout.NORTH);

        // Metrics Table Grid
        JPanel metricsPanel = new JPanel(new GridLayout(4, 2, 8, 8));
        metricsPanel.setBackground(DashboardComponents.CARD_BG);

        if (isProposed) {
            lblPropPrecision = addMetricRow(metricsPanel, "Precision:");
            lblPropRecall = addMetricRow(metricsPanel, "Recall:");
            lblPropF1 = addMetricRow(metricsPanel, "F1-Score:");
            addMetricValue(metricsPanel, "Explainability:", "100.0% (Granular Reasons)");
        } else {
            lblBasePrecision = addMetricRow(metricsPanel, "Precision:");
            lblBaseRecall = addMetricRow(metricsPanel, "Recall:");
            lblBaseF1 = addMetricRow(metricsPanel, "F1-Score:");
            addMetricValue(metricsPanel, "Explainability:", "0.0% (Opaque Binary)");
        }
        card.add(metricsPanel, BorderLayout.CENTER);

        // Confusion Matrix Container
        JPanel matrixContainer = new JPanel(new BorderLayout(4, 4));
        matrixContainer.setBackground(DashboardComponents.CARD_BG);

        JLabel matTitle = new JLabel("CONFUSION MATRIX (HIGH_RISK = POSITIVE):");
        matTitle.setFont(DashboardComponents.FONT_SMALL);
        matTitle.setForeground(DashboardComponents.TEXT_SECONDARY);
        matrixContainer.add(matTitle, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 6, 6));
        grid.setBackground(DashboardComponents.CARD_BG);
        grid.setPreferredSize(new Dimension(280, 80));

        if (isProposed) {
            propConfusionGrid = grid;
        } else {
            baseConfusionGrid = grid;
        }
        matrixContainer.add(grid, BorderLayout.CENTER);

        card.add(matrixContainer, BorderLayout.SOUTH);
        return card;
    }

    private JLabel addMetricRow(JPanel panel, String name) {
        JLabel lName = new JLabel(name);
        lName.setFont(DashboardComponents.FONT_BODY_BOLD);
        lName.setForeground(DashboardComponents.TEXT_SECONDARY);
        panel.add(lName);

        JLabel lVal = new JLabel("0.00");
        lVal.setFont(DashboardComponents.FONT_BODY_BOLD);
        lVal.setForeground(DashboardComponents.TEXT_PRIMARY);
        panel.add(lVal);
        return lVal;
    }

    private void addMetricValue(JPanel panel, String name, String val) {
        JLabel lName = new JLabel(name);
        lName.setFont(DashboardComponents.FONT_BODY_BOLD);
        lName.setForeground(DashboardComponents.TEXT_SECONDARY);
        panel.add(lName);

        JLabel lVal = new JLabel(val);
        lVal.setFont(DashboardComponents.FONT_BODY);
        lVal.setForeground(DashboardComponents.TEXT_PRIMARY);
        panel.add(lVal);
    }

    private JPanel createCasesTableTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(DashboardComponents.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        String[] cols = {
                "Case ID", "Scenario Description", "Distance", "Speed", "Repeats", "Auth",
                "Ground Truth", "Baseline Pred.", "Proposed Score", "Proposed Band", "Proposed Match"
        };

        caseTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        caseTable = new JTable(caseTableModel);
        DashboardComponents.styleTable(caseTable);
        caseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        caseTable.getColumnModel().getColumn(0).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        caseTable.getColumnModel().getColumn(1).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(false));
        caseTable.getColumnModel().getColumn(2).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        caseTable.getColumnModel().getColumn(3).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        caseTable.getColumnModel().getColumn(4).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        caseTable.getColumnModel().getColumn(5).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        caseTable.getColumnModel().getColumn(6).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        caseTable.getColumnModel().getColumn(7).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        caseTable.getColumnModel().getColumn(8).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        caseTable.getColumnModel().getColumn(9).setCellRenderer(new DashboardComponents.RiskBadgeRenderer());
        caseTable.getColumnModel().getColumn(10).setCellRenderer(new MatchBadgeRenderer());

        caseTable.getColumnModel().getColumn(0).setPreferredWidth(65);
        caseTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        caseTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        caseTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        caseTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        caseTable.getColumnModel().getColumn(5).setPreferredWidth(90);
        caseTable.getColumnModel().getColumn(6).setPreferredWidth(90);
        caseTable.getColumnModel().getColumn(7).setPreferredWidth(90);
        caseTable.getColumnModel().getColumn(8).setPreferredWidth(90);
        caseTable.getColumnModel().getColumn(9).setPreferredWidth(90);
        caseTable.getColumnModel().getColumn(10).setPreferredWidth(90);

        JScrollPane scroll = new JScrollPane(caseTable);
        scroll.setBorder(BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMethodologyTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(DashboardComponents.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JTextArea area = new JTextArea();
        area.setBackground(DashboardComponents.CARD_BG);
        area.setForeground(DashboardComponents.TEXT_PRIMARY);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(new Insets(16, 16, 16, 16));

        StringBuilder sb = new StringBuilder();
        sb.append("ACADEMIC BENCHMARK METHODOLOGY & LIMITATIONS\n");
        sb.append("========================================================================================\n\n");
        sb.append("1. OBJECTIVE OF EVALUATION:\n");
        sb.append("The objective of this experimental evaluation is to empirically validate that an explainable,\n");
        sb.append("multi-factor weighted heuristic engine provides superior threat discrimination, false alarm\n");
        sb.append("mitigation, and operational transparency compared to a traditional unweighted threshold filter.\n\n");

        sb.append("2. BASELINE CLASSIFIER SPECIFICATION:\n");
        sb.append("The comparative baseline represents legacy counter-drone boundary detection systems:\n");
        sb.append("  • Classification Rule: Classifies as HIGH_RISK if (Distance <= 300m OR Speed >= 100 km/h).\n");
        sb.append("  • Systematic Shortcomings:\n");
        sb.append("    - Indiscriminately flags authorized fast drones as high-risk threats (elevated False Positive rate).\n");
        sb.append("    - Fails to flag slow unauthorized intruders beyond 300m (elevated False Negative rate).\n");
        sb.append("    - Provides ZERO explanation for alert triggers, causing tactical operator ambiguity.\n\n");

        sb.append("3. PROPOSED EXPLAINABLE ENGINE SUPERIORITY:\n");
        sb.append("  • Multi-Factor Synthesis: Concurrently factors proximity, velocity, burst persistence, and IFF.\n");
        sb.append("  • Explainability: 100% of generated threat alerts feature an itemized factor score breakdown.\n");
        sb.append("  • Safe UNKNOWN Handling: Treats unverified transponders neutrally (0 pts) to avert alarm fatigue.\n");
        sb.append("  • Duplicate Suppression: Prevents repetitive incident tickets for identical targets.\n\n");

        sb.append("4. LIMITATIONS & ETHICAL DISCLOSURE:\n");
        sb.append("  • Synthetic Benchmark: Dataset comprises 15 curated synthetic test cases constructed to test edge\n");
        sb.append("    conditions. Real counter-drone deployments require radar Doppler, RF signature, and EO/IR tracking.\n");
        sb.append("  • Heuristic Weights: Point allocations (+35, +20, +20, +25) are prototype demonstration weights\n");
        sb.append("    and must be recalibrated against empirical site vulnerability assessments before field deployment.\n");
        sb.append("  • Non-AI Notice: The system employs deterministic, explainable heuristic inference, NOT a black-box\n");
        sb.append("    trained neural network or deep learning model.\n");

        area.setText(sb.toString());
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void executeBenchmark() {
        currentReport = evaluationService.runBenchmark(scoringService);

        // Update KPI Cards
        lblPropAccuracy.setText(String.format("%.1f%%", currentReport.proposedAccuracy * 100.0));
        lblBaseAccuracy.setText(String.format("%.1f%%", currentReport.baselineAccuracy * 100.0));
        lblExplainCoverage.setText(String.format("%.1f%%", currentReport.explainabilityCoverageProposed));
        lblDupePrevented.setText(String.valueOf(currentReport.duplicateIncidentsPrevented));

        // Update Proposed Metrics
        lblPropPrecision.setText(String.format("%.2f", currentReport.proposedPrecision));
        lblPropRecall.setText(String.format("%.2f", currentReport.proposedRecall));
        lblPropF1.setText(String.format("%.2f", currentReport.proposedF1));

        // Update Baseline Metrics
        lblBasePrecision.setText(String.format("%.2f", currentReport.baselinePrecision));
        lblBaseRecall.setText(String.format("%.2f", currentReport.baselineRecall));
        lblBaseF1.setText(String.format("%.2f", currentReport.baselineF1));

        // Update Confusion Matrices
        updateConfusionGrid(propConfusionGrid, currentReport.proposedTP, currentReport.proposedFP,
                currentReport.proposedFN, currentReport.proposedTN, DashboardComponents.ACCENT_BLUE);

        updateConfusionGrid(baseConfusionGrid, currentReport.baselineTP, currentReport.baselineFP,
                currentReport.baselineFN, currentReport.baselineTN, DashboardComponents.COLOR_MEDIUM);

        // Update Latency
        long throughput = currentReport.batchProcessingTimeMs > 0 ? (1000L * 1000 / currentReport.batchProcessingTimeMs) : 100000;
        lblBatchLatency.setText(String.format("Execution Time: %d ms (~ %s evaluations/sec)",
                currentReport.batchProcessingTimeMs, String.format("%,d", throughput)));

        // Update Table
        caseTableModel.setRowCount(0);
        for (EvaluationService.EvaluationItemResult item : currentReport.itemResults) {
            caseTableModel.addRow(new Object[]{
                    item.testCase.caseId,
                    item.testCase.description,
                    String.format("%.0fm", item.testCase.distanceMeters),
                    String.format("%.0f km/h", item.testCase.speedKmh),
                    item.testCase.repeatCount,
                    item.testCase.authorizationStatus,
                    item.testCase.groundTruthCategory,
                    item.baselineBinaryClass,
                    item.proposedScore,
                    item.proposedBand,
                    item.proposedCorrect ? "MATCH" : "MISMATCH"
            });
        }
    }

    private void updateConfusionGrid(JPanel grid, int tp, int fp, int fn, int tn, Color accent) {
        grid.removeAll();
        grid.add(createMatrixCell("True Pos (TP)", tp, accent));
        grid.add(createMatrixCell("False Pos (FP)", fp, DashboardComponents.COLOR_CRITICAL));
        grid.add(createMatrixCell("False Neg (FN)", fn, DashboardComponents.COLOR_CRITICAL));
        grid.add(createMatrixCell("True Neg (TN)", tn, accent));
        grid.revalidate();
        grid.repaint();
    }

    private JPanel createMatrixCell(String label, int count, Color col) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(24, 33, 47));
        p.setBorder(BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR));

        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(DashboardComponents.FONT_SMALL);
        lbl.setForeground(DashboardComponents.TEXT_MUTED);

        JLabel val = new JLabel(String.valueOf(count), SwingConstants.CENTER);
        val.setFont(DashboardComponents.FONT_HEADER);
        val.setForeground(col);

        p.add(lbl, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    private void saveResultsToDatabase() {
        if (currentReport == null) {
            JOptionPane.showMessageDialog(this, "Run evaluation first.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String notes = String.format("Proposed F1: %.2f vs Baseline F1: %.2f | Latency: %d ms | Duplicates Blocked: %d",
                currentReport.proposedF1, currentReport.baselineF1,
                currentReport.batchProcessingTimeMs, currentReport.duplicateIncidentsPrevented);

        boolean saved = databaseManager.saveEvaluationResult(
                currentReport.totalCases,
                currentReport.proposedAccuracy,
                currentReport.proposedPrecision,
                currentReport.proposedRecall,
                currentReport.proposedF1,
                notes
        );

        if (saved) {
            JOptionPane.showMessageDialog(this,
                    "Benchmark evaluation results persisted to SQLite 'evaluation_results' table.",
                    "Persistence Confirmed",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Error saving evaluation to SQLite.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class MatchBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            label.setOpaque(true);

            String match = value != null ? value.toString() : "";
            if ("MATCH".equalsIgnoreCase(match)) {
                label.setBackground(new Color(34, 197, 94, 35));
                label.setForeground(DashboardComponents.COLOR_LOW);
                label.setBorder(BorderFactory.createLineBorder(DashboardComponents.COLOR_LOW, 1, true));
            } else {
                label.setBackground(new Color(239, 68, 68, 35));
                label.setForeground(DashboardComponents.COLOR_CRITICAL);
                label.setBorder(BorderFactory.createLineBorder(DashboardComponents.COLOR_CRITICAL, 1, true));
            }
            return label;
        }
    }
}
