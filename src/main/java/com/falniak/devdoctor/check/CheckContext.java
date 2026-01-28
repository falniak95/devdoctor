package com.falniak.devdoctor.check;

import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectType;

import java.nio.file.Path;
import java.util.Set;

/**
 * Context information available to checks during execution.
 *
 * @param targetPath The target path provided by the user
 * @param projectRoot The detected project root directory
 * @param projectTypes The set of detected project types
 * @param detectionResult The full detection result (nullable)
 * @param processExecutor The process executor for running system commands
 */
public record CheckContext(
    Path targetPath,
    Path projectRoot,
    Set<ProjectType> projectTypes,
    DetectionResult detectionResult,
    ProcessExecutor processExecutor
) {
}
