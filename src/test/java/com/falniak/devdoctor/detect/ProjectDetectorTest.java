package com.falniak.devdoctor.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProjectDetectorTest {

    @TempDir
    Path tempDir;

    @Test
    void testFindRootWithMarkerInParent() throws Exception {
        // Create structure: parent/pom.xml, parent/child/grandchild/
        Path parent = tempDir.resolve("parent");
        Path child = parent.resolve("child");
        Path grandchild = child.resolve("grandchild");
        Files.createDirectories(grandchild);
        Files.createFile(parent.resolve("pom.xml"));

        ProjectDetector detector = new ProjectDetector();
        Path root = detector.findProjectRoot(grandchild);

        assertEquals(parent, root);
    }

    @Test
    void testDetectMaven() throws Exception {
        Files.createFile(tempDir.resolve("pom.xml"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.JAVA_MAVEN));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("pom.xml"));
    }

    @Test
    void testDetectGradle() throws Exception {
        Files.createFile(tempDir.resolve("build.gradle"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.JAVA_GRADLE));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("build.gradle"));
    }

    @Test
    void testDetectGradleKotlin() throws Exception {
        Files.createFile(tempDir.resolve("build.gradle.kts"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.JAVA_GRADLE));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("build.gradle.kts"));
    }

    @Test
    void testDetectNode() throws Exception {
        Files.createFile(tempDir.resolve("package.json"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.NODE));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("package.json"));
    }

    @Test
    void testDetectDockerCompose() throws Exception {
        Files.createFile(tempDir.resolve("docker-compose.yml"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.DOCKER_COMPOSE));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("docker-compose.yml"));
    }

    @Test
    void testDetectDockerComposeYml() throws Exception {
        Files.createFile(tempDir.resolve("compose.yml"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.DOCKER_COMPOSE));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("compose.yml"));
    }

    @Test
    void testDetectDockerComposeYaml() throws Exception {
        Files.createFile(tempDir.resolve("compose.yaml"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.DOCKER_COMPOSE));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("compose.yaml"));
    }

    @Test
    void testDetectMultipleTypes() throws Exception {
        Files.createFile(tempDir.resolve("pom.xml"));
        Files.createFile(tempDir.resolve("package.json"));
        Files.createFile(tempDir.resolve("docker-compose.yml"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        Set<ProjectType> types = result.types();
        assertTrue(types.contains(ProjectType.JAVA_MAVEN));
        assertTrue(types.contains(ProjectType.NODE));
        assertTrue(types.contains(ProjectType.DOCKER_COMPOSE));
        assertEquals(3, types.size());
        assertEquals(3, result.markersFound().size());
    }

    @Test
    void testNoMarkersReturnsStartDir() throws Exception {
        // Create a nested directory structure without any markers
        Path nested = tempDir.resolve("nested").resolve("deep");
        Files.createDirectories(nested);

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(nested);

        // Root should be the start directory (normalized)
        assertEquals(nested.toAbsolutePath().normalize(), result.root());
        assertTrue(result.types().isEmpty());
        assertTrue(result.markersFound().isEmpty());
    }

    @Test
    void testDetectTypesInCurrentDir() throws Exception {
        Files.createFile(tempDir.resolve("pom.xml"));
        Files.createFile(tempDir.resolve("package.json"));

        ProjectDetector detector = new ProjectDetector();
        Set<ProjectType> types = detector.detectTypes(tempDir);

        assertTrue(types.contains(ProjectType.JAVA_MAVEN));
        assertTrue(types.contains(ProjectType.NODE));
        assertEquals(2, types.size());
    }

    @Test
    void testFindProjectRootReturnsStartWhenNoMarkers() throws Exception {
        Path start = tempDir.resolve("some").resolve("nested").resolve("path");
        Files.createDirectories(start);

        ProjectDetector detector = new ProjectDetector();
        Path root = detector.findProjectRoot(start);

        assertEquals(start.toAbsolutePath().normalize(), root);
    }

    @Test
    void testGradleDetectsBothVariants() throws Exception {
        Files.createFile(tempDir.resolve("build.gradle"));
        Files.createFile(tempDir.resolve("build.gradle.kts"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.JAVA_GRADLE));
        assertEquals(1, result.types().size()); // Should only detect once
        assertTrue(result.markersFound().contains("build.gradle"));
        assertTrue(result.markersFound().contains("build.gradle.kts"));
        assertEquals(2, result.markersFound().size());
    }
}
