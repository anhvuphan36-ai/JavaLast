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

        JLabel lblTitle = AppTheme.createHeadingLabel("AI Phân tích & Sinh Testcase");
        lblTitle.setHorizontalAlignment(SwingConstants.LEFT);
        add(lblTitle, BorderLayout.NORTH);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topPanel.setBackground(AppTheme.BG_DARK);
        topPanel.add(new JLabel("Chọn đề thi:"));
        problemCombo = new JComboBox<>();
        problemCombo.setPreferredSize(new Dimension(250, 28));
        problemCombo.addActionListener(e -> viewExistingTestcases(false));
        topPanel.add(problemCombo);

        JButton btnRefresh = new JButton("Tải lại");
        btnRefresh.setFont(AppTheme.FONT_BODY);
        btnRefresh.setPreferredSize(new Dimension(90, 32));
        btnRefresh.addActionListener(e -> refreshProblemList());
        topPanel.add(btnRefresh);

        chkGenerateChecker = new JCheckBox("Sinh checker script");
        topPanel.add(chkGenerateChecker);
        topPanel.add(new JLabel("(AI tự động sinh code AC + tính output bằng javac)"));

        JButton btnViewTC = new JButton("Xem testcase hiện có");
        btnViewTC.setFont(AppTheme.FONT_BODY);
        btnViewTC.setPreferredSize(new Dimension(170, 32));
        btnViewTC.addActionListener(e -> viewExistingTestcases());
        topPanel.add(btnViewTC);

        JButton btnDeleteTC = new JButton("Xóa 1 testcase");
        btnDeleteTC.setFont(AppTheme.FONT_BODY);
        btnDeleteTC.setPreferredSize(new Dimension(120, 32));
        btnDeleteTC.setToolTipText("Xóa một testcase theo số thứ tự");
        btnDeleteTC.addActionListener(e -> deleteSingleTestcase());
        topPanel.add(btnDeleteTC);

        JButton btnDeleteAI = new JButton("Xóa testcase AI");
        btnDeleteAI.setFont(AppTheme.FONT_BODY);
        btnDeleteAI.setPreferredSize(new Dimension(130, 32));
        btnDeleteAI.setToolTipText("Xóa các testcase do AI sinh (giữ testcase thủ công)");
        btnDeleteAI.addActionListener(e -> deleteAiTestcases());
        topPanel.add(btnDeleteAI);

        JButton btnDeleteAll = new JButton("Xóa tất cả TC");
        btnDeleteAll.setFont(AppTheme.FONT_BODY);
        btnDeleteAll.setPreferredSize(new Dimension(120, 32));
        btnDeleteAll.setToolTipText("Xóa toàn bộ testcase của đề này");
        btnDeleteAll.addActionListener(e -> deleteAllTestcases());
        topPanel.add(btnDeleteAll);

        add(topPanel, BorderLayout.PAGE_START);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBackground(AppTheme.BG_DARK);
        splitPane.setDividerLocation(300);

        String[] tcColumns = {"#", "Loại", "Input", "Expected Output", "Nguồn"};
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
        testcaseTable.getColumnModel().getColumn(4).setPreferredWidth(50);
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
        JButton btnAnalyze = AppTheme.createAccentButton("Phân tích đề & Sinh testcase", AppTheme.ACCENT_PURPLE);
        btnAnalyze.addActionListener(e -> runAIAnalysis());
        btnPanel.add(btnAnalyze);

        JButton btnAddTC = AppTheme.createAccentButton("+ Thêm testcase thủ công", AppTheme.ACCENT_GREEN);
        btnAddTC.setFont(AppTheme.FONT_BODY);
        btnAddTC.setPreferredSize(new Dimension(180, 36));
        btnAddTC.setToolTipText("Tự thêm testcase thủ công (input + expected output)");
        btnAddTC.addActionListener(e -> addManualTestcase());
        btnPanel.add(btnAddTC);

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
                i++, tc.getTestcaseType(), inputPreview, outputPreview, tc.isAiGenerated() ? "AI" : "Manual"
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

        if (aiWorker != null && !aiWorker.isDone()) {
            aiWorker.cancel(true);
            logArea.append("Đã hủy phân tích trước đó.\n");
        }

        Problem problem = problemService.getProblemById(selected.id);
        if (problem == null) return;

        logArea.setText("");
        logArea.append("=== Đang phân tích đề: " + problem.getTitle() + " ===\n");
        logArea.append("Gọi AI sinh code AC + inputs...\n");

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
                        logArea.append("[OK] AI đã sinh code AC + " +
                            (response.getTestcases() != null ? response.getTestcases().size() : 0) + " inputs!\n");
                        logArea.append("Giải thuật: " + response.getExplanation() + "\n");
                        logArea.append("[Hệ thống đang chạy code AC để tính expected output...]\n");
                        logArea.append("[OK] Output đã được tính bằng javac -- KHÔNG phụ thuộc vào AI tính số!\n");

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
                                        i++, tc.getTestcaseType(), inputPreview, outputPreview, "AI"
                                    });
                                }
                            }
                            logArea.append("Đã lưu " + saved + "/" + tcCount + " testcase vào CSDL.\n");
                        }

                        if (chkGenerateChecker.isSelected()) {
                            logArea.append("Đang sinh checker script...\n");
                            new SwingWorker<String, Void>() {
                                @Override
                                protected String doInBackground() {
                                    return aiService.generateChecker(problem);
                                }
                                @Override
                                protected void done() {
                                    try {
                                        String checkerCode = get();
                                        if (checkerCode != null && !checkerCode.isBlank() && !checkerCode.contains("Lỗi")) {
                                            problemService.updateCheckerScript(problem.getId(), checkerCode);
                                            logArea.append("[OK] Checker script đã được lưu vào DB.\n");
                                        } else {
                                            logArea.append("[!] Không sinh được checker: " + checkerCode + "\n");
                                        }
                                    } catch (Exception ex) {
                                        logArea.append("[LOI] Lỗi sinh checker: " + ex.getMessage() + "\n");
                                    }
                                }
                            }.execute();
                        }

                        if (response.getGeneratedSolution() != null && !response.getGeneratedSolution().isBlank()) {
                            int codeId = problemService.addSampleCode(problem.getId(), response.getGeneratedSolution(), "java", "AC", true);
                            if (codeId > 0) logArea.append("[OK] Đã lưu code AC (ID=" + codeId + ") vào CSDL.\n");
                        }
                    } else {
                        logArea.append("[LOI] " + response.getErrorMessage() + "\n");
                    }
                    logArea.append("=== Hoàn tất ===\n\n");
                } catch (Exception ex) {
                    logArea.append("[LOI] " + ex.getMessage() + "\n");
                    ex.printStackTrace();
                } finally {
                    aiWorker = null;
                }
            }
        };
        aiWorker.execute();
    }

    private void deleteSingleTestcase() {
        ProblemComboItem selected = (ProblemComboItem) problemCombo.getSelectedItem();
        if (selected == null) { JOptionPane.showMessageDialog(this, "Chọn đề thi trước!"); return; }
        String input = JOptionPane.showInputDialog(this,
            "Nhập số thứ tự testcase cần xóa (xem cột # trong bảng):");
        if (input == null || input.trim().isEmpty()) return;
        try {
            int index = Integer.parseInt(input.trim());
            List<Testcase> testcases = problemService.getTestcasesByProblem(selected.id);
            if (index < 1 || index > testcases.size()) {
                JOptionPane.showMessageDialog(this, "Số thứ tự không hợp lệ!");
                return;
            }
            Testcase tc = testcases.get(index - 1);
            int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa testcase #" + index + " (ID=" + tc.getId() + ") của đề [" + selected.title + "]?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean ok = problemService.deleteTestcase(tc.getId());
                if (ok) {
                    logArea.append("Đã xóa testcase #" + index + " (ID=" + tc.getId() + ").\n");
                    viewExistingTestcases(false);
                } else {
                    logArea.append("Xóa testcase thất bại.\n");
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số hợp lệ!");
        }
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
            viewExistingTestcases(false);
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

    private void addManualTestcase() {
        ProblemComboItem selected = (ProblemComboItem) problemCombo.getSelectedItem();
        if (selected == null) { JOptionPane.showMessageDialog(this, "Chọn đề thi trước!"); return; }

        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Input (dữ liệu đầu vào):"));
        JTextArea inputArea = new JTextArea(6, 40);
        inputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        inputArea.setLineWrap(true);
        JScrollPane inputScroll = new JScrollPane(inputArea);
        panel.add(inputScroll);

        panel.add(new JLabel("Expected Output (kết quả mong đợi - để trống nếu muốn AI tính):"));
        JTextArea outputArea = new JTextArea(4, 40);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputArea.setLineWrap(true);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        panel.add(outputScroll);

        String[] types = {"small", "normal", "edge", "anti-wa", "anti-tle", "stress"};
        JComboBox<String> typeCombo = new JComboBox<>(types);
        typeCombo.setSelectedItem("anti-tle");
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typePanel.add(new JLabel("Loại testcase:"));
        typePanel.add(typeCombo);
        panel.add(typePanel);

        JCheckBox chkAutoOutput = new JCheckBox("Tự động tính Expected Output bằng code AC (nếu có)");
        chkAutoOutput.setSelected(true);
        panel.add(chkAutoOutput);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Thêm testcase thủ công - Đề: [" + selected.title + "]",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Input không được để trống!");
            return;
        }

        String expectedOutput = outputArea.getText().trim();
        String type = (String) typeCombo.getSelectedItem();

        if (expectedOutput.isEmpty() && chkAutoOutput.isSelected()) {
            logArea.append("Đang tính expected output bằng code AC...\n");
            List<com.java.model.SampleCode> acCodes = problemService.getSampleCodesByProblem(selected.id);
            String acCode = null;
            for (com.java.model.SampleCode sc : acCodes) {
                if ("AC".equals(sc.getExpectedType()) && "java".equalsIgnoreCase(sc.getLanguage())) {
                    acCode = sc.getCodeContent();
                    break;
                }
            }
            if (acCode == null) {
                JOptionPane.showMessageDialog(this,
                    "Không tìm thấy code AC trong DB.\nHãy nhập Expected Output thủ công hoặc lưu code AC trước.",
                    "Thiếu code AC", JOptionPane.WARNING_MESSAGE);
                return;
            }

            com.java.service.JudgeEngine engine = new com.java.service.JudgeEngine();
            com.java.model.Problem problem = problemService.getProblemById(selected.id);
            int timeLimit = problem != null ? problem.getTimeLimit() : 2000;
            com.java.model.JudgeResult jr = engine.judge(acCode, "java", input, "", timeLimit * 2, 256);

            if ("CE".equals(jr.getStatus()) || "RE".equals(jr.getStatus()) || "TLE".equals(jr.getStatus())) {
                JOptionPane.showMessageDialog(this,
                    "Code AC bị lỗi khi chạy testcase này:\n" + jr.getStatus() + " - " + jr.getErrorMessage(),
                    "Lỗi code AC", JOptionPane.ERROR_MESSAGE);
                return;
            }
            expectedOutput = jr.getActualOutput().trim();
            logArea.append("Đã tính expected output: " + (expectedOutput.length() > 50 ? expectedOutput.substring(0, 50) + "..." : expectedOutput) + "\n");
        }

        if (expectedOutput.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Expected Output không được để trống!");
            return;
        }

        boolean ok = problemService.addTestcaseFull(selected.id, input, expectedOutput, type, false);
        if (ok) {
            logArea.append("[OK] Đã thêm testcase loại [" + type + "] vào đề [" + selected.title + "].\n");
            viewExistingTestcases(false);
        } else {
            logArea.append("[LOI] Thêm testcase thất bại.\n");
            JOptionPane.showMessageDialog(this, "Thêm testcase thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
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
