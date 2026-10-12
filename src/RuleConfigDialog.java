import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * RuleConfigDialog.java
 *
 * Operational tuning console for adjusting heuristic risk weights and thresholds.
 * Changes are validated, applied to the active RiskScoringService, and persisted
 * to SQLite table 'rule_configurations'.
 */
public class RuleConfigDialog extends JDialog {

    private final DatabaseManager databaseManager;
    private final RiskScoringService scoringService;
    private final Runnable onConfigUpdatedCallback;

    private JTextField txtProxWeight;
    private JTextField txtProxThreshold;
    private JTextField txtSpeedWeight;
    private JTextField txtSpeedThreshold;
    private JTextField txtRepWeight;
    private JTextField txtRepThreshold;
    private JTextField txtUnauthWeight;

    public RuleConfigDialog(Frame parent,
                            DatabaseManager databaseManager,
                            RiskScoringService scoringService,
                            Runnable onConfigUpdatedCallback) {
        super(parent, "Heuristic Risk Scoring Rule Configuration", true);
        this.databaseManager = databaseManager;
        this.scoringService = scoringService;
        this.onConfigUpdatedCallback = onConfigUpdatedCallback;

        setSize(580, 480);
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

        JLabel titleLbl = new JLabel("SCORING WEIGHTS & HORIZON THRESHOLDS");
        titleLbl.setFont(DashboardComponents.FONT_HEADER);
        titleLbl.setForeground(DashboardComponents.ACCENT_BLUE);

        JLabel subLbl = new JLabel("Calibrate sensitivity parameters. Persists directly to SQLite database.");
        subLbl.setFont(DashboardComponents.FONT_SMALL);
        subLbl.setForeground(DashboardComponents.TEXT_SECONDARY);

        headerPanel.add(titleLbl, BorderLayout.NORTH);
        headerPanel.add(subLbl, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Form Fields
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(DashboardComponents.BG_DARK);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        RuleConfiguration config = scoringService.getConfiguration();

        // 1. Proximity
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.4;
        form.add(createLabel("Proximity Weight (pts):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.6;
        txtProxWeight = new JTextField(String.valueOf(config.getProximityWeight()));
        DashboardComponents.styleTextField(txtProxWeight);
        form.add(txtProxWeight, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(createLabel("Proximity Horizon (meters):"), gbc);
        gbc.gridx = 1;
        txtProxThreshold = new JTextField(String.valueOf(config.getProximityThresholdMeters()));
        DashboardComponents.styleTextField(txtProxThreshold);
        form.add(txtProxThreshold, gbc);

        // 2. Speed
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(createLabel("Speed Weight (pts):"), gbc);
        gbc.gridx = 1;
        txtSpeedWeight = new JTextField(String.valueOf(config.getSpeedWeight()));
        DashboardComponents.styleTextField(txtSpeedWeight);
        form.add(txtSpeedWeight, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        form.add(createLabel("Speed Threshold (km/h):"), gbc);
        gbc.gridx = 1;
        txtSpeedThreshold = new JTextField(String.valueOf(config.getSpeedThresholdKmh()));
        DashboardComponents.styleTextField(txtSpeedThreshold);
        form.add(txtSpeedThreshold, gbc);

        // 3. Repetition
        gbc.gridx = 0; gbc.gridy = 4;
        form.add(createLabel("Repetition Weight (pts):"), gbc);
        gbc.gridx = 1;
        txtRepWeight = new JTextField(String.valueOf(config.getRepetitionWeight()));
        DashboardComponents.styleTextField(txtRepWeight);
        form.add(txtRepWeight, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        form.add(createLabel("Repetition Threshold (bursts):"), gbc);
        gbc.gridx = 1;
        txtRepThreshold = new JTextField(String.valueOf(config.getRepetitionThresholdCount()));
        DashboardComponents.styleTextField(txtRepThreshold);
        form.add(txtRepThreshold, gbc);

        // 4. Unauthorized
        gbc.gridx = 0; gbc.gridy = 6;
        form.add(createLabel("Unauthorized Status Weight (pts):"), gbc);
        gbc.gridx = 1;
        txtUnauthWeight = new JTextField(String.valueOf(config.getUnauthorizedWeight()));
        DashboardComponents.styleTextField(txtUnauthWeight);
        form.add(txtUnauthWeight, gbc);

        add(form, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(DashboardComponents.HEADER_BG);

        JButton btnReset = DashboardComponents.createSecondaryButton("🔄 Reset Defaults");
        btnReset.addActionListener(e -> resetToDefaults());
        footer.add(btnReset);

        JButton btnCancel = DashboardComponents.createSecondaryButton("Cancel");
        btnCancel.addActionListener(e -> dispose());
        footer.add(btnCancel);

        JButton btnSave = DashboardComponents.createPrimaryButton("💾 Save Configuration");
        btnSave.addActionListener(e -> saveConfig());
        footer.add(btnSave);

        add(footer, BorderLayout.SOUTH);
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(DashboardComponents.FONT_BODY_BOLD);
        l.setForeground(DashboardComponents.TEXT_SECONDARY);
        return l;
    }

    private void resetToDefaults() {
        txtProxWeight.setText(String.valueOf(RuleConfiguration.DEFAULT_PROXIMITY_WEIGHT));
        txtProxThreshold.setText(String.valueOf(RuleConfiguration.DEFAULT_PROXIMITY_THRESHOLD));
        txtSpeedWeight.setText(String.valueOf(RuleConfiguration.DEFAULT_SPEED_WEIGHT));
        txtSpeedThreshold.setText(String.valueOf(RuleConfiguration.DEFAULT_SPEED_THRESHOLD));
        txtRepWeight.setText(String.valueOf(RuleConfiguration.DEFAULT_REPETITION_WEIGHT));
        txtRepThreshold.setText(String.valueOf(RuleConfiguration.DEFAULT_REPETITION_THRESHOLD));
        txtUnauthWeight.setText(String.valueOf(RuleConfiguration.DEFAULT_UNAUTHORIZED_WEIGHT));
    }

    private void saveConfig() {
        try {
            int pw = Integer.parseInt(txtProxWeight.getText().trim());
            double pt = Double.parseDouble(txtProxThreshold.getText().trim());
            int sw = Integer.parseInt(txtSpeedWeight.getText().trim());
            double st = Double.parseDouble(txtSpeedThreshold.getText().trim());
            int rw = Integer.parseInt(txtRepWeight.getText().trim());
            int rt = Integer.parseInt(txtRepThreshold.getText().trim());
            int uw = Integer.parseInt(txtUnauthWeight.getText().trim());

            ValidationUtils.ValidationResult val = ValidationUtils.validateRuleConfiguration(pw, pt, sw, st, rw, rt, uw);
            if (!val.isValid()) {
                JOptionPane.showMessageDialog(this, val.getErrorMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            RuleConfiguration cfg = new RuleConfiguration(pt, pw, st, sw, rt, rw, uw, 0);
            scoringService.setConfiguration(cfg);
            boolean saved = databaseManager.saveRuleConfiguration(cfg);

            if (saved) {
                JOptionPane.showMessageDialog(this,
                        "Rule configuration successfully updated and saved to SQLite.",
                        "Saved",
                        JOptionPane.INFORMATION_MESSAGE);
                if (onConfigUpdatedCallback != null) {
                    onConfigUpdatedCallback.run();
                }
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to persist configuration to SQLite.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values for all weights and thresholds.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
