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
 * Check for Java version requirements.
 */
public class JavaRequirementCheck implements Check {

    private final JavaRequirementReader reader = new JavaRequirementReader();

    @Override
    public String id() {
        return "project.java.requirements";
    }

    @Override
    public CheckResult run(CheckContext ctx) {
        Set<ProjectType> types = ctx.projectTypes();
        if (!types.contains(ProjectType.JAVA_MAVEN) && !types.contains(ProjectType.JAVA_GRADLE)) {
            return new CheckResult(
                id(),
                CheckStatus.NOT_APPLICABLE,
                "Not applicable (no Java project detected)",
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
                "No Java version requirement specified",
                null,
                List.of()
            );
        }

        Requirement requirement = requirementOpt.get();

        // Try to get local version using JavaCheck's extractVersion method
        String localVersion;
        try {
            ExecResult result = ctx.processExecutor().exec(List.of("java", "-version"));
            if (result.exitCode() != 0) {
                return createWarnResult("Java not found", requirement);
            }
            // Use JavaCheck's version extraction logic
            localVersion = extractJavaVersion(result.stderr());
            if (localVersion == null || localVersion.isEmpty()) {
                return createWarnResult("Java version output is empty", requirement);
            }
        } catch (Exception e) {
            return createWarnResult("Java not found: " + e.getMessage(), requirement);
        }

        // Parse versions
        Integer requiredMajor = requirement.parsedMajor();
        Integer localMajor = VersionParser.parseJavaVersion(localVersion);

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

        // Compare versions - treat required Java version as minimum
        // PASS if localMajor >= requiredMajor, FAIL if localMajor < requiredMajor
        boolean satisfies = localMajor >= requiredMajor;

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
                String.format("Install/use Java %d via https://adoptium.net/ or your system package manager", 
                    requiredMajor),
                List.of(),
                Risk.SAFE
            ));
            return new CheckResult(
                id(),
                CheckStatus.FAIL,
                summary,
                String.format("Version mismatch: required minimum major %d, local major %d", requiredMajor, localMajor),
                suggestions
            );
        }
    }

    private String extractJavaVersion(String output) {
        if (output == null || output.isEmpty()) {
            return null;
        }
        // Reuse JavaCheck's pattern exactly
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("version \"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private CheckResult createWarnResult(String details, Requirement requirement) {
        return new CheckResult(
            id(),
            CheckStatus.WARN,
            "Java not found",
            String.format("%s. Required: %s (source: %s)", 
                details, requirement.rawValue(), requirement.sourceFile()),
            List.of()
        );
    }
}
