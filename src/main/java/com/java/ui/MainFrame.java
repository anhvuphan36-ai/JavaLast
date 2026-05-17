package com.java.ui;

import com.java.service.ProblemService;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JButton backBtn;
    private DashboardPanel dashboardPanel;
    private ProblemService problemService = new ProblemService();

    public MainFrame() {
        setTitle("Hệ thống Chấm Code Tự Động (AI Judge)");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(AppTheme.BG_DARKEST);

        // Top bar with back button + title
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(AppTheme.BG_DARKEST);
        topBar.setBorder(BorderFactory.createEmptyBorder(16, 32, 16, 32));

        backBtn = new JButton("← Trang chính");
        backBtn.setFont(AppTheme.FONT_BODY);
        backBtn.setForeground(AppTheme.ACCENT_CYAN);
        backBtn.setBackground(AppTheme.BG_CARD);
        backBtn.setFocusPainted(false);
        backBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.ACCENT_CYAN, 1),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> showPanel("DASHBOARD"));

        JLabel title = AppTheme.createTitleLabel("Hệ thống Chấm Code Tự Động (AI Judge)");
        title.setIconTextGap(12);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(backBtn);
        leftPanel.add(title);
        topBar.add(leftPanel, BorderLayout.WEST);

        JLabel subtitle = AppTheme.createBodyLabel("ICPC / IOI / Codeforces — AI Analysis & Judging");
        subtitle.setHorizontalAlignment(SwingConstants.RIGHT);
        topBar.add(subtitle, BorderLayout.EAST);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(AppTheme.BG_DARK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 32, 32, 32));

        dashboardPanel = new DashboardPanel(this, problemService);
        mainPanel.add(dashboardPanel, "DASHBOARD");
        mainPanel.add(new ProblemEntryPanel(problemService), "PROBLEM_ENTRY");
        mainPanel.add(new AIPanel(problemService), "AI_PANEL");
        mainPanel.add(new CodeSubmitPanel(problemService), "CODE_SUBMIT");
        mainPanel.add(new ResultPanel(problemService), "RESULT");

        setLayout(new BorderLayout());
        add(topBar, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        showPanel("DASHBOARD");
    }

    public void showPanel(String name) {
        backBtn.setVisible(!"DASHBOARD".equals(name));
        cardLayout.show(mainPanel, name);
        if ("DASHBOARD".equals(name) && dashboardPanel != null) {
            dashboardPanel.refreshStats();
        }
    }
}
