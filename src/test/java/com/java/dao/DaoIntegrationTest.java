package com.java.dao;

import com.java.model.Problem;
import com.java.model.SampleCode;
import com.java.model.Submission;
import com.java.model.Testcase;
import com.java.util.DatabaseConnection;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DaoIntegrationTest {

    private ProblemDAO problemDAO;
    private TestcaseDAO testcaseDAO;
    private SampleCodeDAO sampleCodeDAO;
    private SubmissionDAO submissionDAO;

    @BeforeAll
    static void checkDatabase() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed(), "Database connection must be open");
        }
    }

    @BeforeEach
    void setUp() {
        problemDAO = new ProblemDAO();
        testcaseDAO = new TestcaseDAO();
        sampleCodeDAO = new SampleCodeDAO();
        submissionDAO = new SubmissionDAO();
    }

    @Test
    @Order(1)
    @DisplayName("ProblemDAO - CRUD operations")
    void testProblemCRUD() throws SQLException {
        String title = "Test Problem CRUD " + System.currentTimeMillis();

        Problem p = new Problem();
        p.setTitle(title);
        p.setDescription("Test description");
        p.setContestType("ICPC");
        p.setTimeLimit(1000);
        p.setMemoryLimit(128);

        int id = problemDAO.addProblem(p);
        assertTrue(id > 0, "Should return positive ID");

        Problem fetched = problemDAO.getProblemById(id);
        assertNotNull(fetched);
        assertEquals(title, fetched.getTitle());
        assertEquals("Test description", fetched.getDescription());
        assertEquals(1000, fetched.getTimeLimit());

        List<Problem> all = problemDAO.getAllProblems();
        assertTrue(all.stream().anyMatch(x -> x.getId() == id));

        p.setId(id);
        p.setDescription("Updated description");
        assertTrue(problemDAO.updateProblem(p));

        Problem updated = problemDAO.getProblemById(id);
        assertEquals("Updated description", updated.getDescription());

        assertTrue(problemDAO.deleteProblem(id));
        assertNull(problemDAO.getProblemById(id));
    }

    @Test
    @Order(2)
    @DisplayName("TestcaseDAO - CRUD with problem")
    void testTestcaseCRUD_WithProblem() throws SQLException {
        Problem p = createTestProblem("TC CRUD Test");
        int pid = problemDAO.addProblem(p);

        Testcase tc = new Testcase(pid, "1 2\n", "3\n", "small", false);
        assertTrue(testcaseDAO.addTestcase(tc));
        assertTrue(tc.getId() > 0, "Testcase should have generated ID");

        List<Testcase> tcs = testcaseDAO.getTestcasesByProblemId(pid);
        assertTrue(tcs.size() >= 1);
        assertTrue(tcs.stream().anyMatch(t -> t.getId() == tc.getId()));

        tc.setInputData("5 10\n");
        tc.setExpectedOutput("15\n");
        assertTrue(testcaseDAO.updateTestcase(tc));

        Testcase updated = testcaseDAO.getTestcasesByProblemId(pid).stream()
                .filter(t -> t.getId() == tc.getId()).findFirst().orElse(null);
        assertNotNull(updated);
        assertEquals("5 10\n", updated.getInputData());

        assertTrue(testcaseDAO.deleteTestcase(tc.getId()));
        problemDAO.deleteProblem(pid);
    }

    @Test
    @Order(3)
    @DisplayName("SampleCodeDAO - CRUD with problem")
    void testSampleCodeCRUD_WithProblem() throws SQLException {
        Problem p = createTestProblem("SC CRUD Test");
        int pid = problemDAO.addProblem(p);

        SampleCode sc = new SampleCode(pid, "public class Main{}", "java", "AC", false);
        assertTrue(sampleCodeDAO.addSampleCode(sc));
        assertTrue(sc.getId() > 0);

        List<SampleCode> codes = sampleCodeDAO.getByProblemId(pid);
        assertTrue(codes.size() >= 1);

        SampleCode fetched = sampleCodeDAO.getById(sc.getId());
        assertNotNull(fetched);
        assertEquals("java", fetched.getLanguage());
        assertEquals("AC", fetched.getExpectedType());

        sc.setCodeContent("public class Main{public static void main(String[]a){}}");
        assertTrue(sampleCodeDAO.updateSampleCode(sc));

        assertTrue(sampleCodeDAO.deleteSampleCode(sc.getId()));
        problemDAO.deleteProblem(pid);
    }

    @Test
    @Order(4)
    @DisplayName("SubmissionDAO - CRUD operations")
    void testSubmissionCRUD() throws SQLException {
        Problem p = createTestProblem("Sub CRUD Test");
        int pid = problemDAO.addProblem(p);

        Testcase tc = new Testcase(pid, "1\n", "1\n", "normal", false);
        testcaseDAO.addTestcase(tc);

        SampleCode sc = new SampleCode(pid, "public class Main{}", "java", "AC", false);
        sampleCodeDAO.addSampleCode(sc);

        Submission sub = new Submission(pid, sc.getId(), tc.getId(), "1\n", 50, 1024, "AC", null);
        assertTrue(submissionDAO.addSubmission(sub));
        assertTrue(sub.getId() > 0);

        List<Submission> byProblem = submissionDAO.getByProblemId(pid);
        assertTrue(byProblem.size() >= 1);

        List<Submission> byCode = submissionDAO.getBySampleCodeId(sc.getId());
        assertTrue(byCode.size() >= 1);

        Submission fetched = submissionDAO.getById(sub.getId());
        assertNotNull(fetched);
        assertEquals("AC", fetched.getStatus());
        assertEquals(50, fetched.getExecutionTime());

        sub.setStatus("WA");
        sub.setErrorMessage("Wrong output");
        assertTrue(submissionDAO.updateSubmission(sub));

        assertTrue(submissionDAO.deleteSubmission(sub.getId()));
        problemDAO.deleteProblem(pid);
    }

    @Test
    @Order(5)
    @DisplayName("Delete problem cascades to testcases")
    void testDeleteProblem_CascadeDeletesTestcases() throws SQLException {
        Problem p = createTestProblem("Cascade TC Test");
        int pid = problemDAO.addProblem(p);

        testcaseDAO.addTestcase(new Testcase(pid, "1\n", "1\n", "normal", false));
        testcaseDAO.addTestcase(new Testcase(pid, "2\n", "2\n", "normal", false));

        assertTrue(testcaseDAO.getTestcasesByProblemId(pid).size() >= 2);

        problemDAO.deleteProblem(pid);

        List<Testcase> remaining = testcaseDAO.getTestcasesByProblemId(pid);
        assertTrue(remaining.isEmpty(), "Testcases should be cascade deleted");
    }

    @Test
    @Order(6)
    @DisplayName("Delete problem cascades to sample codes")
    void testDeleteProblem_CascadeDeletesSampleCodes() throws SQLException {
        Problem p = createTestProblem("Cascade SC Test");
        int pid = problemDAO.addProblem(p);

        SampleCodeDAO scDAO = new SampleCodeDAO();
        scDAO.addSampleCode(new SampleCode(pid, "code1", "java", "AC", false));
        scDAO.addSampleCode(new SampleCode(pid, "code2", "cpp", "WA", false));

        assertTrue(scDAO.getByProblemId(pid).size() >= 2);

        problemDAO.deleteProblem(pid);

        assertTrue(scDAO.getByProblemId(pid).isEmpty(), "Sample codes should be cascade deleted");
    }

    private Problem createTestProblem(String title) {
        Problem p = new Problem();
        p.setTitle(title);
        p.setDescription("Auto-generated test problem");
        p.setContestType("ICPC");
        p.setTimeLimit(2000);
        p.setMemoryLimit(256);
        return p;
    }
}
