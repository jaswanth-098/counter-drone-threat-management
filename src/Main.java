import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

/**
 * Main.java
 *
 * Central Application Entry Point and Primary Tactical Command Dashboard.
 *
 * Project: Explainable Risk Scoring and Intelligent Alert Prioritization for Counter-Drone Threat Management
 *
 * Integrates:
 * - Real-time SQLite persistence via DatabaseManager
 * - Heuristic explainable risk scoring via RiskScoringService
 * - Multi-criteria alert queue prioritization via AlertManager
 * - Operational incident dispatch via IncidentManagerUI
 * - Empirical evaluation and baseline comparisons via EvaluationUI
 * - Configurable parameter tuning via RuleConfigDialog
 */
public class Main extends JFrame {

    private final DatabaseManager databaseManager;
    private final RiskScoringService scoringService;

    // Header Components
    private JLabel statusDot;
    private JLabel statusText;
    private JPanel statusBadge;

    // Summary Cards
    private DashboardComponents.SummaryCard cardTotalEvents;
    private DashboardComponents.SummaryCard cardCriticalAlerts;
    private DashboardComponents.SummaryCard cardHighAlerts;
    private DashboardComponents.SummaryCard cardActiveIncidents;

    // Risk Bar Chart Component
    private DashboardComponents.RiskBarChart riskBarChart;
    private JLabel lblAnalyticsAvgScore;
    private JLabel lblAnalyticsHighRatio;
    private JLabel lblAnalyticsZonesActive;

    // Prioritized Alerts Table & Container Cards
    private JTable alertTable;
    private DefaultTableModel alertTableModel;
    private JPanel alertTableContainer;
    private CardLayout alertCardLayout;
    private JComboBox<String> alertCategoryFilter;
    private JComboBox<String> alertZoneFilter;
    private JTextField alertSearchField;

    // Detection Events Table & Container Cards
    private JTable eventTable;
    private DefaultTableModel eventTableModel;
    private JPanel eventTableContainer;
    private CardLayout eventCardLayout;

    // Right-Hand Explainable Risk Audit Inspector
    private JPanel inspectorCardPanel;
    private CardLayout inspectorCardLayout;
    private JLabel lblExplainEventId;
    private JLabel lblExplainZone;
    private JLabel lblExplainScore;
    private JLabel lblExplainCategory;
    private JLabel lblExplainDist;
    private JLabel lblExplainSpeed;
    private JLabel lblExplainRepeats;
    private JLabel lblExplainAuth;
    private JPanel rulesBreakdownContainer;
    private JTextArea txtSummaryNarrative;
    private String currentSelectedEventId = null;

