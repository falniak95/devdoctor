package com.falniak.devdoctor.check;

import com.falniak.devdoctor.detect.ProjectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Check that provides information about detected Java projects.
 */
public class JavaProjectInfoCheck implements Check {

    @Override
    public String id() {
        return "project.java";
    }

    @Override
    public CheckResult run(CheckContext ctx) {
        Set<ProjectType> types = ctx.projectTypes();
        if (!types.contains(ProjectType.JAVA_MAVEN) && !types.contains(ProjectType.JAVA_GRADLE)) {
            // Not applicable - return a result indicating it was skipped
            return new CheckResult(
                id(),
                CheckStatus.NOT_APPLICABLE,
                "Not applicable (no Java project detected)",
                null,
                List.of()
            );
        }

        List<String> detectedTypes = new ArrayList<>();
        if (types.contains(ProjectType.JAVA_MAVEN)) {
            detectedTypes.add("Maven");
        }
        if (types.contains(ProjectType.JAVA_GRADLE)) {
            detectedTypes.add("Gradle");
        }

        String summary = "Java project detected (" + String.join(", ", detectedTypes) + ")";
        return new CheckResult(
            id(),
            CheckStatus.INFO,
            summary,
            null,
            List.of()
        );
    }
}
