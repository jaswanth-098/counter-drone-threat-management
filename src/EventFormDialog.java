import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * EventFormDialog.java
 *
 * Professional modal entry form for ingesting simulated counter-drone detections.
 * Validates telemetry inputs, executes explainable risk scoring, commits data
 * transactionally to SQLite, evaluates incident triage rules, and displays
 * an immediate audit breakdown.
 */
public class EventFormDialog extends JDialog {

    private final JTextField eventIdField;
    private final JComboBox<String> zoneComboBox;
    private final JTextField distanceField;
    private final JTextField speedField;
    private final JSpinner repeatSpinner;
    private final JComboBox<String> authComboBox;

    private final DatabaseManager databaseManager;
    private final RiskScoringService scoringService;
    private final Runnable onSaveCallback;

    public EventFormDialog(Frame parent,
                           DatabaseManager databaseManager,
                           RiskScoringService scoringService,
                           Runnable onSaveCallback) {
        super(parent, "Simulate Counter-Drone Detection Event", true);
        this.databaseManager = databaseManager;
        this.scoringService = scoringService;
        this.onSaveCallback = onSaveCallback;

        setSize(620, 640);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(DashboardComponents.BG_DARK);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(DashboardComponents.HEADER_BG);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DashboardComponents.BORDER_COLOR),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        JLabel titleLabel = new JLabel("TARGET TELEMETRY SIMULATION INGESTION");
        titleLabel.setFont(DashboardComponents.FONT_HEADER);
        titleLabel.setForeground(DashboardComponents.ACCENT_BLUE);

