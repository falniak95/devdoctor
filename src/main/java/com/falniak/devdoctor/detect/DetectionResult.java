package com.falniak.devdoctor.detect;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Represents the result of project detection.
 *
 * @param root The detected project root directory
 * @param types The set of detected project types
 * @param markersFound The list of marker files that were found
 */
public record DetectionResult(
    Path root,
    Set<ProjectType> types,
    List<String> markersFound
) {
}
