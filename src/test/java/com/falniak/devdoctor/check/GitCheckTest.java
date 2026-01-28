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

class GitCheckTest {

    @TempDir
    Path tempDir;

    @Test
    void testPassWhenGitAvailable() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("git", new ExecResult(0, "git version 2.40.0", ""));

        CheckContext context = createTestContext(executor);
        GitCheck check = new GitCheck();

        CheckResult result = check.run(context);

        assertEquals("system.git", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertTrue(result.summary().contains("Git is available"));
        assertTrue(result.summary().contains("2.40.0"));
    }

    @Test
    void testPassWhenGitAvailableWithoutVersion() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("git", new ExecResult(0, "git version", ""));

        CheckContext context = createTestContext(executor);
        GitCheck check = new GitCheck();

        CheckResult result = check.run(context);

        assertEquals("system.git", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertEquals("Git is available", result.summary());
    }

    @Test
    void testFailWhenGitNotFound() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("git", new Exception("git: command not found"));

        CheckContext context = createTestContext(executor);
        GitCheck check = new GitCheck();

        CheckResult result = check.run(context);

        assertEquals("system.git", result.id());
        assertEquals(CheckStatus.FAIL, result.status());
        assertEquals("Git not found", result.summary());
        assertNotNull(result.details());
        assertTrue(result.details().contains("git: command not found"));
        assertFalse(result.suggestions().isEmpty());
    }

    @Test
    void testFailWhenGitReturnsNonZero() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("git", new ExecResult(1, "", "fatal: not a git repository"));

        CheckContext context = createTestContext(executor);
        GitCheck check = new GitCheck();

        CheckResult result = check.run(context);

        assertEquals("system.git", result.id());
        assertEquals(CheckStatus.FAIL, result.status());
        assertEquals("Git not found", result.summary());
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