        JLabel subtitleLabel = new JLabel("Input physical radar/RF tracking observations for automated explainable risk assessment.");
        subtitleLabel.setFont(DashboardComponents.FONT_SMALL);
        subtitleLabel.setForeground(DashboardComponents.TEXT_SECONDARY);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        headerPanel.add(subtitleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Center Form Panel
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setBackground(DashboardComponents.BG_DARK);
        formContainer.setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));

        // Quick Preset Buttons Panel (Ideal for viva / faculty demo!)
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        presetPanel.setBackground(DashboardComponents.CARD_BG);
        presetPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR),
                        "DEMONSTRATION QUICK PRESETS",
                        0, 0,
                        DashboardComponents.FONT_SMALL,
                        DashboardComponents.TEXT_SECONDARY
                ),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JButton btnPresetCrit = DashboardComponents.createSecondaryButton("Critical Intruder");
        btnPresetCrit.setFont(DashboardComponents.FONT_SMALL);
        btnPresetCrit.addActionListener(e -> applyPreset("EVT-CRIT-" + (100 + new Random().nextInt(900)),
                "Zone-Alpha (HQ Command)", "150.0", "125.0", 4, DetectionEvent.AUTH_UNAUTHORIZED));

        JButton btnPresetHigh = DashboardComponents.createSecondaryButton("High Unknown Fast");
        btnPresetHigh.setFont(DashboardComponents.FONT_SMALL);
        btnPresetHigh.addActionListener(e -> applyPreset("EVT-HIGH-" + (100 + new Random().nextInt(900)),
                "Zone-Bravo (Runway North)", "280.0", "110.0", 3, DetectionEvent.AUTH_UNKNOWN));

        JButton btnPresetMed = DashboardComponents.createSecondaryButton("Medium Loiterer");
        btnPresetMed.setFont(DashboardComponents.FONT_SMALL);
        btnPresetMed.addActionListener(e -> applyPreset("EVT-MED-" + (100 + new Random().nextInt(900)),
                "Zone-Charlie (Fuel Depot)", "260.0", "45.0", 1, DetectionEvent.AUTH_AUTHORIZED));

        JButton btnPresetLow = DashboardComponents.createSecondaryButton("Low Friendly Patrol");
        btnPresetLow.setFont(DashboardComponents.FONT_SMALL);
        btnPresetLow.addActionListener(e -> applyPreset("EVT-LOW-" + (100 + new Random().nextInt(900)),
                "Zone-Delta (Perimeter West)", "950.0", "30.0", 1, DetectionEvent.AUTH_AUTHORIZED));

        presetPanel.add(btnPresetCrit);
        presetPanel.add(btnPresetHigh);
        presetPanel.add(btnPresetMed);
        presetPanel.add(btnPresetLow);
        formContainer.add(presetPanel);
        formContainer.add(Box.createRigidArea(new Dimension(0, 12)));

        // Form Fields Grid
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(DashboardComponents.BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Event ID
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel lblEventId = createFieldLabel("Event Identifier (ID):");
        fieldsPanel.add(lblEventId, gbc);

        gbc.gridx = 1; gbc.weightx = 0.5;
        eventIdField = new JTextField("EVT-" + (1000 + new Random().nextInt(9000)));
        DashboardComponents.styleTextField(eventIdField);
        fieldsPanel.add(eventIdField, gbc);

        gbc.gridx = 2; gbc.weightx = 0.2;
        JButton btnGenId = DashboardComponents.createSecondaryButton("New ID");
        btnGenId.setFont(DashboardComponents.FONT_SMALL);
        btnGenId.addActionListener(e -> eventIdField.setText("EVT-" + (1000 + new Random().nextInt(9000))));
        fieldsPanel.add(btnGenId, gbc);

        // 2. Zone
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        JLabel lblZone = createFieldLabel("Security Zone:");
        fieldsPanel.add(lblZone, gbc);

        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 0.7;
        String[] zones = {
                "Zone-Alpha (HQ Command)",
                "Zone-Bravo (Runway North)",
                "Zone-Charlie (Fuel Depot)",
                "Zone-Delta (Perimeter West)",
                "Zone-Echo (Radar Installation)"
        };
        zoneComboBox = new JComboBox<>(zones);
        zoneComboBox.setEditable(true);
        DashboardComponents.styleComboBox(zoneComboBox);
        fieldsPanel.add(zoneComboBox, gbc);
        gbc.gridwidth = 1;

        // 3. Distance
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        JLabel lblDist = createFieldLabel("Distance (meters):");
        fieldsPanel.add(lblDist, gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        distanceField = new JTextField("220.0");
        DashboardComponents.styleTextField(distanceField);
        fieldsPanel.add(distanceField, gbc);
        gbc.gridwidth = 1;

        // 4. Speed
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        JLabel lblSpeed = createFieldLabel("Speed (km/h):");
        fieldsPanel.add(lblSpeed, gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        speedField = new JTextField("115.0");
        DashboardComponents.styleTextField(speedField);
        fieldsPanel.add(speedField, gbc);
        gbc.gridwidth = 1;

        // 5. Repetitions
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        JLabel lblRep = createFieldLabel("Detection Repetitions:");
        fieldsPanel.add(lblRep, gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        repeatSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
        repeatSpinner.setFont(DashboardComponents.FONT_BODY);
        fieldsPanel.add(repeatSpinner, gbc);
        gbc.gridwidth = 1;

        // 6. Authorization
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.3;
        JLabel lblAuth = createFieldLabel("IFF Authorization:");
        fieldsPanel.add(lblAuth, gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        String[] authOptions = {
                DetectionEvent.AUTH_UNAUTHORIZED,
                DetectionEvent.AUTH_UNKNOWN,
                DetectionEvent.AUTH_AUTHORIZED
        };
        authComboBox = new JComboBox<>(authOptions);
        DashboardComponents.styleComboBox(authComboBox);
        fieldsPanel.add(authComboBox, gbc);

        formContainer.add(fieldsPanel);
        add(formContainer, BorderLayout.CENTER);

        // Bottom Action Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        buttonPanel.setBackground(DashboardComponents.HEADER_BG);
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DashboardComponents.BORDER_COLOR));

        JButton btnCancel = DashboardComponents.createSecondaryButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = DashboardComponents.createPrimaryButton("⚡ Evaluate Risk & Save Event");
        btnSave.addActionListener(e -> handleSave());

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(DashboardComponents.FONT_BODY_BOLD);
        lbl.setForeground(DashboardComponents.TEXT_PRIMARY);
        return lbl;
    }

    private void applyPreset(String id, String zone, String dist, String speed, int rep, String auth) {
        eventIdField.setText(id);
        zoneComboBox.setSelectedItem(zone);
        distanceField.setText(dist);
        speedField.setText(speed);
        repeatSpinner.setValue(rep);
        authComboBox.setSelectedItem(auth);
    }

    private void handleSave() {
        String eventId = eventIdField.getText().trim();
        String zone = (zoneComboBox.getSelectedItem() != null) ? zoneComboBox.getSelectedItem().toString().trim() : "";
        String distStr = distanceField.getText().trim();
        String speedStr = speedField.getText().trim();
        String repStr = repeatSpinner.getValue().toString();
        String auth = (authComboBox.getSelectedItem() != null) ? authComboBox.getSelectedItem().toString().trim() : "";

        // 1. Input Validation
        ValidationUtils.ValidationResult val = ValidationUtils.validateDetectionEvent(eventId, zone, distStr, speedStr, repStr, auth);
        if (!val.isValid()) {
            JOptionPane.showMessageDialog(this,
                    val.getErrorMessage(),
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        double distance = Double.parseDouble(distStr);
        double speed = Double.parseDouble(speedStr);
        int repeats = Integer.parseInt(repStr);

        // 2. Create DetectionEvent domain entity
        DetectionEvent event = new DetectionEvent(eventId, zone, distance, speed, repeats, auth);

        // 3. Compute Explainable Risk Assessment
        RiskAssessment assessment = scoringService.evaluateRisk(event);

        // 4. Generate Prioritization Rationale
        String priorityReason = AlertManager.generatePrioritizationReason(assessment.getScore(), assessment.getRiskCategory(), event);

        // 5. Transactional commit to SQLite
        boolean saved = databaseManager.saveEventAndAssessment(event, assessment, priorityReason);
        if (!saved) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save detection event into SQLite database. Transaction rolled back.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 6. Refresh Dashboard
        if (onSaveCallback != null) {
            onSaveCallback.run();
        }

        // 7. Show immediate Explainability Audit Dialog
        showAssessmentResultDialog(event, assessment);

        dispose();
    }

    /**
     * Renders a detailed modal audit dialog showcasing the explainable score calculation.
     */
    private void showAssessmentResultDialog(DetectionEvent event, RiskAssessment assessment) {
        JDialog resultDlg = new JDialog(this, "Explainable Threat Assessment Report", true);
        resultDlg.setSize(620, 520);
        resultDlg.setLocationRelativeTo(this);
        resultDlg.getContentPane().setBackground(DashboardComponents.BG_DARK);
        resultDlg.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DashboardComponents.HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel title = new JLabel(String.format("EVENT %s - EVALUATION COMPLETE", assessment.getEventId()));
        title.setFont(DashboardComponents.FONT_HEADER);
        title.setForeground(DashboardComponents.ACCENT_BLUE);

        Color catColor = assessment.getRiskCategory().equals("CRITICAL") ? DashboardComponents.COLOR_CRITICAL :
                assessment.getRiskCategory().equals("HIGH") ? DashboardComponents.COLOR_HIGH :
                        assessment.getRiskCategory().equals("MEDIUM") ? DashboardComponents.COLOR_MEDIUM :
                                DashboardComponents.COLOR_LOW;

        JLabel scoreBadge = new JLabel(String.format(" %s [%d/100] ", assessment.getRiskCategory(), assessment.getScore()));
        scoreBadge.setFont(DashboardComponents.FONT_HEADER);
        scoreBadge.setOpaque(true);
        scoreBadge.setBackground(catColor);
        scoreBadge.setForeground(Color.BLACK);

        header.add(title, BorderLayout.WEST);
        header.add(scoreBadge, BorderLayout.EAST);
        resultDlg.add(header, BorderLayout.NORTH);

        JTextArea area = new JTextArea();
        area.setBackground(DashboardComponents.CARD_BG);
        area.setForeground(DashboardComponents.TEXT_PRIMARY);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setEditable(false);
        area.setMargin(new Insets(14, 14, 14, 14));

        StringBuilder sb = new StringBuilder();
        sb.append("======================================================================\n");
        sb.append("     EXPLAINABLE RISK SCORING AUDIT TRAIL (RULE-BASED INFERENCE)     \n");
        sb.append("======================================================================\n\n");
        sb.append("Event ID           : ").append(event.getEventId()).append("\n");
        sb.append("Perimeter Sector   : ").append(event.getZone()).append("\n");
        sb.append("Distance to Asset  : ").append(String.format("%.1f meters", event.getDistanceMeters())).append("\n");
        sb.append("Kinematic Speed    : ").append(String.format("%.1f km/h", event.getSpeedKmh())).append("\n");
        sb.append("Burst Confirmations: ").append(event.getRepeatCount()).append(" bursts\n");
        sb.append("IFF Transponder    : ").append(event.getAuthorizationStatus()).append("\n\n");

        sb.append("----------------------------------------------------------------------\n");
        sb.append("TRIGGERED RULES & POINT ALLOCATIONS:\n");
        sb.append("----------------------------------------------------------------------\n");
        sb.append(assessment.getFormattedExplanations()).append("\n\n");

        sb.append("----------------------------------------------------------------------\n");
        sb.append("NON-TRIGGERED RULES (WITHHELD POINTS):\n");
        sb.append("----------------------------------------------------------------------\n");
        sb.append(assessment.getFormattedNonTriggered()).append("\n\n");

        sb.append("----------------------------------------------------------------------\n");
        sb.append("TACTICAL SUMMARY & RECOMMENDATION:\n");
        sb.append("----------------------------------------------------------------------\n");
        sb.append(assessment.getHumanReadableSummary()).append("\n\n");

        if (assessment.getScore() >= 50) {
            sb.append(">>> OPERATIONAL TRIAGE: Threat score exceeds threshold (>= 50).\n");
            sb.append("    Security Incident generated or updated under duplicate-control policy.\n");
        }

        area.setText(sb.toString());
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        resultDlg.add(scroll, BorderLayout.CENTER);

        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        closePanel.setBackground(DashboardComponents.HEADER_BG);
        JButton btnClose = DashboardComponents.createPrimaryButton("Dismiss Report");
        btnClose.addActionListener(e -> resultDlg.dispose());
        closePanel.add(btnClose);
        resultDlg.add(closePanel, BorderLayout.SOUTH);

        resultDlg.setVisible(true);
    }
}
