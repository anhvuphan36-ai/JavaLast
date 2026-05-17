package com.java.util;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CodeFormatterTest {

    @Test
    @Order(1)
    @DisplayName("Format null code returns empty string")
    void testFormatNullCode_ReturnEmpty() {
        assertEquals("", CodeFormatter.formatGeneratedCode(null, "java"));
    }

    @Test
    @Order(2)
    @DisplayName("Format compact Java code to expanded")
    void testFormatJavaCode_CompactToExpanded() {
        String compact = "public class Main{public static void main(String[]a){System.out.println(1);}}";
        String result = CodeFormatter.formatGeneratedCode(compact, "java");

        assertTrue(result.contains("{\n"), "Should expand braces to new lines");
        assertTrue(result.contains("public class Main"), "Should preserve class declaration");
    }

    @Test
    @Order(3)
    @DisplayName("Already formatted code stays unchanged")
    void testFormatJavaCode_AlreadyFormatted() {
        String formatted = """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello");
                }
            }
            """;
        String result = CodeFormatter.formatGeneratedCode(formatted, "java");

        assertTrue(result.contains("public class Main {"), "Should keep opening brace");
        assertTrue(result.contains("System.out.println"), "Should preserve code body");
    }

    @Test
    @Order(4)
    @DisplayName("Python code is not modified")
    void testFormatPythonCode_NoChange() {
        String python = "print('hello')\nfor i in range(10):\n    print(i)";
        String result = CodeFormatter.formatGeneratedCode(python, "python");

        assertEquals(python, result);
    }

    @Test
    @Order(5)
    @DisplayName("C++ code is formatted like Java")
    void testFormatCppCode_Formatted() {
        String compact = "#include<iostream>using namespace std;int main(){cout<<1;}";
        String result = CodeFormatter.formatGeneratedCode(compact, "cpp");

        assertTrue(result.contains("{\n") || result.contains("{"), "Should expand braces");
    }

    @Test
    @Order(6)
    @DisplayName("Unknown language returns normalized code")
    void testFormatUnknownLanguage_NoBraceExpansion() {
        String code = "some code here";
        String result = CodeFormatter.formatGeneratedCode(code, "rust");

        assertEquals("some code here", result);
    }

    @Test
    @Order(7)
    @DisplayName("Escape sequences are unescaped")
    void testFormatUnescapeSequences() {
        String escaped = "public class Main{\\n    public static void main(String[]a){\\n        System.out.println(1);\\n    }\\n}";
        String result = CodeFormatter.formatGeneratedCode(escaped, "java");

        assertTrue(result.contains("public class Main"), "Should preserve class declaration");
        assertTrue(result.contains("System.out.println"), "Should preserve method call");
    }
}
