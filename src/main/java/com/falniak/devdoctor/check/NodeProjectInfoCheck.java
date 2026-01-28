package com.falniak.devdoctor.check;

import com.falniak.devdoctor.detect.ProjectType;

import java.util.List;
import java.util.Set;

/**
 * Check that provides information about detected Node.js projects.
 */
public class NodeProjectInfoCheck implements Check {

    @Override
    public String id() {
        return "project.node";
    }

    @Override
    public CheckResult run(CheckContext ctx) {
        Set<ProjectType> types = ctx.projectTypes();
        if (!types.contains(ProjectType.NODE)) {
            // Not applicable - return a result indicating it was skipped
            return new CheckResult(
                id(),
                CheckStatus.INFO,
                "Not applicable (no Node.js project detected)",
                null,
                List.of()
            );
        }

        return new CheckResult(
            id(),
            CheckStatus.INFO,
            "Node.js project detected",
            null,
            List.of()
        );
    }
}
