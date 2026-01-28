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

class JavaRequirementCheckTest {

    @TempDir
    Path tempDir;

    @Test
    void testNotApplicableWhenNoJavaProject() {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor, EnumSet.noneOf(ProjectType.class));
        JavaRequirementCheck check = new JavaRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.java.requirements", result.id());
        assertEquals(CheckStatus.NOT_APPLICABLE, result.status());
    }

    @Test
    void testInfoWhenNoRequirementFound() throws Exception {
        Files.createFile(tempDir.resolve("pom.xml"));
        Files.writeString(tempDir.resolve("pom.xml"), 
            "<?xml version=\"1.0\"?><project></project>");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.JAVA_MAVEN));
        JavaRequirementCheck check = new JavaRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.java.requirements", result.id());
        assertEquals(CheckStatus.INFO, result.status());
        assertTrue(result.summary().contains("No Java version requirement specified"));
    }

    @Test
    void testPassWhenRequirementMatches() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), 
            "<?xml version=\"1.0\"?>\n" +
            "<project>\n" +
            "  <properties>\n" +
            "    <maven.compiler.release>17</maven.compiler.release>\n" +
            "  </properties>\n" +
            "</project>");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("java", new ExecResult(0, "", "openjdk version \"17.0.1\" 2021-10-19"));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.JAVA_MAVEN));
        JavaRequirementCheck check = new JavaRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.java.requirements", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertTrue(result.summary().contains("Required: 17"));
        assertTrue(result.summary().contains("Local: 17.0.1"));
    }

    @Test
    void testFailWhenLocalIsLowerThanRequired() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), 
            "<?xml version=\"1.0\"?>\n" +
            "<project>\n" +
            "  <properties>\n" +
            "    <maven.compiler.release>21</maven.compiler.release>\n" +
            "  </properties>\n" +
            "</project>");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("java", new ExecResult(0, "", "openjdk version \"17.0.1\" 2021-10-19"));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.JAVA_MAVEN));
        JavaRequirementCheck check = new JavaRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.java.requirements", result.id());
        assertEquals(CheckStatus.FAIL, result.status());
        assertTrue(result.summary().contains("Required: 21"));
        assertTrue(result.summary().contains("Local: 17.0.1"));
        assertFalse(result.suggestions().isEmpty());
    }

    @Test
    void testPassWhenLocalIsHigherThanRequired() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), 
            "<?xml version=\"1.0\"?>\n" +
            "<project>\n" +
            "  <properties>\n" +
            "    <maven.compiler.release>17</maven.compiler.release>\n" +
            "  </properties>\n" +
            "</project>");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("java", new ExecResult(0, "", "openjdk version \"21.0.8\" 2024-10-15"));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.JAVA_MAVEN));
        JavaRequirementCheck check = new JavaRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.java.requirements", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertTrue(result.summary().contains("Required: 17"));
        assertTrue(result.summary().contains("Local: 21.0.8"));
    }

    @Test
    void testWarnWhenJavaNotFound() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), 
            "<?xml version=\"1.0\"?>\n" +
            "<project>\n" +
            "  <properties>\n" +
            "    <maven.compiler.release>17</maven.compiler.release>\n" +
            "  </properties>\n" +
            "</project>");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("java", new Exception("java: command not found"));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.JAVA_MAVEN));
        JavaRequirementCheck check = new JavaRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.java.requirements", result.id());
        assertEquals(CheckStatus.WARN, result.status());
        assertTrue(result.summary().contains("Java not found"));
    }

    @Test
    void testReadsFromJavaVersionProperty() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), 
            "<?xml version=\"1.0\"?>\n" +
            "<project>\n" +
            "  <properties>\n" +
            "    <java.version>17</java.version>\n" +
            "  </properties>\n" +
            "</project>");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("java", new ExecResult(0, "", "openjdk version \"17.0.1\" 2021-10-19"));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.JAVA_MAVEN));
        JavaRequirementCheck check = new JavaRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.java.requirements", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertTrue(result.summary().contains("Required: 17"));
    }

    @Test
    void testReadsFromGradle() throws Exception {
        Files.writeString(tempDir.resolve("build.gradle"), 
            "sourceCompatibility = JavaVersion.VERSION_17\n");

        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("java", new ExecResult(0, "", "openjdk version \"17.0.1\" 2021-10-19"));

        CheckContext context = createTestContext(executor, EnumSet.of(ProjectType.JAVA_GRADLE));
        JavaRequirementCheck check = new JavaRequirementCheck();

        CheckResult result = check.run(context);

        assertEquals("project.java.requirements", result.id());
        assertEquals(CheckStatus.PASS, result.status());
        assertTrue(result.summary().contains("Required: 17"));
        assertTrue(result.summary().contains("build.gradle"));
    }

    private CheckContext createTestContext(FakeProcessExecutor executor, Set<ProjectType> types) {
        DetectionResult detectionResult = new DetectionResult(tempDir, types, List.of());
        return new CheckContext(tempDir, tempDir, types, detectionResult, executor);
    }
}
