package com.java.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class FileManager {
    private static String ROOT_DIR;
    private static String PROBLEMS_DIR;
    private static String TESTCASES_DIR;
    private static String SUBMISSIONS_DIR;
    private static String SAMPLES_DIR;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        Properties props = new Properties();
        try (InputStream input = FileManager.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            System.err.println("Loi doc config.properties: " + e.getMessage());
        }
        ROOT_DIR = props.getProperty("file.root.dir", "./JudgeSystemData");
        PROBLEMS_DIR = ROOT_DIR + "/Problems";
        TESTCASES_DIR = ROOT_DIR + "/Testcases";
        SUBMISSIONS_DIR = ROOT_DIR + "/Submissions";
        SAMPLES_DIR = ROOT_DIR + "/Samples";
    }

    public static void initFolders() {
        createDirIfNotExists(ROOT_DIR);
        createDirIfNotExists(PROBLEMS_DIR);
        createDirIfNotExists(TESTCASES_DIR);
        createDirIfNotExists(SUBMISSIONS_DIR);
        createDirIfNotExists(SAMPLES_DIR);
    }

    private static void createDirIfNotExists(String dirPath) {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                System.out.println("Da tao thu muc: " + dirPath);
            } catch (IOException e) {
                System.err.println("Khong tao duoc thu muc " + dirPath + ": " + e.getMessage());
            }
        }
    }

    // === Problem Attachment ===
    public static String saveProblemImage(int problemId, String sourceImagePath) throws IOException {
        return saveProblemAttachment(problemId, sourceImagePath);
    }

    public static String saveProblemAttachment(int problemId, String sourcePath) throws IOException {
        String problemDir = PROBLEMS_DIR + "/problem_" + problemId;
        createDirIfNotExists(problemDir);

        String fileName = Paths.get(sourcePath).getFileName().toString();
        String destPath = problemDir + "/" + fileName;
        Files.copy(Paths.get(sourcePath), Paths.get(destPath),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return destPath;
    }

    // === Testcase Files ===
    public static String saveTestcaseInput(int problemId, int testcaseId, String content) throws IOException {
        String tcDir = TESTCASES_DIR + "/problem_" + problemId;
        createDirIfNotExists(tcDir);
        String filePath = tcDir + "/tc_" + testcaseId + "_input.txt";
        saveToFile(filePath, content);
        return filePath;
    }

    public static String saveTestcaseOutput(int problemId, int testcaseId, String content) throws IOException {
        String tcDir = TESTCASES_DIR + "/problem_" + problemId;
        createDirIfNotExists(tcDir);
        String filePath = tcDir + "/tc_" + testcaseId + "_output.txt";
        saveToFile(filePath, content);
        return filePath;
    }

    // === Sample Code Files ===
    public static String saveSampleCode(int problemId, int codeId, String code, String language) throws IOException {
        String sampleDir = SAMPLES_DIR + "/problem_" + problemId;
        createDirIfNotExists(sampleDir);
        String ext = getFileExtension(language);
        String filePath = sampleDir + "/sample_" + codeId + ext;
        saveToFile(filePath, code);
        return filePath;
    }

    // === Checker Script ===
    public static String saveCheckerScript(int problemId, String checkerContent) throws IOException {
        String problemDir = PROBLEMS_DIR + "/problem_" + problemId;
        createDirIfNotExists(problemDir);
        String filePath = problemDir + "/checker.py";
        saveToFile(filePath, checkerContent);
        return filePath;
    }

    // === Submission Output ===
    public static String saveSubmissionOutput(int submissionId, String output) throws IOException {
        String subDir = SUBMISSIONS_DIR;
        createDirIfNotExists(subDir);
        String filePath = subDir + "/submission_" + submissionId + "_output.txt";
        saveToFile(filePath, output);
        return filePath;
    }

    // === Generic file operations ===
    public static void saveToFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    public static String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
    }

    public static void deleteFile(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
    }

    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    // === Helpers ===
    private static String getFileExtension(String language) {
        switch (language.toLowerCase()) {
            case "java": return ".java";
            case "cpp":
            case "c++": return ".cpp";
            case "python":
            case "py": return ".py";
            default: return ".txt";
        }
    }

    public static String getRootDir() { return ROOT_DIR; }
    public static String getProblemsDir() { return PROBLEMS_DIR; }
    public static String getTestcasesDir() { return TESTCASES_DIR; }
    public static String getSubmissionsDir() { return SUBMISSIONS_DIR; }
    public static String getSamplesDir() { return SAMPLES_DIR; }
}
