package com.java.model;

import java.sql.Timestamp;

public class Problem {
    private int id;
    private String title;
    private String description;
    private String imagePath;
    private int timeLimit;
    private int memoryLimit;
    private String contestType;
    private String checkerScript;
    private Timestamp createdAt;

    public Problem() {}

    public Problem(String title, String description, String imagePath, int timeLimit, int memoryLimit, String contestType) {
        this.title = title;
        this.description = description;
        this.imagePath = imagePath;
        this.timeLimit = timeLimit;
        this.memoryLimit = memoryLimit;
        this.contestType = contestType;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }

    public int getMemoryLimit() { return memoryLimit; }
    public void setMemoryLimit(int memoryLimit) { this.memoryLimit = memoryLimit; }

    public String getContestType() { return contestType; }
    public void setContestType(String contestType) { this.contestType = contestType; }

    public String getCheckerScript() { return checkerScript; }
    public void setCheckerScript(String checkerScript) { this.checkerScript = checkerScript; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Problem{id=" + id + ", title='" + title + "', contestType='" + contestType + "'}";
    }
}
