package com.java.ui;

import com.java.model.Problem;
import com.java.model.Submission;
import com.java.service.ProblemService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResultPanel extends JPanel {
    private final ProblemService problemService;
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
        JLabel title = AppTheme.createHeadingLabel("Kết quả chấm bài");
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

        String[] columns = {"Submit", "Đề thi", "Kết quả", "Trạng thái", "Time (ms)", "Memory", "Lần nộp", "Lỗi"};
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
        resultTable.getColumnModel().getColumn(3).setCellRenderer(new StatusPillRenderer());
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        resultTable.getColumnModel().getColumn(7).setPreferredWidth(240);

        add(AppTheme.createStyledScrollPane(resultTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnPanel.setBackground(AppTheme.BG_DARK);
        btnPanel.setOpaque(false);
        JButton btnLoad = AppTheme.createPrimaryButton("Tải kết quả");
        JButton btnClear = AppTheme.createSecondaryButton("Xóa hiển thị");
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
        List<Submission> submissions = new ArrayList<>();

        if (filter != null && filter.id > 0) {
            submissions.addAll(problemService.getSubmissionsByProblem(filter.id));
        } else {
            for (Problem p : problemService.getAllProblems()) {
                submissions.addAll(problemService.getSubmissionsByProblem(p.getId()));
            }
        }

        Map<Integer, AttemptSummary> attempts = new LinkedHashMap<>();
        for (Submission s : submissions) {
            attempts.computeIfAbsent(s.getSampleCodeId(), id -> new AttemptSummary(id, s.getProblemId()))
                    .add(s);
        }

        List<AttemptSummary> rows = new ArrayList<>(attempts.values());
        rows.sort(Comparator.comparing((AttemptSummary a) -> a.lastSubmittedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        int fullAc = 0;
        int partial = 0;
        int failed = 0;
        for (AttemptSummary attempt : rows) {
            String status = attempt.overallStatus();
            if ("AC".equals(status)) fullAc++;
            else if (attempt.acCount > 0) partial++;
            else failed++;

            tableModel.addRow(new Object[]{
                    attempt.sampleCodeId,
                    attempt.problemId,
                    attempt.acCount + "/" + attempt.total,
                    status,
                    attempt.totalTimeMs,
                    attempt.maxMemoryKb > 0 ? attempt.maxMemoryKb + " KB" : "N/A",
                    attempt.lastSubmittedAt != null ? attempt.lastSubmittedAt.toString() : "",
                    attempt.errorPreview()
            });
        }

        statsLabel.setText(String.format("Tổng submit: %d | AC: %d | Một phần: %d | Lỗi: %d",
                rows.size(), fullAc, partial, failed));
        statsLabel.setForeground(AppTheme.TEXT_SECONDARY);
    }

    private static class AttemptSummary {
        private final int sampleCodeId;
        private final int problemId;
        private int total;
        private int acCount;
        private int totalTimeMs;
        private int maxMemoryKb;
        private Timestamp lastSubmittedAt;
        private String firstError;
        private boolean hasTle;
        private boolean hasCe;
        private boolean hasRe;
        private boolean hasWa;

        private AttemptSummary(int sampleCodeId, int problemId) {
            this.sampleCodeId = sampleCodeId;
            this.problemId = problemId;
        }

        private void add(Submission s) {
            total++;
            totalTimeMs += Math.max(0, s.getExecutionTime());
            maxMemoryKb = Math.max(maxMemoryKb, s.getMemoryUsed());
            if (s.getSubmittedAt() != null && (lastSubmittedAt == null || s.getSubmittedAt().after(lastSubmittedAt))) {
                lastSubmittedAt = s.getSubmittedAt();
            }

            String status = s.getStatus();
            if ("AC".equals(status)) acCount++;
            else if ("TLE".equals(status)) hasTle = true;
            else if ("CE".equals(status)) hasCe = true;
            else if ("RE".equals(status)) hasRe = true;
            else if ("WA".equals(status)) hasWa = true;

            if (firstError == null && s.getErrorMessage() != null && !s.getErrorMessage().isBlank()) {
                firstError = s.getErrorMessage();
            }
        }

        private String overallStatus() {
            if (total > 0 && acCount == total) return "AC";
            if (hasCe) return "CE";
            if (hasRe) return "RE";
            if (hasTle) return "TLE";
            if (hasWa || acCount > 0) return "WA";
            return "OTHER";
        }

        private String errorPreview() {
            if (firstError == null) return "";
            String cleaned = firstError.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
            return cleaned.length() > 80 ? cleaned.substring(0, 80) + "..." : cleaned;
        }
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
