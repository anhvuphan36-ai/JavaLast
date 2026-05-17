package com.java.ui;

import com.java.model.Problem;
import com.java.service.ProblemService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DashboardPanel extends JPanel {
    private MainFrame mainFrame;
    private ProblemService problemService;
    private JLabel statsLabel;
    private JLabel problemCountLabel;
    private JLabel codeCountLabel;
    private SwingWorker<Void, Void> statsWorker;

    public DashboardPanel(MainFrame mainFrame, ProblemService problemService) {
        this.mainFrame = mainFrame;
        this.problemService = problemService;
        setLayout(new BorderLayout(0, 24));
        setBackground(AppTheme.BG_DARK);
        setBorder(AppTheme.BORDER_EMPTY_LG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.BG_DARK);
        header.setOpaque(false);
        JLabel welcome = AppTheme.createHeadingLabel("Dashboard");
        welcome.setFont(AppTheme.FONT_TITLE);
        statsLabel = AppTheme.createBodyLabel("Đang tải thống kê...");
        header.add(welcome, BorderLayout.WEST);
        header.add(statsLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel statsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        statsRow.setBackground(AppTheme.BG_DARK);
        statsRow.setOpaque(false);
        statsRow.add(createStatCard("Đề thi", "0", AppTheme.ACCENT_CYAN));
        statsRow.add(createStatCard("Code mẫu", "0", AppTheme.ACCENT_GREEN));
        add(statsRow, BorderLayout.CENTER);

        JPanel grid = new JPanel(new GridLayout(3, 3, 20, 20));
        grid.setBackground(AppTheme.BG_DARK);
        grid.setOpaque(false);

        grid.add(createCard("Nhập đề thi", "Tạo bài toán mới với text hoặc ảnh", AppTheme.ACCENT_CYAN, "PROBLEM_ENTRY"));
        grid.add(createCard("AI Phân tích", "AI sinh testcase + checker tự động", AppTheme.ACCENT_PURPLE, "AI_PANEL"));
        grid.add(createCard("Nộp code", "Nhập code và chấm bài", AppTheme.ACCENT_GREEN, "CODE_SUBMIT"));
        grid.add(createCard("Kết quả chấm", "Xem trạng thái chấm bài", AppTheme.ACCENT_YELLOW, "RESULT"));
        grid.add(createCard("Hướng dẫn", "Xem tài liệu sử dụng", AppTheme.TEXT_SECONDARY, "DOCS"));
        grid.add(createCard("Thoát", "Đóng ứng dụng", AppTheme.ACCENT_RED, "EXIT"));

        add(grid, BorderLayout.SOUTH);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refreshStats();
    }

    public void refreshStats() {
        if (statsWorker != null && !statsWorker.isDone()) {
            statsWorker.cancel(true);
        }
        statsWorker = new SwingWorker<>() {
            private int problemCount = 0;
            private int codeCount = 0;

            @Override
            protected Void doInBackground() {
                try {
                    List<Problem> problems = problemService.getAllProblems();
                    problemCount = problems.size();
                    for (Problem p : problems) {
                        codeCount += problemService.getSampleCodesByProblem(p.getId()).size();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                statsLabel.setText(String.format("Tổng: %d đề bài trong hệ thống", problemCount));
                updateStatCardValue(0, String.valueOf(problemCount));
                updateStatCardValue(1, String.valueOf(codeCount));
            }
        };
        statsWorker.execute();
    }

    private void updateStatCardValue(int index, String value) {
        if (index == 0 && problemCountLabel != null) {
            problemCountLabel.setText(value);
        } else if (index == 1 && codeCountLabel != null) {
            codeCountLabel.setText(value);
        }
    }

    private JPanel createStatCard(String label, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(AppTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
                AppTheme.BORDER_EMPTY_MD
        ));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(AppTheme.FONT_TITLE);
        valueLabel.setForeground(accent);

        if ("Đề thi".equals(label)) problemCountLabel = valueLabel;
        if ("Code mẫu".equals(label)) codeCountLabel = valueLabel;

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.FONT_SMALL);
        lbl.setForeground(AppTheme.TEXT_SECONDARY);

        card.add(valueLabel, BorderLayout.CENTER);
        card.add(lbl, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createCard(String title, String desc, Color accent, String action) {
        JPanel card = new JPanel(new BorderLayout(12, 8));
        card.setBackground(AppTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                AppTheme.BORDER_EMPTY_MD
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(AppTheme.FONT_SUBHEAD);
        lblTitle.setForeground(accent);

        JLabel lblDesc = new JLabel(desc);
        lblDesc.setFont(AppTheme.FONT_SMALL);
        lblDesc.setForeground(AppTheme.TEXT_SECONDARY);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        textPanel.setBackground(AppTheme.BG_CARD);
        textPanel.setOpaque(false);
        textPanel.add(lblTitle);
        textPanel.add(lblDesc);

        card.add(textPanel, BorderLayout.CENTER);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                card.setBackground(AppTheme.BG_CARD.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                card.setBackground(AppTheme.BG_CARD);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if ("EXIT".equals(action)) {
                    com.java.util.DatabaseConnection.closePool();
                    System.exit(0);
                }
                else if ("DOCS".equals(action)) {
                    String docs =
                        "============================================\n" +
                        "  HƯỚNG DẪN CÀI ĐẶT\n" +
                        "============================================\n" +
                        "Yêu cầu hệ thống:\n" +
                        "  - Java JDK 17+ (https://adoptium.net)\n" +
                        "  - MySQL 8.0+ hoặc Docker\n" +
                        "  - g++ (cho C++): MinGW-w64 trên Windows\n\n" +
                        "Cấu hình database:\n" +
                        "  Chạy Docker: docker run -d -p 3306:3306\n" +
                        "    -e MYSQL_ROOT_PASSWORD=root\n" +
                        "    -e MYSQL_DATABASE=JudgeSystem mysql:8.0\n\n" +
                        "Cấu hình AI (file config.properties):\n" +
                        "  ai.api.key=<Google Gemini API Key>\n" +
                        "  ai.model=gemini-2.5-flash\n" +
                        "  Lấy key tại: aistudio.google.com/apikey\n\n" +
                        "Chạy chương trình:\n" +
                        "  java -jar JudgeSystem-1.0-SNAPSHOT.jar\n\n" +
                        "============================================\n" +
                        "  HƯỚNG DẪN SỬ DỤNG\n" +
                        "============================================\n" +
                        "Bước 1 - Nhập đề thi:\n" +
                        "  Vào 'Nhập đề thi' -> Điền tiêu đề, nội dung\n" +
                        "  -> Chọn ảnh (tùy chọn) -> Lưu vào CSDL\n\n" +
                        "Bước 2 - AI Phân tích & Sinh testcase:\n" +
                        "  Vào 'AI Phân tích' -> Chọn đề -> 'Phân tích'\n" +
                        "  -> AI sinh code AC + 5 inputs đa dạng\n" +
                        "  -> Hệ thống tự chạy code AC để tính output\n" +
                        "  -> Testcase luôn đúng 100%\n\n" +
                        "Bước 3 - Kiểm tra testcase:\n" +
                        "  Vào 'Nộp code mẫu' -> Chọn đề và loại code:\n" +
                        "  - [AC] -> Chấm -> phải All AC\n" +
                        "    (xác nhận testcase đúng)\n" +
                        "  - [WA] -> Chấm -> phải có WA\n" +
                        "    (xác nhận testcase đủ mạnh)\n" +
                        "  - [TLE] -> Chấm -> phải có TLE\n" +
                        "    (xác nhận testcase đủ lớn)\n" +
                        "  -> 'Lưu code mẫu' để lưu vào CSDL\n\n" +
                        "Bước 4 - Xem kết quả:\n" +
                        "  Vào 'Kết quả chấm' -> xem lịch sử submissions";
                    JTextArea ta = new JTextArea(docs);
                    ta.setEditable(false);
                    ta.setFont(new Font("Consolas", Font.PLAIN, 13));
                    ta.setBackground(AppTheme.BG_CARD);
                    ta.setForeground(AppTheme.TEXT_PRIMARY);
                    JScrollPane sp = new JScrollPane(ta);
                    sp.setPreferredSize(new Dimension(520, 480));
                    JOptionPane.showMessageDialog(card, sp, "Hướng dẫn cài đặt & sử dụng", JOptionPane.INFORMATION_MESSAGE);
                }
                else mainFrame.showPanel(action);
            }
        });
        return card;
    }
}
