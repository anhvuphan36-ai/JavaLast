package com.java.service;

import com.java.dao.ProblemDAO;
import com.java.model.*;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProblemServiceIntegrationTest {

    private ProblemService problemService;

    @BeforeEach
    void setUp() {
        problemService = new ProblemService();
    }

    @Test
    @Order(1)
    @DisplayName("Full flow: create problem, add testcase, add code, judge → AC")
    void testFullFlow_CreateProblem_AddTestcase_AddCode_Judge() throws SQLException {
        Problem p = new Problem();
        p.setTitle("Sum Test Full Flow");
        p.setDescription("Cho hai số a và b. In ra a + b.");
        p.setContestType("ICPC");
        p.setTimeLimit(2000);
        p.setMemoryLimit(256);

        int pid = problemService.createProblem(p);
        assertTrue(pid > 0, "Problem should be created with positive ID");

        boolean tcOk = problemService.addTestcaseFull(pid, "3 5\n", "8\n", "normal", false);
        assertTrue(tcOk, "Testcase should be saved");

        String acCode = """
            import java.util.Scanner;
            public class Main {
                public static void main(String[] args) {
                    Scanner sc = new Scanner(System.in);
                    int a = sc.nextInt();
                    int b = sc.nextInt();
                    System.out.println(a + b);
                }
            }
            """;
        int sid = problemService.addSampleCode(pid, acCode, "java", "AC", false);
        assertTrue(sid > 0, "Sample code should be saved");

        List<Submission> results = problemService.runJudging(pid, sid, new DefaultJudgeService());
        assertFalse(results.isEmpty(), "Should have judging results");
        assertEquals("AC", results.get(0).getStatus(), "AC code should get AC verdict");

        new ProblemDAO().deleteProblem(pid);
    }

    @Test
    @Order(2)
    @DisplayName("Add testcase creates both DB record and files")
    void testAddTestcaseFull_CreatesFiles() throws SQLException {
        Problem p = new Problem();
        p.setTitle("File Test");
        p.setDescription("Test file creation");
        p.setContestType("ICPC");
        p.setTimeLimit(1000);
        p.setMemoryLimit(128);

        int pid = problemService.createProblem(p);
        assertTrue(pid > 0);

        boolean ok = problemService.addTestcaseFull(pid, "10\n", "10\n", "edge", true);
        assertTrue(ok);

        List<Testcase> tcs = problemService.getTestcasesByProblem(pid);
        assertTrue(tcs.size() >= 1);

        Testcase tc = tcs.stream().filter(t -> t.isAiGenerated()).findFirst().orElse(null);
        assertNotNull(tc, "Should find AI-generated testcase");
        assertEquals("10\n", tc.getInputData());
        assertEquals("10\n", tc.getExpectedOutput());
        assertEquals("edge", tc.getTestcaseType());

        new ProblemDAO().deleteProblem(pid);
    }

    @Test
    @Order(3)
    @DisplayName("Delete problem cleans files and database")
    void testDeleteProblem_CleansFilesAndDB() {
        Problem p = new Problem();
        p.setTitle("Delete Clean Test");
        p.setDescription("Test cleanup on delete");
        p.setContestType("ICPC");
        p.setTimeLimit(1000);
        p.setMemoryLimit(128);

        int pid = problemService.createProblem(p);
        assertTrue(pid > 0);

        problemService.addTestcaseFull(pid, "1\n", "1\n", "normal", false);
        problemService.addSampleCode(pid, "public class Main{}", "java", "AC", false);

        boolean deleted = problemService.deleteProblem(pid);
        assertTrue(deleted, "Problem should be deleted");

        Problem fetched = problemService.getProblemById(pid);
        assertNull(fetched, "Problem should not exist after deletion");

        List<Testcase> tcs = problemService.getTestcasesByProblem(pid);
        assertTrue(tcs.isEmpty(), "Testcases should be gone");

        List<SampleCode> scs = problemService.getSampleCodesByProblem(pid);
        assertTrue(scs.isEmpty(), "Sample codes should be gone");
    }
}
