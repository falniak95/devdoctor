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

class PythonRequirementCheckTest {

    @TempDir
    Path tempDir;

    @Test
    void testNotApplicableWhenNoPythonProject() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor, EnumSet.noneOf(ProjectType.class));
        PythonRequirementCheck check = new PythonRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.python.requirements", result.id());
        assertEquals(CheckStatus.NOT_APPLICABLE, result.status());
    }

    @Test
    void testInfoWhenNoRequirementFound() throws Exception {
        Files.createFile(tempDir.resolve("requirements.txt"));

        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.PYTHON_REQUIREMENTS));
        PythonRequirementCheck check = new PythonRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.python.requirements", result.id());
        assertEquals(CheckStatus.INFO, result.status());
        assertTrue(result.summary().contains("No Python version requirement specified"));
    }

    @Test
    void testPassWhenRequirementMatches() throws Exception {
        Files.writeString(tempDir.resolve(".python-version"), "3.11");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("python", new ExecResult(0, "", "Python 3.11.0"));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.PYTHON_PYPROJECT));
        PythonRequirementCheck check = new PythonRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.python.requirements", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertTrue(result.summary().contains("Required: 3.11"));
        assertTrue(result.summary().contains("Local: 3.11.0"));
    }

    @Test
    void testFailWhenRequirementMismatches() throws Exception {
        Files.writeString(tempDir.resolve(".python-version"), "3.11");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("python", new Exception("not found"));
        executor.setResult("python3", new ExecResult(0, "", "Python 3.9.0"));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.PYTHON_PYPROJECT));
        PythonRequirementCheck check = new PythonRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.python.requirements", result.id());
        assertEquals(CheckStatus.FAIL, result.status());
        assertTrue(result.summary().contains("Required: 3.11"));
        assertTrue(result.summary().contains("Local: 3.9.0"));
        assertFalse(result.suggestions().isEmpty());
    }

    @Test
    void testWarnWhenPythonNotFound() throws Exception {
        Files.writeString(tempDir.resolve(".python-version"), "3.11");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("python", new Exception("not found"));
        executor.setException("python3", new Exception("not found"));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.PYTHON_PYPROJECT));
        PythonRequirementCheck check = new PythonRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.python.requirements", result.id());
        assertEquals(CheckStatus.WARN, result.status());
        assertTrue(result.summary().contains("Python not found"));
    }

    @Test
    void testReadsFromPyprojectToml() throws Exception {
        Files.writeString(tempDir.resolve("pyproject.toml"), 
            "[project]\nrequires-python = \">=3.9\"");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("python", new ExecResult(0, "", "Python 3.11.0"));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.PYTHON_PYPROJECT));
        PythonRequirementCheck check = new PythonRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.python.requirements", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertTrue(result.summary().contains("pyproject.toml"));
    }

    private CheckContext createTestContext(FakeProcessExecutor executor, Set<ProjectType> types) {
        DetectionResult detectionResult = new DetectionResult(tempDir, types, List.of());
        return new CheckContext(tempDir, tempDir, types, detectionResult, executor);
    }
}
