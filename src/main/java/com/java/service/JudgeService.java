package com.java.service;

import com.java.model.JudgeResult;

public interface JudgeService {

    /**
     * Compile và chạy code với 1 testcase, trả về kết quả chấm.
     * @param sourceCodePath đường dẫn file code nguồn
     * @param language ngôn ngữ: "java", "cpp"
     * @param inputPath đường dẫn file input
     * @param expectedOutputPath đường dẫn file expected output
     * @param timeLimitMs giới hạn thời gian (ms)
     * @return JudgeResult chứa status (AC/WA/TLE/MLE/RE/CE)
     */
    JudgeResult judge(String sourceCodePath, String language, String inputPath, String expectedOutputPath, int timeLimitMs);

    /**
     * Chấm với custom checker script.
     * @param sourceCodePath đường dẫn file code nguồn
     * @param language ngôn ngữ
     * @param inputPath đường dẫn file input
     * @param expectedOutputPath đường dẫn file expected output
     * @param checkerPath đường dẫn script checker
     * @param timeLimitMs giới hạn thời gian
     * @return JudgeResult
     */
    JudgeResult judgeWithChecker(String sourceCodePath, String language, String inputPath, String expectedOutputPath, String checkerPath, int timeLimitMs);
}
