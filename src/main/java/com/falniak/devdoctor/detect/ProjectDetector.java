package com.falniak.devdoctor.detect;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Detects project types by scanning the filesystem for marker files.
 */
public class ProjectDetector {

    /**
     * Detects the project root and types starting from the given path.
     *
     * @param start The starting directory path
     * @return DetectionResult containing the root path, detected types, and markers found
     */
    public DetectionResult detect(Path start) {
        Path normalizedStart = start.toAbsolutePath().normalize();
        Path root = findProjectRoot(normalizedStart);
        Set<ProjectType> types = detectTypes(root);
        List<String> markersFound = collectMarkers(root, types);
        
        return new DetectionResult(root, types, markersFound);
    }

    /**
     * Walks up the directory tree to find the project root.
     * A root is defined as a directory containing any known marker file.
     *
     * @param start The starting directory path
     * @return The project root path, or the start path if no markers are found
     */
    public Path findProjectRoot(Path start) {
        Path current = start.toAbsolutePath().normalize();
        Path root = current.getRoot();

        while (current != null && !current.equals(root)) {
            if (hasAnyMarker(current)) {
                return current;
            }
            Path parent = current.getParent();
            if (parent == null || parent.equals(current)) {
                break;
            }
            current = parent;
        }

        // If we reached the filesystem root without finding markers,
        // return the original start directory
        return start.toAbsolutePath().normalize();
    }

    /**
     * Detects all project types present in the given root directory.
     *
     * @param root The root directory to scan
     * @return A set of detected project types
     */
    public Set<ProjectType> detectTypes(Path root) {
        Set<ProjectType> types = EnumSet.noneOf(ProjectType.class);

        if (hasMarker(root, "pom.xml")) {
            types.add(ProjectType.JAVA_MAVEN);
        }

        if (hasMarker(root, "build.gradle") || hasMarker(root, "build.gradle.kts")) {
            types.add(ProjectType.JAVA_GRADLE);
        }

        if (hasMarker(root, "package.json")) {
            types.add(ProjectType.NODE);
        }

        if (hasMarker(root, "docker-compose.yml") 
            || hasMarker(root, "compose.yml") 
            || hasMarker(root, "compose.yaml")) {
            types.add(ProjectType.DOCKER_COMPOSE);
        }

        // Python detection
        if (hasMarker(root, "pyproject.toml")) {
            types.add(ProjectType.PYTHON_PYPROJECT);
        }
        if (hasMarker(root, "requirements.txt")) {
            types.add(ProjectType.PYTHON_REQUIREMENTS);
        }
        if (hasMarker(root, "Pipfile")) {
            types.add(ProjectType.PYTHON_PIPENV);
        }
        if (hasMarker(root, "setup.py")) {
            types.add(ProjectType.PYTHON_SETUPPY);
        }

        // Go detection
        if (hasMarker(root, "go.mod")) {
            types.add(ProjectType.GO_MODULES);
        }

        // Rust detection
        if (hasMarker(root, "Cargo.toml")) {
            types.add(ProjectType.RUST_CARGO);
        }

        // .NET detection
        if (hasDotNetSolution(root)) {
            types.add(ProjectType.DOTNET_SOLUTION);
        }
        if (hasDotNetCSharpProject(root)) {
            types.add(ProjectType.DOTNET_CSHARP_PROJECT);
        }
        if (hasDotNetFSharpProject(root)) {
            types.add(ProjectType.DOTNET_FSHARP_PROJECT);
        }

        return types;
    }

    /**
     * Checks if the given directory contains any known marker file.
     *
     * @param dir The directory to check
     * @return true if any marker file exists
     */
    private boolean hasAnyMarker(Path dir) {
        return hasMarker(dir, "pom.xml")
            || hasMarker(dir, "build.gradle")
            || hasMarker(dir, "build.gradle.kts")
            || hasMarker(dir, "package.json")
            || hasMarker(dir, "docker-compose.yml")
            || hasMarker(dir, "compose.yml")
            || hasMarker(dir, "compose.yaml")
            || hasMarker(dir, "pyproject.toml")
            || hasMarker(dir, "requirements.txt")
            || hasMarker(dir, "Pipfile")
            || hasMarker(dir, "setup.py")
            || hasMarker(dir, "go.mod")
            || hasMarker(dir, "Cargo.toml")
            || hasDotNetSolution(dir)
            || hasDotNetCSharpProject(dir)
            || hasDotNetFSharpProject(dir);
    }

    /**
     * Checks if a specific marker file exists in the given directory.
     *
     * @param dir The directory to check
     * @param marker The marker file name
     * @return true if the marker file exists and is a regular file
     */
    private boolean hasMarker(Path dir, String marker) {
        Path markerPath = dir.resolve(marker);
        return Files.exists(markerPath) && Files.isRegularFile(markerPath);
    }

