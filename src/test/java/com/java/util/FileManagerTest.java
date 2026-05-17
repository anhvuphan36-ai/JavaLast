package com.java.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileManagerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        FileManager.initFolders();
    }

    @Test
    @Order(1)
    @DisplayName("Save and read testcase input")
    void testSaveAndReadTestcaseInput() throws IOException {
        String content = "3 5\n";
        String path = FileManager.saveTestcaseInput(99999, 1, content);

        assertTrue(Files.exists(Path.of(path)), "File should exist");
        assertEquals(content, FileManager.readFile(path));

        Files.deleteIfExists(Path.of(path));
    }

    @Test
    @Order(2)
    @DisplayName("Save and read testcase output")
    void testSaveAndReadTestcaseOutput() throws IOException {
        String content = "8\n";
        String path = FileManager.saveTestcaseOutput(99999, 2, content);

        assertTrue(Files.exists(Path.of(path)), "File should exist");
        assertEquals(content, FileManager.readFile(path));

        Files.deleteIfExists(Path.of(path));
    }

    @Test
    @Order(3)
    @DisplayName("Delete file removes it")
    void testDeleteFile() throws IOException {
        String content = "test";
        String path = FileManager.saveTestcaseInput(99999, 3, content);
        assertTrue(Files.exists(Path.of(path)));

        FileManager.deleteFile(path);
        assertFalse(Files.exists(Path.of(path)), "File should be deleted");
    }

    @Test
    @Order(4)
    @DisplayName("Read non-existent file throws IOException")
    void testReadNonExistentFile_ThrowsIOException() {
        assertThrows(IOException.class, () -> {
            FileManager.readFile("/nonexistent/path/file.txt");
        });
    }

    @Test
    @Order(5)
    @DisplayName("Save sample code with correct extension")
    void testSaveSampleCode_JavaExtension() throws IOException {
        String code = "public class Main {}";
        String path = FileManager.saveSampleCode(99999, 100, code, "java");

        assertTrue(path.endsWith(".java"), "Java code should have .java extension");
        assertTrue(Files.exists(Path.of(path)));

        Files.deleteIfExists(Path.of(path));
    }

    @Test
    @Order(6)
    @DisplayName("Save sample code C++ with correct extension")
    void testSaveSampleCode_CppExtension() throws IOException {
        String code = "#include <iostream>";
        String path = FileManager.saveSampleCode(99999, 101, code, "cpp");

        assertTrue(path.endsWith(".cpp"), "C++ code should have .cpp extension");

        Files.deleteIfExists(Path.of(path));
    }

    @Test
    @Order(7)
    @DisplayName("Save sample code Python with correct extension")
    void testSaveSampleCode_PythonExtension() throws IOException {
        String code = "print('hello')";
        String path = FileManager.saveSampleCode(99999, 102, code, "python");

        assertTrue(path.endsWith(".py"), "Python code should have .py extension");

        Files.deleteIfExists(Path.of(path));
    }

    @Test
    @Order(8)
    @DisplayName("File exists check works correctly")
    void testFileExists() throws IOException {
        String path = FileManager.saveTestcaseInput(99999, 4, "test");
        assertTrue(FileManager.fileExists(path));

        FileManager.deleteFile(path);
        assertFalse(FileManager.fileExists(path));
    }
}
