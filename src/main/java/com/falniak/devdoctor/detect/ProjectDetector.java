package com.falniak.devdoctor.detect;

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
            || hasMarker(dir, "compose.yaml");
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

        return markers;
    }
}
