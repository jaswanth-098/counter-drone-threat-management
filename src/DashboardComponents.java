import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * DashboardComponents.java
 *
 * Professional UI/UX component library for the Counter-Drone Threat Management System.
 * Enforces a cohesive dark military/defense operations theme:
 * - Slate/charcoal backgrounds
 * - Electric-blue accents
 * - High-contrast readable typography
 * - Standardized color-coding for tactical threat bands and incident states
 * - Custom-painted charts and badge cell renderers
 */
public class DashboardComponents {

    // --- Core Palette ---
    public static final Color BG_DARK = new Color(15, 23, 42);       // #0F172A
    public static final Color CARD_BG = new Color(30, 41, 59);      // #1E293B
    public static final Color CARD_HOVER = new Color(39, 52, 73);
    public static final Color HEADER_BG = new Color(20, 29, 44);
    public static final Color BORDER_COLOR = new Color(51, 65, 85);  // #334155
    public static final Color ACCENT_BLUE = new Color(56, 189, 248); // #38BDF8 (Electric Cyan/Blue)
    public static final Color ACCENT_HOVER = new Color(14, 165, 233);
    public static final Color ACCENT_PURPLE = new Color(168, 85, 247);

    // --- Typography Colors ---
    public static final Color TEXT_PRIMARY = new Color(248, 250, 252); // Crisp White #F8FAFC
    public static final Color TEXT_SECONDARY = new Color(148, 163, 184); // Slate 400 #94A3B8
    public static final Color TEXT_MUTED = new Color(100, 116, 139);     // Slate 500 #64748B

    // --- Threat Band Colors ---
    public static final Color COLOR_CRITICAL = new Color(239, 68, 68); // Red #EF4444
    public static final Color COLOR_HIGH = new Color(249, 115, 22);     // Orange #F97316
    public static final Color COLOR_MEDIUM = new Color(234, 179, 8);   // Amber/Yellow #EAB308
    public static final Color COLOR_LOW = new Color(34, 197, 94);      // Green #22C55E

    // --- Incident Status Colors ---
    public static final Color COLOR_OPEN = new Color(239, 68, 68);
    public static final Color COLOR_IN_PROGRESS = new Color(234, 179, 8);
    public static final Color COLOR_RESOLVED = new Color(34, 197, 94);

    // --- Typography Fonts ---
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_METRIC = new Font("Segoe UI", Font.BOLD, 26);

    /**
     * Summary card displaying key operational metric with color-coded accent strip.
     */
    public static class SummaryCard extends JPanel {
        private final JLabel titleLabel;
        private final JLabel valueLabel;
        private final JLabel subtitleLabel;
        private final Color accentColor;

        public SummaryCard(String title, String initialValue, String subtitle, Color accentColor) {
            this.accentColor = accentColor;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(12, 16, 12, 16)
            ));

