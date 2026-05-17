package com.java.model;

import java.sql.Timestamp;

public class SampleCode {
    private int id;
    private int problemId;
    private String codeContent;
    private String language;       // java, cpp, python
    private String expectedType;   // AC, WA, TLE, MLE, RE
    private boolean isAiGenerated;
    private Timestamp createdAt;

    public SampleCode() {}

    public SampleCode(int problemId, String codeContent, String language, String expectedType, boolean isAiGenerated) {
        this.problemId = problemId;
        this.codeContent = codeContent;
        this.language = language;
        this.expectedType = expectedType;
        this.isAiGenerated = isAiGenerated;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProblemId() { return problemId; }
    public void setProblemId(int problemId) { this.problemId = problemId; }

    public String getCodeContent() { return codeContent; }
    public void setCodeContent(String codeContent) { this.codeContent = codeContent; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getExpectedType() { return expectedType; }
    public void setExpectedType(String expectedType) { this.expectedType = expectedType; }

    public boolean isAiGenerated() { return isAiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { isAiGenerated = aiGenerated; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "SampleCode{id=" + id + ", problemId=" + problemId + ", lang='" + language + "', expected='" + expectedType + "'}";
    }
}
