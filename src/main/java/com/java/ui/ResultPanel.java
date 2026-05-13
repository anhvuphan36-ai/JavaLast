package com.java.ui;

import com.java.model.Problem;
import com.java.model.Submission;
import com.java.service.ProblemService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ResultPanel extends JPanel {
    private ProblemService problemService;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel statsLabel;
    private JComboBox<ProblemComboItem> filterCombo;

    public ResultPanel(ProblemService problemService) {
        this.problemService = problemService;
        setLayout(new BorderLayout(16, 16));
        setBackground(AppTheme.BG_DARK);
        setBorder(AppTheme.BORDER_EMPTY_LG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.BG_DARK);
        header.setOpaque(false);
        JLabel title = AppTheme.createHeadingLabel("📊 Kết quả chấm bài");
        statsLabel = AppTheme.createBodyLabel("Chưa có dữ liệu");
        header.add(title, BorderLayout.WEST);
        header.add(statsLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBackground(AppTheme.BG_DARK);
        filterPanel.add(new JLabel("Lọc theo đề:"));
        filterCombo = new JComboBox<>();
        filterCombo.setPreferredSize(new Dimension(250, 28));
        refreshFilterList();
        filterCombo.addItemListener(e -> loadResults());
        filterPanel.add(filterCombo);
        add(filterPanel, BorderLayout.BEFORE_FIRST_LINE);

        String[] columns = {"ID", "Đề thi", "Code mẫu", "Testcase", "Status", "Time (ms)", "Memory (KB)", "Error"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        resultTable = new JTable(tableModel);
        resultTable.setFont(AppTheme.FONT_SMALL);
        resultTable.setRowHeight(36);
        resultTable.setGridColor(AppTheme.BORDER_COLOR);
        resultTable.setDefaultRenderer(Object.class, new AlternatingRowRenderer());
        resultTable.setShowGrid(true);
        resultTable.setIntercellSpacing(new Dimension(1, 1));
        resultTable.getTableHeader().setFont(AppTheme.FONT_SUBHEAD);
        resultTable.getTableHeader().setBackground(AppTheme.BG_CARD);
        resultTable.getTableHeader().setForeground(AppTheme.TEXT_PRIMARY);
        resultTable.getTableHeader().setPreferredSize(new Dimension(0, 40));

        resultTable.getColumnModel().getColumn(4).setCellRenderer(new StatusPillRenderer());
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        resultTable.getColumnModel().getColumn(7).setPreferredWidth(200);

        JScrollPane scroll = AppTheme.createStyledScrollPane(resultTable);
        add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnPanel.setBackground(AppTheme.BG_DARK);
        btnPanel.setOpaque(false);
        JButton btnLoad = AppTheme.createPrimaryButton("🔄 Tải kết quả");
        JButton btnClear = AppTheme.createSecondaryButton("🗑 Xóa kết quả");
        btnLoad.addActionListener(e -> loadResults());
        btnClear.addActionListener(e -> clearResults());
        btnPanel.add(btnLoad);
        btnPanel.add(btnClear);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void refreshFilterList() {
        filterCombo.removeAllItems();
        filterCombo.addItem(new ProblemComboItem(-1, "Tất cả đề thi"));
        for (Problem p : problemService.getAllProblems()) {
            filterCombo.addItem(new ProblemComboItem(p.getId(), p.getTitle()));
        }
    }

    private void clearResults() {
        tableModel.setRowCount(0);
        statsLabel.setText("Chưa có dữ liệu");
    }

    private void loadResults() {
        tableModel.setRowCount(0);
        ProblemComboItem filter = (ProblemComboItem) filterCombo.getSelectedItem();
        List<Submission> allSubs = new ArrayList<>();

        if (filter != null && filter.id > 0) {
            allSubs.addAll(problemService.getSubmissionsByProblem(filter.id));
        } else {
            for (Problem p : problemService.getAllProblems()) {
                allSubs.addAll(problemService.getSubmissionsByProblem(p.getId()));
            }
        }

        int ac = 0, wa = 0, tle = 0, other = 0;
        for (Submission s : allSubs) {
            String errorPreview = s.getErrorMessage() != null ?
                (s.getErrorMessage().length() > 40 ? s.getErrorMessage().substring(0, 40) + "..." : s.getErrorMessage()) : "";
            tableModel.addRow(new Object[]{
                    s.getId(), s.getProblemId(), s.getSampleCodeId(), s.getTestcaseId(),
                    s.getStatus(), s.getExecutionTime(), s.getMemoryUsed(), errorPreview
            });
            switch (s.getStatus()) {
                case "AC" -> ac++;
                case "WA" -> wa++;
                case "TLE" -> tle++;
                default -> other++;
            }
        }
        statsLabel.setText(String.format("Tổng: %d | ✅ AC: %d  ❌ WA: %d  ⏱ TLE: %d  ⚠ Other: %d",
                allSubs.size(), ac, wa, tle, other));
        statsLabel.setForeground(AppTheme.TEXT_SECONDARY);
    }

    private static class StatusPillRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = new JLabel(value != null ? value.toString() : "", SwingConstants.CENTER);
            String status = value != null ? value.toString() : "";
            Color bg = AppTheme.statusColor(status);
            label.setFont(AppTheme.FONT_BODY.deriveFont(Font.BOLD));
            label.setOpaque(true);
            label.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 40));
            label.setForeground(bg.brighter());
            label.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
            return label;
        }
    }

    private static class AlternatingRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? AppTheme.BG_DARK : AppTheme.BG_CARD);
                c.setForeground(AppTheme.TEXT_PRIMARY);
            }
            return c;
        }
    }

    private static class ProblemComboItem {
        int id;
        String title;
        ProblemComboItem(int id, String title) { this.id = id; this.title = title; }
        @Override public String toString() { return id < 0 ? title : "[" + id + "] " + title; }
    }
}
