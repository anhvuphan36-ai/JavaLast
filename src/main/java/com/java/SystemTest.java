package com.java;

import com.java.dao.*;
import com.java.model.*;
import com.java.service.*;
import com.java.util.DatabaseConnection;
import com.java.util.FileManager;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class SystemTest {
    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== AI-Powered CP Judge System - System Test ===\n");

        t1_dbConnection();
        t2_problemCRUD();
        t3_testcaseCRUD();
        t4_sampleCodeCRUD();
        t5_fileManager();
        t6_judgeEngine();
        t7_submissionFlow();

        System.out.printf("\nRESULT: %d PASSED | %d FAILED%n", pass, fail);
        if (fail > 0) System.exit(1);
    }

    static void ok(boolean c, String msg) {
        if (c) { pass++; System.out.println("  [PASS] " + msg); }
        else   { fail++; System.out.println("  [FAIL] " + msg); }
    }

    static void t1_dbConnection() {
        System.out.println(">> TEST 1: Database Connection");
        try (var conn = DatabaseConnection.getConnection()) {
            ok(conn != null && !conn.isClosed(), "MySQL connected");
        } catch (Exception e) { ok(false, "MySQL: " + e.getMessage()); }
    }

    static void t2_problemCRUD() throws Exception {
        System.out.println(">> TEST 2: Problem CRUD");
        ProblemDAO d = new ProblemDAO();
        Problem p = new Problem();
        p.setTitle("T" + System.currentTimeMillis());
        p.setDescription("d");
        p.setContestType("ICPC");
        p.setTimeLimit(1000);
        p.setMemoryLimit(64);
        int id = d.addProblem(p);
        ok(id > 0, "Create Problem (id=" + id + ")");

        Problem f = d.getProblemById(id);
        ok(f != null && f.getTitle().equals(p.getTitle()), "Read Problem by ID");

        ok(d.getAllProblems().stream().anyMatch(x -> x.getId() == id), "List contains new problem");

        p.setId(id); p.setDescription("updated");
        ok(d.updateProblem(p), "Update Problem");
        ok(d.deleteProblem(id), "Delete Problem");
    }

    static void t3_testcaseCRUD() throws Exception {
        System.out.println(">> TEST 3: Testcase CRUD");
        ProblemDAO pd = new ProblemDAO();
        Problem pr = new Problem(); pr.setTitle("tc"); pr.setDescription("d"); pr.setContestType("ICPC");
        int pid = pd.addProblem(pr);

        TestcaseDAO td = new TestcaseDAO();
        Testcase tc = new Testcase(pid, "1 2\n", "3\n", "small", false);
        ok(td.addTestcase(tc) && tc.getId() > 0, "Create Testcase");
        ok(td.getTestcasesByProblemId(pid).size() >= 1, "Read Testcases by Problem");
        pd.deleteProblem(pid);
    }

    static void t4_sampleCodeCRUD() throws Exception {
        System.out.println(">> TEST 4: SampleCode CRUD");
        ProblemDAO pd = new ProblemDAO();
        Problem pr = new Problem(); pr.setTitle("sc"); pr.setDescription("d"); pr.setContestType("ICPC");
        int pid = pd.addProblem(pr);

        SampleCodeDAO sd = new SampleCodeDAO();
        SampleCode sc = new SampleCode(pid, "class Main{public static void main(String[]a){System.out.println(1);}}", "java", "AC", false);
        ok(sd.addSampleCode(sc) && sc.getId() > 0, "Create SampleCode");
        ok(sd.getByProblemId(pid).size() >= 1, "Read SampleCodes by Problem");
        pd.deleteProblem(pid);
    }

    static void t5_fileManager() {
        System.out.println(">> TEST 5: FileManager");
        try {
            String path = FileManager.saveTestcaseInput(99999, 1, "hello");
            ok(Files.exists(Paths.get(path)), "Save file exists");
            ok("hello".equals(FileManager.readFile(path)), "Read file correct");
            Files.deleteIfExists(Paths.get(path));
        } catch (Exception e) { ok(false, "FileManager: " + e.getMessage()); }
    }

    static void t6_judgeEngine() {
        System.out.println(">> TEST 6: JudgeEngine");
        JudgeEngine e = new JudgeEngine();
        String ac = "import java.util.Scanner;public class Main{public static void main(String[]a){Scanner s=new Scanner(System.in);System.out.println(s.nextInt()+s.nextInt());}}";
        String wa = "import java.util.Scanner;public class Main{public static void main(String[]a){Scanner s=new Scanner(System.in);System.out.println(s.nextInt()-s.nextInt());}}";
        String tle = "public class Main{public static void main(String[]a){while(true);}}";

        ok("AC".equals(e.judge(ac, "java", "3 5\n", "8\n", 2000, 256).getStatus()), "AC code (3+5=8)");
        ok("WA".equals(e.judge(wa, "java", "3 5\n", "8\n", 2000, 256).getStatus()), "WA code (3-5!=8)");
        ok("TLE".equals(e.judge(tle, "java", "1\n", "1\n", 500, 256).getStatus()), "TLE code (infinite loop)");
    }

    static void t7_submissionFlow() throws Exception {
        System.out.println(">> TEST 7: Full Submission Flow");
        ProblemService ps = new ProblemService();
        Problem p = new Problem(); p.setTitle("fl"); p.setDescription("sum"); p.setTimeLimit(2000); p.setMemoryLimit(256);
        int pid = ps.createProblem(p);
        ok(pid > 0, "Create problem in flow");

        ok(ps.addTestcaseFull(pid, "3 5\n", "8\n", "small", false), "Add testcase");
        String code = "import java.util.Scanner;public class Main{public static void main(String[]a){Scanner s=new Scanner(System.in);System.out.println(s.nextInt()+s.nextInt());}}";
        int sid = ps.addSampleCode(pid, code, "java", "AC", false);
        ok(sid > 0, "Add sample code AC");

        List<Submission> res = ps.runJudging(pid, sid, new DefaultJudgeService());
        ok(!res.isEmpty() && "AC".equals(res.get(0).getStatus()), "Judging returns AC");
        ok(ps.getSubmissionsByProblem(pid).size() >= 1, "Submission saved to DB");

        new ProblemDAO().deleteProblem(pid);
    }
}
