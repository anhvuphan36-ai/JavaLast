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
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProblemService {
    private static final Logger logger = Logger.getLogger(ProblemService.class.getName());

    private ProblemDAO problemDAO = new ProblemDAO();
    private TestcaseDAO testcaseDAO = new TestcaseDAO();
    private SampleCodeDAO sampleCodeDAO = new SampleCodeDAO();
    private SubmissionDAO submissionDAO = new SubmissionDAO();

    public int createProblem(Problem p) {
        if (p == null) return -1;
        if (p.getTitle() == null || p.getTitle().trim().isEmpty()) return -1;
        if (p.getTitle().length() > 255) return -1;
        try {
            int generatedId = problemDAO.addProblem(p);
            if (generatedId > 0) {
                p.setId(generatedId);
                return generatedId;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to create problem: " + p.getTitle(), e);
        }
        return -1;
    }

    public List<Problem> getAllProblems() {
        try {
            return problemDAO.getAllProblems();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get all problems", e);
            return new ArrayList<>();
        }
    }

    public Problem getProblemById(int id) {
        try {
            return problemDAO.getProblemById(id);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get problem by id: " + id, e);
            return null;
        }
    }

    public boolean updateCheckerScript(int problemId, String checkerScript) {
        try {
            return problemDAO.updateCheckerScript(problemId, checkerScript);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update checker script for problem: " + problemId, e);
            return false;
        }
    }

    public boolean updateProblem(Problem problem) {
        try {
            return problemDAO.updateProblem(problem);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update problem: " + problem.getId(), e);
            return false;
        }
    }

    public boolean addTestcaseFull(int problemId, String inputContent, String outputContent, String type, boolean aiGenerated) {
        if (problemId <= 0) return false;
        if (inputContent == null || outputContent == null) return false;

        if (aiGenerated) {
            List<Testcase> existing = testcaseDAO.getTestcasesByProblemId(problemId);
            for (Testcase tc : existing) {
                if (tc.getInputData() != null && tc.getInputData().trim().equals(inputContent.trim())) {
                    return true;
                }
            }
        }

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
                logger.log(Level.WARNING, "File save failed for testcase " + tc.getId() + ", rolling back DB record", e);
                testcaseDAO.deleteTestcase(tc.getId());
                return false;
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

    public boolean deleteTestcase(int testcaseId) {
        try {
            return testcaseDAO.deleteTestcase(testcaseId);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to delete testcase: " + testcaseId, e);
            return false;
        }
    }

    /**
     * Chạy code AC với từng testcase input, lấy actual output làm expected output mới.
     * Trả về số testcase đã cập nhật thành công.
     */
    public int recomputeExpectedOutputs(int problemId, String acCode, String language, JudgeService judgeService) {
        List<Testcase> testcases = testcaseDAO.getTestcasesByProblemId(problemId);
        if (testcases.isEmpty()) return 0;
        Problem problem = getProblemById(problemId);
        if (problem == null) return 0;

        JudgeEngine engine = new JudgeEngine();
        int updated = 0;
        for (Testcase tc : testcases) {
            try {
                JudgeResult result = engine.judge(acCode, language, tc.getInputData(), "",
                        problem.getTimeLimit(), problem.getMemoryLimit());
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
                logger.log(Level.WARNING, "Error recomputing output for testcase " + tc.getId(), e);
            }
        }
        return updated;
    }

    public int deleteAiTestcasesForProblem(int problemId) {
        return testcaseDAO.deleteAiGeneratedByProblemId(problemId);
    }

    public int addSampleCode(int problemId, String code, String language, String expectedType, boolean aiGenerated) {
        if (problemId <= 0) return -1;
        if (code == null || code.trim().isEmpty()) return -1;
        if (language == null || language.trim().isEmpty()) return -1;
        String[] validLangs = {"java", "cpp", "c++", "python", "py"};
        boolean validLang = false;
        for (String l : validLangs) {
            if (l.equals(language.toLowerCase())) { validLang = true; break; }
        }
        if (!validLang) return -1;
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
                logger.log(Level.WARNING, "File save failed for sample code " + sc.getId(), e);
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

    public int deleteAISampleCodesForProblem(int problemId) {
        return sampleCodeDAO.deleteAiGeneratedByProblemId(problemId);
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
            logger.log(Level.SEVERE, "Failed to save sample code file for judging", e);
            return results;
        }

        for (Testcase tc : testcases) {
            String inputPath;
            String expectedPath;
            try {
                inputPath = FileManager.saveTestcaseInput(problemId, tc.getId(), tc.getInputData());
                expectedPath = FileManager.saveTestcaseOutput(problemId, tc.getId(), tc.getExpectedOutput());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to save testcase files for TC " + tc.getId(), e);
                continue;
            }

            JudgeResult jr;
            if (problem.getCheckerScript() != null && !problem.getCheckerScript().isBlank()) {
                try {
                    String checkerPath = FileManager.saveCheckerScript(problemId, problem.getCheckerScript());
                    jr = judgeService.judgeWithChecker(codePath, sampleCode.getLanguage(), inputPath, expectedPath, checkerPath, problem.getTimeLimit());
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
            sub.setSampleCodeId(-1);
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

    public List<Submission> getAllSubmissions() {
        return submissionDAO.getAllSubmissions();
    }

    public boolean deleteProblem(int id) {
        Problem p = getProblemById(id);
        if (p == null) return false;

        List<Submission> subs = getSubmissionsByProblem(id);
        List<Testcase> tcs = getTestcasesByProblem(id);
        List<SampleCode> scs = getSampleCodesByProblem(id);

        for (Submission sub : subs) {
            try {
                FileManager.deleteFile(FileManager.getSubmissionsDir() + "/submission_" + sub.getId() + "_output.txt");
            } catch (IOException e) {
                logger.log(Level.FINE, "Failed to delete submission file: " + sub.getId(), e);
            }
        }

        for (Testcase tc : tcs) {
            try {
                FileManager.deleteFile(FileManager.getTestcasesDir() + "/problem_" + id + "/tc_" + tc.getId() + "_input.txt");
                FileManager.deleteFile(FileManager.getTestcasesDir() + "/problem_" + id + "/tc_" + tc.getId() + "_output.txt");
            } catch (IOException e) {
                logger.log(Level.FINE, "Failed to delete testcase file: " + tc.getId(), e);
            }
        }

        for (SampleCode sc : scs) {
            try {
                String ext = ".txt";
                String lang = sc.getLanguage().toLowerCase();
                if ("java".equals(lang)) ext = ".java";
                else if ("cpp".equals(lang) || "c++".equals(lang)) ext = ".cpp";
                else if ("python".equals(lang) || "py".equals(lang)) ext = ".py";
                FileManager.deleteFile(FileManager.getSamplesDir() + "/problem_" + id + "/sample_" + sc.getId() + ext);
            } catch (IOException e) {
                logger.log(Level.FINE, "Failed to delete sample code file: " + sc.getId(), e);
            }
        }

        if (p.getImagePath() != null && !p.getImagePath().isBlank()) {
            try {
                FileManager.deleteFile(p.getImagePath());
            } catch (IOException e) {
                logger.log(Level.FINE, "Failed to delete image: " + p.getImagePath(), e);
            }
        }

        try {
            FileManager.deleteFile(FileManager.getProblemsDir() + "/problem_" + id + "/checker.py");
        } catch (IOException e) {
            logger.log(Level.FINE, "Failed to delete checker script for problem: " + id, e);
        }

        try {
            boolean deleted = problemDAO.deleteProblem(id);
            if (!deleted) {
                logger.log(Level.WARNING, "Failed to delete problem from DB: " + id);
                return false;
            }
            problemDAO.resetAutoIncrement();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "DB delete failed for problem: " + id, e);
            return false;
        }

        return true;
    }

}
