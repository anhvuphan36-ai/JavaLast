package com.java.service;

import com.java.model.JudgeResult;
import com.java.util.FileManager;

import java.io.IOException;

public class DefaultJudgeService implements JudgeService {
    private JudgeEngine engine = new JudgeEngine();

    @Override
    public JudgeResult judge(String sourceCodePath, String language, String inputPath, String expectedOutputPath, int timeLimitMs) {
        try {
            String code = FileManager.readFile(sourceCodePath);
            String input = FileManager.readFile(inputPath);
            String expected = FileManager.readFile(expectedOutputPath);
            return engine.judge(code, language, input, expected, timeLimitMs, 256);
        } catch (IOException e) {
            return JudgeResult.runtimeError("IO Error: " + e.getMessage());
        }
    }

    @Override
    public JudgeResult judgeWithChecker(String sourceCodePath, String language, String inputPath, String expectedOutputPath, String checkerPath, int timeLimitMs) {
        try {
            String code = FileManager.readFile(sourceCodePath);
            String input = FileManager.readFile(inputPath);
            String checker = FileManager.readFile(checkerPath);

            java.io.File checkerFile = java.io.File.createTempFile("checker_", ".py");
            java.nio.file.Files.writeString(checkerFile.toPath(), checker, java.nio.charset.StandardCharsets.UTF_8);

            java.io.File inputFile = java.io.File.createTempFile("input_", ".txt");
            java.nio.file.Files.writeString(inputFile.toPath(), input, java.nio.charset.StandardCharsets.UTF_8);

            JudgeResult runResult = engine.judge(code, language, input, "", timeLimitMs, 256);

            if (!"RE".equals(runResult.getStatus()) && !"CE".equals(runResult.getStatus()) && !"TLE".equals(runResult.getStatus())) {
                java.io.File actualFile = java.io.File.createTempFile("actual_", ".txt");
                java.nio.file.Files.writeString(actualFile.toPath(), runResult.getActualOutput() != null ? runResult.getActualOutput() : "", java.nio.charset.StandardCharsets.UTF_8);

                java.io.File expectedFile = java.io.File.createTempFile("expected_", ".txt");
                String expected = FileManager.readFile(expectedOutputPath);
                java.nio.file.Files.writeString(expectedFile.toPath(), expected != null ? expected : "", java.nio.charset.StandardCharsets.UTF_8);

                String pythonCmd = findPythonCommand();
                if (pythonCmd == null) {
                    return JudgeResult.runtimeError("Cannot find Python interpreter. Install Python and add to PATH.");
                }

                ProcessBuilder pb = new ProcessBuilder(pythonCmd, checkerFile.getAbsolutePath(),
                        inputFile.getAbsolutePath(), expectedFile.getAbsolutePath(), actualFile.getAbsolutePath());
                pb.redirectErrorStream(true);
                Process proc = pb.start();

                boolean finished = proc.waitFor(timeLimitMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (!finished) {
                    proc.destroyForcibly();
                    runResult.setStatus("WA");
                    runResult.setErrorMessage("Checker timed out after " + timeLimitMs + "ms");
                    return runResult;
                }

                String checkerOutput = new String(proc.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                String trimmed = checkerOutput.trim();
                if (trimmed.startsWith("AC") || trimmed.startsWith("OK")) {
                    runResult.setStatus("AC");
                } else if (trimmed.startsWith("WA")) {
                    runResult.setStatus("WA");
                } else if (trimmed.startsWith("PARTIAL")) {
                    runResult.setStatus("WA");
                } else {
                    runResult.setStatus("WA");
                    runResult.setErrorMessage("Checker output unrecognized: " + trimmed);
                }
            }

            return runResult;
        } catch (IOException e) {
            return JudgeResult.runtimeError("IO Error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return JudgeResult.runtimeError("Checker interrupted: " + e.getMessage());
        }
    }

    private String findPythonCommand() {
        String[] candidates = System.getProperty("os.name").toLowerCase().startsWith("windows")
                ? new String[]{"python", "py", "python3"}
                : new String[]{"python3", "python"};
        for (String cmd : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String out = new String(p.getInputStream().readAllBytes()).toLowerCase();
                boolean exited = p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                if (!exited) { p.destroyForcibly(); continue; }
                if (p.exitValue() == 0 && out.contains("python")) {
                    return cmd;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
