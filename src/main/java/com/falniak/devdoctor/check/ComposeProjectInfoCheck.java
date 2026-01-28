package com.falniak.devdoctor.check;

import com.falniak.devdoctor.detect.ProjectType;

import java.util.List;
import java.util.Set;

/**
 * Check that provides information about detected Docker Compose projects.
 */
public class ComposeProjectInfoCheck implements Check {

    @Override
    public String id() {
        return "project.compose";
    }

    @Override
    public CheckResult run(CheckContext ctx) {
        Set<ProjectType> types = ctx.projectTypes();
        if (!types.contains(ProjectType.DOCKER_COMPOSE)) {
            // Not applicable - return a result indicating it was skipped
            return new CheckResult(
                id(),
                CheckStatus.NOT_APPLICABLE,
                "Not applicable (no Docker Compose project detected)",
                null,
                List.of()
            );
        }

        return new CheckResult(
            id(),
            CheckStatus.INFO,
            "Docker Compose detected",
            null,
            List.of()
        );
    }
}
