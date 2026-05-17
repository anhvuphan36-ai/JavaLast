package com.java.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public final class AppTheme {

    // ===== COLOR PALETTE (Modern Clean Light) =====
    public static final Color BG_DARKEST   = new Color(0xF8, 0xFA, 0xFC); // Slate 50
    public static final Color BG_DARK      = new Color(0xF1, 0xF5, 0xF9); // Slate 100
    public static final Color BG_CARD      = Color.WHITE;
    public static final Color BG_INPUT     = Color.WHITE;

    public static final Color ACCENT_CYAN  = new Color(0x02, 0x84, 0xC7); // Sky 600 (Primary Blue)
    public static final Color ACCENT_GREEN = new Color(0x16, 0xA3, 0x4A); // Green 600
    public static final Color ACCENT_RED   = new Color(0xDC, 0x26, 0x26); // Red 600
    public static final Color ACCENT_YELLOW= new Color(0xD9, 0x77, 0x06); // Amber 600
    public static final Color ACCENT_PURPLE= new Color(0x7C, 0x3A, 0xED); // Violet 600

    public static final Color TEXT_PRIMARY   = new Color(0x0F, 0x17, 0x2A); // Slate 900
    public static final Color TEXT_SECONDARY = new Color(0x47, 0x55, 0x69); // Slate 600
    public static final Color TEXT_MUTED     = new Color(0x94, 0xA3, 0xB8); // Slate 400

    // ===== FONTS (Larger, readable) =====
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBHEAD = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_CODE    = new Font("Consolas", Font.PLAIN, 14);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BUTTON  = new Font("Segoe UI", Font.BOLD, 14);

    // ===== BORDERS =====
    public static final Color BORDER_COLOR = new Color(0xE2, 0xE8, 0xF0); // Slate 200
    public static final Border BORDER_CARD = new LineBorder(BORDER_COLOR, 1, true);
    public static final Border BORDER_INPUT = new LineBorder(new Color(0xCB, 0xD5, 0xE1), 1, true); // Slate 300
    public static final Border BORDER_EMPTY_SM = new EmptyBorder(8, 12, 8, 12);
    public static final Border BORDER_EMPTY_MD = new EmptyBorder(16, 20, 16, 20);
    public static final Border BORDER_EMPTY_LG = new EmptyBorder(24, 32, 24, 32);

    // ===== STATUS COLORS =====
    public static Color statusColor(String status) {
        return switch (status) {
            case "AC" -> ACCENT_GREEN;
            case "WA" -> ACCENT_RED;
            case "TLE" -> ACCENT_YELLOW;
            case "RE", "CE", "MLE" -> TEXT_MUTED;
            default -> TEXT_SECONDARY;
        };
    }

    // ===== UI FACTORIES =====
    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setForeground(Color.WHITE);
        btn.setBackground(ACCENT_CYAN);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 24, 10, 24));
        btn.setMinimumSize(new Dimension(80, 36));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { btn.setBackground(ACCENT_CYAN.darker()); }
            public void mouseExited(java.awt.event.MouseEvent evt) { btn.setBackground(ACCENT_CYAN); }
        });
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setForeground(TEXT_PRIMARY);
        btn.setBackground(BG_CARD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(9, 23, 9, 23)
        ));
        btn.setMinimumSize(new Dimension(80, 36));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { btn.setBackground(BG_DARK); }
            public void mouseExited(java.awt.event.MouseEvent evt) { btn.setBackground(BG_CARD); }
        });
        return btn;
    }

    public static JButton createAccentButton(String text, Color bg) {
        JButton btn = createPrimaryButton(text);
        btn.setBackground(bg);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { btn.setBackground(bg.darker()); }
            public void mouseExited(java.awt.event.MouseEvent evt) { btn.setBackground(bg); }
        });
        return btn;
    }

    public static JLabel createTitleLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    public static JLabel createHeadingLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_HEADING);
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    public static JLabel createBodyLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_SECONDARY);
        return lbl;
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BORDER_CARD,
                BORDER_EMPTY_MD
        ));
        return panel;
    }

    public static JScrollPane createStyledScrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BORDER_CARD);
        sp.getViewport().setBackground(BG_CARD);
        sp.setBackground(BG_CARD);
        return sp;
    }

    public static JTextArea createStyledTextArea(int rows, int cols) {
        JTextArea ta = new JTextArea(rows, cols);
        ta.setFont(FONT_CODE);
        ta.setBackground(BG_INPUT);
        ta.setForeground(TEXT_PRIMARY);
        ta.setCaretColor(TEXT_PRIMARY);
        ta.setBorder(BORDER_EMPTY_SM);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setSelectionColor(new Color(0xBA, 0xE6, 0xFD)); // Sky 200
        ta.setSelectedTextColor(TEXT_PRIMARY);
        return ta;
    }

    public static JTextField createStyledTextField(int cols) {
        JTextField tf = new JTextField(cols);
        tf.setFont(FONT_BODY);
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(TEXT_PRIMARY);
        tf.setBorder(BorderFactory.createCompoundBorder(BORDER_INPUT, BORDER_EMPTY_SM));
        tf.setSelectionColor(new Color(0xBA, 0xE6, 0xFD));
        tf.setSelectedTextColor(TEXT_PRIMARY);
        return tf;
    }

    public static JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(FONT_BODY);
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_PRIMARY);
        cb.setBorder(BORDER_EMPTY_SM);
        cb.setPreferredSize(new Dimension(cb.getPreferredSize().width, 38));
        return cb;
    }

    public static void applyGlobalTheme() {
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
            UIManager.put("Panel.background", BG_DARK);
            UIManager.put("OptionPane.background", BG_DARK);
            UIManager.put("TextField.background", BG_INPUT);
            UIManager.put("TextArea.background", BG_INPUT);
            UIManager.put("ComboBox.background", BG_INPUT);
            UIManager.put("Table.background", BG_CARD);
            UIManager.put("Table.foreground", TEXT_PRIMARY);
            UIManager.put("Table.gridColor", BORDER_COLOR);
            UIManager.put("Table.selectionBackground", new Color(0xE0, 0xF2, 0xFE)); // Sky 100
            UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
            UIManager.put("Button.background", BG_CARD);
            UIManager.put("Button.foreground", TEXT_PRIMARY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