    /**
     * Collects the list of marker files that were found for the detected types.
     *
     * @param root The root directory
     * @param types The detected project types
     * @return List of marker file names that were found
     */
    private List<String> collectMarkers(Path root, Set<ProjectType> types) {
        List<String> markers = new ArrayList<>();

        if (types.contains(ProjectType.JAVA_MAVEN) && hasMarker(root, "pom.xml")) {
            markers.add("pom.xml");
        }

        if (types.contains(ProjectType.JAVA_GRADLE)) {
            if (hasMarker(root, "build.gradle")) {
                markers.add("build.gradle");
            }
            if (hasMarker(root, "build.gradle.kts")) {
                markers.add("build.gradle.kts");
            }
        }

        if (types.contains(ProjectType.NODE) && hasMarker(root, "package.json")) {
            markers.add("package.json");
        }

        if (types.contains(ProjectType.DOCKER_COMPOSE)) {
            if (hasMarker(root, "docker-compose.yml")) {
                markers.add("docker-compose.yml");
            }
            if (hasMarker(root, "compose.yml")) {
                markers.add("compose.yml");
            }
            if (hasMarker(root, "compose.yaml")) {
                markers.add("compose.yaml");
            }
        }

        // Python markers
        if (types.contains(ProjectType.PYTHON_PYPROJECT) && hasMarker(root, "pyproject.toml")) {
            markers.add("pyproject.toml");
        }
        if (types.contains(ProjectType.PYTHON_REQUIREMENTS) && hasMarker(root, "requirements.txt")) {
            markers.add("requirements.txt");
        }
        if (types.contains(ProjectType.PYTHON_PIPENV) && hasMarker(root, "Pipfile")) {
            markers.add("Pipfile");
        }
        if (types.contains(ProjectType.PYTHON_SETUPPY) && hasMarker(root, "setup.py")) {
            markers.add("setup.py");
        }

        // Go markers
        if (types.contains(ProjectType.GO_MODULES) && hasMarker(root, "go.mod")) {
            markers.add("go.mod");
        }

        // Rust markers
        if (types.contains(ProjectType.RUST_CARGO) && hasMarker(root, "Cargo.toml")) {
            markers.add("Cargo.toml");
        }

        // .NET markers
        if (types.contains(ProjectType.DOTNET_SOLUTION)) {
            markers.addAll(findDotNetSolutions(root));
        }
        if (types.contains(ProjectType.DOTNET_CSHARP_PROJECT)) {
            markers.addAll(findDotNetCSharpProjects(root));
        }
        if (types.contains(ProjectType.DOTNET_FSHARP_PROJECT)) {
            markers.addAll(findDotNetFSharpProjects(root));
        }

        return markers;
    }

    /**
     * Checks if the directory contains any .NET solution files (*.sln).
     *
     * @param dir The directory to check
     * @return true if any .sln file exists
     */
    private boolean hasDotNetSolution(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.sln")) {
            return stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks if the directory contains any C# project files (*.csproj).
     *
     * @param dir The directory to check
     * @return true if any .csproj file exists
     */
    private boolean hasDotNetCSharpProject(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.csproj")) {
            return stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks if the directory contains any F# project files (*.fsproj).
     *
     * @param dir The directory to check
     * @return true if any .fsproj file exists
     */
    private boolean hasDotNetFSharpProject(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.fsproj")) {
            return stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Finds all .NET solution files (*.sln) in the directory.
     *
     * @param dir The directory to search
     * @return List of .sln file names
     */
    private List<String> findDotNetSolutions(Path dir) {
        List<String> solutions = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.sln")) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    solutions.add(path.getFileName().toString());
                }
            }
        } catch (IOException e) {
            // Ignore and return empty list
        }
        return solutions;
    }

    /**
     * Finds all C# project files (*.csproj) in the directory.
     *
     * @param dir The directory to search
     * @return List of .csproj file names
     */
    private List<String> findDotNetCSharpProjects(Path dir) {
        List<String> projects = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.csproj")) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    projects.add(path.getFileName().toString());
                }
            }
        } catch (IOException e) {
            // Ignore and return empty list
        }
        return projects;
    }

    /**
     * Finds all F# project files (*.fsproj) in the directory.
     *
     * @param dir The directory to search
     * @return List of .fsproj file names
     */
    private List<String> findDotNetFSharpProjects(Path dir) {
        List<String> projects = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.fsproj")) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    projects.add(path.getFileName().toString());
                }
            }
        } catch (IOException e) {
            // Ignore and return empty list
        }
        return projects;
    }
}
