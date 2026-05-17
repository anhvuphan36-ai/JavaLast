package com.java.service;

import com.java.model.Problem;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for GeminiAIService.
 * Requires active API key in config.properties.
 * Skipped automatically if API key is placeholder or empty.
 */
@Tag("integration")
public class GeminiAIIntegrationTest {

    private static boolean hasValidApiKey = false;

    @BeforeAll
    public static void checkApiKey() {
        Properties props = new Properties();
        try (InputStream input = GeminiAIIntegrationTest.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
                String key = props.getProperty("ai.api.key", "");
                hasValidApiKey = key != null && !key.isBlank() && !key.contains("YOUR");
            }
        } catch (IOException e) {
            hasValidApiKey = false;
        }
        Assumptions.assumeTrue(hasValidApiKey, "Skipping AI integration tests: no valid API key configured");
    }

    @Test
    public void testGenerateSolutionReturnsCode() {
        GeminiAIService aiService = new GeminiAIService();
        Problem problem = new Problem();
        problem.setTitle("Tinh tong 2 so");
        problem.setDescription("Nhap vao 2 so nguyen a va b, in ra tong cua chung.");
        problem.setTimeLimit(1000);
        problem.setMemoryLimit(256);

        String code = aiService.generateSolution(problem, "java", "AC");

        assertNotNull(code, "Code should not be null");
        assertFalse(code.startsWith("// Lỗi"), "Code should not start with error: " + code);
        assertFalse(code.isBlank(), "Code should not be blank");
        assertTrue(code.contains("class") || code.contains("void main"),
                "Code should contain Java class or main method: " + code);
    }

    @Test
    public void testGenerateCheckerReturnsPython() {
        GeminiAIService aiService = new GeminiAIService();
        Problem problem = new Problem();
        problem.setTitle("Tinh tong 2 so");
        problem.setDescription("Nhap vao 2 so nguyen a va b, in ra tong cua chung.");

        String checker = aiService.generateChecker(problem);

        assertNotNull(checker, "Checker should not be null");
        assertFalse(checker.startsWith("# Lỗi"), "Checker should not start with error: " + checker);
        assertFalse(checker.isBlank(), "Checker should not be blank");
    }
}
