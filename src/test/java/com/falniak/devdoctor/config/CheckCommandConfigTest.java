package com.falniak.devdoctor.config;

import com.falniak.devdoctor.check.Check;
import com.falniak.devdoctor.check.CheckContext;
import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckRunner;
import com.falniak.devdoctor.check.CheckStatus;
import com.falniak.devdoctor.check.FakeProcessExecutor;
import com.falniak.devdoctor.check.ProcessExecutor;
import com.falniak.devdoctor.check.render.ConsoleRenderer;
import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CheckCommandConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void testIgnoreChecksFiltersOutSpecifiedChecks() throws Exception {
        // Create config file
        String yaml = """
            ignore_checks:
              - test.check1
              - test.check3
            """;
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);

        // Create test checks
        List<Check> allChecks = List.of(
            new TestCheck("test.check1", CheckStatus.PASS),
            new TestCheck("test.check2", CheckStatus.PASS),
            new TestCheck("test.check3", CheckStatus.PASS),
            new TestCheck("test.check4", CheckStatus.PASS)
        );

        // Load config
        ConfigLoader loader = new ConfigLoader();
        DevDoctorConfig config = loader.loadFromProjectRoot(tempDir).orElseThrow();

        // Apply filtering (simulating CheckCommand logic)
        Set<String> ignoreSet = config.ignoreChecks();
        List<Check> filteredChecks = allChecks.stream()
            .filter(check -> !ignoreSet.contains(check.id()))
            .toList();

        // Verify filtering
        assertEquals(2, filteredChecks.size());
        assertEquals("test.check2", filteredChecks.get(0).id());
        assertEquals("test.check4", filteredChecks.get(1).id());
    }

    @Test
    void testIgnoreChecksPreventsExecution() throws Exception {
        // Create config file
        String yaml = """
            ignore_checks:
              - test.check1
            """;
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);

        // Create test checks with execution tracking
        TrackableCheck check1 = new TrackableCheck("test.check1", CheckStatus.PASS);
        TrackableCheck check2 = new TrackableCheck("test.check2", CheckStatus.PASS);
        List<Check> allChecks = List.of(check1, check2);

        // Load config
        ConfigLoader loader = new ConfigLoader();
        DevDoctorConfig config = loader.loadFromProjectRoot(tempDir).orElseThrow();

        // Apply filtering
        Set<String> ignoreSet = config.ignoreChecks();
        List<Check> filteredChecks = allChecks.stream()
            .filter(check -> !ignoreSet.contains(check.id()))
            .toList();

        // Run checks
        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor);
        CheckRunner runner = new CheckRunner();
        List<CheckResult> results = runner.runChecks(filteredChecks, context);

        // Verify ignored check was not executed
        assertEquals(1, results.size());
        assertEquals("test.check2", results.get(0).id());
        assertFalse(check1.wasExecuted());
        assertTrue(check2.wasExecuted());
    }

    @Test
    void testRequireChecksAppendsToSummaryWhenFailed() throws Exception {
        // Create config file
        String yaml = """
            require_checks:
              - test.check1
              - test.check2
            """;
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);

        // Create test checks - one passes, one fails
        List<Check> checks = List.of(
            new TestCheck("test.check1", CheckStatus.FAIL),
            new TestCheck("test.check2", CheckStatus.PASS),
            new TestCheck("test.check3", CheckStatus.FAIL)
        );

        // Run checks
        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor);
        CheckRunner runner = new CheckRunner();
        List<CheckResult> results = runner.runChecks(checks, context);

        // Determine failed required checks (simulating CheckCommand logic)
        ConfigLoader loader = new ConfigLoader();
        DevDoctorConfig config = loader.loadFromProjectRoot(tempDir).orElseThrow();
        Set<String> requireSet = config.requireChecks();
        Set<String> failedRequiredChecks = results.stream()
            .filter(result -> requireSet.contains(result.id()))
            .filter(result -> result.status() == CheckStatus.FAIL)
            .map(CheckResult::id)
            .collect(java.util.stream.Collectors.toSet());

        // Verify failed required checks
        assertEquals(Set.of("test.check1"), failedRequiredChecks);

        // Test renderer output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(outContent));
            
            DetectionResult detectionResult = new DetectionResult(
                tempDir, 
                EnumSet.noneOf(ProjectType.class), 
                List.of()
            );
            ConsoleRenderer renderer = new ConsoleRenderer(false, false);
            renderer.render(detectionResult, results, failedRequiredChecks);
            
            String output = outContent.toString();
            assertTrue(output.contains("Required checks failed: test.check1"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRequireChecksDoesNotAppendWhenAllPass() throws Exception {
        // Create config file
        String yaml = """
            require_checks:
              - test.check1
              - test.check2
            """;
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);

        // Create test checks - all pass
        List<Check> checks = List.of(
            new TestCheck("test.check1", CheckStatus.PASS),
            new TestCheck("test.check2", CheckStatus.PASS)
        );

        // Run checks
        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor);
        CheckRunner runner = new CheckRunner();
        List<CheckResult> results = runner.runChecks(checks, context);

        // Determine failed required checks
        ConfigLoader loader = new ConfigLoader();
        DevDoctorConfig config = loader.loadFromProjectRoot(tempDir).orElseThrow();
        Set<String> requireSet = config.requireChecks();
        Set<String> failedRequiredChecks = results.stream()
            .filter(result -> requireSet.contains(result.id()))
            .filter(result -> result.status() == CheckStatus.FAIL)
            .map(CheckResult::id)
            .collect(java.util.stream.Collectors.toSet());

        // Verify no failed required checks
        assertTrue(failedRequiredChecks.isEmpty());

        // Test renderer output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(outContent));
            
            DetectionResult detectionResult = new DetectionResult(
                tempDir, 
                EnumSet.noneOf(ProjectType.class), 
                List.of()
            );
            ConsoleRenderer renderer = new ConsoleRenderer(false, false);
            renderer.render(detectionResult, results, failedRequiredChecks);
            
            String output = outContent.toString();
            assertFalse(output.contains("Required checks failed"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testConfigLoadingFromProjectRoot() throws Exception {
        String yaml = """
            ignore_checks:
              - system.java
            """;
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);

        ConfigLoader loader = new ConfigLoader();
        assertTrue(loader.loadFromProjectRoot(tempDir).isPresent());
    }

    @Test
    void testConfigLoadingFromExplicitPath() throws Exception {
        String yaml = """
            ignore_checks:
              - system.java
            """;
        Path configFile = tempDir.resolve("custom-config.yml");
        Files.writeString(configFile, yaml);

        ConfigLoader loader = new ConfigLoader();
        DevDoctorConfig config = loader.loadFromExplicitPath(configFile);
        assertEquals(Set.of("system.java"), config.ignoreChecks());
    }

    private CheckContext createTestContext(ProcessExecutor executor) {
        Path targetPath = tempDir;
        Set<ProjectType> types = EnumSet.noneOf(ProjectType.class);
        DetectionResult detectionResult = new DetectionResult(targetPath, types, List.of());
        return new CheckContext(targetPath, targetPath, types, detectionResult, executor);
    }

    /**
     * Simple test check implementation.
     */
    private static class TestCheck implements Check {
        private final String id;
        private final CheckStatus status;

        TestCheck(String id, CheckStatus status) {
            this.id = id;
            this.status = status;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public CheckResult run(CheckContext ctx) {
            return new CheckResult(id, status, "Test summary", null, List.of());
        }
    }

    /**
     * Test check that tracks execution.
     */
    private static class TrackableCheck implements Check {
        private final String id;
        private final CheckStatus status;
        private boolean executed = false;

        TrackableCheck(String id, CheckStatus status) {
            this.id = id;
            this.status = status;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public CheckResult run(CheckContext ctx) {
            executed = true;
            return new CheckResult(id, status, "Test summary", null, List.of());
        }

        boolean wasExecuted() {
            return executed;
        }
    }
}