            titleLabel = new JLabel(title.toUpperCase());
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            titleLabel.setForeground(TEXT_SECONDARY);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            valueLabel = new JLabel(initialValue);
            valueLabel.setFont(FONT_METRIC);
            valueLabel.setForeground(TEXT_PRIMARY);
            valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(FONT_SMALL);
            subtitleLabel.setForeground(TEXT_MUTED);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            add(titleLabel);
            add(Box.createRigidArea(new Dimension(0, 4)));
            add(valueLabel);
            add(Box.createRigidArea(new Dimension(0, 4)));
            add(subtitleLabel);
        }

        public void setValue(String value) {
            valueLabel.setText(value);
            repaint();
        }

        public void setSubtitle(String subtitle) {
            subtitleLabel.setText(subtitle);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Draw left accent color bar (width 4px)
            g2.setColor(accentColor);
            g2.fillRoundRect(0, 4, 4, getHeight() - 8, 4, 4);
            g2.dispose();
        }
    }

    /**
     * Custom-painted risk distribution bar visualization component.
     * Renders horizontal progress bars for LOW, MEDIUM, HIGH, CRITICAL categories
     * with counts and dynamically computed percentage distributions.
     */
    public static class RiskBarChart extends JPanel {
        private int lowCount = 0;
        private int mediumCount = 0;
        private int highCount = 0;
        private int criticalCount = 0;

        public RiskBarChart() {
            setBackground(CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(14, 16, 14, 16)
            ));
            setPreferredSize(new Dimension(300, 190));
        }

        public void updateCounts(int low, int medium, int high, int critical) {
            this.lowCount = Math.max(0, low);
            this.mediumCount = Math.max(0, medium);
            this.highCount = Math.max(0, high);
            this.criticalCount = Math.max(0, critical);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int total = lowCount + mediumCount + highCount + criticalCount;

            // Draw Section Title
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(TEXT_PRIMARY);
            g2.drawString("THREAT BAND DISTRIBUTION", 16, 22);

            g2.setFont(FONT_SMALL);
            g2.setColor(TEXT_SECONDARY);
            g2.drawString("Active Contact Tiers (Total: " + total + ")", 16, 38);

            if (total == 0) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g2.setColor(TEXT_SECONDARY);
                g2.drawString("NO THREAT DATA RECORDED", 16, 80);
                g2.setFont(FONT_SMALL);
                g2.setColor(TEXT_MUTED);
                g2.drawString("Ingest simulated detection events to visualize the real-time threat band distribution.", 16, 102);
                g2.dispose();
                return;
            }

            int startY = 56;
            int barHeight = 16;
            int gap = 28;
            int labelWidth = 72;
            int valueWidth = 60;
            int availableBarWidth = getWidth() - 32 - labelWidth - valueWidth;
            if (availableBarWidth < 40) availableBarWidth = 40;

            drawCategoryRow(g2, "CRITICAL", criticalCount, total, COLOR_CRITICAL, 16, startY, labelWidth, availableBarWidth, barHeight);
            drawCategoryRow(g2, "HIGH", highCount, total, COLOR_HIGH, 16, startY + gap, labelWidth, availableBarWidth, barHeight);
            drawCategoryRow(g2, "MEDIUM", mediumCount, total, COLOR_MEDIUM, 16, startY + (gap * 2), labelWidth, availableBarWidth, barHeight);
            drawCategoryRow(g2, "LOW", lowCount, total, COLOR_LOW, 16, startY + (gap * 3), labelWidth, availableBarWidth, barHeight);

            g2.dispose();
        }

        private void drawCategoryRow(Graphics2D g2, String label, int count, int total, Color color,
                                     int x, int y, int labelWidth, int barWidth, int barHeight) {
            // Category text
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(TEXT_SECONDARY);
            g2.drawString(label, x, y + 12);

            int barX = x + labelWidth;

            // Background track
            g2.setColor(new Color(45, 55, 72));
            g2.fillRoundRect(barX, y, barWidth, barHeight, 6, 6);

            // Colored fill
            double ratio = total > 0 ? (double) count / total : 0.0;
            int filledWidth = (int) Math.round(barWidth * ratio);
            if (count > 0 && filledWidth < 6) filledWidth = 6; // minimum visual dot

            if (filledWidth > 0) {
                g2.setColor(color);
                g2.fillRoundRect(barX, y, filledWidth, barHeight, 6, 6);
            }

            // Count and percentage text
            int pct = total > 0 ? (int) Math.round(ratio * 100.0) : 0;
            g2.setFont(FONT_SMALL);
            g2.setColor(TEXT_PRIMARY);
            String statsText = String.format("%d (%d%%)", count, pct);
            g2.drawString(statsText, barX + barWidth + 8, y + 12);
        }
    }

    /**
     * Table cell renderer formatting risk categories as stylish rounded pills.
     */
    public static class RiskBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            label.setOpaque(true);

            String cat = value != null ? value.toString().toUpperCase() : "";
            Color badgeBg;
            Color badgeFg;

            switch (cat) {
                case "CRITICAL":
                    badgeBg = new Color(239, 68, 68, 40);
                    badgeFg = COLOR_CRITICAL;
                    break;
                case "HIGH":
                    badgeBg = new Color(249, 115, 22, 40);
                    badgeFg = COLOR_HIGH;
                    break;
                case "MEDIUM":
                    badgeBg = new Color(234, 179, 8, 40);
                    badgeFg = COLOR_MEDIUM;
                    break;
                case "LOW":
                    badgeBg = new Color(34, 197, 94, 40);
                    badgeFg = COLOR_LOW;
                    break;
                default:
                    badgeBg = new Color(51, 65, 85, 40);
                    badgeFg = TEXT_SECONDARY;
                    break;
            }

            if (isSelected) {
                label.setBackground(new Color(51, 65, 85));
                label.setForeground(badgeFg);
            } else {
                label.setBackground(badgeBg);
                label.setForeground(badgeFg);
            }

            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(badgeFg, 1, true),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));

            return label;
        }
    }

    /**
     * Table cell renderer formatting incident lifecycle status as color-coded badges.
     */
    public static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            label.setOpaque(true);

            String status = value != null ? value.toString().toUpperCase() : "";
            Color badgeBg;
            Color badgeFg;

            switch (status) {
                case "OPEN":
                    badgeBg = new Color(239, 68, 68, 35);
                    badgeFg = COLOR_OPEN;
                    break;
                case "IN_PROGRESS":
                    badgeBg = new Color(234, 179, 8, 35);
                    badgeFg = COLOR_IN_PROGRESS;
                    break;
                case "RESOLVED":
                    badgeBg = new Color(34, 197, 94, 35);
                    badgeFg = COLOR_RESOLVED;
                    break;
                default:
                    badgeBg = new Color(51, 65, 85, 35);
                    badgeFg = TEXT_SECONDARY;
                    break;
            }

            if (isSelected) {
                label.setBackground(new Color(51, 65, 85));
                label.setForeground(badgeFg);
            } else {
                label.setBackground(badgeBg);
                label.setForeground(badgeFg);
            }

            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(badgeFg, 1, true),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));

            return label;
        }
    }

    /**
     * Default cell renderer for general text and numbers with alternating dark row striping.
     */
    public static class DarkTableCellRenderer extends DefaultTableCellRenderer {
        private final boolean center;

        public DarkTableCellRenderer(boolean center) {
            this.center = center;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (center) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                label.setHorizontalAlignment(SwingConstants.LEFT);
            }
            label.setFont(FONT_BODY);

            if (isSelected) {
                label.setBackground(new Color(51, 65, 85));
                label.setForeground(TEXT_PRIMARY);
            } else {
                if (row % 2 == 0) {
                    label.setBackground(CARD_BG);
                } else {
                    label.setBackground(new Color(24, 33, 47));
                }
                label.setForeground(TEXT_PRIMARY);
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            return label;
        }
    }

    /**
     * Applies complete dark styling to a JTable.
     */
    public static void styleTable(JTable table) {
        table.setBackground(CARD_BG);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(51, 65, 85));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFont(FONT_BODY);

        JTableHeader header = table.getTableHeader();
        header.setBackground(HEADER_BG);
        header.setForeground(ACCENT_BLUE);
        header.setFont(FONT_HEADER);
        header.setPreferredSize(new Dimension(header.getWidth(), 34));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        header.setReorderingAllowed(false);
    }

    /**
     * Button styling: Primary Electric Blue button.
     */
    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(Color.BLACK);
        btn.setBackground(ACCENT_BLUE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_BLUE.darker(), 1, true),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(ACCENT_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(ACCENT_BLUE);
            }
        });
        return btn;
    }

    /**
     * Button styling: Secondary Dark Slate button.
     */
    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(TEXT_PRIMARY);
        btn.setBackground(new Color(51, 65, 85));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR.brighter(), 1, true),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(71, 85, 105));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(51, 65, 85));
            }
        });
        return btn;
    }

    /**
     * Button styling: Danger Red button.
     */
    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(COLOR_CRITICAL);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_CRITICAL.darker(), 1, true),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));
        return btn;
    }

    /**
     * Styles text field for dark theme.
     */
    public static void styleTextField(JTextField tf) {
        tf.setBackground(new Color(15, 23, 42));
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(ACCENT_BLUE);
        tf.setFont(FONT_BODY);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    /**
     * Styles combo box for dark theme.
     */
    public static void styleComboBox(JComboBox<?> cb) {
        cb.setBackground(new Color(30, 41, 59));
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_BODY);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));
    }

    /**
     * Builds a reusable empty-state panel for tables with no records.
     */
    public static JPanel createEmptyStatePanel(String title, String message) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));

        JLabel iconLabel = new JLabel("🛡️");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(FONT_BODY);
        msgLabel.setForeground(TEXT_SECONDARY);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalGlue());
        panel.add(iconLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(msgLabel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }
}
