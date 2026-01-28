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

class NodeRequirementCheckTest {

    @TempDir
    Path tempDir;

    @Test
    void testNotApplicableWhenNoNodeProject() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor, EnumSet.noneOf(ProjectType.class));
        NodeRequirementCheck check = new NodeRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.node.requirements", result.id());
        assertEquals(CheckStatus.NOT_APPLICABLE, result.status());
    }

    @Test
    void testInfoWhenNoRequirementFound() throws Exception {
        Files.createFile(tempDir.resolve("package.json"));
        Files.writeString(tempDir.resolve("package.json"), "{}");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.NODE));
        NodeRequirementCheck check = new NodeRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.node.requirements", result.id());
        assertEquals(CheckStatus.INFO, result.status());
        assertTrue(result.summary().contains("No Node.js version requirement specified"));
    }

    @Test
    void testPassWhenRequirementMatches() throws Exception {
        Files.writeString(tempDir.resolve(".nvmrc"), "18");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("node", new ExecResult(0, "v18.19.0\n", ""));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.NODE));
        NodeRequirementCheck check = new NodeRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.node.requirements", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertTrue(result.summary().contains("Required: 18"));
        assertTrue(result.summary().contains("Local: v18.19.0"));
    }

    @Test
    void testFailWhenRequirementMismatches() throws Exception {
        Files.writeString(tempDir.resolve(".nvmrc"), "20");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("node", new ExecResult(0, "v18.19.0\n", ""));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.NODE));
        NodeRequirementCheck check = new NodeRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.node.requirements", result.id());
        assertEquals(CheckStatus.FAIL, result.status());
        assertTrue(result.summary().contains("Required: 20"));
        assertTrue(result.summary().contains("Local: v18.19.0"));
        assertFalse(result.suggestions().isEmpty());
    }

    @Test
    void testWarnWhenNodeNotFound() throws Exception {
        Files.writeString(tempDir.resolve(".nvmrc"), "18");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("node", new Exception("node: command not found"));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.NODE));
        NodeRequirementCheck check = new NodeRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.node.requirements", result.id());
        assertEquals(CheckStatus.WARN, result.status());
        assertTrue(result.summary().contains("Node.js not found"));
    }

    @Test
    void testWarnWhenUnparseableRequirement() throws Exception {
        Files.writeString(tempDir.resolve(".nvmrc"), "invalid-version");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("node", new ExecResult(0, "v18.19.0\n", ""));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.NODE));
        NodeRequirementCheck check = new NodeRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.node.requirements", result.id());
        assertEquals(CheckStatus.WARN, result.status());
        assertTrue(result.summary().contains("not recognized"));
    }

    @Test
    void testReadsFromPackageJson() throws Exception {
        Files.writeString(tempDir.resolve("package.json"), 
            "{\"engines\": {\"node\": \">=18\"}}");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("node", new ExecResult(0, "v18.19.0\n", ""));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.NODE));
        NodeRequirementCheck check = new NodeRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.node.requirements", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertTrue(result.summary().contains("package.json"));
    }

    private CheckContext createTestContext(FakeProcessExecutor executor, Set<ProjectType> types) {
        DetectionResult detectionResult = new DetectionResult(tempDir, types, List.of());
        return new CheckContext(tempDir, tempDir, types, detectionResult, executor);
    }
}
