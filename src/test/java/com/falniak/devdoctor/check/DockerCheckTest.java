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

class DockerCheckTest {

    @TempDir
    Path tempDir;

    @Test
    void testPassWhenDockerAvailable() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("docker", new ExecResult(0, "Docker version 24.0.0, build abc123", ""));

        CheckContext context = createTestContext(executor);
        DockerCheck check = new DockerCheck();

        CheckResult result = check.run(context);

        assertEquals("system.docker", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertTrue(result.summary().contains("Docker is available"));
        assertTrue(result.summary().contains("24.0.0"));
    }

    @Test
    void testPassWhenDockerAvailableWithoutVersion() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("docker", new ExecResult(0, "Docker version", ""));

        CheckContext context = createTestContext(executor);
        DockerCheck check = new DockerCheck();

        CheckResult result = check.run(context);

        assertEquals("system.docker", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertEquals("Docker is available", result.summary());
    }

    @Test
    void testFailWhenDockerNotFound() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("docker", new Exception("docker: command not found"));

        CheckContext context = createTestContext(executor);
        DockerCheck check = new DockerCheck();

        CheckResult result = check.run(context);

        assertEquals("system.docker", result.id());
        assertEquals(CheckStatus.FAIL, result.status());
        assertEquals("Docker not found", result.summary());
        assertNotNull(result.details());
        assertTrue(result.details().contains("docker: command not found"));
        assertFalse(result.suggestions().isEmpty());
        
        // Verify generic suggestion exists
        boolean hasGenericSuggestion = result.suggestions().stream()
            .anyMatch(s -> s.message() != null && 
                s.message().contains("Install Docker from https://www.docker.com/get-started"));
        assertTrue(hasGenericSuggestion, "Should include generic Docker installation suggestion");
    }

    @Test
    void testFailWhenDockerReturnsNonZero() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("docker", new ExecResult(1, "", "Error: Docker daemon not running"));

        CheckContext context = createTestContext(executor);
        DockerCheck check = new DockerCheck();

        CheckResult result = check.run(context);

        assertEquals("system.docker", result.id());
        assertEquals(CheckStatus.FAIL, result.status());
        assertEquals("Docker not found", result.summary());
        assertTrue(result.details().contains("non-zero exit code"));
        assertFalse(result.suggestions().isEmpty());
    }

    @Test
    void testWindowsWingetSuggestionIncluded() {
        // This test verifies that on Windows, the winget suggestion is included
        // We check the actual OS at runtime
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = osName.contains("windows");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("docker", new Exception("docker: command not found"));

        CheckContext context = createTestContext(executor);
        DockerCheck check = new DockerCheck();

        CheckResult result = check.run(context);

        assertEquals("system.docker", result.id());
        assertEquals(CheckStatus.FAIL, result.status());
        
        // Check if winget suggestion is present (should be on Windows)
        boolean hasWingetSuggestion = result.suggestions().stream()
            .anyMatch(s -> s.commands() != null && 
                s.commands().contains("winget install -e --id Docker.DockerDesktop"));
        
        if (isWindows) {
            assertTrue(hasWingetSuggestion, "On Windows, should include winget suggestion");
        } else {
            assertFalse(hasWingetSuggestion, "On non-Windows, should not include winget suggestion");
        }
    }

    @Test
    void testWingetSuggestionAppearsInVerboseOutput() {
        // This test verifies that the winget suggestion appears when rendered in verbose mode
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = osName.contains("windows");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("docker", new Exception("docker: command not found"));

        CheckContext context = createTestContext(executor);
        DockerCheck check = new DockerCheck();
        CheckResult result = check.run(context);

        // Render in verbose mode
        com.falniak.devdoctor.check.render.ConsoleRenderer renderer = 
            new com.falniak.devdoctor.check.render.ConsoleRenderer(false, true);
        
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outputStream));
        
        try {
            DetectionResult detectionResult = new DetectionResult(tempDir, EnumSet.noneOf(ProjectType.class), List.of());
            renderer.render(detectionResult, List.of(result));
            
            String output = outputStream.toString();
            
            // Verify generic suggestion appears
            assertTrue(output.contains("Install Docker from https://www.docker.com/get-started"));
            
            // On Windows, verify winget command appears
            if (isWindows) {
                assertTrue(output.contains("> winget install -e --id Docker.DockerDesktop"),
                    "On Windows, verbose output should include winget command");
            }
        } finally {
            System.setOut(originalOut);
        }
    }

    private CheckContext createTestContext(ProcessExecutor executor) {
        Path targetPath = tempDir;
        Set<ProjectType> types = EnumSet.noneOf(ProjectType.class);
        DetectionResult detectionResult = new DetectionResult(targetPath, types, List.<String>of());
        return new CheckContext(targetPath, targetPath, types, detectionResult, executor);
    }
}
