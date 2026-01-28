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
 * Check for Python version requirements.
 */
public class PythonRequirementCheck implements Check {

    private final PythonRequirementReader reader = new PythonRequirementReader();

    @Override
    public String id() {
        return "project.python.requirements";
    }

    @Override
    public CheckResult run(CheckContext ctx) {
        Set<ProjectType> types = ctx.projectTypes();
        boolean hasPythonProject = types.contains(ProjectType.PYTHON_PYPROJECT) ||
            types.contains(ProjectType.PYTHON_REQUIREMENTS) ||
            types.contains(ProjectType.PYTHON_PIPENV) ||
            types.contains(ProjectType.PYTHON_SETUPPY);

        if (!hasPythonProject) {
            return new CheckResult(
                id(),
                CheckStatus.NOT_APPLICABLE,
                "Not applicable (no Python project detected)",
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
                "No Python version requirement specified",
                null,
                List.of()
            );
        }

        Requirement requirement = requirementOpt.get();

        // Try to get local version (try python first, then python3)
        String localVersion = null;
        try {
            ExecResult result = ctx.processExecutor().exec(List.of("python", "--version"));
            if (result.exitCode() == 0) {
                localVersion = result.stdout().trim();
                if (localVersion.isEmpty()) {
                    localVersion = result.stderr().trim();
                }
            }
        } catch (Exception e) {
            // Try python3
        }

        if (localVersion == null || localVersion.isEmpty()) {
            try {
                ExecResult result = ctx.processExecutor().exec(List.of("python3", "--version"));
                if (result.exitCode() == 0) {
                    localVersion = result.stdout().trim();
                    if (localVersion.isEmpty()) {
                        localVersion = result.stderr().trim();
                    }
                }
            } catch (Exception e) {
                // Continue
            }
        }

        if (localVersion == null || localVersion.isEmpty()) {
            return createWarnResult("Python not found", requirement);
        }

        // Extract version from "Python 3.11.0" format
        String versionStr = localVersion;
        if (localVersion.startsWith("Python ")) {
            versionStr = localVersion.substring(7).trim();
        }

        // Parse versions
        Integer requiredMajor = requirement.parsedMajor();
        Integer localMajor = VersionParser.parsePythonVersion(versionStr);

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
        // For Python, always compare major.minor for accurate comparison
        boolean satisfies;
        Integer requiredMinor = VersionParser.parsePythonVersionMinor(requirement.rawValue());
        Integer localMinor = VersionParser.parsePythonVersionMinor(versionStr);
        
        if (requiredMinor == null || localMinor == null) {
            // Fallback to major-only comparison if minor parsing fails
            if (requirement.rawValue().trim().startsWith(">=")) {
                satisfies = VersionParser.satisfiesRequirement(
                    requirement.rawValue(), 
                    versionStr, 
                    VersionParser::parsePythonVersion
                );
            } else {
                satisfies = requiredMajor.equals(localMajor);
            }
        } else {
            // Compare major.minor
            if (requirement.rawValue().trim().startsWith(">=")) {
                // Range version: local >= required (compare major.minor)
                satisfies = localMinor >= requiredMinor;
            } else {
                // Exact version: local == required
                satisfies = requiredMinor.equals(localMinor);
            }
        }

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
                String.format("Install/use Python %d.x via pyenv or your system package manager", requiredMajor),
                List.of(String.format("pyenv install %s", requirement.rawValue())),
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
            "Python not found",
            String.format("%s. Required: %s (source: %s)", 
                details, requirement.rawValue(), requirement.sourceFile()),
            List.of()
        );
    }
}
