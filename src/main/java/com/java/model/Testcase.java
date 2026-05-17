package com.java.model;

public class Testcase {
    private int id;
    private int problemId;
    private String inputData;
    private String expectedOutput;
    private String testcaseType;  // small, large, edge, normal
    private boolean isAiGenerated;

    public Testcase() {}

    public Testcase(int problemId, String inputData, String expectedOutput, String testcaseType, boolean isAiGenerated) {
        this.problemId = problemId;
        this.inputData = inputData;
        this.expectedOutput = expectedOutput;
        this.testcaseType = testcaseType;
        this.isAiGenerated = isAiGenerated;
    }

    public Testcase(int id, int problemId, String inputData, String expectedOutput, String testcaseType, boolean isAiGenerated) {
        this.id = id;
        this.problemId = problemId;
        this.inputData = inputData;
        this.expectedOutput = expectedOutput;
        this.testcaseType = testcaseType;
        this.isAiGenerated = isAiGenerated;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProblemId() { return problemId; }
    public void setProblemId(int problemId) { this.problemId = problemId; }

    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }

    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

    public String getTestcaseType() { return testcaseType; }
    public void setTestcaseType(String testcaseType) { this.testcaseType = testcaseType; }

    public boolean isAiGenerated() { return isAiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { isAiGenerated = aiGenerated; }

    @Override
    public String toString() {
        return "Testcase{id=" + id + ", problemId=" + problemId + ", type='" + testcaseType + "', ai=" + isAiGenerated + "}";
    }
}
