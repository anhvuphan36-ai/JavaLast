package com.java.service;

import com.java.model.JudgeResult;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class JudgeEngine {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "JudgeSystem";
    private static final int DEFAULT_TIME_LIMIT = 2000;
    private static final int DEFAULT_MEMORY_LIMIT = 256;

    public JudgeEngine() {
        new File(TEMP_DIR).mkdirs();
    }

    public JudgeResult judge(String code, String language, String input, String expectedOutput, int timeLimit, int memoryLimit) {
        if (timeLimit <= 0) timeLimit = DEFAULT_TIME_LIMIT;
        if (memoryLimit <= 0) memoryLimit = DEFAULT_MEMORY_LIMIT;

        String runId = "judge_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(10000);
        Path workDir = Paths.get(TEMP_DIR, runId);

        try {
            Files.createDirectories(workDir);

            Path sourceFile = writeSourceFile(workDir, code, language);

            CompileResult compileResult = compile(sourceFile, language, workDir);
            if (!compileResult.success) {
                return JudgeResult.compileError(compileResult.output);
            }

            RunResult runResult = run(workDir, language, input, timeLimit, memoryLimit, runId);

            if (runResult.timedOut) {
                return JudgeResult.timeLimitExceeded(timeLimit);
            }

            if (runResult.exitCode != 0) {
                return JudgeResult.runtimeError(runResult.stderr);
            }

            String actualOutput = runResult.stdout;
            if (compareOutput(actualOutput, expectedOutput)) {
                return JudgeResult.accepted(actualOutput, runResult.executionTimeMs, runResult.memoryUsedKB);
            } else {
                JudgeResult result = JudgeResult.wrongAnswer(actualOutput, runResult.executionTimeMs, runResult.memoryUsedKB);
                result.setErrorMessage(buildWrongAnswerMessage(actualOutput, expectedOutput));
                return result;
            }

        } catch (Exception e) {
            return JudgeResult.runtimeError("Judge system error: " + e.getMessage());
        } finally {
            try {
                deleteRecursively(workDir);
            } catch (Exception ignored) {}
        }
    }

    private Path writeSourceFile(Path workDir, String code, String language) throws IOException {
        String fileName;
        String processedCode = code;
        switch (language.toLowerCase()) {
            case "java":
                String className = extractJavaClassName(code);
                fileName = className + ".java";
                break;
            case "cpp":
            case "c++":
                fileName = "solution.cpp";
                processedCode = sanitizeCppCode(code);
                break;
            case "python":
            case "py":
                fileName = "solution.py";
                break;
            default:
                fileName = "solution.txt";
        }
        Path sourceFile = workDir.resolve(fileName);
        Files.writeString(sourceFile, processedCode, java.nio.charset.StandardCharsets.UTF_8);
        return sourceFile;
    }

    private String sanitizeCppCode(String code) {
        String s = code;
        // Bỏ thư viện conio.h cổ điển không tương thích với gcc hiện đại
        s = s.replaceAll("(?i)#include\\s*<conio\\.h>", "// #include <conio.h> (Removed by AI Judge)");
        s = s.replaceAll("(?i)#include\\s*\"conio\\.h\"", "// #include \"conio.h\" (Removed by AI Judge)");
        
        // C++ chuẩn bắt buộc int main, không dùng void main
        s = s.replaceAll("(?i)void\\s+main\\s*\\(", "int main(");
        
        // Các hàm của Turbo C gây lỗi biên dịch
        s = s.replaceAll("(?i)clrscr\\s*\\(\\)\\s*;", "// clrscr();");
        s = s.replaceAll("(?i)getch\\s*\\(\\)\\s*;", "// getch();");
        
        return s;
    }

    private String extractJavaClassName(String code) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("public\\s+class\\s+(\\w+)");
        java.util.regex.Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Main";
    }

    private CompileResult compile(Path sourceFile, String language, Path workDir) throws IOException, InterruptedException {
        switch (language.toLowerCase()) {
            case "java":
                return compileJava(sourceFile, workDir);
            case "cpp":
            case "c++":
                return compileCpp(sourceFile, workDir);
            case "python":
            case "py":
                CompileResult result = new CompileResult();
                result.success = true;
                result.output = "";
                return result;
            default:
                CompileResult err = new CompileResult();
                err.success = false;
                err.output = "Ngôn ngữ không được hỗ trợ: " + language;
                return err;
        }
    }

    private CompileResult compileJava(Path sourceFile, Path workDir) throws IOException, InterruptedException {
        try {
            ProcessBuilder pb = new ProcessBuilder("javac", "-encoding", "UTF-8", sourceFile.getFileName().toString());
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = readStream(process.getInputStream());
            boolean exited = process.waitFor(30, TimeUnit.SECONDS);
            if (!exited) {
                process.destroyForcibly();
                CompileResult result = new CompileResult();
                result.success = false;
                result.output = "Compile timeout (30s)";
                return result;
            }

            CompileResult result = new CompileResult();
            result.success = (process.exitValue() == 0);
            result.output = output;
            return result;
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Cannot run program")) {
                CompileResult result = new CompileResult();
                result.success = false;
                result.output = "Lỗi: Không tìm thấy 'javac'. Vui lòng cài đặt Java JDK và thêm vào biến môi trường PATH.";
                return result;
            }
            throw e;
        }
    }

    private CompileResult compileCpp(Path sourceFile, Path workDir) throws IOException, InterruptedException {
        try {
            ProcessBuilder pb = new ProcessBuilder("g++", "-O2", "-std=c++17", "-o", "solution", sourceFile.getFileName().toString());
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = readStream(process.getInputStream());
            boolean exited = process.waitFor(30, TimeUnit.SECONDS);
            if (!exited) {
                process.destroyForcibly();
                CompileResult result = new CompileResult();
                result.success = false;
                result.output = "Compile timeout (30s)";
                return result;
            }

            CompileResult result = new CompileResult();
            result.success = (process.exitValue() == 0);
            result.output = output;
            return result;
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Cannot run program")) {
                CompileResult result = new CompileResult();
                result.success = false;
                result.output = "Lỗi: Không tìm thấy 'g++'. Vui lòng cài đặt MinGW (g++) trên Windows và thêm vào biến môi trường PATH.";
                return result;
            }
            throw e;
        }
    }

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");

    private RunResult run(Path workDir, String language, String input, int timeLimitMs, int memoryLimitMB, String runId) throws IOException, InterruptedException {
        ProcessBuilder pb;
        switch (language.toLowerCase()) {
            case "java":
                String className = findJavaClassName(workDir);
                pb = new ProcessBuilder("java", "-cp", ".", className);
                break;
            case "cpp":
            case "c++":
                pb = IS_WINDOWS
                    ? new ProcessBuilder(workDir.resolve("solution.exe").toString())
                    : new ProcessBuilder("./solution");
                break;
            case "python":
            case "py":
                String pythonCommand = findPythonCommand();
                if (pythonCommand == null) {
                    RunResult err = new RunResult();
                    err.exitCode = -1;
                    err.stderr = "Không tìm thấy Python thật. Trên Windows, 'python' hiện đang là Microsoft Store alias. "
                            + "Hãy cài Python từ python.org và tick 'Add python.exe to PATH', hoặc tắt App execution alias cho python.exe.";
                    return err;
                }
                pb = new ProcessBuilder(pythonCommand, "solution.py");
                break;
            default:
                RunResult err = new RunResult();
                err.exitCode = -1;
                err.stderr = "Ngôn ngữ không được hỗ trợ";
                return err;
        }
        pb.directory(workDir.toFile());

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Cannot run program \"python")) {
                RunResult err = new RunResult();
                err.exitCode = -1;
                err.stderr = "Lỗi: Không tìm thấy 'python'. Vui lòng cài đặt Python và thêm vào biến môi trường PATH.";
                return err;
            }
            throw e;
        }

        try (OutputStream os = process.getOutputStream()) {
            os.write(input.getBytes("UTF-8"));
            os.flush();
        }

        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();

        final int MAX_OUTPUT = 10 * 1024 * 1024; // 10MB

        Thread stdoutThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (stdoutBuilder.length() + line.length() + 1 > MAX_OUTPUT) break;
                    stdoutBuilder.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });

        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (stderrBuilder.length() + line.length() + 1 > MAX_OUTPUT) break;
                    stderrBuilder.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });

        stdoutThread.start();
        stderrThread.start();

        long startTime = System.currentTimeMillis();
        boolean exited = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
        long executionTime = System.currentTimeMillis() - startTime;

        RunResult result = new RunResult();

        if (!exited) {
            process.destroyForcibly();
            result.timedOut = true;
            result.executionTimeMs = (int) executionTime;
            return result;
        }

        stdoutThread.join(2000);
        stderrThread.join(2000);

        result.exitCode = process.exitValue();
        result.stdout = stdoutBuilder.toString();
        result.stderr = stderrBuilder.toString();
        result.executionTimeMs = (int) executionTime;
        result.memoryUsedKB = estimateMemoryUsage(process, memoryLimitMB);

        if (result.memoryUsedKB > memoryLimitMB * 1024) {
            result.stderr = "Memory Limit Exceeded (" + result.memoryUsedKB + "KB > " + (memoryLimitMB * 1024) + "KB)";
        }

        return result;
    }

    private String findJavaClassName(Path workDir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(workDir, "*.class")) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (!name.contains("$")) {
                    return name.replace(".class", "");
                }
            }
        } catch (IOException ignored) {}
        return "Main";
    }

    private String findPythonCommand() {
        String[] candidates = IS_WINDOWS ? new String[]{"python", "py", "python3"} : new String[]{"python3", "python"};
        for (String candidate : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(candidate, "--version");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = readStream(process.getInputStream()).toLowerCase();
                boolean exited = process.waitFor(5, TimeUnit.SECONDS);
                if (!exited) {
                    process.destroyForcibly();
                    continue;
                }
                if (process.exitValue() == 0 && output.contains("python")) {
                    return candidate;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean compareOutput(String actual, String expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;

        String normalizedActual = normalizeOutput(actual);
        String normalizedExpected = normalizeOutput(expected);

        if (normalizedActual.equals(normalizedExpected)) {
            return true;
        }

        return compareTokens(normalizedActual, normalizedExpected)
                || compareLabelValues(normalizedActual, normalizedExpected)
                || compareNumericMultiset(normalizedActual, normalizedExpected);
    }

    private String normalizeOutput(String output) {
        if (output == null) return "";
        String[] lines = output.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = stripTrailing(lines[i]);
            if (sb.length() > 0) sb.append("\n");
            sb.append(line);
        }
        return sb.toString().trim();
    }

    private String normalizeTokens(String output) {
        if (output == null) return "";
        return output.trim().replaceAll("\\s+", " ");
    }

    private boolean compareTokens(String actual, String expected) {
        String normalizedActual = normalizeTokens(actual);
        String normalizedExpected = normalizeTokens(expected);
        if (normalizedActual.equals(normalizedExpected)) {
            return true;
        }
        if (normalizedActual.isEmpty() || normalizedExpected.isEmpty()) {
            return normalizedActual.equals(normalizedExpected);
        }

        String[] actualTokens = normalizedActual.split(" ");
        String[] expectedTokens = normalizedExpected.split(" ");
        if (actualTokens.length != expectedTokens.length) {
            return false;
        }

        for (int i = 0; i < actualTokens.length; i++) {
            if (actualTokens[i].equals(expectedTokens[i])) {
                continue;
            }
            if (!numericEquals(actualTokens[i], expectedTokens[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean numericEquals(String actual, String expected) {
        try {
            double a = Double.parseDouble(normalizeNumber(actual));
            double e = Double.parseDouble(normalizeNumber(expected));
            double diff = Math.abs(a - e);
            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(e)));
            return diff <= 1e-6 * scale;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean compareNumericMultiset(String actual, String expected) {
        java.util.List<Double> actualNumbers = extractNumbers(actual);
        java.util.List<Double> expectedNumbers = extractNumbers(expected);
        if (actualNumbers.isEmpty() || expectedNumbers.isEmpty()) {
            return false;
        }
        if (actualNumbers.size() != expectedNumbers.size()) {
            return false;
        }

        actualNumbers.sort(Double::compareTo);
        expectedNumbers.sort(Double::compareTo);
        for (int i = 0; i < actualNumbers.size(); i++) {
            if (!numericEquals(String.valueOf(actualNumbers.get(i)), String.valueOf(expectedNumbers.get(i)))) {
                return false;
            }
        }
        return true;
    }

    private boolean compareLabelValues(String actual, String expected) {
        java.util.List<Double> expectedValues = extractValuesAfterColon(expected);
        if (expectedValues.isEmpty()) {
            return false;
        }

        java.util.List<Double> actualNumbers = extractNumbers(actual);
        if (actualNumbers.size() < expectedValues.size()) {
            return false;
        }

        java.util.List<Double> actualValues;
        if (actualNumbers.size() == expectedValues.size()) {
            actualValues = actualNumbers;
        } else {
            actualValues = actualNumbers.subList(actualNumbers.size() - expectedValues.size(), actualNumbers.size());
        }

        for (int i = 0; i < expectedValues.size(); i++) {
            if (!numericEquals(String.valueOf(actualValues.get(i)), String.valueOf(expectedValues.get(i)))) {
                return false;
            }
        }
        return true;
    }

    private java.util.List<Double> extractValuesAfterColon(String output) {
        java.util.List<Double> values = new java.util.ArrayList<>();
        if (output == null) {
            return values;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile(":\\s*([-+]?\\d+(?:[\\.,]\\d+)?(?:[eE][-+]?\\d+)?)")
                .matcher(output);
        while (matcher.find()) {
            try {
                values.add(Double.parseDouble(normalizeNumber(matcher.group(1))));
            } catch (NumberFormatException ignored) {}
        }
        return values;
    }

    private java.util.List<Double> extractNumbers(String output) {
        java.util.List<Double> numbers = new java.util.ArrayList<>();
        if (output == null) {
            return numbers;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?<![A-Za-z_])[-+]?\\d+(?:[\\.,]\\d+)?(?:[eE][-+]?\\d+)?(?![A-Za-z_])")
                .matcher(output);
        while (matcher.find()) {
            try {
                numbers.add(Double.parseDouble(normalizeNumber(matcher.group())));
            } catch (NumberFormatException ignored) {}
        }
        return numbers;
    }

    private String normalizeNumber(String value) {
        return value == null ? "" : value.replace(',', '.');
    }

    private String buildWrongAnswerMessage(String actual, String expected) {
        return "Expected: " + preview(expected) + " | Actual: " + preview(actual);
    }

    private String preview(String output) {
        if (output == null) return "";
        String oneLine = output.replace("\r\n", "\n").replace('\r', '\n').trim().replace("\n", "\\n");
        return oneLine.length() > 160 ? oneLine.substring(0, 160) + "..." : oneLine;
    }

    private String readStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    private String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) <= ' ') {
            end--;
        }
        return s.substring(0, end);
    }

    private int estimateMemoryUsage(Process process, int memoryLimitMB) {
        try {
            if (process.isAlive()) {
                process.destroyForcibly();
                return memoryLimitMB * 1024 + 1;
            }
            ProcessHandle ph = process.toHandle();
            // ProcessHandle.info() không có RSS trên Windows, fallback: dùng peakWorkingSet từ OS
            if (IS_WINDOWS) {
                // Windows: không có API đơn giản, return 0 để tránh false MLE
                return 0;
            }
            // Linux/Mac: đọc /proc/{pid}/statm
            try {
                Path statm = Paths.get("/proc", String.valueOf(ph.pid()), "statm");
                if (Files.exists(statm)) {
                    String content = Files.readString(statm, java.nio.charset.StandardCharsets.UTF_8);
                    String[] parts = content.trim().split("\\s+");
                    long pages = Long.parseLong(parts[1]); // resident pages
                    return (int) (pages * 4); // assume 4KB page
                }
            } catch (Exception ignored) {}
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                      .map(Path::toFile)
                      .forEach(File::delete);
            }
        }
    }

    private static class CompileResult {
        boolean success;
        String output;
    }

    private static class RunResult {
        boolean timedOut = false;
        int exitCode = 0;
        String stdout = "";
        String stderr = "";
        int executionTimeMs = 0;
        int memoryUsedKB = 0;
    }
}
