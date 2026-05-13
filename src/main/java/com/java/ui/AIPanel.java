package com.java.ui;

import com.java.model.AIResponse;
import com.java.model.Problem;

import com.java.model.Testcase;
import com.java.service.AIService;
import com.java.service.GeminiAIService;
import com.java.service.ProblemService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AIPanel extends JPanel {
    private ProblemService problemService;
    private AIService aiService = new GeminiAIService();
    private JComboBox<ProblemComboItem> problemCombo;
    private JTextArea logArea;
    private JCheckBox chkGenerateChecker;
    private JTable testcaseTable;
    private DefaultTableModel testcaseTableModel;
    private SwingWorker<AIResponse, Void> aiWorker;

    public AIPanel(ProblemService problemService) {
        this.problemService = problemService;
        setLayout(new BorderLayout(16, 16));
        setBackground(AppTheme.BG_DARK);
        setBorder(AppTheme.BORDER_EMPTY_LG);

        JLabel lblTitle = AppTheme.createHeadingLabel("🤖 AI Phân tích & Sinh Testcase");
        lblTitle.setHorizontalAlignment(SwingConstants.LEFT);
        add(lblTitle, BorderLayout.NORTH);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topPanel.setBackground(AppTheme.BG_DARK);
        topPanel.add(new JLabel("Chọn đề thi:"));
        problemCombo = new JComboBox<>();
        problemCombo.setPreferredSize(new Dimension(250, 28));
        problemCombo.addActionListener(e -> viewExistingTestcases(false));
        topPanel.add(problemCombo);

        JButton btnRefresh = new JButton("🔄 Tải lại");
        btnRefresh.addActionListener(e -> refreshProblemList());
        topPanel.add(btnRefresh);

        chkGenerateChecker = new JCheckBox("Sinh checker script");
        topPanel.add(chkGenerateChecker);
        topPanel.add(new JLabel("(AI tự động sinh code AC + tính output bằng javac)"));

        JButton btnViewTC = new JButton("📋 Xem testcase hiện có");
        btnViewTC.addActionListener(e -> viewExistingTestcases());
        topPanel.add(btnViewTC);

        JButton btnDeleteAI = AppTheme.createAccentButton("🗑 Xóa testcase AI", AppTheme.ACCENT_YELLOW);
        btnDeleteAI.setToolTipText("Xóa các testcase do AI sinh (giữ testcase thủ công)");
        btnDeleteAI.addActionListener(e -> deleteAiTestcases());
        topPanel.add(btnDeleteAI);

        JButton btnDeleteAll = AppTheme.createAccentButton("💥 Xóa tất cả TC", AppTheme.ACCENT_RED);
        btnDeleteAll.setToolTipText("Xóa toàn bộ testcase của đề này");
        btnDeleteAll.addActionListener(e -> deleteAllTestcases());
        topPanel.add(btnDeleteAll);

        add(topPanel, BorderLayout.PAGE_START);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBackground(AppTheme.BG_DARK);
        splitPane.setDividerLocation(300);

        String[] tcColumns = {"#", "Loại", "Input", "Expected Output", "AI?"};
        testcaseTableModel = new DefaultTableModel(tcColumns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        testcaseTable = new JTable(testcaseTableModel);
        testcaseTable.setFont(AppTheme.FONT_SMALL);
        testcaseTable.setRowHeight(28);
        testcaseTable.setDefaultRenderer(Object.class, new AlternatingRowRenderer());
        testcaseTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        testcaseTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        testcaseTable.getColumnModel().getColumn(2).setPreferredWidth(250);
        testcaseTable.getColumnModel().getColumn(3).setPreferredWidth(250);
        testcaseTable.getColumnModel().getColumn(4).setPreferredWidth(40);
        JScrollPane tcScroll = new JScrollPane(testcaseTable);
        tcScroll.setBorder(BorderFactory.createTitledBorder("Testcases"));
        splitPane.setTopComponent(tcScroll);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(AppTheme.BG_INPUT);
        logArea.setForeground(AppTheme.TEXT_PRIMARY);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));
        splitPane.setBottomComponent(logScroll);

        add(splitPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnAnalyze = AppTheme.createAccentButton("🚀 Phân tích đề & Sinh testcase", AppTheme.ACCENT_PURPLE);
        btnAnalyze.addActionListener(e -> runAIAnalysis());
        btnPanel.add(btnAnalyze);
        add(btnPanel, BorderLayout.SOUTH);
        
        refreshProblemList();
    }

    private void refreshProblemList() {
        problemCombo.removeAllItems();
        List<Problem> problems = problemService.getAllProblems();
        for (Problem p : problems) {
            problemCombo.addItem(new ProblemComboItem(p.getId(), p.getTitle()));
        }
    }

    private void viewExistingTestcases() {
        viewExistingTestcases(true);
    }

    private void viewExistingTestcases(boolean showLog) {
        if (testcaseTable == null || testcaseTableModel == null) return;
        
        ProblemComboItem selected = (ProblemComboItem) problemCombo.getSelectedItem();
        if (selected == null) {
            if (showLog) JOptionPane.showMessageDialog(this, "Chọn đề thi trước!");
            return;
        }
        testcaseTableModel.setRowCount(0);
        List<Testcase> testcases = problemService.getTestcasesByProblem(selected.id);
        int i = 1;
        for (Testcase tc : testcases) {
            String inputPreview = tc.getInputData() != null ?
                (tc.getInputData().length() > 80 ? tc.getInputData().substring(0, 80) + "..." : tc.getInputData()) : "";
            String outputPreview = tc.getExpectedOutput() != null ?
                (tc.getExpectedOutput().length() > 80 ? tc.getExpectedOutput().substring(0, 80) + "..." : tc.getExpectedOutput()) : "";
            testcaseTableModel.addRow(new Object[]{
                i++, tc.getTestcaseType(), inputPreview, outputPreview, tc.isAiGenerated() ? "✓" : "✗"
            });
        }
        if (showLog) {
            logArea.append("Đã tải " + testcases.size() + " testcase của đề [" + selected.title + "].\n");
        }
        if (testcaseTable.getParent() != null && testcaseTable.getParent().getParent() instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) testcaseTable.getParent().getParent();
            if (scroll.getBorder() instanceof javax.swing.border.TitledBorder) {
                ((javax.swing.border.TitledBorder) scroll.getBorder()).setTitle("Testcases của: " + selected.title);
                scroll.repaint();
            }
        }
    }

    private void runAIAnalysis() {
        ProblemComboItem selected = (ProblemComboItem) problemCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đề thi!");
            return;
        }

        Problem problem = problemService.getProblemById(selected.id);
        if (problem == null) return;

        logArea.setText("");
        logArea.append("=== Đang phân tích đề: " + problem.getTitle() + " ===\n");
        logArea.append("Gọi Gemini API sinh code AC + inputs...\n");

        aiWorker = new SwingWorker<>() {
            @Override
            protected AIResponse doInBackground() {
                return aiService.analyzeProblem(problem);
            }

            @Override
            protected void done() {
                try {
                    AIResponse response = get();
                    if (response.isSuccess()) {
                        logArea.append("✅ AI đã sinh code AC + " +
                            (response.getTestcases() != null ? response.getTestcases().size() : 0) + " inputs!\n");
                        logArea.append("Giải thuật: " + response.getExplanation() + "\n");
                        logArea.append("[Hệ thống đang chạy code AC để tính expected output...]\n");
                        logArea.append("✅ Output đã được tính bằng javac — KHÔNG phụ thuộc vào AI tính số!\n");

                        int tcCount = response.getTestcases() != null ? response.getTestcases().size() : 0;
                        logArea.append("Đã có " + tcCount + " testcase với output chính xác.\n");

                        if (response.getTestcases() != null) {
                            testcaseTableModel.setRowCount(0);
                            int saved = 0;
                            int i = 1;
                            for (var tc : response.getTestcases()) {
                                boolean ok = problemService.addTestcaseFull(problem.getId(), tc.getInputData(), tc.getExpectedOutput(), tc.getTestcaseType(), true);
                                if (ok) {
                                    saved++;
                                    String inputPreview = tc.getInputData() != null ?
                                        (tc.getInputData().length() > 80 ? tc.getInputData().substring(0, 80) + "..." : tc.getInputData()) : "";
                                    String outputPreview = tc.getExpectedOutput() != null ?
                                        (tc.getExpectedOutput().length() > 80 ? tc.getExpectedOutput().substring(0, 80) + "..." : tc.getExpectedOutput()) : "";
                                    testcaseTableModel.addRow(new Object[]{
                                        i++, tc.getTestcaseType(), inputPreview, outputPreview, "✓"
                                    });
                                }
                            }
                            logArea.append("Đã lưu " + saved + "/" + tcCount + " testcase vào CSDL.\n");
                        }

                        if (chkGenerateChecker.isSelected() && response.getGeneratedChecker() != null && !response.getGeneratedChecker().isBlank()) {
                            problemService.updateCheckerScript(problem.getId(), response.getGeneratedChecker());
                            logArea.append("✅ Checker script đã được lưu vào DB.\n");
                        }

                        if (response.getGeneratedSolution() != null && !response.getGeneratedSolution().isBlank()) {
                            int codeId = problemService.addSampleCode(problem.getId(), response.getGeneratedSolution(), "java", "AC", true);
                            if (codeId > 0) logArea.append("✅ Đã lưu code AC (ID=" + codeId + ") vào CSDL.\n");
                        }
                    } else {
                        logArea.append("❌ Lỗi: " + response.getErrorMessage() + "\n");
                    }
                    logArea.append("=== Hoàn tất ===\n\n");
                } catch (Exception ex) {
                    logArea.append("❌ Lỗi: " + ex.getMessage() + "\n");
                    ex.printStackTrace();
                } finally {
                    aiWorker = null;
                }
            }
        };
        aiWorker.execute();
    }

    private void deleteAiTestcases() {
        ProblemComboItem selected = (ProblemComboItem) problemCombo.getSelectedItem();
        if (selected == null) { JOptionPane.showMessageDialog(this, "Chọn đề thi trước!"); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Xóa tất cả testcase DO AI SINH của đề [" + selected.title + "]?",
            "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            int count = problemService.deleteAiTestcasesForProblem(selected.id);
            logArea.append("Đã xóa " + count + " testcase AI của đề [" + selected.title + "].\n");
            testcaseTableModel.setRowCount(0);
        }
    }

    private void deleteAllTestcases() {
        ProblemComboItem selected = (ProblemComboItem) problemCombo.getSelectedItem();
        if (selected == null) { JOptionPane.showMessageDialog(this, "Chọn đề thi trước!"); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Xóa TOÀN BỘ testcase của đề [" + selected.title + "]? Không thể hoàn tác!",
            "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            int count = problemService.deleteAllTestcasesForProblem(selected.id);
            logArea.append("Đã xóa " + count + " testcase của đề [" + selected.title + "].\n");
            testcaseTableModel.setRowCount(0);
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
        @Override public String toString() { return "[" + id + "] " + title; }
    }
}
