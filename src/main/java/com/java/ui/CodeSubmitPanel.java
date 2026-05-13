package com.java.ui;

import com.java.model.Problem;
import com.java.model.SampleCode;
import com.java.model.Submission;
import com.java.service.DefaultJudgeService;
import com.java.service.ProblemService;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Files;
import java.util.List;

public class CodeSubmitPanel extends JPanel {
    private ProblemService problemService;
    private JComboBox<ProblemComboItem> problemCombo;
    private JComboBox<String> languageCombo;
    private JComboBox<String> expectedTypeCombo;
    private JTextArea codeArea;
    private JTable resultTable;
    private DefaultTableModel resultTableModel;
    private JLabel statusLabel;
    private SwingWorker<List<Submission>, Void> judgeWorker;

    public CodeSubmitPanel(ProblemService problemService) {
        this.problemService = problemService;
        setLayout(new BorderLayout(16, 16));
        setBackground(AppTheme.BG_DARK);
        setBorder(AppTheme.BORDER_EMPTY_LG);

        JLabel lblTitle = AppTheme.createHeadingLabel("💻 Nộp code mẫu & Chấm thử");
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppTheme.BG_DARK);
        headerPanel.add(lblTitle, BorderLayout.NORTH);

        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(AppTheme.BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(new JLabel("Đề thi:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        problemCombo = new JComboBox<>();
        problemCombo.setPreferredSize(new Dimension(300, 28));
        topPanel.add(problemCombo, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JButton btnRefresh = new JButton("🔄");
        btnRefresh.addActionListener(e -> refreshProblems());
        topPanel.add(btnRefresh, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        topPanel.add(new JLabel("Ngôn ngữ:"), gbc);
        gbc.gridx = 1;
        languageCombo = new JComboBox<>(new String[]{"java", "cpp", "python"});
        topPanel.add(languageCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        topPanel.add(new JLabel("Loại code nộp:"), gbc);
        gbc.gridx = 1;
        expectedTypeCombo = new JComboBox<>(new String[]{"AC", "WA", "TLE"});
        expectedTypeCombo.setToolTipText(
            "AC = code đúng (kiểm testcase đúng không)\n" +
            "WA = code sai ló đức (kiểm testcase có bắt được không)\n" +
            "TLE = code chạm (kiểm testcase có đủ lớn không)");
        topPanel.add(expectedTypeCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 2;
        JButton btnUploadFile = new JButton("📁 Chọn file");
        btnUploadFile.addActionListener(e -> uploadCodeFile());
        topPanel.add(btnUploadFile, gbc);

        gbc.gridx = 3; gbc.gridy = 2;
        JButton btnGenerateAI = new JButton("🤖 AI Sinh Code Mẫu");
        btnGenerateAI.addActionListener(e -> generateAISampleCode());
        topPanel.add(btnGenerateAI, gbc);

        headerPanel.add(topPanel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        codeArea = AppTheme.createStyledTextArea(15, 50);
        add(new JScrollPane(codeArea), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setBackground(AppTheme.BG_DARK);

        statusLabel = new JLabel("Sẵn sàng chấm");
        statusLabel.setForeground(AppTheme.TEXT_SECONDARY);
        rightPanel.add(statusLabel, BorderLayout.NORTH);

        String[] columns = {"Testcase", "Status", "Time (ms)", "Expected", "Actual", "Error"};
        resultTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        resultTable = new JTable(resultTableModel);
        resultTable.setFont(AppTheme.FONT_SMALL);
        resultTable.setRowHeight(32);
        resultTable.setDefaultRenderer(Object.class, new AlternatingRowRenderer());
        resultTable.getColumnModel().getColumn(1).setCellRenderer(new StatusRenderer());
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(75);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        resultTable.getColumnModel().getColumn(5).setPreferredWidth(180);

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setPreferredSize(new Dimension(600, 300));
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel codeListPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        codeListPanel.setBackground(AppTheme.BG_DARK);
        JButton btnViewCodes = new JButton("Xem code đã lưu");
        btnViewCodes.addActionListener(e -> viewExistingCodes());
        JButton btnDeleteCodes = new JButton("🗑 Xóa mã");
        btnDeleteCodes.addActionListener(e -> deleteSampleCode());
        codeListPanel.add(btnViewCodes);
        codeListPanel.add(btnDeleteCodes);
        rightPanel.add(codeListPanel, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnSave = AppTheme.createAccentButton("💾 Lưu code mẫu", AppTheme.ACCENT_GREEN);
        JButton btnJudge = AppTheme.createAccentButton("⚖️ Chấm thử", AppTheme.ACCENT_CYAN);
        JButton btnSetAC = AppTheme.createAccentButton("📐 Tạo Expected Output từ code này", AppTheme.ACCENT_PURPLE);
        btnSetAC.setToolTipText("Chạy code hiện tại với từng testcase input, lấy kết quả làm expected output mới (chỉ dùng khi code này đúúng)");
        btnSave.addActionListener(e -> saveCode());
        btnJudge.addActionListener(e -> runJudge());
        btnSetAC.addActionListener(e -> recomputeExpected());
        btnPanel.add(btnSave);
        btnPanel.add(btnJudge);
        btnPanel.add(btnSetAC);
        add(btnPanel, BorderLayout.SOUTH);

        refreshProblems();
    }

    private void refreshProblems() {
        problemCombo.removeAllItems();
        List<Problem> list = problemService.getAllProblems();
        for (Problem p : list) {
            problemCombo.addItem(new ProblemComboItem(p.getId(), p.getTitle()));
        }
    }

    private void generateAISampleCode() {
        ProblemComboItem selected = (ProblemComboItem) problemCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Chọn đề thi trước!");
            return;
        }
        Problem p = problemService.getProblemById(selected.id);
        if (p == null) return;

        String lang = (String) languageCombo.getSelectedItem();
        String type = (String) expectedTypeCombo.getSelectedItem();
        
        String[] options = {"Chỉ " + lang, "Cả 3 ngôn ngữ (Java, C++, Python)"};
        int choice = JOptionPane.showOptionDialog(this,
            "Bạn muốn AI sinh code mẫu cho ngôn ngữ nào?",
            "Tùy chọn AI",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);

        if (choice < 0) return;

        if (choice == 0) {
            statusLabel.setText("⏳ Đang nhờ AI sinh code " + type + " (" + lang + ")...");
            statusLabel.setForeground(AppTheme.ACCENT_PURPLE);
            codeArea.setText("// Đang sinh code... Vui lòng đợi trong vài giây...");

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    return new com.java.service.GeminiAIService().generateSolution(p, lang, type);
                }
                @Override
                protected void done() {
                    try {
                        String code = get();
                        codeArea.setText(code);
                        statusLabel.setText("✅ AI đã sinh xong code mẫu " + type);
                        statusLabel.setForeground(AppTheme.ACCENT_GREEN);
                    } catch (Exception ex) {
                        codeArea.setText("// Lỗi: " + ex.getMessage());
                        statusLabel.setText("❌ Lỗi sinh code AI");
                        statusLabel.setForeground(AppTheme.ACCENT_RED);
                    }
                }
            }.execute();
        } else {
            statusLabel.setText("⏳ Đang nhờ AI sinh code 3 ngôn ngữ (" + type + ")...");
            statusLabel.setForeground(AppTheme.ACCENT_PURPLE);
            codeArea.setText("// Đang sinh code cho Java, C++, Python...\n// Quá trình này sẽ mất khoảng 10-15 giây, hệ thống tự động lưu khi xong.");

            new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() throws Exception {
                    String[] allLangs = {"java", "cpp", "python"};
                    com.java.service.GeminiAIService aiService = new com.java.service.GeminiAIService();
                    for (String l : allLangs) {
                        publish("⏳ Đang sinh " + l + "...");
                        String code = aiService.generateSolution(p, l, type);
                        if (!code.startsWith("// Lỗi")) {
                            problemService.addSampleCode(p.getId(), code, l, type, true);
                        }
                    }
                    return null;
                }

                @Override
                protected void process(List<String> chunks) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }

                @Override
                protected void done() {
                    statusLabel.setText("✅ Đã sinh và lưu code 3 ngôn ngữ!");
                    statusLabel.setForeground(AppTheme.ACCENT_GREEN);
                    codeArea.setText("// Đã hoàn tất!\n// Hãy nhấn nút 'Xem code đã lưu' bên dưới để kiểm tra.");
                    JOptionPane.showMessageDialog(CodeSubmitPanel.this, "Đã sinh xong cả 3 ngôn ngữ!\nVui lòng nhấn 'Xem code đã lưu' để kiểm tra.", "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
                }
            }.execute();
        }
    }

    private void uploadCodeFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Code files", "java", "cpp", "py", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = Files.readString(chooser.getSelectedFile().toPath());
                codeArea.setText(content);
                String name = chooser.getSelectedFile().getName().toLowerCase();
                if (name.endsWith(".java")) languageCombo.setSelectedItem("java");
                else if (name.endsWith(".cpp")) languageCombo.setSelectedItem("cpp");
                else if (name.endsWith(".py")) languageCombo.setSelectedItem("python");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi đọc file: " + e.getMessage());
            }
        }
    }

    private void viewExistingCodes() {
        ProblemComboItem selected = (ProblemComboItem) problemCombo.getSelectedItem();
        if (selected == null) return;

        List<SampleCode> codes = problemService.getSampleCodesByProblem(selected.id);
        if (codes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có code mẫu nào cho đề này.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append(String.format("📌 ĐỀ THI: [%d] %s\n", selected.id, selected.title));
        sb.append("====================================================\n\n");
        
        for (SampleCode sc : codes) {
            sb.append(String.format("🔹 [Mã Code: %d] Ngôn ngữ: %s | Loại: %s | AI Sinh: %s\n", 
                sc.getId(), sc.getLanguage(), sc.getExpectedType(), sc.isAiGenerated() ? "Có" : "Không"));
            sb.append(sc.getCodeContent()).append("\n\n");
        }

        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(800, 600));
        JOptionPane.showMessageDialog(this, sp, "Code mẫu đã lưu", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSampleCode() {
        String input = JOptionPane.showInputDialog(this, "Nhập ID của code mẫu cần xóa (Xem ID bằng nút bên cạnh):");
        if (input != null && !input.trim().isEmpty()) {
            try {
                int id = Integer.parseInt(input.trim());
                if (problemService.deleteSampleCode(id)) {
                    JOptionPane.showMessageDialog(this, "✅ Đã xóa code mẫu ID=" + id);
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Không tìm thấy code mẫu ID=" + id);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "ID không hợp lệ!");
            }
        }
    }

    private void saveCode() {
        ProblemComboItem selected = (ProblemComboItem) problemCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Chọn đề thi!");
            return;
        }
        String code = codeArea.getText().trim();
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Code trống!");
            return;
        }
        int id = problemService.addSampleCode(selected.id, code, (String) languageCombo.getSelectedItem(), (String) expectedTypeCombo.getSelectedItem(), false);
        if (id > 0) {
            JOptionPane.showMessageDialog(this, "✅ Lưu code thành công! ID = " + id);
        } else {
            JOptionPane.showMessageDialog(this, "❌ Lưu code thất bại!");
        }
    }

    private void runJudge() {
        ProblemComboItem selected = (ProblemComboItem) problemCombo.getSelectedItem();
        if (selected == null) return;
        String code = codeArea.getText().trim();
        if (code.isEmpty()) return;

        if (judgeWorker != null && !judgeWorker.isDone()) {
            judgeWorker.cancel(true);
            statusLabel.setText("Dừng chấm.");
            return;
        }

        List<com.java.model.Testcase> testcases = problemService.getTestcasesByProblem(selected.id);
        if (testcases.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có testcase nào! Hãy vào AI Phân tích để sinh testcase trước.");
            return;
        }

        resultTableModel.setRowCount(0);
        statusLabel.setText("⏳ Đang chấm bài... (nhấn lại để dừng)");
        statusLabel.setForeground(AppTheme.ACCENT_YELLOW);

        String language = (String) languageCombo.getSelectedItem();
        String expectedType = (String) expectedTypeCombo.getSelectedItem();

        judgeWorker = new SwingWorker<>() {
            int tempCodeId = -1;

            @Override
            protected List<Submission> doInBackground() {
                tempCodeId = problemService.addSampleCode(selected.id, code, language, "TEMP", false);
                if (tempCodeId <= 0) return new java.util.ArrayList<>();
                try {
                    return problemService.runJudging(selected.id, tempCodeId, new DefaultJudgeService());
                } finally {
                    problemService.deleteSampleCode(tempCodeId);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Submission> results = get();
                    if (results.isEmpty() && tempCodeId <= 0) {
                        statusLabel.setText("❌ Lỗi lưu code.");
                        statusLabel.setForeground(AppTheme.ACCENT_RED);
                        return;
                    }
                    int ac = 0, wa = 0, tle = 0, other = 0;
                    List<com.java.model.Testcase> tcs = problemService.getTestcasesByProblem(selected.id);
                    java.util.Map<Integer, com.java.model.Testcase> tcMap = new java.util.HashMap<>();
                    for (com.java.model.Testcase tc : tcs) tcMap.put(tc.getId(), tc);

                    for (Submission sub : results) {
                        com.java.model.Testcase tc = tcMap.get(sub.getTestcaseId());
                        String expected = tc != null && tc.getExpectedOutput() != null ?
                            shorten(tc.getExpectedOutput(), 30) : "";
                        String actual = sub.getActualOutput() != null ?
                            shorten(sub.getActualOutput(), 30) : "";
                        String errorPreview = sub.getErrorMessage() != null ?
                            shorten(sub.getErrorMessage(), 40) : "";
                        resultTableModel.addRow(new Object[]{
                            "TC#" + sub.getTestcaseId(),
                            sub.getStatus(),
                            sub.getExecutionTime() + "ms",
                            expected,
                            actual,
                            errorPreview
                        });
                        switch (sub.getStatus()) {
                            case "AC" -> ac++;
                            case "WA" -> wa++;
                            case "TLE" -> tle++;
                            default -> other++;
                        }
                    }
                    String verdict = buildVerdict(expectedType, ac, wa, tle, other, results.size());
                    statusLabel.setText(verdict);
                    boolean isGood = isGoodVerdict(expectedType, ac, wa, tle, results.size());
                    statusLabel.setForeground(isGood ? AppTheme.ACCENT_GREEN : AppTheme.ACCENT_RED);
                } catch (Exception ex) {
                    if (!isCancelled()) {
                        statusLabel.setText("❌ Lỗi: " + ex.getMessage());
                        statusLabel.setForeground(AppTheme.ACCENT_RED);
                    }
                } finally {
                    judgeWorker = null;
                }
            }
        };
        judgeWorker.execute();
    }

    private void recomputeExpected() {
        ProblemComboItem selected = (ProblemComboItem) problemCombo.getSelectedItem();
        if (selected == null) return;
        String code = codeArea.getText().trim();
        if (code.isEmpty()) { JOptionPane.showMessageDialog(this, "Dán code vào trước!"); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Chạy code này với tất cả testcase input của đề [" + selected.title + "],\n" +
            "và ghi đè expected output bằng kết quả thực tế?\n\n" +
            "CHỈ DÙNG KHI CODE NÀY LÀ CODE AC CHÍNH XÁC!",
            "Xác nhận tạo Expected Output",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        statusLabel.setText("⏳ Đang tính lại expected output...");
        statusLabel.setForeground(AppTheme.ACCENT_YELLOW);

        String language = (String) languageCombo.getSelectedItem();
        String code2 = code;
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return problemService.recomputeExpectedOutputs(
                    selected.id, code2, language, new DefaultJudgeService());
            }
            @Override
            protected void done() {
                try {
                    int count = get();
                    statusLabel.setText("✅ Đã cập nhật expected output cho " + count + " testcase.");
                    statusLabel.setForeground(AppTheme.ACCENT_GREEN);
                    JOptionPane.showMessageDialog(CodeSubmitPanel.this,
                        "✅ Đã cập nhật " + count + " testcase!\nGiờ chấm lại sẽ cho kết quả chính xác.",
                        "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    statusLabel.setText("❌ Lỗi: " + ex.getMessage());
                    statusLabel.setForeground(AppTheme.ACCENT_RED);
                }
            }
        }.execute();
    }

    private String buildVerdict(String expectedType, int ac, int wa, int tle, int other, int total) {
        switch (expectedType) {
            case "AC":
                if (wa == 0 && tle == 0 && other == 0 && ac > 0)
                    return "✅ All AC (" + ac + "/" + total + ") — Testcase ĐÚNG, code chạy đúng!";
                else
                    return String.format("⚠ AC:%d WA:%d TLE:%d / %d — Testcase có thể SAI hoặc code chưa đúng!", ac, wa, tle, total);
            case "WA":
                if (wa > 0)
                    return String.format("✅ WA:%d AC:%d / %d — Testcase ĐỦ MẠNH, bắt được code sai!", wa, ac, total);
                else
                    return String.format("⚠ All AC (%d/%d) — Testcase QUÁ YẾU, không bắt được code sai!", ac, total);
            case "TLE":
                if (tle > 0)
                    return String.format("✅ TLE:%d / %d — Testcase ĐỦ LỚN, bắt được code chậm!", tle, total);
                else
                    return String.format("⚠ Không có TLE (%d/%d AC) — Testcase chưa đủ lớn!", ac, total);
            default:
                return String.format("AC:%d WA:%d TLE:%d / %d", ac, wa, tle, total);
        }
    }

    private boolean isGoodVerdict(String expectedType, int ac, int wa, int tle, int total) {
        switch (expectedType) {
            case "AC":  return wa == 0 && tle == 0 && ac > 0;
            case "WA":  return wa > 0;
            case "TLE": return tle > 0;
            default:    return true;
        }
    }

    private String shorten(String s, int maxLen) {
        if (s == null) return "";
        String oneLine = s.replace("\n", "⏎").replace("\r", "");
        return oneLine.length() > maxLen ? oneLine.substring(0, maxLen) + "…" : oneLine;
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

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = new JLabel(value != null ? value.toString() : "", SwingConstants.CENTER);
            label.setOpaque(true);
            label.setFont(AppTheme.FONT_BODY.deriveFont(Font.BOLD));

            String status = value != null ? value.toString() : "";
            Color bg = AppTheme.statusColor(status);
            label.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 50));
            label.setForeground(bg.brighter());
            label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            return label;
        }
    }
}
