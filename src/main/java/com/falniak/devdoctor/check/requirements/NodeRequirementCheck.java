package com.falniak.devdoctor.check.requirements;

import com.falniak.devdoctor.check.Check;
import com.falniak.devdoctor.check.CheckContext;
import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckStatus;
import com.falniak.devdoctor.check.ExecResult;
import com.falniak.devdoctor.check.Risk;
import com.falniak.devdoctor.check.Suggestion;
import com.falniak.devdoctor.detect.ProjectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Check for Node.js version requirements.
 */
public class NodeRequirementCheck implements Check {

    private final NodeRequirementReader reader = new NodeRequirementReader();

    @Override
    public String id() {
        return "project.node.requirements";
    }

    @Override
    public CheckResult run(CheckContext ctx) {
        Set<ProjectType> types = ctx.projectTypes();
        if (!types.contains(ProjectType.NODE)) {
            return new CheckResult(
                id(),
                CheckStatus.NOT_APPLICABLE,
                "Not applicable (no Node.js project detected)",
                null,
                List.of()
            );
        }

        // Try to read requirement
        Optional<Requirement> requirementOpt = reader.read(ctx.projectRoot());
        if (requirementOpt.isEmpty()) {
            return new CheckResult(
                id(),
                CheckStatus.INFO,
                "No Node.js version requirement specified",
                null,
                List.of()
            );
        }

        Requirement requirement = requirementOpt.get();

        // Try to get local version
        String localVersion;
        try {
            ExecResult result = ctx.processExecutor().exec(List.of("node", "-v"));
            if (result.exitCode() != 0) {
                return createWarnResult("Node.js not found", requirement);
            }
            localVersion = result.stdout().trim();
            if (localVersion.isEmpty()) {
                return createWarnResult("Node.js version output is empty", requirement);
            }
        } catch (Exception e) {
            return createWarnResult("Node.js not found: " + e.getMessage(), requirement);
        }

        // Parse versions
        Integer requiredMajor = requirement.parsedMajor();
        Integer localMajor = VersionParser.parseNodeVersion(localVersion);

        if (requiredMajor == null) {
            return new CheckResult(
                id(),
                CheckStatus.WARN,
                "Required version format not recognized",
                String.format("Required: %s (source: %s), Local: %s", 
                    requirement.rawValue(), requirement.sourceFile(), localVersion),
                List.of()
            );
        }

        if (localMajor == null) {
            return new CheckResult(
                id(),
                CheckStatus.WARN,
                "Local version format not recognized",
                String.format("Required: %s (source: %s), Local: %s", 
                    requirement.rawValue(), requirement.sourceFile(), localVersion),
                List.of()
            );
        }

        // Compare versions
        boolean satisfies = VersionParser.satisfiesRequirement(
            requirement.rawValue(), 
            localVersion, 
            VersionParser::parseNodeVersion
        );

        String summary = String.format("Required: %s (source: %s), Local: %s", 
            requirement.rawValue(), requirement.sourceFile(), localVersion);

        if (satisfies) {
            return new CheckResult(
                id(),
                CheckStatus.PASS,
                summary,
                null,
                List.of()
            );
        } else {
            List<Suggestion> suggestions = new ArrayList<>();
            suggestions.add(new Suggestion(
                String.format("Install/use Node.js %d.x via nvm or your system package manager", requiredMajor),
                List.of(String.format("nvm install %d", requiredMajor)),
                Risk.SAFE
            ));
            return new CheckResult(
                id(),
                CheckStatus.FAIL,
                summary,
                String.format("Version mismatch: required major %d, local major %d", requiredMajor, localMajor),
                suggestions
            );
        }
    }

    private CheckResult createWarnResult(String details, Requirement requirement) {
        return new CheckResult(
            id(),
            CheckStatus.WARN,
            "Node.js not found",
            String.format("%s. Required: %s (source: %s)", 
                details, requirement.rawValue(), requirement.sourceFile()),
            List.of()
        );
    }
}
