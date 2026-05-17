package com.java.service;

import com.java.model.JudgeResult;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JudgeEngineTest {

    private JudgeEngine engine;

    @BeforeEach
    void setUp() {
        engine = new JudgeEngine();
    }

    // ===== ACCEPTED TESTS =====

    @Test
    @Order(1)
    @DisplayName("Java AC - Sum two numbers")
    void testJavaAC_SumTwoNumbers() {
        String code = """
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
        JudgeResult result = engine.judge(code, "java", "3 5\n", "8\n", 2000, 256);
        assertEquals("AC", result.getStatus());
        assertEquals("8", result.getActualOutput().trim());
        assertTrue(result.getExecutionTime() >= 0);
    }

    @Test
    @Order(2)
    @DisplayName("Java AC - Print hello world")
    void testJavaAC_HelloWorld() {
        String code = """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello");
                }
            }
            """;
        JudgeResult result = engine.judge(code, "java", "", "Hello\n", 2000, 256);
        assertEquals("AC", result.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Java AC - Read all input and echo")
    void testJavaAC_EchoInput() {
        String code = """
            import java.util.Scanner;
            public class Main {
                public static void main(String[] args) {
                    Scanner sc = new Scanner(System.in);
                    String line = sc.nextLine();
                    System.out.println(line);
                }
            }
            """;
        JudgeResult result = engine.judge(code, "java", "test input\n", "test input\n", 2000, 256);
        assertEquals("AC", result.getStatus());
    }

    // ===== WRONG ANSWER TESTS =====

    @Test
    @Order(4)
    @DisplayName("Java WA - Wrong operator (subtract instead of add)")
    void testJavaWA_WrongOperator() {
        String code = """
            import java.util.Scanner;
            public class Main {
                public static void main(String[] args) {
                    Scanner sc = new Scanner(System.in);
                    int a = sc.nextInt();
                    int b = sc.nextInt();
                    System.out.println(a - b);
                }
            }
            """;
        JudgeResult result = engine.judge(code, "java", "3 5\n", "8\n", 2000, 256);
        assertEquals("WA", result.getStatus());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @Order(5)
    @DisplayName("Java WA - Off by one error")
    void testJavaWA_OffByOne() {
        String code = """
            import java.util.Scanner;
            public class Main {
                public static void main(String[] args) {
                    Scanner sc = new Scanner(System.in);
                    int n = sc.nextInt();
                    System.out.println(n + 1);
                }
            }
            """;
        JudgeResult result = engine.judge(code, "java", "5\n", "5\n", 2000, 256);
        assertEquals("WA", result.getStatus());
    }

    // ===== TIME LIMIT EXCEEDED TESTS =====

    @Test
    @Order(6)
    @DisplayName("Java TLE - Infinite loop")
    void testJavaTLE_InfiniteLoop() {
        String code = """
            public class Main {
                public static void main(String[] args) {
                    while (true) {}
                }
            }
            """;
        JudgeResult result = engine.judge(code, "java", "1\n", "1\n", 500, 256);
        assertEquals("TLE", result.getStatus());
        assertTrue(result.getExecutionTime() >= 400);
    }

    @Test
    @Order(7)
    @DisplayName("Java TLE - Very slow computation")
    void testJavaTLE_SlowComputation() {
        String code = """
            public class Main {
                public static void main(String[] args) {
                    long sum = 0;
                    for (long i = 0; i < 5_000_000_000L; i++) {
                        sum += i;
                    }
                    System.out.println(sum);
                }
            }
            """;
        JudgeResult result = engine.judge(code, "java", "", "0\n", 500, 256);
        assertEquals("TLE", result.getStatus());
    }

    // ===== COMPILE ERROR TESTS =====

    @Test
    @Order(8)
    @DisplayName("Java CE - Syntax error")
    void testJavaCE_SyntaxError() {
        String code = """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("missing semicolon")
                }
            }
            """;
        JudgeResult result = engine.judge(code, "java", "", "", 2000, 256);
        assertEquals("CE", result.getStatus());
        assertNotNull(result.getErrorMessage());
        assertFalse(result.getErrorMessage().isBlank());
    }

    @Test
    @Order(9)
    @DisplayName("Java CE - Undefined variable")
    void testJavaCE_UndefinedVariable() {
        String code = """
            public class Main {
                public static void main(String[] args) {
                    System.out.println(undefinedVar);
                }
            }
            """;
        JudgeResult result = engine.judge(code, "java", "", "", 2000, 256);
        assertEquals("CE", result.getStatus());
    }

    // ===== RUNTIME ERROR TESTS =====

    @Test
    @Order(10)
    @DisplayName("Java RE - Array index out of bounds")
    void testJavaRE_ArrayOutOfBounds() {
        String code = """
            public class Main {
                public static void main(String[] args) {
                    int[] arr = new int[1];
                    System.out.println(arr[5]);
                }
            }
            """;
        JudgeResult result = engine.judge(code, "java", "", "", 2000, 256);
        assertEquals("RE", result.getStatus());
    }

    // ===== OUTPUT COMPARISON TESTS =====

    @Test
    @Order(11)
    @DisplayName("Compare output - trailing whitespace ignored")
    void testCompareOutput_TrailingWhitespace() {
        String code = """
            public class Main {
                public static void main(String[] args) {
                    System.out.print("42");
                }
            }
            """;
        JudgeResult result = engine.judge(code, "java", "", "42\n", 2000, 256);
        assertEquals("AC", result.getStatus());
    }

    @Test
    @Order(12)
    @DisplayName("Compare output - numeric tolerance for floats")
    void testCompareOutput_NumericTolerance() {
        String code = """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("3.14159265");
                }
            }
            """;
        JudgeResult result = engine.judge(code, "java", "", "3.1415926\n", 2000, 256);
        assertEquals("AC", result.getStatus());
    }

    // ===== HELPER METHOD TESTS =====

    @Test
    @Order(13)
    @DisplayName("Extract Java class name from code")
    void testExtractJavaClassName() {
        String code1 = "public class Solution { }";
        String code2 = "public class Main { }";
        String code3 = "class Foo { }";

        assertEquals("Solution", extractClassName(code1));
        assertEquals("Main", extractClassName(code2));
        assertEquals("Main", extractClassName(code3));
    }

    private String extractClassName(String code) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("public\\s+class\\s+(\\w+)");
        java.util.regex.Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Main";
    }
}
