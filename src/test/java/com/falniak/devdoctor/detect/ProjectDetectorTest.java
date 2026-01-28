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

    // Python detection tests
    @Test
    void testDetectPythonPyproject() throws Exception {
        Files.createFile(tempDir.resolve("pyproject.toml"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.PYTHON_PYPROJECT));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("pyproject.toml"));
    }

    @Test
    void testDetectPythonRequirements() throws Exception {
        Files.createFile(tempDir.resolve("requirements.txt"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.PYTHON_REQUIREMENTS));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("requirements.txt"));
    }

    @Test
    void testDetectPythonPipenv() throws Exception {
        Files.createFile(tempDir.resolve("Pipfile"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.PYTHON_PIPENV));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("Pipfile"));
    }

    @Test
    void testDetectPythonSetupPy() throws Exception {
        Files.createFile(tempDir.resolve("setup.py"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.PYTHON_SETUPPY));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("setup.py"));
    }

    @Test
    void testDetectMultiplePythonMarkers() throws Exception {
        Files.createFile(tempDir.resolve("pyproject.toml"));
        Files.createFile(tempDir.resolve("requirements.txt"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        Set<ProjectType> types = result.types();
        assertTrue(types.contains(ProjectType.PYTHON_PYPROJECT));
        assertTrue(types.contains(ProjectType.PYTHON_REQUIREMENTS));
        assertEquals(2, types.size());
        assertTrue(result.markersFound().contains("pyproject.toml"));
        assertTrue(result.markersFound().contains("requirements.txt"));
        assertEquals(2, result.markersFound().size());
    }

    // Go detection tests
    @Test
    void testDetectGoModules() throws Exception {
        Files.createFile(tempDir.resolve("go.mod"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.GO_MODULES));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("go.mod"));
    }

    // Rust detection tests
    @Test
    void testDetectRustCargo() throws Exception {
        Files.createFile(tempDir.resolve("Cargo.toml"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.RUST_CARGO));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("Cargo.toml"));
    }

    // .NET detection tests
    @Test
    void testDetectDotNetSolution() throws Exception {
        Files.createFile(tempDir.resolve("MyProject.sln"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.DOTNET_SOLUTION));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("MyProject.sln"));
    }

    @Test
    void testDetectDotNetCSharpProject() throws Exception {
        Files.createFile(tempDir.resolve("App.csproj"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.DOTNET_CSHARP_PROJECT));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("App.csproj"));
    }

    @Test
    void testDetectDotNetFSharpProject() throws Exception {
        Files.createFile(tempDir.resolve("Program.fsproj"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        assertTrue(result.types().contains(ProjectType.DOTNET_FSHARP_PROJECT));
        assertEquals(1, result.types().size());
        assertTrue(result.markersFound().contains("Program.fsproj"));
    }

    @Test
    void testDetectMultipleDotNetTypes() throws Exception {
        Files.createFile(tempDir.resolve("Solution.sln"));
        Files.createFile(tempDir.resolve("App.csproj"));
        Files.createFile(tempDir.resolve("Lib.fsproj"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        Set<ProjectType> types = result.types();
        assertTrue(types.contains(ProjectType.DOTNET_SOLUTION));
        assertTrue(types.contains(ProjectType.DOTNET_CSHARP_PROJECT));
        assertTrue(types.contains(ProjectType.DOTNET_FSHARP_PROJECT));
        assertEquals(3, types.size());
        assertTrue(result.markersFound().contains("Solution.sln"));
        assertTrue(result.markersFound().contains("App.csproj"));
        assertTrue(result.markersFound().contains("Lib.fsproj"));
        assertEquals(3, result.markersFound().size());
    }

    // Multi-type detection tests
    @Test
    void testDetectPythonAndDockerCompose() throws Exception {
        Files.createFile(tempDir.resolve("pyproject.toml"));
        Files.createFile(tempDir.resolve("docker-compose.yml"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        Set<ProjectType> types = result.types();
        assertTrue(types.contains(ProjectType.PYTHON_PYPROJECT));
        assertTrue(types.contains(ProjectType.DOCKER_COMPOSE));
        assertEquals(2, types.size());
        assertEquals(2, result.markersFound().size());
    }

    @Test
    void testDetectGoAndRust() throws Exception {
        Files.createFile(tempDir.resolve("go.mod"));
        Files.createFile(tempDir.resolve("Cargo.toml"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        Set<ProjectType> types = result.types();
        assertTrue(types.contains(ProjectType.GO_MODULES));
        assertTrue(types.contains(ProjectType.RUST_CARGO));
        assertEquals(2, types.size());
        assertEquals(2, result.markersFound().size());
    }

    @Test
    void testDetectPythonAndNode() throws Exception {
        Files.createFile(tempDir.resolve("requirements.txt"));
        Files.createFile(tempDir.resolve("package.json"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        Set<ProjectType> types = result.types();
        assertTrue(types.contains(ProjectType.PYTHON_REQUIREMENTS));
        assertTrue(types.contains(ProjectType.NODE));
        assertEquals(2, types.size());
        assertEquals(2, result.markersFound().size());
    }

    @Test
    void testDetectAllNewTypes() throws Exception {
        Files.createFile(tempDir.resolve("pyproject.toml"));
        Files.createFile(tempDir.resolve("go.mod"));
        Files.createFile(tempDir.resolve("Cargo.toml"));
        Files.createFile(tempDir.resolve("Solution.sln"));

        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(tempDir);

        Set<ProjectType> types = result.types();
        assertTrue(types.contains(ProjectType.PYTHON_PYPROJECT));
        assertTrue(types.contains(ProjectType.GO_MODULES));
        assertTrue(types.contains(ProjectType.RUST_CARGO));
        assertTrue(types.contains(ProjectType.DOTNET_SOLUTION));
        assertEquals(4, types.size());
        assertEquals(4, result.markersFound().size());
    }

    // Walk-up root detection tests for new markers
    @Test
    void testFindRootWithPythonMarker() throws Exception {
        Path parent = tempDir.resolve("parent");
        Path child = parent.resolve("child");
        Files.createDirectories(child);
        Files.createFile(parent.resolve("pyproject.toml"));

        ProjectDetector detector = new ProjectDetector();
        Path root = detector.findProjectRoot(child);

        assertEquals(parent, root);
    }

    @Test
    void testFindRootWithGoMarker() throws Exception {
        Path parent = tempDir.resolve("parent");
        Path child = parent.resolve("child");
        Files.createDirectories(child);
        Files.createFile(parent.resolve("go.mod"));

        ProjectDetector detector = new ProjectDetector();
        Path root = detector.findProjectRoot(child);

        assertEquals(parent, root);
    }

    @Test
    void testFindRootWithDotNetMarker() throws Exception {
        Path parent = tempDir.resolve("parent");
        Path child = parent.resolve("child");
        Files.createDirectories(child);
        Files.createFile(parent.resolve("MyProject.sln"));

        ProjectDetector detector = new ProjectDetector();
        Path root = detector.findProjectRoot(child);

        assertEquals(parent, root);
    }
}
