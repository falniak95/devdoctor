package com.falniak.devdoctor.check.render;

import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckStatus;
import com.falniak.devdoctor.check.Risk;
import com.falniak.devdoctor.check.Suggestion;
import com.falniak.devdoctor.config.DevDoctorConfig;
import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectType;
import com.falniak.devdoctor.report.CheckReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JsonRendererTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String getOutput() {
        return outputStream.toString().trim();
    }

    @Test
    void testValidJsonOutput() throws Exception {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of())
        );

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.empty(), null, false);

        String json = getOutput();
        // Should be parseable JSON
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        assertNotNull(report);
    }

    @Test
    void testRequiredKeysExist() throws Exception {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of())
        );

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.empty(), null, false);

        String json = getOutput();
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        
        assertNotNull(report.tool(), "tool should exist");
        assertNotNull(report.project(), "project should exist");
        assertNotNull(report.summary(), "summary should exist");
        assertNotNull(report.checks(), "checks should exist");
        // config can be null
    }

    @Test
    void testSummaryCountsMatch() throws Exception {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of()),
            new CheckResult("system.git", CheckStatus.PASS, "Git is available", null, List.of()),
            new CheckResult("system.node", CheckStatus.WARN, "Node.js version is old", null, List.of()),
            new CheckResult("system.docker", CheckStatus.FAIL, "Docker not found", null, List.of()),
            new CheckResult("project.java", CheckStatus.INFO, "Java project detected", null, List.of()),
            new CheckResult("project.node", CheckStatus.NOT_APPLICABLE, "Not applicable", null, List.of())
        );

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.empty(), null, false);

        String json = getOutput();
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        
        assertEquals(2, report.summary().pass(), "PASS count should be 2");
        assertEquals(1, report.summary().warn(), "WARN count should be 1");
        assertEquals(1, report.summary().fail(), "FAIL count should be 1");
        assertEquals(1, report.summary().info(), "INFO count should be 1");
        assertEquals(1, report.summary().notApplicable(), "NOT_APPLICABLE count should be 1");
    }

    @Test
    void testProjectTypesUseDisplayNames() throws Exception {
        Set<ProjectType> types = EnumSet.of(ProjectType.JAVA_MAVEN, ProjectType.NODE);
        DetectionResult detectionResult = createDetectionResult(types);
        List<CheckResult> results = List.of();

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.empty(), null, false);

        String json = getOutput();
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        
        List<String> projectTypes = report.project().types();
        assertEquals(2, projectTypes.size());
        assertTrue(projectTypes.contains("Java (Maven)"));
        assertTrue(projectTypes.contains("Node.js"));
    }

    @Test
    void testStatusValuesAreEnumNames() throws Exception {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of()),
            new CheckResult("system.git", CheckStatus.WARN, "Git version is old", null, List.of()),
            new CheckResult("system.docker", CheckStatus.FAIL, "Docker not found", null, List.of()),
            new CheckResult("project.java", CheckStatus.INFO, "Java project detected", null, List.of()),
            new CheckResult("project.node", CheckStatus.NOT_APPLICABLE, "Not applicable", null, List.of())
        );

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.empty(), null, false);

        String json = getOutput();
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        
        assertEquals("PASS", report.checks().get(0).status());
        assertEquals("WARN", report.checks().get(1).status());
        assertEquals("FAIL", report.checks().get(2).status());
        assertEquals("INFO", report.checks().get(3).status());
        assertEquals("NOT_APPLICABLE", report.checks().get(4).status());
    }

    @Test
    void testAllChecksIncludedIncludingNotApplicable() throws Exception {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of()),
            new CheckResult("project.java", CheckStatus.NOT_APPLICABLE, "Not applicable", null, List.of()),
            new CheckResult("project.node", CheckStatus.NOT_APPLICABLE, "Not applicable", null, List.of())
        );

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.empty(), null, false);

        String json = getOutput();
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        
        assertEquals(3, report.checks().size(), "All checks should be included");
        assertTrue(report.checks().stream().anyMatch(c -> c.id().equals("system.java")));
        assertTrue(report.checks().stream().anyMatch(c -> c.id().equals("project.java")));
        assertTrue(report.checks().stream().anyMatch(c -> c.id().equals("project.node")));
    }

    @Test
    void testPrettyPrintingWorks() throws Exception {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of())
        );

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.empty(), null, true);

        String json = getOutput();
        // Pretty printed JSON should contain newlines
        assertTrue(json.contains("\n"), "Pretty printed JSON should contain newlines");
        
        // Should still be parseable
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        assertNotNull(report);
    }

    @Test
    void testConfigInfoIncludedWhenPresent() throws Exception {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of();
        DevDoctorConfig config = new DevDoctorConfig(
            Set.of("system.docker"),
            Set.of("system.java"),
            List.of()
        );
        Path configPath = tempDir.resolve(".devdoctor.yml");

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.of(config), configPath, false);

        String json = getOutput();
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        
        assertNotNull(report.config(), "config should exist when provided");
        assertEquals(configPath.toString(), report.config().path());
        assertEquals(Set.of("system.docker"), report.config().ignoreChecks());
        assertEquals(Set.of("system.java"), report.config().requireChecks());
    }

    @Test
    void testConfigInfoIsNullWhenNotPresent() throws Exception {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of();

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.empty(), null, false);

        String json = getOutput();
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        
        assertNull(report.config(), "config should be null when not provided");
    }

    @Test
    void testToolInfo() throws Exception {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of();

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.empty(), null, false);

        String json = getOutput();
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        
        assertEquals("devdoctor", report.tool().name());
        assertEquals("1.0-SNAPSHOT", report.tool().version());
    }

    @Test
    void testSuggestionsIncluded() throws Exception {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult(
                "system.git",
                CheckStatus.FAIL,
                "Git not found",
                "Details here",
                List.of(
                    new Suggestion(
                        "Install Git",
                        List.of("choco install git", "brew install git"),
                        Risk.SAFE
                    ),
                    new Suggestion(
                        "Alternative method",
                        List.of(),
                        Risk.CAUTION
                    )
                )
            )
        );

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.empty(), null, false);

        String json = getOutput();
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        
        var check = report.checks().get(0);
        assertNotNull(check.suggestions());
        assertEquals(2, check.suggestions().size());
        assertEquals("Install Git", check.suggestions().get(0).message());
        assertEquals(List.of("choco install git", "brew install git"), check.suggestions().get(0).commands());
        assertEquals("SAFE", check.suggestions().get(0).risk());
        assertEquals("Alternative method", check.suggestions().get(1).message());
        assertEquals("CAUTION", check.suggestions().get(1).risk());
    }

    @Test
    void testDetailsIncluded() throws Exception {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult(
                "system.git",
                CheckStatus.FAIL,
                "Git not found",
                "Git command returned non-zero exit code: 1",
                List.of()
            )
        );

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.empty(), null, false);

        String json = getOutput();
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        
        var check = report.checks().get(0);
        assertEquals("Git command returned non-zero exit code: 1", check.details());
    }

    @Test
    void testNullDetailsAndSuggestionsOmitted() throws Exception {
        DetectionResult detectionResult = createDetectionResult(EnumSet.noneOf(ProjectType.class));
        List<CheckResult> results = List.of(
            new CheckResult("system.java", CheckStatus.PASS, "Java is available", null, List.of())
        );

        JsonRenderer renderer = new JsonRenderer();
        renderer.render(detectionResult, results, Optional.empty(), null, false);

        String json = getOutput();
        CheckReport report = objectMapper.readValue(json, CheckReport.class);
        
        var check = report.checks().get(0);
        assertNull(check.details());
        assertNull(check.suggestions());
    }

    private DetectionResult createDetectionResult(Set<ProjectType> types) {
        return new DetectionResult(tempDir, types, List.of());
    }
}
