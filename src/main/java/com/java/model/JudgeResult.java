package com.java.model;

public class JudgeResult {
    private String status;         // AC, WA, TLE, MLE, RE, CE
    private String actualOutput;
    private int executionTime;     // ms
    private int memoryUsed;        // KB
    private String errorMessage;

    public JudgeResult() {}

    public JudgeResult(String status, String actualOutput, int executionTime, int memoryUsed, String errorMessage) {
        this.status = status;
        this.actualOutput = actualOutput;
        this.executionTime = executionTime;
        this.memoryUsed = memoryUsed;
        this.errorMessage = errorMessage;
    }

    // Static factory methods
    public static JudgeResult accepted(String output, int time, int memory) {
        return new JudgeResult("AC", output, time, memory, null);
    }

    public static JudgeResult wrongAnswer(String output, int time, int memory) {
        return new JudgeResult("WA", output, time, memory, null);
    }

    public static JudgeResult timeLimitExceeded(int time) {
        return new JudgeResult("TLE", null, time, 0, "Time Limit Exceeded");
    }

    public static JudgeResult memoryLimitExceeded(int memory) {
        return new JudgeResult("MLE", null, 0, memory, "Memory Limit Exceeded");
    }

    public static JudgeResult runtimeError(String error) {
        return new JudgeResult("RE", null, 0, 0, error);
    }

    public static JudgeResult compileError(String error) {
        return new JudgeResult("CE", null, 0, 0, error);
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getActualOutput() { return actualOutput; }
    public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }

    public int getExecutionTime() { return executionTime; }
    public void setExecutionTime(int executionTime) { this.executionTime = executionTime; }

    public int getMemoryUsed() { return memoryUsed; }
    public void setMemoryUsed(int memoryUsed) { this.memoryUsed = memoryUsed; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public String toString() {
        return "JudgeResult{status='" + status + "', time=" + executionTime + "ms, memory=" + memoryUsed + "KB}";
    }
}
