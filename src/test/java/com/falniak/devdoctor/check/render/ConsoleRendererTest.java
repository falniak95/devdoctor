package com.falniak.devdoctor.check.render;

import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckStatus;
import com.falniak.devdoctor.check.Risk;
import com.falniak.devdoctor.check.Suggestion;
import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleRendererTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String getOutput() {
        return outputStream.toString();
    }

    @Test
    void testDefaultOutputHidesNotApplicable() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available (21.0.8)", null, List.of()),
            new CheckResult("project.java", CheckStatus.NOT_APPLICABLE, "Not applicable (no Java project detected)", null, List.of()),
            new CheckResult("project.node", CheckStatus.NOT_APPLICABLE, "Not applicable (no Node.js project detected)", null, List.of())
        );

        ConsoleRenderer renderer = new ConsoleRenderer(false, false);
        renderer.render(detectionResult, results);

        String output = getOutput();
        assertTrue(output.contains("system.java"));
        assertTrue(output.contains("Java is available"));
        assertFalse(output.contains("project.java"));
        assertFalse(output.contains("project.node"));
        assertFalse(output.contains("NA=")); // NA is no longer shown in summary
    }

    @Test
    void testShowNaIncludesNotApplicable() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available (21.0.8)", null, List.of()),
            new CheckResult("project.java", CheckStatus.NOT_APPLICABLE, "Not applicable (no Java project detected)", null, List.of())
        );

        ConsoleRenderer renderer = new ConsoleRenderer(true, false);
        renderer.render(detectionResult, results);

        String output = getOutput();
        assertTrue(output.contains("system.java"));
        assertTrue(output.contains("project.java"));
        assertTrue(output.contains("Not applicable"));
    }

    @Test
    void testVerboseShowsDetailsAndSuggestions() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult(
                "system.git",
                CheckStatus.FAIL,
                "Git not found",
                "Git command returned non-zero exit code: 1",
                List.of(new Suggestion(
                    "Install Git from https://git-scm.com/downloads",
                    List.of("choco install git", "brew install git"),
                    Risk.SAFE
                ))
            )
        );

        ConsoleRenderer renderer = new ConsoleRenderer(false, true);
        renderer.render(detectionResult, results);

        String output = getOutput();
        assertTrue(output.contains("system.git"));
        assertTrue(output.contains("Git not found"));
        assertTrue(output.contains("Git command returned non-zero exit code: 1"));
        assertTrue(output.contains("> choco install git"));
        assertTrue(output.contains("> brew install git"));
        assertTrue(output.contains("Install Git from https://git-scm.com/downloads"));
    }

    @Test
    void testNonVerboseHidesDetailsAndSuggestions() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult(
                "system.git",
                CheckStatus.FAIL,
                "Git not found",
                "Git command returned non-zero exit code: 1",
                List.of(new Suggestion(
                    "Install Git",
                    List.of("choco install git"),
                    Risk.SAFE
                ))
            )
        );

        ConsoleRenderer renderer = new ConsoleRenderer(false, false);
        renderer.render(detectionResult, results);

        String output = getOutput();
        assertTrue(output.contains("system.git"));
        assertTrue(output.contains("Git not found"));
        assertFalse(output.contains("Git command returned non-zero exit code"));
        assertFalse(output.contains("> choco install git"));
        assertFalse(output.contains("Install Git"));
    }

    @Test
    void testSummaryCountsAreCorrect() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of()),
            new CheckResult("system.git", CheckStatus.PASS, "Git is available", null, List.of()),
            new CheckResult("system.node", CheckStatus.WARN, "Node.js version is old", null, List.of()),
            new CheckResult("system.docker", CheckStatus.FAIL, "Docker not found", null, List.of()),
            new CheckResult("project.java", CheckStatus.INFO, "Java project detected", null, List.of()),
            new CheckResult("project.node", CheckStatus.NOT_APPLICABLE, "Not applicable", null, List.of())
        );

        ConsoleRenderer renderer = new ConsoleRenderer(false, false);
        renderer.render(detectionResult, results);

        String output = getOutput();
        assertTrue(output.contains("PASS=2"));
        assertTrue(output.contains("WARN=1"));
        assertTrue(output.contains("FAIL=1"));
        assertTrue(output.contains("INFO=1"));
        assertFalse(output.contains("NA=")); // NA is no longer shown in summary
    }

    @Test
    void testGroupingSystemAndProjectChecks() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of()),
            new CheckResult("system.git", CheckStatus.PASS, "Git is available", null, List.of()),
            new CheckResult("project.java", CheckStatus.NOT_APPLICABLE, "Not applicable", null, List.of())
        );

        ConsoleRenderer renderer = new ConsoleRenderer(true, false);
        renderer.render(detectionResult, results);

        String output = getOutput();
        // Check that "System checks" section appears before "Project checks"
        int systemIndex = output.indexOf("System checks");
        int projectIndex = output.indexOf("Project checks");
        assertTrue(systemIndex >= 0);
        assertTrue(projectIndex >= 0);
        assertTrue(systemIndex < projectIndex);
        
        // Verify system checks appear in system section
        int javaIndex = output.indexOf("system.java");
        int gitIndex = output.indexOf("system.git");
        assertTrue(javaIndex > systemIndex && javaIndex < projectIndex);
        assertTrue(gitIndex > systemIndex && gitIndex < projectIndex);
        
        // Verify project checks appear in project section
        int projectJavaIndex = output.indexOf("project.java");
        assertTrue(projectJavaIndex > projectIndex);
    }

    @Test
    void testNextStepsMessageWhenFailExists() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of()),
            new CheckResult("system.git", CheckStatus.FAIL, "Git not found", null, List.of())
        );

        ConsoleRenderer renderer = new ConsoleRenderer(false, false);
        renderer.render(detectionResult, results);

        String output = getOutput();
        assertTrue(output.contains("Next steps: re-run with --verbose to see details and suggestions."));
    }

    @Test
    void testNoNextStepsMessageWhenNoFail() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of()),
            new CheckResult("system.git", CheckStatus.PASS, "Git is available", null, List.of())
        );

        ConsoleRenderer renderer = new ConsoleRenderer(false, false);
        renderer.render(detectionResult, results);

        String output = getOutput();
        assertFalse(output.contains("Next steps"));
    }

    @Test
    void testNoNextStepsWhenVerbose() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of()),
            new CheckResult("system.git", CheckStatus.FAIL, "Git not found", null, List.of())
        );

        ConsoleRenderer renderer = new ConsoleRenderer(false, true);
        renderer.render(detectionResult, results);

        String output = getOutput();
        assertFalse(output.contains("Next steps"));
    }

    @Test
    void testNextStepsWithWarnOnly() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of()),
            new CheckResult("system.git", CheckStatus.WARN, "Git version is old", null, List.of())
        );

        ConsoleRenderer renderer = new ConsoleRenderer(false, false);
        renderer.render(detectionResult, results);

        String output = getOutput();
        assertTrue(output.contains("Next steps: re-run with --verbose to see recommendations."));
    }

    @Test
    void testNoNextStepsWhenWarnAndFailButVerbose() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of()),
            new CheckResult("system.git", CheckStatus.WARN, "Git version is old", null, List.of()),
            new CheckResult("system.docker", CheckStatus.FAIL, "Docker not found", null, List.of())
        );

        ConsoleRenderer renderer = new ConsoleRenderer(false, true);
        renderer.render(detectionResult, results);

        String output = getOutput();
        assertFalse(output.contains("Next steps"));
    }

    @Test
    void testHeaderWithProjectRootAndTypes() {
        Set<ProjectType> types = EnumSet.of(ProjectType.JAVA_MAVEN, ProjectType.NODE);
        DetectionResult detectionResult = createDetectionResult(types);
        List<CheckResult> results = List.of();

        ConsoleRenderer renderer = new ConsoleRenderer(false, false);
        renderer.render(detectionResult, results);

        String output = getOutput();
        assertTrue(output.contains("Project root: " + tempDir));
        assertTrue(output.contains("Detected project types:"));
        assertTrue(output.contains("Java (Maven)"));
        assertTrue(output.contains("Node.js"));
    }

    @Test
    void testHeaderWithNoTypes() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of();

        ConsoleRenderer renderer = new ConsoleRenderer(false, false);
        renderer.render(detectionResult, results);

        String output = getOutput();
        assertTrue(output.contains("Project root: " + tempDir));
        assertTrue(output.contains("Detected project types:"));
        assertTrue(output.contains("  None"));
    }

    @Test
    void testOneLineFormatPerCheck() {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available (21.0.8)", null, List.of()),
            new CheckResult("system.git", CheckStatus.WARN, "Git version is old", null, List.of())
        );

        ConsoleRenderer renderer = new ConsoleRenderer(false, false);
        renderer.render(detectionResult, results);

        String output = getOutput();
        // Check format: [STATUS] id  summary
        assertTrue(output.contains("[PASS] system.java  Java is available (21.0.8)"));
        assertTrue(output.contains("[WARN] system.git  Git version is old"));
    }

    private DetectionResult createDetectionResult(Set<ProjectType> types) {
        return new DetectionResult(tempDir, types, List.of());
    }
}
