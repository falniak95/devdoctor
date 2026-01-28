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

class CheckRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void testRunChecksInOrder() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor);

        Check check1 = new TestCheck("check1", CheckStatus.PASS);
        Check check2 = new TestCheck("check2", CheckStatus.FAIL);
        Check check3 = new TestCheck("check3", CheckStatus.INFO);

        CheckRunner runner = new CheckRunner();
        List<CheckResult> results = runner.runChecks(List.of(check1, check2, check3), context);

        assertEquals(3, results.size());
        assertEquals("check1", results.get(0).id());
        assertEquals(CheckStatus.PASS, results.get(0).status());
        assertEquals("check2", results.get(1).id());
        assertEquals(CheckStatus.FAIL, results.get(1).status());
        assertEquals("check3", results.get(2).id());
        assertEquals(CheckStatus.INFO, results.get(2).status());
    }

    @Test
    void testRunChecksCollectsResults() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor);

        Check check = new TestCheck("test", CheckStatus.WARN);
        CheckRunner runner = new CheckRunner();
        List<CheckResult> results = runner.runChecks(List.of(check), context);

        assertEquals(1, results.size());
        CheckResult result = results.get(0);
        assertEquals("test", result.id());
        assertEquals(CheckStatus.WARN, result.status());
        assertEquals("Test summary", result.summary());
    }

    @Test
    void testRunChecksWithEmptyList() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor);

        CheckRunner runner = new CheckRunner();
        List<CheckResult> results = runner.runChecks(List.of(), context);

        assertTrue(results.isEmpty());
    }

    private CheckContext createTestContext(ProcessExecutor executor) {
        Path targetPath = tempDir;
        Set<ProjectType> types = EnumSet.noneOf(ProjectType.class);
        DetectionResult detectionResult = new DetectionResult(targetPath, types, List.<String>of());
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
}
