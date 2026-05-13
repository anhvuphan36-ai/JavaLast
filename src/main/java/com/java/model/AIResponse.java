package com.java.model;

import java.util.List;

public class AIResponse {
    private String explanation;
    private List<Testcase> testcases;
    private String generatedSolution;
    private String generatedChecker;
    private String cleanDescription;
    private boolean success;
    private String errorMessage;

    public AIResponse() {}

    public String getCleanDescription() { return cleanDescription; }
    public void setCleanDescription(String cleanDescription) { this.cleanDescription = cleanDescription; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public List<Testcase> getTestcases() { return testcases; }
    public void setTestcases(List<Testcase> testcases) { this.testcases = testcases; }

    public String getGeneratedSolution() { return generatedSolution; }
    public void setGeneratedSolution(String generatedSolution) { this.generatedSolution = generatedSolution; }

    public String getGeneratedChecker() { return generatedChecker; }
    public void setGeneratedChecker(String generatedChecker) { this.generatedChecker = generatedChecker; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
