import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * IncidentManagerUI.java
 *
 * Dedicated incident triage and response management window.
 * Allows tactical operations center personnel to track active drone incursions,
 * transition lifecycle states (OPEN -> IN_PROGRESS -> RESOLVED), assign response teams,
 * and maintain an operational log audit trail.
 *
 * Enforces strict duplicate-incident prevention policies to avoid multiple active tickets
 * for the same physical detection event.
 */
public class IncidentManagerUI extends JDialog {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DatabaseManager databaseManager;
    private final Runnable onDataChangedCallback;

    private JTable incidentTable;
    private DefaultTableModel tableModel;

    // Detail editing fields
    private JLabel lblSelectedIncidentId;
    private JLabel lblSelectedEventId;
    private JLabel lblRelatedThreat;
    private JComboBox<String> comboStatus;
    private JTextField txtAssignedTo;
    private JTextArea txtNotes;
    private JLabel lblCreatedAt;
    private JLabel lblUpdatedAt;

    private JComboBox<String> filterStatusCombo;
    private String initialFilterEventId;

    public IncidentManagerUI(Frame parent, DatabaseManager databaseManager, Runnable onDataChangedCallback) {
        this(parent, databaseManager, onDataChangedCallback, null);
    }

    public IncidentManagerUI(Frame parent, DatabaseManager databaseManager, Runnable onDataChangedCallback, String targetEventId) {
        super(parent, "Operational Incident Management & Triage Console", true);
        this.databaseManager = databaseManager;
        this.onDataChangedCallback = onDataChangedCallback;
        this.initialFilterEventId = targetEventId;

        setSize(1080, 700);
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

        JLabel titleLbl = new JLabel("TACTICAL INCIDENT MANAGEMENT & DISPATCH");
        titleLbl.setFont(DashboardComponents.FONT_HEADER);
        titleLbl.setForeground(DashboardComponents.ACCENT_BLUE);

        JLabel subLbl = new JLabel("Triage active threats, update assignment vectors, and enforce duplicate-incident suppression policies.");
        subLbl.setFont(DashboardComponents.FONT_SMALL);
        subLbl.setForeground(DashboardComponents.TEXT_SECONDARY);

        titleGrp.add(titleLbl);
        titleGrp.add(Box.createRigidArea(new Dimension(0, 4)));
        titleGrp.add(subLbl);
        headerPanel.add(titleGrp, BorderLayout.WEST);

        // Header controls (Filter & Create)
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setBackground(DashboardComponents.HEADER_BG);

        JLabel lblFilter = new JLabel("Status Filter:");
        lblFilter.setFont(DashboardComponents.FONT_SMALL);
        lblFilter.setForeground(DashboardComponents.TEXT_SECONDARY);
        filterPanel.add(lblFilter);

        filterStatusCombo = new JComboBox<>(new String[]{"ALL", "OPEN", "IN_PROGRESS", "RESOLVED"});
        DashboardComponents.styleComboBox(filterStatusCombo);
        filterStatusCombo.addActionListener(e -> loadIncidents());
        filterPanel.add(filterStatusCombo);

        JButton btnNewManual = DashboardComponents.createSecondaryButton("➕ Manual Incident");
        btnNewManual.addActionListener(e -> showManualIncidentDialog());
        filterPanel.add(btnNewManual);

        JButton btnRefresh = DashboardComponents.createSecondaryButton("🔄 Refresh");
        btnRefresh.addActionListener(e -> loadIncidents());
        filterPanel.add(btnRefresh);

        headerPanel.add(filterPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Center Split: Left = Table, Right = Detail Inspector / Editor
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(640);
        splitPane.setBackground(DashboardComponents.BG_DARK);
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        // --- Left Table Panel ---
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(DashboardComponents.CARD_BG);
        tableContainer.setBorder(BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true));

        String[] cols = {"Incident ID", "Event ID", "Status", "Assigned Unit", "Updated At"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        incidentTable = new JTable(tableModel);
        DashboardComponents.styleTable(incidentTable);
        incidentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Renderers
        incidentTable.getColumnModel().getColumn(0).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(false));
        incidentTable.getColumnModel().getColumn(1).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(false));
        incidentTable.getColumnModel().getColumn(2).setCellRenderer(new DashboardComponents.StatusBadgeRenderer());
        incidentTable.getColumnModel().getColumn(3).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(false));
        incidentTable.getColumnModel().getColumn(4).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));

        incidentTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        incidentTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        incidentTable.getColumnModel().getColumn(2).setPreferredWidth(95);
        incidentTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        incidentTable.getColumnModel().getColumn(4).setPreferredWidth(130);

        incidentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displaySelectedIncidentDetails();
            }
        });

        JScrollPane tableScroll = new JScrollPane(incidentTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableContainer.add(tableScroll, BorderLayout.CENTER);
        splitPane.setLeftComponent(tableContainer);

        // --- Right Detail Editor Panel ---
        JPanel detailContainer = new JPanel(new BorderLayout());
        detailContainer.setBackground(DashboardComponents.CARD_BG);
        detailContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JLabel detailTitle = new JLabel("INCIDENT DETAILS & AUDIT LOG");
        detailTitle.setFont(DashboardComponents.FONT_HEADER);
        detailTitle.setForeground(DashboardComponents.ACCENT_BLUE);
        detailContainer.add(detailTitle, BorderLayout.NORTH);

        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setBackground(DashboardComponents.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Incident ID (Read only)
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formGrid.add(createDetailLabel("Incident ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        lblSelectedIncidentId = new JLabel("- Select an incident -");
        lblSelectedIncidentId.setFont(DashboardComponents.FONT_BODY_BOLD);
        lblSelectedIncidentId.setForeground(DashboardComponents.TEXT_PRIMARY);
        formGrid.add(lblSelectedIncidentId, gbc);

        // Event ID
        gbc.gridx = 0; gbc.gridy = 1;
        formGrid.add(createDetailLabel("Event ID:"), gbc);
        gbc.gridx = 1;
        lblSelectedEventId = new JLabel("-");
        lblSelectedEventId.setFont(DashboardComponents.FONT_BODY);
        lblSelectedEventId.setForeground(DashboardComponents.ACCENT_BLUE);
        formGrid.add(lblSelectedEventId, gbc);

        // Related Threat Posture
        gbc.gridx = 0; gbc.gridy = 2;
        formGrid.add(createDetailLabel("Threat Assessment:"), gbc);
        gbc.gridx = 1;
        lblRelatedThreat = new JLabel("-");
        lblRelatedThreat.setFont(DashboardComponents.FONT_BODY_BOLD);
        lblRelatedThreat.setForeground(DashboardComponents.COLOR_HIGH);
        formGrid.add(lblRelatedThreat, gbc);

        // Status ComboBox
        gbc.gridx = 0; gbc.gridy = 3;
        formGrid.add(createDetailLabel("Status:"), gbc);
        gbc.gridx = 1;
        comboStatus = new JComboBox<>(new String[]{"OPEN", "IN_PROGRESS", "RESOLVED"});
        DashboardComponents.styleComboBox(comboStatus);
        formGrid.add(comboStatus, gbc);

        // Assigned To
        gbc.gridx = 0; gbc.gridy = 4;
        formGrid.add(createDetailLabel("Assigned Unit:"), gbc);
        gbc.gridx = 1;
        txtAssignedTo = new JTextField();
        DashboardComponents.styleTextField(txtAssignedTo);
        formGrid.add(txtAssignedTo, gbc);

        // Created At
        gbc.gridx = 0; gbc.gridy = 5;
        formGrid.add(createDetailLabel("Created:"), gbc);
        gbc.gridx = 1;
        lblCreatedAt = new JLabel("-");
        lblCreatedAt.setFont(DashboardComponents.FONT_SMALL);
        lblCreatedAt.setForeground(DashboardComponents.TEXT_SECONDARY);
        formGrid.add(lblCreatedAt, gbc);

        // Updated At
        gbc.gridx = 0; gbc.gridy = 6;
        formGrid.add(createDetailLabel("Last Updated:"), gbc);
        gbc.gridx = 1;
        lblUpdatedAt = new JLabel("-");
        lblUpdatedAt.setFont(DashboardComponents.FONT_SMALL);
        lblUpdatedAt.setForeground(DashboardComponents.TEXT_SECONDARY);
        formGrid.add(lblUpdatedAt, gbc);

        // Notes Area
        gbc.gridx = 0; gbc.gridy = 7;
        formGrid.add(createDetailLabel("Notes / Log:"), gbc);
        gbc.gridx = 1;
        txtNotes = new JTextArea(5, 20);
        txtNotes.setBackground(new Color(15, 23, 42));
        txtNotes.setForeground(DashboardComponents.TEXT_PRIMARY);
        txtNotes.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);
        JScrollPane noteScroll = new JScrollPane(txtNotes);
        noteScroll.setBorder(BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR));
        formGrid.add(noteScroll, gbc);

        detailContainer.add(formGrid, BorderLayout.CENTER);

        // Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actionPanel.setBackground(DashboardComponents.CARD_BG);

        JButton btnUpdate = DashboardComponents.createPrimaryButton("💾 Save Incident Changes");
        btnUpdate.addActionListener(e -> updateSelectedIncident());
        actionPanel.add(btnUpdate);

        detailContainer.add(actionPanel, BorderLayout.SOUTH);
        splitPane.setRightComponent(detailContainer);

        add(splitPane, BorderLayout.CENTER);

        // Load initial data
        loadIncidents();
    }

    private JLabel createDetailLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(DashboardComponents.FONT_BODY_BOLD);
        l.setForeground(DashboardComponents.TEXT_SECONDARY);
        return l;
    }

    public void loadIncidents() {
        tableModel.setRowCount(0);
        List<Incident> list = databaseManager.getAllIncidents();
        String filter = filterStatusCombo.getSelectedItem() != null ? filterStatusCombo.getSelectedItem().toString() : "ALL";

        for (Incident inc : list) {
            if (!filter.equals("ALL") && !inc.getStatus().equalsIgnoreCase(filter)) {
                continue;
            }
            tableModel.addRow(new Object[]{
                    inc.getIncidentId(),
                    inc.getEventId(),
                    inc.getStatus(),
                    inc.getAssignedTo(),
                    inc.getUpdatedAt()
            });
        }

        int selectedRow = 0;
        if (tableModel.getRowCount() > 0) {
            if (initialFilterEventId != null) {
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (initialFilterEventId.equalsIgnoreCase((String) tableModel.getValueAt(i, 1))) {
                        selectedRow = i;
                        break;
                    }
                }
            }
            incidentTable.setRowSelectionInterval(selectedRow, selectedRow);
        } else {
            clearDetailFields();
        }
    }

    private void displaySelectedIncidentDetails() {
        int row = incidentTable.getSelectedRow();
        if (row < 0) {
            clearDetailFields();
            return;
        }

        String incId = (String) tableModel.getValueAt(row, 0);
        List<Incident> all = databaseManager.getAllIncidents();
        for (Incident inc : all) {
            if (inc.getIncidentId().equals(incId)) {
                lblSelectedIncidentId.setText(inc.getIncidentId());
                lblSelectedEventId.setText(inc.getEventId());
                comboStatus.setSelectedItem(inc.getStatus());
                txtAssignedTo.setText(inc.getAssignedTo());
                txtNotes.setText(inc.getNotes());
                lblCreatedAt.setText(inc.getCreatedAt());
                lblUpdatedAt.setText(inc.getUpdatedAt());

                RiskAssessment ra = databaseManager.getLatestAssessment(inc.getEventId());
                DetectionEvent ev = databaseManager.getEventById(inc.getEventId());
                if (ra != null) {
                    lblRelatedThreat.setText(String.format("Score %d/100 [%s] (Dist: %.0fm, Spd: %.0fkm/h)",
                            ra.getScore(), ra.getRiskCategory(),
                            ev != null ? ev.getDistanceMeters() : 0.0,
                            ev != null ? ev.getSpeedKmh() : 0.0));
                    Color c = ra.getRiskCategory().equals("CRITICAL") ? DashboardComponents.COLOR_CRITICAL :
                            ra.getRiskCategory().equals("HIGH") ? DashboardComponents.COLOR_HIGH :
                            ra.getRiskCategory().equals("MEDIUM") ? DashboardComponents.COLOR_MEDIUM :
                            DashboardComponents.COLOR_LOW;
                    lblRelatedThreat.setForeground(c);
                } else {
                    lblRelatedThreat.setText("Threat Assessment: Nominal / Unassessed");
                    lblRelatedThreat.setForeground(DashboardComponents.TEXT_SECONDARY);
                }
                return;
            }
        }
    }

    private void clearDetailFields() {
        lblSelectedIncidentId.setText("- None -");
        lblSelectedEventId.setText("-");
        lblRelatedThreat.setText("-");
        comboStatus.setSelectedIndex(0);
        txtAssignedTo.setText("");
        txtNotes.setText("");
        lblCreatedAt.setText("-");
        lblUpdatedAt.setText("-");
    }

    private void updateSelectedIncident() {
        String incId = lblSelectedIncidentId.getText();
        if (incId.equals("- None -") || incId.startsWith("-")) {
            JOptionPane.showMessageDialog(this, "Select an incident from the table first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String status = (String) comboStatus.getSelectedItem();
        String assigned = txtAssignedTo.getText().trim();
        String notes = txtNotes.getText().trim();

        ValidationUtils.ValidationResult val = ValidationUtils.validateIncident(incId, lblSelectedEventId.getText(), status, assigned);
        if (!val.isValid()) {
            JOptionPane.showMessageDialog(this, val.getErrorMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean updated = databaseManager.updateIncident(incId, status, assigned, notes);
        if (updated) {
            JOptionPane.showMessageDialog(this, "Incident " + incId + " updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadIncidents();
            if (onDataChangedCallback != null) {
                onDataChangedCallback.run();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update incident in database.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showManualIncidentDialog() {
        JDialog dlg = new JDialog(this, "Create Manual Security Incident", true);
        dlg.setSize(480, 360);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(DashboardComponents.BG_DARK);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(DashboardComponents.BG_DARK);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        form.add(createDetailLabel("Incident ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        JTextField txtId = new JTextField("INC-" + (System.currentTimeMillis() % 1000000));
        DashboardComponents.styleTextField(txtId);
        form.add(txtId, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(createDetailLabel("Associated Event ID:"), gbc);
        gbc.gridx = 1;
        JTextField txtEvent = new JTextField();
        DashboardComponents.styleTextField(txtEvent);
        form.add(txtEvent, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(createDetailLabel("Assigned Team:"), gbc);
        gbc.gridx = 1;
        JTextField txtAssign = new JTextField("Quick Reaction Force Alpha");
        DashboardComponents.styleTextField(txtAssign);
        form.add(txtAssign, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        form.add(createDetailLabel("Initial Notes:"), gbc);
        gbc.gridx = 1;
        JTextField txtInitNotes = new JTextField("Manual incident opened by security controller.");
        DashboardComponents.styleTextField(txtInitNotes);
        form.add(txtInitNotes, gbc);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btns.setBackground(DashboardComponents.HEADER_BG);

        JButton btnCancel = DashboardComponents.createSecondaryButton("Cancel");
        btnCancel.addActionListener(e -> dlg.dispose());

        JButton btnSubmit = DashboardComponents.createPrimaryButton("Create Incident");
        btnSubmit.addActionListener(e -> {
            String incId = txtId.getText().trim();
            String evId = txtEvent.getText().trim();
            String assign = txtAssign.getText().trim();
            String notes = txtInitNotes.getText().trim();

            if (databaseManager.getEventById(evId) == null) {
                JOptionPane.showMessageDialog(dlg,
                        "Event ID '" + evId + "' does not exist in the database.\nPlease enter a valid registered detection event ID.",
                        "Unknown Event",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            Incident inc = new Incident(incId, evId, "OPEN", assign, notes);
            try {
                boolean created = databaseManager.createIncident(inc);
                if (created) {
                    JOptionPane.showMessageDialog(dlg, "Incident created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dlg.dispose();
                    loadIncidents();
                    if (onDataChangedCallback != null) {
                        onDataChangedCallback.run();
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dlg,
                        ex.getMessage(),
                        "Policy Suppression / Duplicate Error",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        btns.add(btnCancel);
        btns.add(btnSubmit);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }
}
