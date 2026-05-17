package com.java.ui;

import com.java.model.AIResponse;
import com.java.model.Problem;
import com.java.model.Testcase;
import com.java.service.AIService;
import com.java.service.GeminiAIService;
import com.java.service.ProblemService;
import com.java.util.FileManager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ProblemEntryPanel extends JPanel {
    private ProblemService problemService;
    private AIService aiService = new GeminiAIService();
    private JTextField titleField;
    private JTextArea descArea;
    private JLabel attachmentLabel;
    private JLabel autoGenerateStatusLabel;
    private JComboBox<String> contestTypeBox;
    private JTextField timeLimitField;
    private JTextField memoryLimitField;
    private String selectedAttachmentPath = null;
    private JList<Problem> problemList;
    private DefaultListModel<Problem> problemListModel;

    public ProblemEntryPanel(ProblemService problemService) {
        this.problemService = problemService;
        setLayout(new BorderLayout(16, 16));
        setBackground(AppTheme.BG_DARK);
        setBorder(AppTheme.BORDER_EMPTY_LG);

        JLabel lblTitle = AppTheme.createHeadingLabel("Nhập đề thi mới");
        lblTitle.setHorizontalAlignment(SwingConstants.LEFT);
        add(lblTitle, BorderLayout.NORTH);

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setBackground(AppTheme.BG_DARK);
        leftPanel.setPreferredSize(new Dimension(280, 0));

        JLabel listLabel = new JLabel("Danh sách đề đã có:");
        listLabel.setForeground(AppTheme.TEXT_SECONDARY);
        leftPanel.add(listLabel, BorderLayout.NORTH);

        problemListModel = new DefaultListModel<>();
        problemList = new JList<>(problemListModel);
        problemList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Problem) {
                    Problem p = (Problem) value;
                    setText("[" + p.getId() + "] " + p.getTitle());
                }
                setBackground(isSelected ? AppTheme.ACCENT_CYAN.darker() : AppTheme.BG_CARD);
                setForeground(isSelected ? Color.WHITE : AppTheme.TEXT_PRIMARY);
                return this;
            }
        });
        problemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        problemList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Problem selected = problemList.getSelectedValue();
                if (selected != null) loadProblemToForm(selected);
            }
        });

        JScrollPane listScroll = new JScrollPane(problemList);
        listScroll.setBorder(AppTheme.BORDER_CARD);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        JButton btnDelete = AppTheme.createAccentButton("Xóa đề", AppTheme.ACCENT_RED);
        btnDelete.addActionListener(e -> deleteSelectedProblem());
        leftPanel.add(btnDelete, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(AppTheme.BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(createFormLabel("Tiêu đề:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        titleField = AppTheme.createStyledTextField(30);
        formPanel.add(titleField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(createFormLabel("Nội dung đề:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        descArea = AppTheme.createStyledTextArea(10, 30);
        formPanel.add(new JScrollPane(descArea), gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0;
        formPanel.add(createFormLabel("File đề bài:"), gbc);
        gbc.gridx = 1;
        JPanel attachmentPanel = new JPanel(new BorderLayout(8, 4));
        attachmentPanel.setBackground(AppTheme.BG_DARK);
        attachmentLabel = new JLabel("Chưa chọn file", JLabel.CENTER);
        attachmentLabel.setPreferredSize(new Dimension(180, 120));
        attachmentLabel.setBorder(BorderFactory.createLineBorder(AppTheme.TEXT_MUTED));
        attachmentLabel.setForeground(AppTheme.TEXT_MUTED);
        JButton btnChooseFile = new JButton("Chọn ảnh...");
        btnChooseFile.addActionListener(e -> chooseProblemFile());
        attachmentPanel.add(attachmentLabel, BorderLayout.CENTER);
        attachmentPanel.add(btnChooseFile, BorderLayout.SOUTH);
        formPanel.add(attachmentPanel, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(createFormLabel("Loại kỳ thi:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        contestTypeBox = AppTheme.createStyledComboBox(new String[]{"ICPC", "IOI", "Codeforces", "AtCoder", "Other"});
        formPanel.add(contestTypeBox, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(createFormLabel("Time limit (ms):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        timeLimitField = AppTheme.createStyledTextField(10);
        timeLimitField.setText("2000");
        formPanel.add(timeLimitField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(createFormLabel("Memory limit (MB):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        memoryLimitField = AppTheme.createStyledTextField(10);
        memoryLimitField.setText("256");
        formPanel.add(memoryLimitField, gbc);

        add(formPanel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout(4, 4));
        southPanel.setBackground(AppTheme.BG_DARK);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnPanel.setBackground(AppTheme.BG_DARK);
        JButton btnSave = AppTheme.createPrimaryButton("Lưu đề mới");
        JButton btnUpdate = AppTheme.createAccentButton("Cập nhật", AppTheme.ACCENT_YELLOW);
        JButton btnNew = AppTheme.createSecondaryButton("Mới");
        btnSave.addActionListener(e -> saveProblem());
        btnUpdate.addActionListener(e -> updateExistingProblem());
        btnNew.addActionListener(e -> clearForm());
        btnPanel.add(btnSave);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnNew);
        autoGenerateStatusLabel = new JLabel(" ");
        autoGenerateStatusLabel.setForeground(AppTheme.TEXT_SECONDARY);
        autoGenerateStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        southPanel.add(btnPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        refreshProblemList();
    }

    private JLabel createFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppTheme.FONT_BODY);
        lbl.setForeground(AppTheme.TEXT_SECONDARY);
        return lbl;
    }

    private void refreshProblemList() {
        problemListModel.clear();
        List<Problem> problems = problemService.getAllProblems();
        for (Problem p : problems) {
            problemListModel.addElement(p);
        }
    }

    private void loadProblemToForm(Problem p) {
        titleField.setText(p.getTitle());
        descArea.setText(p.getDescription() != null ? p.getDescription() : "");
        contestTypeBox.setSelectedItem(p.getContestType());
        timeLimitField.setText(String.valueOf(p.getTimeLimit()));
        memoryLimitField.setText(String.valueOf(p.getMemoryLimit()));
        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            attachmentLabel.setText(new File(p.getImagePath()).getName());
            attachmentLabel.setIcon(null);
            if (isImageFile(p.getImagePath())) {
                try {
                    attachmentLabel.setIcon(new ImageIcon(new ImageIcon(p.getImagePath()).getImage().getScaledInstance(160, 100, Image.SCALE_SMOOTH)));
                } catch (Exception e) {
                    attachmentLabel.setText("Không tải được file");
                }
            }
        } else {
            attachmentLabel.setText("Chưa có file");
            attachmentLabel.setIcon(null);
        }
        selectedAttachmentPath = p.getImagePath();
    }

    private void chooseProblemFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif", "bmp", "webp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            selectedAttachmentPath = file.getAbsolutePath();
            attachmentLabel.setText(file.getName());
            attachmentLabel.setIcon(null);
            if (isImageFile(selectedAttachmentPath)) {
                attachmentLabel.setIcon(new ImageIcon(new ImageIcon(selectedAttachmentPath).getImage().getScaledInstance(160, 100, Image.SCALE_SMOOTH)));
            }
        }
    }

    private void triggerAutoGenerateTestcases(Problem p) {
        SwingWorker<AutoGenerationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AutoGenerationResult doInBackground() {
                AIResponse res = aiService.analyzeProblem(p);
                if (res == null) {
                    return AutoGenerationResult.failed("AI không trả về kết quả.");
                }
                if (!res.isSuccess() || res.getTestcases() == null || res.getTestcases().isEmpty()) {
                    return AutoGenerationResult.failed(res.getErrorMessage());
                }

                problemService.deleteAiTestcasesForProblem(p.getId());
                int saved = 0;
                for (Testcase tc : res.getTestcases()) {
                    boolean ok = problemService.addTestcaseFull(
                            p.getId(),
                            tc.getInputData(),
                            tc.getExpectedOutput(),
                            tc.getTestcaseType(),
                            true
                    );
                    if (ok) saved++;
                }

                if (res.getGeneratedSolution() != null && !res.getGeneratedSolution().isBlank()) {
                    problemService.addSampleCode(p.getId(), res.getGeneratedSolution(), "java", "AC", true);
                }

                return AutoGenerationResult.succeeded(saved);
            }

            @Override
            protected void done() {
                try {
                    AutoGenerationResult result = get();
                    if (autoGenerateStatusLabel != null) {
                        if (result.isSuccess()) {
                            autoGenerateStatusLabel.setText("[OK] Sinh " + result.getSavedTestcases() + " testcase + code AC");
                            autoGenerateStatusLabel.setForeground(new java.awt.Color(0x16, 0xA3, 0x4A));
                        } else {
                            autoGenerateStatusLabel.setText("[!] AI: " + result.getErrorMessage());
                            autoGenerateStatusLabel.setForeground(new java.awt.Color(0xDC, 0x26, 0x26));
                        }
                    }
                } catch (Exception ex) {
                    if (autoGenerateStatusLabel != null) {
                        autoGenerateStatusLabel.setText("[LOI] " + ex.getMessage());
                        autoGenerateStatusLabel.setForeground(new java.awt.Color(0xDC, 0x26, 0x26));
                    }
                }
            }
        };
        worker.execute();
    }

    private void saveProblem() {
        String title = titleField.getText().trim();
        String desc = descArea.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tiêu đề!");
            return;
        }
        if (desc.isEmpty() && selectedAttachmentPath == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập nội dung đề hoặc chọn ảnh!");
            return;
        }
        if (title.length() > 255) {
            JOptionPane.showMessageDialog(this, "Tiêu đề không được quá 255 ký tự!");
            return;
        }

        int timeLimit, memoryLimit;
        try {
            timeLimit = Integer.parseInt(timeLimitField.getText().trim());
            memoryLimit = Integer.parseInt(memoryLimitField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Time limit / Memory limit phải là số nguyên!");
            return;
        }
        if (timeLimit <= 0 || timeLimit > 30000) {
            JOptionPane.showMessageDialog(this, "Time limit phải từ 1 đến 30000 ms (30 giây)!");
            return;
        }
        if (memoryLimit <= 0 || memoryLimit > 2048) {
            JOptionPane.showMessageDialog(this, "Memory limit phải từ 1 đến 2048 MB!");
            return;
        }

        Problem p = new Problem();
        p.setTitle(title);
        p.setDescription(desc.isEmpty() ? "Nội dung đề trong file đính kèm." : desc);
        p.setContestType((String) contestTypeBox.getSelectedItem());
        p.setTimeLimit(timeLimit);
        p.setMemoryLimit(memoryLimit);

        int problemId = problemService.createProblem(p);
        if (problemId > 0) {
            p.setId(problemId);
            if (selectedAttachmentPath != null) {
                try {
                    String savedPath = FileManager.saveProblemAttachment(problemId, selectedAttachmentPath);
                    p.setImagePath(savedPath);
                    problemService.updateProblem(p);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            triggerAutoGenerateTestcases(p);
            clearForm();
            refreshProblemList();
        } else {
            JOptionPane.showMessageDialog(this, "Lưu đề thi thất bại!");
        }
    }

    private void updateExistingProblem() {
        Problem selected = problemList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Chọn đề cần cập nhật trong danh sách bên trái!");
            return;
        }
        String title = titleField.getText().trim();
        String desc = descArea.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tiêu đề!");
            return;
        }
        if (desc.isEmpty() && selectedAttachmentPath == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập nội dung đề hoặc chọn ảnh!");
            return;
        }
        int timeLimit, memoryLimit;
        try {
            timeLimit = Integer.parseInt(timeLimitField.getText().trim());
            memoryLimit = Integer.parseInt(memoryLimitField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Time limit / Memory limit phải là số nguyên!");
            return;
        }
        if (timeLimit <= 0 || timeLimit > 30000 || memoryLimit <= 0 || memoryLimit > 2048) {
            JOptionPane.showMessageDialog(this, "Time limit / Memory limit không hợp lệ!");
            return;
        }

        selected.setTitle(title);
        selected.setDescription(desc.isEmpty() ? "Nội dung đề trong file đính kèm." : desc);
        selected.setContestType((String) contestTypeBox.getSelectedItem());
        selected.setTimeLimit(timeLimit);
        selected.setMemoryLimit(memoryLimit);
        if (selectedAttachmentPath != null && !selectedAttachmentPath.isBlank()) {
            try {
                String savedPath = FileManager.saveProblemAttachment(selected.getId(), selectedAttachmentPath);
                selected.setImagePath(savedPath);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (problemService.updateProblem(selected)) {
            refreshProblemList();
        } else {
            JOptionPane.showMessageDialog(this, "Cập nhật thất bại!");
        }
    }

    private void deleteSelectedProblem() {
        Problem selected = problemList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Chọn đề cần xóa!");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Xóa đề '" + selected.getTitle() + "' (ID=" + selected.getId() + ")?\nSẽ xóa tất cả testcase, code mẫu, và kết quả liên quan!",
            "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            boolean ok = problemService.deleteProblem(selected.getId());
            if (ok) {
                JOptionPane.showMessageDialog(this, "Đã xóa!");
                clearForm();
                refreshProblemList();
            } else {
                JOptionPane.showMessageDialog(this, "Xóa thất bại!");
            }
        }
    }

    private void clearForm() {
        titleField.setText("");
        descArea.setText("");
        selectedAttachmentPath = null;
        attachmentLabel.setText("Chưa chọn file");
        attachmentLabel.setIcon(null);
        timeLimitField.setText("2000");
        memoryLimitField.setText("256");
        problemList.clearSelection();
    }

    private boolean isImageFile(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp");
    }

    private static class AutoGenerationResult {
        private final boolean success;
        private final int savedTestcases;
        private final String errorMessage;

        private AutoGenerationResult(boolean success, int savedTestcases, String errorMessage) {
            this.success = success;
            this.savedTestcases = savedTestcases;
            this.errorMessage = errorMessage;
        }

        boolean isSuccess() { return success; }
        int getSavedTestcases() { return savedTestcases; }
        String getErrorMessage() { return errorMessage; }

        private static AutoGenerationResult succeeded(int savedTestcases) {
            return new AutoGenerationResult(true, savedTestcases, null);
        }

        private static AutoGenerationResult failed(String errorMessage) {
            return new AutoGenerationResult(false, 0, errorMessage);
        }
    }
}
