package com.falniak.devdoctor.check.requirements;

import com.falniak.devdoctor.check.CheckContext;
import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckStatus;
import com.falniak.devdoctor.check.ExecResult;
import com.falniak.devdoctor.check.FakeProcessExecutor;
import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GoRequirementCheckTest {

    @TempDir
    Path tempDir;

    @Test
    void testNotApplicableWhenNoGoProject() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor, EnumSet.noneOf(ProjectType.class));
        GoRequirementCheck check = new GoRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.go.requirements", result.id());
        assertEquals(CheckStatus.NOT_APPLICABLE, result.status());
    }

    @Test
    void testInfoWhenNoRequirementFound() throws Exception {
        Files.createFile(tempDir.resolve("go.mod"));
        Files.writeString(tempDir.resolve("go.mod"), "module test\n");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.GO_MODULES));
        GoRequirementCheck check = new GoRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.go.requirements", result.id());
        assertEquals(CheckStatus.INFO, result.status());
        assertTrue(result.summary().contains("No Go version requirement specified"));
    }

    @Test
    void testPassWhenRequirementMatches() throws Exception {
        Files.writeString(tempDir.resolve("go.mod"), "module test\ngo 1.21\n");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("go", new ExecResult(0, "go version go1.21.0 linux/amd64\n", ""));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.GO_MODULES));
        GoRequirementCheck check = new GoRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.go.requirements", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertTrue(result.summary().contains("Required: 1.21"));
        assertTrue(result.summary().contains("Local: go 1.21.0"));
    }

    @Test
    void testFailWhenRequirementMismatches() throws Exception {
        Files.writeString(tempDir.resolve("go.mod"), "module test\ngo 1.21\n");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("go", new ExecResult(0, "go version go1.20.0 linux/amd64\n", ""));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.GO_MODULES));
        GoRequirementCheck check = new GoRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.go.requirements", result.id());
        assertEquals(CheckStatus.FAIL, result.status());
        assertTrue(result.summary().contains("Required: 1.21"));
        assertTrue(result.summary().contains("Local: go 1.20.0"));
        assertFalse(result.suggestions().isEmpty());
    }

    @Test
    void testWarnWhenGoNotFound() throws Exception {
        Files.writeString(tempDir.resolve("go.mod"), "module test\ngo 1.21\n");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("go", new Exception("go: command not found"));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.GO_MODULES));
        GoRequirementCheck check = new GoRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.go.requirements", result.id());
        assertEquals(CheckStatus.WARN, result.status());
        assertTrue(result.summary().contains("Go not found"));
    }

    private CheckContext createTestContext(FakeProcessExecutor executor, Set<ProjectType> types) {
        DetectionResult detectionResult = new DetectionResult(tempDir, types, List.of());
        return new CheckContext(tempDir, tempDir, types, detectionResult, executor);
    }
}