    // Incident Summary Footer Metrics
    private JLabel lblOpenIncCount;
    private JLabel lblInProgressIncCount;
    private JLabel lblResolvedIncCount;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            Main dashboard = new Main();
            dashboard.setVisible(true);
        });
    }

    public Main() {
        super("COUNTER-DRONE THREAT MANAGEMENT — Tactical Command & Control");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setMinimumSize(new Dimension(1120, 740));
        setLocationRelativeTo(null);
        getContentPane().setBackground(DashboardComponents.BG_DARK);
        setLayout(new BorderLayout());

        // Initialize Services & Database
        this.databaseManager = DatabaseManager.getInstance();
        RuleConfiguration loadedConfig = this.databaseManager.loadRuleConfiguration();
        this.scoringService = new RiskScoringService(loadedConfig);

        // 1. Build Header Bar
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Build Main Content Layout
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBackground(DashboardComponents.BG_DARK);
        mainContent.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // 2A. Top Summary Metric Cards Row
        mainContent.add(createSummaryCardsRow(), BorderLayout.NORTH);

        // 2B. Center Split: Tables (Left) vs Explainability Inspector (Right)
        mainContent.add(createCenterWorkspace(), BorderLayout.CENTER);

        // 2C. Bottom Operational Quick Actions & Incident Status Bar
        mainContent.add(createBottomActionAndIncidentBar(), BorderLayout.SOUTH);

        add(mainContent, BorderLayout.CENTER);

        // Load initial live data from SQLite
        refreshDashboardData();
    }

    /**
     * Header bar containing military C2 system title, dynamic status indicator, and quick tools.
     */
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DashboardComponents.HEADER_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DashboardComponents.BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));

        // Title and Subtitle
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(DashboardComponents.HEADER_BG);

        JLabel titleLbl = new JLabel("COUNTER-DRONE THREAT MANAGEMENT");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 19));
        titleLbl.setForeground(DashboardComponents.TEXT_PRIMARY);

        JLabel subLbl = new JLabel("Explainable Risk Scoring and Intelligent Alert Prioritization");
        subLbl.setFont(DashboardComponents.FONT_SUBTITLE);
        subLbl.setForeground(DashboardComponents.ACCENT_BLUE);

        titlePanel.add(titleLbl);
        titlePanel.add(Box.createRigidArea(new Dimension(0, 3)));
        titlePanel.add(subLbl);
        header.add(titlePanel, BorderLayout.WEST);

        // Right side: Dynamic System Status Badge + Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        actionPanel.setBackground(DashboardComponents.HEADER_BG);

        // Dynamic System Status Indicator
        statusBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        statusBadge.setBackground(new Color(34, 197, 94, 30));
        statusBadge.setBorder(BorderFactory.createLineBorder(DashboardComponents.COLOR_LOW, 1, true));

        statusDot = new JLabel("●");
        statusDot.setForeground(DashboardComponents.COLOR_LOW);
        statusDot.setFont(new Font("Segoe UI", Font.BOLD, 14));

        statusText = new JLabel("SYSTEM READY");
        statusText.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusText.setForeground(DashboardComponents.COLOR_LOW);

        statusBadge.add(statusDot);
        statusBadge.add(statusText);
        actionPanel.add(statusBadge);

        // Header Action Buttons
        JButton btnAddEvent = DashboardComponents.createPrimaryButton("➕ Ingest Detection");
        btnAddEvent.addActionListener(e -> openEventFormDialog());
        actionPanel.add(btnAddEvent);

        JButton btnRefresh = DashboardComponents.createSecondaryButton("🔄 Refresh");
        btnRefresh.addActionListener(e -> refreshDashboardData());
        actionPanel.add(btnRefresh);

        JButton btnSettings = DashboardComponents.createSecondaryButton("⚙️ Configure Rules");
        btnSettings.addActionListener(e -> openRuleConfigDialog());
        actionPanel.add(btnSettings);

        header.add(actionPanel, BorderLayout.EAST);
        return header;
    }

    /**
     * Updates the dynamic status indicator based on actual SQLite database health.
     */
    private void updateSystemStatusIndicator() {
        boolean healthy = databaseManager.isDatabaseReady();
        if (healthy) {
            statusBadge.setBackground(new Color(34, 197, 94, 30));
            statusBadge.setBorder(BorderFactory.createLineBorder(DashboardComponents.COLOR_LOW, 1, true));
            statusDot.setForeground(DashboardComponents.COLOR_LOW);
            statusText.setText("SYSTEM READY");
            statusText.setForeground(DashboardComponents.COLOR_LOW);
        } else {
            statusBadge.setBackground(new Color(239, 68, 68, 30));
            statusBadge.setBorder(BorderFactory.createLineBorder(DashboardComponents.COLOR_CRITICAL, 1, true));
            statusDot.setForeground(DashboardComponents.COLOR_CRITICAL);
            statusText.setText("DATABASE ERROR");
            statusText.setForeground(DashboardComponents.COLOR_CRITICAL);
        }
    }

    /**
     * Row of 4 high-level summary KPI cards.
     */
    private JPanel createSummaryCardsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 14));
        row.setBackground(DashboardComponents.BG_DARK);
        row.setPreferredSize(new Dimension(1000, 95));

        cardTotalEvents = new DashboardComponents.SummaryCard(
                "TOTAL DETECTIONS", "0", "Recorded radar/RF contacts", DashboardComponents.ACCENT_BLUE);

        cardCriticalAlerts = new DashboardComponents.SummaryCard(
                "CRITICAL THREATS", "0", "Score 75-100 (Immediate alert)", DashboardComponents.COLOR_CRITICAL);

        cardHighAlerts = new DashboardComponents.SummaryCard(
                "HIGH-RISK ALERTS", "0", "Score 50-74 (Active triage)", DashboardComponents.COLOR_HIGH);

        cardActiveIncidents = new DashboardComponents.SummaryCard(
                "ACTIVE INCIDENTS", "0", "OPEN or IN_PROGRESS tickets", DashboardComponents.ACCENT_PURPLE);

        row.add(cardTotalEvents);
        row.add(cardCriticalAlerts);
        row.add(cardHighAlerts);
        row.add(cardActiveIncidents);

        return row;
    }

    /**
     * Center workspace with split pane: Left (Tabs for Alerts / Detections / Analytics), Right (Explainability Inspector).
     */
    private JSplitPane createCenterWorkspace() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(840);
        split.setBackground(DashboardComponents.BG_DARK);
        split.setBorder(BorderFactory.createEmptyBorder());

        // Left Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(DashboardComponents.BG_DARK);
        tabs.setForeground(DashboardComponents.TEXT_PRIMARY);
        tabs.setFont(DashboardComponents.FONT_BODY_BOLD);

        tabs.addTab("Prioritized Alert Queue", createAlertQueuePanel());
        tabs.addTab("Recent Detection Events", createRecentEventsPanel());
        tabs.addTab("Threat Analytics", createThreatAnalyticsPanel());

        split.setLeftComponent(tabs);

        // Right Inspector
        split.setRightComponent(createExplainabilityInspectorPanel());

        return split;
    }

    /**
     * Tab 1: Prioritized Alert Queue with multi-criteria filtering, search, and empty-state handling.
     */
    private JPanel createAlertQueuePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(DashboardComponents.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));

        // Filter Controls Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterBar.setBackground(DashboardComponents.CARD_BG);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        filterBar.add(createFilterLabel("Threat Level:"));
        alertCategoryFilter = new JComboBox<>(new String[]{"ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"});
        DashboardComponents.styleComboBox(alertCategoryFilter);
        alertCategoryFilter.addActionListener(e -> reloadAlertQueue());
        filterBar.add(alertCategoryFilter);

        filterBar.add(createFilterLabel("Zone:"));
        alertZoneFilter = new JComboBox<>(new String[]{
                "ALL",
                "Zone-Alpha (HQ Command)",
                "Zone-Bravo (Runway North)",
                "Zone-Charlie (Fuel Depot)",
                "Zone-Delta (Perimeter West)"
        });
        DashboardComponents.styleComboBox(alertZoneFilter);
        alertZoneFilter.addActionListener(e -> reloadAlertQueue());
        filterBar.add(alertZoneFilter);

        filterBar.add(createFilterLabel("Search Event ID:"));
        alertSearchField = new JTextField(9);
        DashboardComponents.styleTextField(alertSearchField);
        alertSearchField.addActionListener(e -> reloadAlertQueue());
        filterBar.add(alertSearchField);

        JButton btnApplyFilter = DashboardComponents.createSecondaryButton("Filter");
        btnApplyFilter.setFont(DashboardComponents.FONT_SMALL);
        btnApplyFilter.addActionListener(e -> reloadAlertQueue());
        filterBar.add(btnApplyFilter);

        JButton btnResetFilter = DashboardComponents.createSecondaryButton("Reset");
        btnResetFilter.setFont(DashboardComponents.FONT_SMALL);
        btnResetFilter.addActionListener(e -> {
            alertCategoryFilter.setSelectedIndex(0);
            alertZoneFilter.setSelectedIndex(0);
            alertSearchField.setText("");
            reloadAlertQueue();
        });
        filterBar.add(btnResetFilter);

        panel.add(filterBar, BorderLayout.NORTH);

        // Alert Queue Table with exact required columns:
        // Event ID, Zone, Risk Score, Threat Level, Distance, Speed, Authorization, Detection Time
        String[] cols = {
                "Event ID", "Zone", "Risk Score", "Threat Level", "Distance", "Speed", "Authorization", "Detection Time"
        };
        alertTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        alertTable = new JTable(alertTableModel);
        DashboardComponents.styleTable(alertTable);
        alertTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        alertTable.getColumnModel().getColumn(0).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(false));
        alertTable.getColumnModel().getColumn(1).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(false));
        alertTable.getColumnModel().getColumn(2).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        alertTable.getColumnModel().getColumn(3).setCellRenderer(new DashboardComponents.RiskBadgeRenderer());
        alertTable.getColumnModel().getColumn(4).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        alertTable.getColumnModel().getColumn(5).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        alertTable.getColumnModel().getColumn(6).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        alertTable.getColumnModel().getColumn(7).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));

        alertTable.getColumnModel().getColumn(0).setPreferredWidth(95);
        alertTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        alertTable.getColumnModel().getColumn(2).setPreferredWidth(75);
        alertTable.getColumnModel().getColumn(3).setPreferredWidth(95);
        alertTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        alertTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        alertTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        alertTable.getColumnModel().getColumn(7).setPreferredWidth(130);

        alertTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = alertTable.getSelectedRow();
                if (row >= 0) {
                    String eventId = (String) alertTableModel.getValueAt(row, 0);
                    inspectEventRiskAssessment(eventId);
                }
            }
        });

        // Container with CardLayout to handle Table View vs Empty-State Message
        alertCardLayout = new CardLayout();
        alertTableContainer = new JPanel(alertCardLayout);
        alertTableContainer.setBackground(DashboardComponents.BG_DARK);

        JScrollPane scroll = new JScrollPane(alertTable);
        scroll.setBorder(BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true));
        alertTableContainer.add(scroll, "TABLE");

        JPanel emptyPanel = DashboardComponents.createEmptyStatePanel(
                "NO ACTIVE ALERTS",
                "No drone contacts match the active filter criteria or queue is currently clear."
        );
        alertTableContainer.add(emptyPanel, "EMPTY");

        panel.add(alertTableContainer, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Tab 2: Recent Detection Events Table with empty-state handling.
     */
    private JPanel createRecentEventsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(DashboardComponents.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));

        // Exact columns requested:
        // Event ID, Zone, Distance, Speed, Repeat Count, Authorization, Created or updated time, Calculated risk score, Threat category
        String[] cols = {
                "Event ID", "Zone", "Distance", "Speed", "Repeat Count", "Authorization", "Timestamp", "Calculated Score", "Threat Category"
        };
        eventTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        eventTable = new JTable(eventTableModel);
        DashboardComponents.styleTable(eventTable);
        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        eventTable.getColumnModel().getColumn(0).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(false));
        eventTable.getColumnModel().getColumn(1).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(false));
        eventTable.getColumnModel().getColumn(2).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        eventTable.getColumnModel().getColumn(3).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        eventTable.getColumnModel().getColumn(4).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        eventTable.getColumnModel().getColumn(5).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        eventTable.getColumnModel().getColumn(6).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        eventTable.getColumnModel().getColumn(7).setCellRenderer(new DashboardComponents.DarkTableCellRenderer(true));
        eventTable.getColumnModel().getColumn(8).setCellRenderer(new DashboardComponents.RiskBadgeRenderer());

        eventTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = eventTable.getSelectedRow();
                if (row >= 0) {
                    String eventId = (String) eventTableModel.getValueAt(row, 0);
                    inspectEventRiskAssessment(eventId);
                }
            }
        });

        eventCardLayout = new CardLayout();
        eventTableContainer = new JPanel(eventCardLayout);
        eventTableContainer.setBackground(DashboardComponents.BG_DARK);

        JScrollPane scroll = new JScrollPane(eventTable);
        scroll.setBorder(BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true));
        eventTableContainer.add(scroll, "TABLE");

        JPanel emptyPanel = DashboardComponents.createEmptyStatePanel(
                "NO DETECTION EVENTS RECORDED",
                "Click 'Ingest Detection' to simulate a drone radar/RF contact."
        );
        eventTableContainer.add(emptyPanel, "EMPTY");

        panel.add(eventTableContainer, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Tab 3: Threat Analytics and Visual Distribution Charts with empty-state support.
     */
    private JPanel createThreatAnalyticsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 14, 14));
        panel.setBackground(DashboardComponents.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        riskBarChart = new DashboardComponents.RiskBarChart();
        panel.add(riskBarChart);

        // Analytics Summary Card
        JPanel policyCard = new JPanel(new BorderLayout(10, 10));
        policyCard.setBackground(DashboardComponents.CARD_BG);
        policyCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblTitle = new JLabel("AIR DEFENSE SITUATIONAL ANALYTICS");
        lblTitle.setFont(DashboardComponents.FONT_HEADER);
        lblTitle.setForeground(DashboardComponents.ACCENT_BLUE);
        policyCard.add(lblTitle, BorderLayout.NORTH);

        // Stat Grid
        JPanel statGrid = new JPanel(new GridLayout(3, 1, 8, 8));
        statGrid.setBackground(DashboardComponents.CARD_BG);

        lblAnalyticsAvgScore = createAnalyticsStatLabel("Average Risk Score: -- / 100");
        lblAnalyticsHighRatio = createAnalyticsStatLabel("High/Critical Incident Ratio: --%");
        lblAnalyticsZonesActive = createAnalyticsStatLabel("Active Surveillance Zones: --");

        statGrid.add(lblAnalyticsAvgScore);
        statGrid.add(lblAnalyticsHighRatio);
        statGrid.add(lblAnalyticsZonesActive);

        policyCard.add(statGrid, BorderLayout.CENTER);

        // Footer policy note
        JTextArea policyText = new JTextArea(4, 20);
        policyText.setBackground(DashboardComponents.CARD_BG);
        policyText.setForeground(DashboardComponents.TEXT_SECONDARY);
        policyText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        policyText.setEditable(false);
        policyText.setLineWrap(true);
        policyText.setWrapStyleWord(true);
        policyText.setText(
                "Heuristic Policy Notice: Detections with scores >= 50 automatically generate incidents. " +
                "Duplicate tickets are suppressed for active contacts. UNKNOWN authorization receives 0 pts with verification alert."
        );
        policyCard.add(policyText, BorderLayout.SOUTH);

        panel.add(policyCard);
        return panel;
    }

    private JLabel createAnalyticsStatLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(DashboardComponents.FONT_BODY_BOLD);
        l.setForeground(DashboardComponents.TEXT_PRIMARY);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return l;
    }

    /**
     * Right-Hand Panel: Explainable Risk Audit Inspector.
     */
    private JPanel createExplainabilityInspectorPanel() {
        inspectorCardLayout = new CardLayout();
        inspectorCardPanel = new JPanel(inspectorCardLayout);
        inspectorCardPanel.setBackground(DashboardComponents.CARD_BG);
        inspectorCardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        inspectorCardPanel.setPreferredSize(new Dimension(480, 520));

        // 1. Empty State Panel
        JPanel emptyPanel = DashboardComponents.createEmptyStatePanel(
                "NO EVENT SELECTED",
                "Select a detection or alert from the queue to view its explainable risk audit."
        );
        inspectorCardPanel.add(emptyPanel, "EMPTY");

        // 2. Active Audit Panel
        JPanel auditPanel = new JPanel(new BorderLayout(8, 8));
        auditPanel.setBackground(DashboardComponents.CARD_BG);

        // Header Section
        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(DashboardComponents.CARD_BG);
        head.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));

        JPanel headLeft = new JPanel();
        headLeft.setLayout(new BoxLayout(headLeft, BoxLayout.Y_AXIS));
        headLeft.setBackground(DashboardComponents.CARD_BG);

        JLabel t = new JLabel("EXPLAINABLE RISK AUDIT");
        t.setFont(DashboardComponents.FONT_HEADER);
        t.setForeground(DashboardComponents.ACCENT_BLUE);

        lblExplainEventId = new JLabel("Event ID: --");
        lblExplainEventId.setFont(DashboardComponents.FONT_BODY_BOLD);
        lblExplainEventId.setForeground(DashboardComponents.TEXT_PRIMARY);

        lblExplainZone = new JLabel("Zone: --");
        lblExplainZone.setFont(DashboardComponents.FONT_SMALL);
        lblExplainZone.setForeground(DashboardComponents.TEXT_SECONDARY);

        headLeft.add(t);
        headLeft.add(Box.createRigidArea(new Dimension(0, 3)));
        headLeft.add(lblExplainEventId);
        headLeft.add(Box.createRigidArea(new Dimension(0, 2)));
        headLeft.add(lblExplainZone);
        head.add(headLeft, BorderLayout.WEST);

        // Score Badge
        JPanel scoreBadgePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        scoreBadgePanel.setBackground(DashboardComponents.CARD_BG);

        lblExplainScore = new JLabel("Score: --");
        lblExplainScore.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblExplainScore.setForeground(DashboardComponents.TEXT_PRIMARY);

        lblExplainCategory = new JLabel(" [TIER] ");
        lblExplainCategory.setFont(DashboardComponents.FONT_BODY_BOLD);
        lblExplainCategory.setOpaque(true);
        lblExplainCategory.setBackground(new Color(51, 65, 85));
        lblExplainCategory.setForeground(DashboardComponents.TEXT_PRIMARY);
        lblExplainCategory.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        scoreBadgePanel.add(lblExplainScore);
        scoreBadgePanel.add(lblExplainCategory);
        head.add(scoreBadgePanel, BorderLayout.EAST);
        auditPanel.add(head, BorderLayout.NORTH);

        // Center Scrollable Container: Telemetry Grid + Rules Breakdown + Narrative
        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setBackground(DashboardComponents.CARD_BG);

        // Telemetry Grid (2x2)
        JPanel telemGrid = new JPanel(new GridLayout(2, 2, 6, 6));
        telemGrid.setBackground(DashboardComponents.CARD_BG);
        telemGrid.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR),
                        "SENSOR TELEMETRY OBSERVATIONS",
                        0, 0, DashboardComponents.FONT_SMALL, DashboardComponents.TEXT_SECONDARY
                ),
                BorderFactory.createEmptyBorder(4, 8, 6, 8)
        ));

        lblExplainDist = createTelemValueLabel("Distance: --");
        lblExplainSpeed = createTelemValueLabel("Speed: --");
        lblExplainRepeats = createTelemValueLabel("Repeats: --");
        lblExplainAuth = createTelemValueLabel("IFF Status: --");

        telemGrid.add(lblExplainDist);
        telemGrid.add(lblExplainSpeed);
        telemGrid.add(lblExplainRepeats);
        telemGrid.add(lblExplainAuth);
        centerContent.add(telemGrid);
        centerContent.add(Box.createRigidArea(new Dimension(0, 8)));

        // Rules Breakdown Container
        JLabel lblRulesHeader = new JLabel("HEURISTIC RULE-BY-RULE AUDIT TRAIL:");
        lblRulesHeader.setFont(DashboardComponents.FONT_BODY_BOLD);
        lblRulesHeader.setForeground(DashboardComponents.ACCENT_BLUE);
        lblRulesHeader.setAlignmentX(LEFT_ALIGNMENT);
        centerContent.add(lblRulesHeader);
        centerContent.add(Box.createRigidArea(new Dimension(0, 4)));

        rulesBreakdownContainer = new JPanel();
        rulesBreakdownContainer.setLayout(new BoxLayout(rulesBreakdownContainer, BoxLayout.Y_AXIS));
        rulesBreakdownContainer.setBackground(DashboardComponents.CARD_BG);
        centerContent.add(rulesBreakdownContainer);
        centerContent.add(Box.createRigidArea(new Dimension(0, 8)));

        // Decision recommendation narrative
        JLabel lblNarrativeHeader = new JLabel("OPERATIONAL TRIAGE RECOMMENDATION:");
        lblNarrativeHeader.setFont(DashboardComponents.FONT_BODY_BOLD);
        lblNarrativeHeader.setForeground(DashboardComponents.TEXT_SECONDARY);
        lblNarrativeHeader.setAlignmentX(LEFT_ALIGNMENT);
        centerContent.add(lblNarrativeHeader);
        centerContent.add(Box.createRigidArea(new Dimension(0, 4)));

        txtSummaryNarrative = new JTextArea(3, 20);
        txtSummaryNarrative.setBackground(new Color(15, 23, 42));
        txtSummaryNarrative.setForeground(DashboardComponents.TEXT_PRIMARY);
        txtSummaryNarrative.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSummaryNarrative.setEditable(false);
        txtSummaryNarrative.setLineWrap(true);
        txtSummaryNarrative.setWrapStyleWord(true);
        txtSummaryNarrative.setMargin(new Insets(6, 8, 6, 8));

        JScrollPane scrNarrative = new JScrollPane(txtSummaryNarrative);
        scrNarrative.setBorder(BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR));
        centerContent.add(scrNarrative);

        JScrollPane centerScroll = new JScrollPane(centerContent);
        centerScroll.setBorder(BorderFactory.createEmptyBorder());
        auditPanel.add(centerScroll, BorderLayout.CENTER);

        // Bottom Button: View Related Incidents
        JPanel inspectorFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        inspectorFooter.setBackground(DashboardComponents.CARD_BG);

        JButton btnViewRelated = DashboardComponents.createPrimaryButton("🚨 View Related Incidents");
        btnViewRelated.addActionListener(e -> {
            if (currentSelectedEventId != null) {
                IncidentManagerUI ui = new IncidentManagerUI(this, databaseManager, this::refreshDashboardData, currentSelectedEventId);
                ui.setVisible(true);
            } else {
                openIncidentManagerUI();
            }
        });
        inspectorFooter.add(btnViewRelated);

        auditPanel.add(inspectorFooter, BorderLayout.SOUTH);
        inspectorCardPanel.add(auditPanel, "AUDIT");

        return inspectorCardPanel;
    }

    private JLabel createTelemValueLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(DashboardComponents.FONT_SMALL);
        l.setForeground(DashboardComponents.TEXT_PRIMARY);
        return l;
    }

    private JLabel createFilterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(DashboardComponents.FONT_SMALL);
        l.setForeground(DashboardComponents.TEXT_SECONDARY);
        return l;
    }

    /**
     * Bottom Operational Quick Actions & Incident Status Bar.
     */
    private JPanel createBottomActionAndIncidentBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(DashboardComponents.HEADER_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DashboardComponents.BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        // Left: Incident Lifecycle Counters
        JPanel incCountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        incCountPanel.setBackground(DashboardComponents.HEADER_BG);

        JLabel incTitle = new JLabel("INCIDENT STATUS:");
        incTitle.setFont(DashboardComponents.FONT_BODY_BOLD);
        incTitle.setForeground(DashboardComponents.TEXT_SECONDARY);
        incCountPanel.add(incTitle);

        lblOpenIncCount = createStatusCountBadge("OPEN: 0", DashboardComponents.COLOR_OPEN);
        lblInProgressIncCount = createStatusCountBadge("IN_PROGRESS: 0", DashboardComponents.COLOR_IN_PROGRESS);
        lblResolvedIncCount = createStatusCountBadge("RESOLVED: 0", DashboardComponents.COLOR_RESOLVED);

        incCountPanel.add(lblOpenIncCount);
        incCountPanel.add(lblInProgressIncCount);
        incCountPanel.add(lblResolvedIncCount);
        bar.add(incCountPanel, BorderLayout.WEST);

        // Right: Command Action Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(DashboardComponents.HEADER_BG);

        JButton btnIncidents = DashboardComponents.createSecondaryButton("🚨 Manage Incidents");
        btnIncidents.addActionListener(e -> openIncidentManagerUI());
        actions.add(btnIncidents);

        JButton btnEval = DashboardComponents.createSecondaryButton("📊 Run Evaluation");
        btnEval.addActionListener(e -> openEvaluationUI());
        actions.add(btnEval);

        JButton btnAbout = DashboardComponents.createSecondaryButton("ℹ️ Viva Guide / About");
        btnAbout.addActionListener(e -> showAcademicAboutDialog());
        actions.add(btnAbout);

        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private JLabel createStatusCountBadge(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(DashboardComponents.FONT_BODY_BOLD);
        l.setForeground(color);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1, true),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        return l;
    }

    /**
     * Refreshes all metrics, tables, charts, and selections from SQLite.
     */
    public void refreshDashboardData() {
        updateSystemStatusIndicator();

        // 1. Refresh Summary Metrics directly from SQLite
        Map<String, Integer> metrics = databaseManager.getDashboardMetrics();
        cardTotalEvents.setValue(String.valueOf(metrics.get("totalEvents")));
        cardCriticalAlerts.setValue(String.valueOf(metrics.get("criticalAlerts")));
        cardHighAlerts.setValue(String.valueOf(metrics.get("highAlerts")));
        int activeIncidents = metrics.get("openIncidents") + metrics.get("inProgressIncidents");
        cardActiveIncidents.setValue(String.valueOf(activeIncidents));

        // 2. Refresh Incident Counter Bar
        lblOpenIncCount.setText("OPEN: " + metrics.get("openIncidents"));
        lblInProgressIncCount.setText("IN_PROGRESS: " + metrics.get("inProgressIncidents"));
        lblResolvedIncCount.setText("RESOLVED: " + metrics.get("resolvedIncidents"));

        // 3. Refresh Threat Analytics Bar Chart & Stat labels
        int low = metrics.get("lowAlerts");
        int med = metrics.get("mediumAlerts");
        int high = metrics.get("highAlerts");
        int crit = metrics.get("criticalAlerts");
        int total = low + med + high + crit;

        if (riskBarChart != null) {
            riskBarChart.updateCounts(low, med, high, crit);
        }

        if (lblAnalyticsAvgScore != null) {
            List<DetectionEvent> events = databaseManager.getAllDetectionEvents();
            double sumScore = 0.0;
            for (DetectionEvent ev : events) {
                RiskAssessment ra = databaseManager.getLatestAssessment(ev.getEventId());
                if (ra != null) sumScore += ra.getScore();
            }
            double avg = events.isEmpty() ? 0.0 : sumScore / events.size();
            lblAnalyticsAvgScore.setText(String.format("Average Risk Score: %.1f / 100", avg));

            double highCritRatio = total > 0 ? ((double) (high + crit) / total) * 100.0 : 0.0;
            lblAnalyticsHighRatio.setText(String.format("High/Critical Incursion Ratio: %.1f%%", highCritRatio));

            long distinctZones = events.stream().map(DetectionEvent::getZone).distinct().count();
            lblAnalyticsZonesActive.setText(String.format("Active Surveillance Zones: %d", distinctZones));
        }

        // 4. Reload Prioritized Alert Queue
        reloadAlertQueue();

        // 5. Reload Recent Detection Events Table
        reloadRecentEventsTable();
    }

    private void reloadAlertQueue() {
        alertTableModel.setRowCount(0);
        List<AlertItem> rawAlerts = databaseManager.getAllAlerts();

        String catFilter = alertCategoryFilter != null ? (String) alertCategoryFilter.getSelectedItem() : "ALL";
        String zoneFilter = alertZoneFilter != null ? (String) alertZoneFilter.getSelectedItem() : "ALL";
        String search = alertSearchField != null ? alertSearchField.getText() : "";

        List<AlertItem> prioritized = AlertManager.prioritizeAndFilter(rawAlerts, catFilter, zoneFilter, search);

        for (AlertItem item : prioritized) {
            alertTableModel.addRow(new Object[]{
                    item.getEventId(),
                    item.getZone(),
                    item.getScore(),
                    item.getRiskCategory(),
                    String.format("%.1fm", item.getDistanceMeters()),
                    String.format("%.1f km/h", item.getSpeedKmh()),
                    item.getAuthorizationStatus(),
                    item.getAlertTimestamp()
            });
        }

        if (alertTableModel.getRowCount() > 0) {
            alertCardLayout.show(alertTableContainer, "TABLE");
            alertTable.setRowSelectionInterval(0, 0);
            String firstEventId = (String) alertTableModel.getValueAt(0, 0);
            inspectEventRiskAssessment(firstEventId);
        } else {
            alertCardLayout.show(alertTableContainer, "EMPTY");
            inspectorCardLayout.show(inspectorCardPanel, "EMPTY");
            currentSelectedEventId = null;
        }
    }

    private void reloadRecentEventsTable() {
        eventTableModel.setRowCount(0);
        List<DetectionEvent> events = databaseManager.getAllDetectionEvents();

        for (DetectionEvent ev : events) {
            RiskAssessment ra = databaseManager.getLatestAssessment(ev.getEventId());
            int score = ra != null ? ra.getScore() : 0;
            String cat = ra != null ? ra.getRiskCategory() : "LOW";

            eventTableModel.addRow(new Object[]{
                    ev.getEventId(),
                    ev.getZone(),
                    String.format("%.1fm", ev.getDistanceMeters()),
                    String.format("%.1f km/h", ev.getSpeedKmh()),
                    ev.getRepeatCount(),
                    ev.getAuthorizationStatus(),
                    ev.getUpdatedAt(),
                    score,
                    cat
            });
        }

        if (eventTableModel.getRowCount() > 0) {
            eventCardLayout.show(eventTableContainer, "TABLE");
        } else {
            eventCardLayout.show(eventTableContainer, "EMPTY");
        }
    }

    /**
     * Updates the Explainability Inspection panel for a selected event ID.
     */
    private void inspectEventRiskAssessment(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            inspectorCardLayout.show(inspectorCardPanel, "EMPTY");
            currentSelectedEventId = null;
            return;
        }

        DetectionEvent event = databaseManager.getEventById(eventId);
        if (event == null) {
            inspectorCardLayout.show(inspectorCardPanel, "EMPTY");
            currentSelectedEventId = null;
            return;
        }

        // Re-evaluate using current active scoring service to ensure live rule configuration reflects
        RiskAssessment ra = scoringService.evaluateRisk(event);
        currentSelectedEventId = eventId;

        // Header
        lblExplainEventId.setText("Event ID: " + event.getEventId());
        lblExplainZone.setText("Zone: " + event.getZone());
        lblExplainScore.setText(String.format("Score: %d / 100", ra.getScore()));

        Color catColor = ra.getRiskCategory().equals("CRITICAL") ? DashboardComponents.COLOR_CRITICAL :
                ra.getRiskCategory().equals("HIGH") ? DashboardComponents.COLOR_HIGH :
                ra.getRiskCategory().equals("MEDIUM") ? DashboardComponents.COLOR_MEDIUM :
                DashboardComponents.COLOR_LOW;

        lblExplainCategory.setText(" [" + ra.getRiskCategory() + "] ");
        lblExplainCategory.setBackground(catColor);
        lblExplainCategory.setForeground(Color.BLACK);

        // Telemetry Grid
        lblExplainDist.setText(String.format("Distance: %.1fm", event.getDistanceMeters()));
        lblExplainSpeed.setText(String.format("Speed: %.1f km/h", event.getSpeedKmh()));
        lblExplainRepeats.setText(String.format("Repeats: %d bursts", event.getRepeatCount()));
        lblExplainAuth.setText("IFF Status: " + event.getAuthorizationStatus());

        // Rebuild Rule-by-Rule Itemized Cards
        rulesBreakdownContainer.removeAll();
        List<RiskAssessment.RuleEvaluationDetail> details = ra.getRuleDetails();

        for (RiskAssessment.RuleEvaluationDetail detail : details) {
            JPanel ruleCard = new JPanel(new BorderLayout(6, 4));
            ruleCard.setBackground(new Color(24, 33, 47));
            ruleCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(detail.isTriggered() ? DashboardComponents.ACCENT_BLUE : DashboardComponents.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));

            JPanel topRow = new JPanel(new BorderLayout());
            topRow.setBackground(new Color(24, 33, 47));

            JLabel lblRuleName = new JLabel(detail.getRuleName());
            lblRuleName.setFont(DashboardComponents.FONT_BODY_BOLD);
            lblRuleName.setForeground(DashboardComponents.TEXT_PRIMARY);

            JLabel lblBadge = new JLabel(detail.isTriggered() ?
                    String.format(" TRIGGERED (+%d pts) ", detail.getPoints()) :
                    " WITHHELD (0 pts) ");
            lblBadge.setFont(DashboardComponents.FONT_SMALL);
            lblBadge.setOpaque(true);
            if (detail.isTriggered()) {
                lblBadge.setBackground(new Color(56, 189, 248, 40));
                lblBadge.setForeground(DashboardComponents.ACCENT_BLUE);
                lblBadge.setBorder(BorderFactory.createLineBorder(DashboardComponents.ACCENT_BLUE, 1, true));
            } else {
                lblBadge.setBackground(new Color(51, 65, 85, 40));
                lblBadge.setForeground(DashboardComponents.TEXT_SECONDARY);
                lblBadge.setBorder(BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true));
            }

            topRow.add(lblRuleName, BorderLayout.WEST);
            topRow.add(lblBadge, BorderLayout.EAST);
            ruleCard.add(topRow, BorderLayout.NORTH);

            JTextArea txtExpl = new JTextArea(detail.getExplanation());
            txtExpl.setFont(DashboardComponents.FONT_SMALL);
            txtExpl.setForeground(detail.isTriggered() ? DashboardComponents.TEXT_PRIMARY : DashboardComponents.TEXT_SECONDARY);
            txtExpl.setBackground(new Color(24, 33, 47));
            txtExpl.setEditable(false);
            txtExpl.setLineWrap(true);
            txtExpl.setWrapStyleWord(true);
            ruleCard.add(txtExpl, BorderLayout.CENTER);

            rulesBreakdownContainer.add(ruleCard);
            rulesBreakdownContainer.add(Box.createRigidArea(new Dimension(0, 6)));
        }

        // Narrative Summary
        txtSummaryNarrative.setText(ra.getHumanReadableSummary());
        txtSummaryNarrative.setCaretPosition(0);

        rulesBreakdownContainer.revalidate();
        rulesBreakdownContainer.repaint();
        inspectorCardLayout.show(inspectorCardPanel, "AUDIT");
    }

    // --- Action Handlers ---

    private void openEventFormDialog() {
        EventFormDialog dlg = new EventFormDialog(this, databaseManager, scoringService, this::refreshDashboardData);
        dlg.setVisible(true);
    }

    private void openIncidentManagerUI() {
        IncidentManagerUI ui = new IncidentManagerUI(this, databaseManager, this::refreshDashboardData, currentSelectedEventId);
        ui.setVisible(true);
    }

    private void openEvaluationUI() {
        EvaluationUI ui = new EvaluationUI(this, databaseManager, scoringService);
        ui.setVisible(true);
    }

    private void openRuleConfigDialog() {
        RuleConfigDialog dlg = new RuleConfigDialog(this, databaseManager, scoringService, this::refreshDashboardData);
        dlg.setVisible(true);
    }

    /**
     * Viva Guide and About dialog containing all 15 required sections and mandatory prototype disclaimer.
     */
    private void showAcademicAboutDialog() {
        JDialog dlg = new JDialog(this, "Academic Project Information & Viva Defense Summary", true);
        dlg.setSize(840, 680);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(DashboardComponents.BG_DARK);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DashboardComponents.HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("ACADEMIC PROJECT DEFENSE & VIVA GUIDE");
        title.setFont(DashboardComponents.FONT_HEADER);
        title.setForeground(DashboardComponents.ACCENT_BLUE);

        JLabel sub = new JLabel("Comprehensive specification, architecture, formulas, and academic defense brief.");
        sub.setFont(DashboardComponents.FONT_SMALL);
        sub.setForeground(DashboardComponents.TEXT_SECONDARY);

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        dlg.add(header, BorderLayout.NORTH);

        JTextArea area = new JTextArea();
        area.setBackground(DashboardComponents.CARD_BG);
        area.setForeground(DashboardComponents.TEXT_PRIMARY);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(new Insets(16, 18, 16, 18));

        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================================\n");
        sb.append("  EXPLAINABLE RISK SCORING AND INTELLIGENT ALERT PRIORITIZATION FOR COUNTER-DRONE C-UAS \n");
        sb.append("========================================================================================\n\n");

        sb.append("1. PROJECT TITLE:\n");
        sb.append("   Explainable Risk Scoring and Intelligent Alert Prioritization for Counter-Drone Threat Management\n\n");

        sb.append("2. PROBLEM STATEMENT:\n");
        sb.append("   Commercial and recreational drones pose severe security risks to critical infrastructure. Conventional\n");
        sb.append("   perimeter detection systems trigger high false-alarm rates (alarm fatigue) or rely on black-box neural\n");
        sb.append("   networks that fail to explain why an alert was generated, hindering tactical decision-making.\n\n");

        sb.append("3. OBJECTIVES:\n");
        sb.append("   • Implement deterministic, explainable heuristic scoring for multi-sensor drone telemetry.\n");
        sb.append("   • Prioritize tactical alerts dynamically using multi-tier sorting (Score, Category, Time).\n");
        sb.append("   • Prevent operator ticket fatigue through duplicate event updates and incident suppression.\n");
        sb.append("   • Provide empirical benchmark comparisons against naive single-threshold baseline systems.\n\n");

        sb.append("4. EXISTING SYSTEM LIMITATIONS:\n");
        sb.append("   • Legacy boundary radar triggers indiscriminately regardless of flight plan authorization.\n");
        sb.append("   • Deep learning prototypes lack explainability and require high-power GPU computing.\n");
        sb.append("   • Lack of automated triage generates dozens of redundant tickets for continuous radar tracks.\n\n");

        sb.append("5. PROPOSED SYSTEM:\n");
        sb.append("   A modular, lightweight Java desktop C2 console integrating SQLite ACID persistence, explainable\n");
        sb.append("   scoring with factor audit trails, multi-criteria prioritization, and incident dispatch.\n\n");

        sb.append("6. ARCHITECTURE & TECHNOLOGY STACK:\n");
        sb.append("   • Language & Runtime : Java 27 (compatible with JDK 17+)\n");
        sb.append("   • GUI Presentation   : Java Swing (Custom dark military operations theme, Graphics2D)\n");
        sb.append("   • Database           : Embedded SQLite 3 (data/counterdrone.db) via SQLite JDBC Driver\n");
        sb.append("   • Build & Execution  : Standard javac and java CLI commands (No Maven, Spring, or MySQL)\n\n");

        sb.append("7. RISK SCORING FORMULA:\n");
        sb.append("   Composite Score S = min(100, w_prox * I(dist <= thresh_prox) + w_spd * I(spd >= thresh_spd) +\n");
        sb.append("                                w_rep * I(reps >= thresh_rep) + w_unauth * I(auth == UNAUTHORIZED))\n");
        sb.append("   Default Prototype Parameters:\n");
        sb.append("   • Proximity  : Dist <= 300m    --> +35 pts\n");
        sb.append("   • Velocity   : Speed >= 100km/h --> +20 pts\n");
        sb.append("   • Persistence: Repeats >= 3     --> +20 pts\n");
        sb.append("   • IFF Auth   : UNAUTHORIZED     --> +25 pts\n");
        sb.append("   • Max Score  : 100 points\n\n");

        sb.append("8. RISK CATEGORY RANGES:\n");
        sb.append("   • LOW      : 0 – 24  pts (Routine monitoring)\n");
        sb.append("   • MEDIUM   : 25 – 49 pts (Active tracking & sensor verification)\n");
        sb.append("   • HIGH     : 50 – 74 pts (Tactical triage & incident auto-dispatch)\n");
        sb.append("   • CRITICAL : 75 – 100 pts (Immediate interdiction readiness protocol)\n\n");

        sb.append("9. DATABASE TABLES:\n");
        sb.append("   1. detection_events    : Stores radar spatial, kinematic, and IFF telemetry.\n");
        sb.append("   2. risk_assessments    : Stores calculated score, category, explanations, and audit logs.\n");
        sb.append("   3. alerts              : Stores active prioritized alerts with rank justification.\n");
        sb.append("   4. incidents           : Stores dispatch tickets, assignee, status, and audit notes.\n");
        sb.append("   5. rule_configurations : Persists tunable weights and horizon thresholds.\n");
        sb.append("   6. evaluation_results  : Persists benchmark accuracy, precision, recall, and F1 results.\n\n");

        sb.append("10. EXPLAINABILITY:\n");
        sb.append("   Unlike opaque machine learning models, 100% of risk assessments provide an itemized factor\n");
        sb.append("   breakdown showing exact triggered points, withheld score reasons, and safe handling of UNKNOWN IFF.\n\n");

        sb.append("11. DUPLICATE HANDLING POLICIES:\n");
        sb.append("   • Duplicate Events    : Existing event records are updated in SQLite rather than duplicated.\n");
        sb.append("   • Duplicate Incidents : Suppresses redundant OPEN/IN_PROGRESS tickets for the same Event ID.\n");
        sb.append("   • Incident Reopening  : Permits new ticket generation once previous ticket is marked RESOLVED.\n\n");

        sb.append("12. TESTING METHODOLOGY:\n");
        sb.append("   Validated against a 15-scenario synthetic test suite covering edge cases, single-rule triggers,\n");
        sb.append("   multi-vector incursions, and boundary conditions. Automated self-tests pass with 100% accuracy.\n\n");

        sb.append("13. LIMITATIONS:\n");
        sb.append("   • Uses synthetic sensor inputs rather than physical millimeter-wave radar feeds.\n");
        sb.append("   • Illustrative scoring weights represent prototype demonstration assumptions.\n\n");

        sb.append("14. FUTURE SCOPE:\n");
        sb.append("   • Ingestion of ASTERIX Cat 010/020 and MAVLink / RemoteID telemetry.\n");
        sb.append("   • Automated EW RF jammer triggering and directional antenna slew-to-cue.\n\n");

        sb.append("15. MANDATORY PROJECT STATEMENT:\n");
        sb.append("   \"This project is a simulated, rule-based decision-support prototype. It does not perform\n");
        sb.append("    real sensor detection and does not use a trained machine-learning model.\"\n");

        area.setText(sb.toString());
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(DashboardComponents.BORDER_COLOR, 1, true));
        dlg.add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        btnPanel.setBackground(DashboardComponents.HEADER_BG);
        JButton btnClose = DashboardComponents.createPrimaryButton("Dismiss Guide");
        btnClose.addActionListener(e -> dlg.dispose());
        btnPanel.add(btnClose);
        dlg.add(btnPanel, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }
}
