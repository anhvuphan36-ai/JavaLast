package com.java.model;

import java.sql.Timestamp;

public class Submission {
    private int id;
    private int problemId;
    private int sampleCodeId;
    private int testcaseId;
    private String actualOutput;
    private int executionTime;   // milliseconds
    private int memoryUsed;      // KB
    private String status;       // AC, WA, TLE, MLE, RE, PENDING
    private String errorMessage;
    private Timestamp submittedAt;

    public Submission() {}

    public Submission(int problemId, int sampleCodeId, int testcaseId, String actualOutput, int executionTime, int memoryUsed, String status, String errorMessage) {
        this.problemId = problemId;
        this.sampleCodeId = sampleCodeId;
        this.testcaseId = testcaseId;
        this.actualOutput = actualOutput;
        this.executionTime = executionTime;
        this.memoryUsed = memoryUsed;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProblemId() { return problemId; }
    public void setProblemId(int problemId) { this.problemId = problemId; }

    public int getSampleCodeId() { return sampleCodeId; }
    public void setSampleCodeId(int sampleCodeId) { this.sampleCodeId = sampleCodeId; }

    public int getTestcaseId() { return testcaseId; }
    public void setTestcaseId(int testcaseId) { this.testcaseId = testcaseId; }

    public String getActualOutput() { return actualOutput; }
    public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }

    public int getExecutionTime() { return executionTime; }
    public void setExecutionTime(int executionTime) { this.executionTime = executionTime; }

    public int getMemoryUsed() { return memoryUsed; }
    public void setMemoryUsed(int memoryUsed) { this.memoryUsed = memoryUsed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Timestamp getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }

    @Override
    public String toString() {
        return "Submission{id=" + id + ", status='" + status + "', time=" + executionTime + "ms}";
    }
}
