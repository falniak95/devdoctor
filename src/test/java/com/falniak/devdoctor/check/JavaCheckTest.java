package com.falniak.devdoctor.check;

import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JavaCheckTest {

    @TempDir
    Path tempDir;

    @Test
    void testPassWhenJavaAvailable() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("java", new ExecResult(0, "", "openjdk version \"17.0.1\" 2021-10-19"));

        CheckContext context = createTestContext(executor);
        JavaCheck check = new JavaCheck();

        CheckResult result = check.run(context);

        assertEquals("system.java", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertTrue(result.summary().contains("Java is available"));
        assertTrue(result.summary().contains("17.0.1"));
    }

    @Test
    void testPassWhenJavaAvailableWithoutVersion() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("java", new ExecResult(0, "", "java version"));

        CheckContext context = createTestContext(executor);
        JavaCheck check = new JavaCheck();

        CheckResult result = check.run(context);

        assertEquals("system.java", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertEquals("Java is available", result.summary());
    }

    @Test
    void testFailWhenJavaNotFound() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("java", new Exception("java: command not found"));

        CheckContext context = createTestContext(executor);
        JavaCheck check = new JavaCheck();

        CheckResult result = check.run(context);

        assertEquals("system.java", result.id());
        assertEquals(CheckStatus.FAIL, result.status());
        assertEquals("Java not found", result.summary());
        assertNotNull(result.details());
        assertTrue(result.details().contains("java: command not found"));
        assertFalse(result.suggestions().isEmpty());
    }

    @Test
    void testFailWhenJavaReturnsNonZero() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("java", new ExecResult(1, "", "Error: Java not properly configured"));

        CheckContext context = createTestContext(executor);
        JavaCheck check = new JavaCheck();

        CheckResult result = check.run(context);

        assertEquals("system.java", result.id());
        assertEquals(CheckStatus.FAIL, result.status());
        assertEquals("Java not found", result.summary());
        assertTrue(result.details().contains("non-zero exit code"));
        assertFalse(result.suggestions().isEmpty());
    }

    private CheckContext createTestContext(ProcessExecutor executor) {
        Path targetPath = tempDir;
        Set<ProjectType> types = EnumSet.noneOf(ProjectType.class);
        DetectionResult detectionResult = new DetectionResult(targetPath, types, List.<String>of());
        return new CheckContext(targetPath, targetPath, types, detectionResult, executor);
    }
}
