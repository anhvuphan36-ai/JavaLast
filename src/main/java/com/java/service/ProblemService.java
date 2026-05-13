package com.java.service;

import com.java.dao.ProblemDAO;
import com.java.dao.SampleCodeDAO;
import com.java.dao.SubmissionDAO;
import com.java.dao.TestcaseDAO;
import com.java.model.*;
import com.java.util.FileManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProblemService {
    private ProblemDAO problemDAO = new ProblemDAO();
    private TestcaseDAO testcaseDAO = new TestcaseDAO();
    private SampleCodeDAO sampleCodeDAO = new SampleCodeDAO();
    private SubmissionDAO submissionDAO = new SubmissionDAO();

    public int createProblem(Problem p) {
        try {
            int generatedId = problemDAO.addProblem(p);
            if (generatedId > 0) {
                p.setId(generatedId);
                return generatedId;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<Problem> getAllProblems() {
        try {
            return problemDAO.getAllProblems();
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Problem getProblemById(int id) {
        try {
            return problemDAO.getProblemById(id);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean updateCheckerScript(int problemId, String checkerScript) {
        try {
            return problemDAO.updateCheckerScript(problemId, checkerScript);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateProblem(Problem problem) {
        try {
            return problemDAO.updateProblem(problem);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addTestcaseFull(int problemId, String inputContent, String outputContent, String type, boolean aiGenerated) {
        Testcase tc = new Testcase();
        tc.setProblemId(problemId);
        tc.setInputData(inputContent);
        tc.setExpectedOutput(outputContent);
        tc.setTestcaseType(type);
        tc.setAiGenerated(aiGenerated);

        boolean saved = testcaseDAO.addTestcase(tc);
        if (saved && tc.getId() > 0) {
            try {
                FileManager.saveTestcaseInput(problemId, tc.getId(), inputContent);
                FileManager.saveTestcaseOutput(problemId, tc.getId(), outputContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return saved;
    }

    public List<Testcase> getTestcasesByProblem(int problemId) {
        return testcaseDAO.getTestcasesByProblemId(problemId);
    }

    public int deleteAllTestcasesForProblem(int problemId) {
        return testcaseDAO.deleteAllByProblemId(problemId);
    }

    /**
     * Chạy code AC với từng testcase input, lấy actual output làm expected output mới.
     * Trả về số testcase đã cập nhật thành công.
     */
    public int recomputeExpectedOutputs(int problemId, String acCode, String language, JudgeService judgeService) {
        List<Testcase> testcases = testcaseDAO.getTestcasesByProblemId(problemId);
        if (testcases.isEmpty()) return 0;

        JudgeEngine engine = new JudgeEngine();
        int updated = 0;
        for (Testcase tc : testcases) {
            try {
                JudgeResult result = engine.judge(acCode, language, tc.getInputData(), "", 5000, 256);
                if (result.getActualOutput() != null && !result.getActualOutput().isBlank()
                        && !"RE".equals(result.getStatus()) && !"CE".equals(result.getStatus())
                        && !"TLE".equals(result.getStatus())) {
                    String newOutput = result.getActualOutput().trim();
                    boolean ok = testcaseDAO.updateExpectedOutput(tc.getId(), newOutput);
                    if (ok) {
                        try {
                            FileManager.saveTestcaseOutput(problemId, tc.getId(), newOutput);
                        } catch (IOException ignored) {}
                        updated++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return updated;
    }

    public int deleteAiTestcasesForProblem(int problemId) {
        return testcaseDAO.deleteAiGeneratedByProblemId(problemId);
    }

    public int addSampleCode(int problemId, String code, String language, String expectedType, boolean aiGenerated) {
        SampleCode sc = new SampleCode();
        sc.setProblemId(problemId);
        sc.setCodeContent(code);
        sc.setLanguage(language);
        sc.setExpectedType(expectedType);
        sc.setAiGenerated(aiGenerated);

        if (sampleCodeDAO.addSampleCode(sc)) {
            try {
                FileManager.saveSampleCode(problemId, sc.getId(), code, language);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sc.getId();
        }
        return -1;
    }

    public List<SampleCode> getSampleCodesByProblem(int problemId) {
        return sampleCodeDAO.getByProblemId(problemId);
    }

    public boolean deleteSampleCode(int id) {
        return sampleCodeDAO.deleteSampleCode(id);
    }

    public List<Submission> runJudging(int problemId, int sampleCodeId, JudgeService judgeService) {
        Problem problem = getProblemById(problemId);
        SampleCode sampleCode = sampleCodeDAO.getById(sampleCodeId);
        List<Testcase> testcases = testcaseDAO.getTestcasesByProblemId(problemId);
        List<Submission> results = new ArrayList<>();

        if (problem == null || sampleCode == null || testcases.isEmpty()) {
            return results;
        }

        String codePath;
        try {
            codePath = FileManager.saveSampleCode(problemId, sampleCodeId, sampleCode.getCodeContent(), sampleCode.getLanguage());
        } catch (IOException e) {
            e.printStackTrace();
            return results;
        }

        for (Testcase tc : testcases) {
            String inputPath;
            String expectedPath;
            try {
                inputPath = FileManager.saveTestcaseInput(problemId, tc.getId(), tc.getInputData());
                expectedPath = FileManager.saveTestcaseOutput(problemId, tc.getId(), tc.getExpectedOutput());
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            JudgeResult jr;
            if (problem.getCheckerScript() != null && !problem.getCheckerScript().isBlank()) {
                try {
                    String checkerPath = FileManager.saveCheckerScript(problemId, problem.getCheckerScript());
                    jr = judgeService.judgeWithChecker(codePath, sampleCode.getLanguage(), inputPath, checkerPath, problem.getTimeLimit());
                } catch (IOException e) {
                    jr = JudgeResult.runtimeError("Checker IO Error: " + e.getMessage());
                }
            } else {
                jr = judgeService.judge(codePath, sampleCode.getLanguage(), inputPath, expectedPath, problem.getTimeLimit());
            }

            Submission sub = new Submission();
            sub.setProblemId(problemId);
            sub.setSampleCodeId(sampleCodeId);
            sub.setTestcaseId(tc.getId());
            sub.setActualOutput(jr.getActualOutput());
            sub.setExecutionTime(jr.getExecutionTime());
            sub.setMemoryUsed(jr.getMemoryUsed());
            sub.setStatus(jr.getStatus());
            sub.setErrorMessage(jr.getErrorMessage());

            submissionDAO.addSubmission(sub);
            results.add(sub);
        }

        return results;
    }

    public List<Submission> runAdHocJudging(int problemId, String code, String language) {
        Problem problem = getProblemById(problemId);
        List<Testcase> testcases = testcaseDAO.getTestcasesByProblemId(problemId);
        List<Submission> results = new ArrayList<>();

        if (problem == null || code == null || code.isBlank() || testcases.isEmpty()) {
            return results;
        }

        JudgeEngine engine = new JudgeEngine();
        for (Testcase tc : testcases) {
            JudgeResult jr = engine.judge(
                    code,
                    language,
                    tc.getInputData(),
                    tc.getExpectedOutput(),
                    problem.getTimeLimit(),
                    problem.getMemoryLimit()
            );

            Submission sub = new Submission();
            sub.setProblemId(problemId);
            sub.setSampleCodeId(0);
            sub.setTestcaseId(tc.getId());
            sub.setActualOutput(jr.getActualOutput());
            sub.setExecutionTime(jr.getExecutionTime());
            sub.setMemoryUsed(jr.getMemoryUsed());
            sub.setStatus(jr.getStatus());
            sub.setErrorMessage(jr.getErrorMessage());
            results.add(sub);
        }

        return results;
    }

    public List<Submission> getSubmissionsByProblem(int problemId) {
        return submissionDAO.getByProblemId(problemId);
    }

    public boolean deleteProblem(int id) {
        Problem p = getProblemById(id);
        if (p == null) return false;

        // Xóa file testcase
        List<Testcase> tcs = getTestcasesByProblem(id);
        for (Testcase tc : tcs) {
            try {
                FileManager.deleteFile(FileManager.getTestcasesDir() + "/problem_" + id + "/tc_" + tc.getId() + "_input.txt");
                FileManager.deleteFile(FileManager.getTestcasesDir() + "/problem_" + id + "/tc_" + tc.getId() + "_output.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Xóa file sample code
        List<SampleCode> scs = getSampleCodesByProblem(id);
        for (SampleCode sc : scs) {
            try {
                String ext = ".txt";
                String lang = sc.getLanguage().toLowerCase();
                if ("java".equals(lang)) ext = ".java";
                else if ("cpp".equals(lang) || "c++".equals(lang)) ext = ".cpp";
                else if ("python".equals(lang) || "py".equals(lang)) ext = ".py";
                FileManager.deleteFile(FileManager.getSamplesDir() + "/problem_" + id + "/sample_" + sc.getId() + ext);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Xóa ảnh đề bài
        if (p.getImagePath() != null && !p.getImagePath().isBlank()) {
            try {
                FileManager.deleteFile(p.getImagePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Xóa checker script
        try {
            FileManager.deleteFile(FileManager.getProblemsDir() + "/problem_" + id + "/checker.py");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return problemDAO.deleteProblem(id);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
