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
 * Check for Go version requirements.
 */
public class GoRequirementCheck implements Check {

    private final GoRequirementReader reader = new GoRequirementReader();

    @Override
    public String id() {
        return "project.go.requirements";
    }

    @Override
    public CheckResult run(CheckContext ctx) {
        Set<ProjectType> types = ctx.projectTypes();
        if (!types.contains(ProjectType.GO_MODULES)) {
            return new CheckResult(
                id(),
                CheckStatus.NOT_APPLICABLE,
                "Not applicable (no Go project detected)",
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
                "No Go version requirement specified",
                null,
                List.of()
            );
        }

        Requirement requirement = requirementOpt.get();

        // Try to get local version
        String localVersion;
        try {
            ExecResult result = ctx.processExecutor().exec(List.of("go", "version"));
            if (result.exitCode() != 0) {
                return createWarnResult("Go not found", requirement);
            }
            localVersion = result.stdout().trim();
            if (localVersion.isEmpty()) {
                return createWarnResult("Go version output is empty", requirement);
            }
        } catch (Exception e) {
            return createWarnResult("Go not found: " + e.getMessage(), requirement);
        }

        // Extract version from "go version go1.21.0 ..." format
        String versionStr = localVersion;
        if (localVersion.startsWith("go version ")) {
            String[] parts = localVersion.split("\\s+");
            if (parts.length >= 3) {
                versionStr = parts[2]; // "go1.21.0"
                if (versionStr.startsWith("go")) {
                    versionStr = "go " + versionStr.substring(2); // "go 1.21.0"
                }
            }
        }

        // Parse versions
        Integer requiredMajor = requirement.parsedMajor();
        Integer localMajor = VersionParser.parseGoVersion(versionStr);

        if (requiredMajor == null) {
            return new CheckResult(
                id(),
                CheckStatus.WARN,
                "Required version format not recognized",
                String.format("Required: %s (source: %s), Local: %s", 
                    requirement.rawValue(), requirement.sourceFile(), versionStr),
                List.of()
            );
        }

        if (localMajor == null) {
            return new CheckResult(
                id(),
                CheckStatus.WARN,
                "Local version format not recognized",
                String.format("Required: %s (source: %s), Local: %s", 
                    requirement.rawValue(), requirement.sourceFile(), versionStr),
                List.of()
            );
        }

        // Compare versions
        boolean satisfies = VersionParser.satisfiesRequirement(
            "go " + requirement.rawValue(), 
            versionStr, 
            VersionParser::parseGoVersion
        );

        String summary = String.format("Required: %s (source: %s), Local: %s", 
            requirement.rawValue(), requirement.sourceFile(), versionStr);

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
                String.format("Install/use Go %s via https://go.dev/dl/ or your system package manager", 
                    requirement.rawValue()),
                List.of(),
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
            "Go not found",
            String.format("%s. Required: %s (source: %s)", 
                details, requirement.rawValue(), requirement.sourceFile()),
            List.of()
        );
    }
}
