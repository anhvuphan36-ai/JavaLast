package com.java.service;

import com.java.model.AIResponse;
import com.java.model.Problem;

public interface AIService {

    /**
     * Phân tích đề thi (text hoặc ảnh) và sinh testcase + checker script.
     * @param problem đề thi đã nhập (có title, description, imagePath)
     * @return AIResponse chứa testcases, checker, explanation
     */
    AIResponse analyzeProblem(Problem problem);

    /**
     * Tự động sinh code mẫu cho đề thi.
     * @param problem đề thi
     * @param language ngôn ngữ: "java", "cpp"
     * @param expectedType "AC", "WA", "TLE"
     * @return source code mẫu
     */
    String generateSolution(Problem problem, String language, String expectedType);

    /**
     * Sinh checker script tùy chỉnh nếu đề yêu cầu chấm linh hoạt.
     * @param problem đề thi
     * @return script checker (Python)
     */
    String generateChecker(Problem problem);
}
