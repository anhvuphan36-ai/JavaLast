package com.java.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

public class TleValidationTest {

    private final JudgeEngine engine = new JudgeEngine();

    @Test
    public void testOnCode_O1_ShouldAC(@TempDir Path tempDir) {
        String code = """
            import java.util.Scanner;
            public class Main {
                public static void main(String[] args) {
                    Scanner sc = new Scanner(System.in);
                    int n = sc.nextInt();
                    long sum = 0;
                    for (int i = 0; i < n; i++) {
                        sum += sc.nextInt();
                    }
                    System.out.println(sum);
                }
            }
            """;
        String input = "100000\n" + "1 ".repeat(100000);
        var result = engine.judge(code, "java", input, "", 2000, 256);
        assertEquals("AC", result.getStatus(), "O(n) code should AC with n=100000");
    }

    @Test
    public void testOnCode_O2_SmallN_ShouldAC(@TempDir Path tempDir) {
        String code = """
            import java.util.Scanner;
            public class Main {
                public static void main(String[] args) {
                    Scanner sc = new Scanner(System.in);
                    int n = sc.nextInt();
                    int[] a = new int[n];
                    for (int i = 0; i < n; i++) a[i] = sc.nextInt();
                    long sum = 0;
                    for (int i = 0; i < n; i++)
                        for (int j = 0; j < n; j++)
                            sum += a[i] * a[j];
                    System.out.println(sum);
                }
            }
            """;
        String input = "100\n" + "1 ".repeat(100);
        var result = engine.judge(code, "java", input, "", 2000, 256);
        assertEquals("AC", result.getStatus(), "O(n^2) code should AC with small n=100");
    }

    @Test
    public void testOnCode_O2_LargeN_ShouldTLE(@TempDir Path tempDir) {
        String code = """
            import java.util.Scanner;
            public class Main {
                public static void main(String[] args) {
                    Scanner sc = new Scanner(System.in);
                    int n = sc.nextInt();
                    int[] a = new int[n];
                    for (int i = 0; i < n; i++) a[i] = sc.nextInt();
                    long sum = 0;
                    for (int i = 0; i < n; i++)
                        for (int j = 0; j < n; j++)
                            sum += a[i] * a[j];
                    System.out.println(sum);
                }
            }
            """;
        StringBuilder sb = new StringBuilder();
        sb.append("50000\n");
        for (int i = 0; i < 50000; i++) {
            sb.append((i % 1000) - 500).append(' ');
        }
        String input = sb.toString();
        var result = engine.judge(code, "java", input, "", 2000, 256);
        assertEquals("TLE", result.getStatus(), "O(n^2) code with n=50000 should TLE with -Xint");
    }

    @Test
    public void testValidateTleEffectiveness_SmallInput_Rejected() {
        GeminiAIService aiService = new GeminiAIService();
        String smallInput = "10\n1 2 3 4 5 6 7 8 9 10";
        assertFalse(aiService.validateTleEffectiveness(smallInput, 50, 2000),
                "Small input (n=10) should be rejected for anti-tle");
    }

    @Test
    public void testValidateTleEffectiveness_LargeInput_FastAC_Rejected() {
        GeminiAIService aiService = new GeminiAIService();
        String largeInput = "100000\n" + "1 ".repeat(33333) + "1";
        int veryFastTime = 50;
        int problemTimeLimit = 2000;
        assertFalse(aiService.validateTleEffectiveness(largeInput, veryFastTime, problemTimeLimit),
                "Large input with very fast AC (50ms/2000ms = 2.5%) should be rejected");
    }

    @Test
    public void testValidateTleEffectiveness_MediumInput_Accepted() {
        GeminiAIService aiService = new GeminiAIService();
        String mediumInput = "5000\n" + "1 ".repeat(5000);
        int moderateTime = 800;
        int problemTimeLimit = 2000;
        assertTrue(aiService.validateTleEffectiveness(mediumInput, moderateTime, problemTimeLimit),
                "Medium input with moderate AC time should be accepted");
    }
}
