package com.falniak.devdoctor.check;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Check for Java availability and version.
 */
public class JavaCheck implements Check {

    private static final Pattern VERSION_PATTERN = Pattern.compile("version \"([^\"]+)\"");

    @Override
    public String id() {
        return "system.java";
    }

    @Override
    public CheckResult run(CheckContext ctx) {
        try {
            ExecResult result = ctx.processExecutor().exec(List.of("java", "-version"));
            if (result.exitCode() == 0) {
                String version = extractVersion(result.stderr());
                String summary = version != null ? "Java is available (" + version + ")" : "Java is available";
                return new CheckResult(
                    id(),
                    CheckStatus.PASS,
                    summary,
                    null,
                    List.of()
                );
            } else {
                return createFailResult("Java command returned non-zero exit code: " + result.exitCode());
            }
        } catch (Exception e) {
            return createFailResult("Java not found: " + e.getMessage());
        }
    }

    private String extractVersion(String output) {
        if (output == null || output.isEmpty()) {
            return null;
        }
        var matcher = VERSION_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private CheckResult createFailResult(String details) {
        List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(new Suggestion(
            "Install Java from https://adoptium.net/ or your system package manager",
            List.of(),
            Risk.SAFE
        ));
        return new CheckResult(
            id(),
            CheckStatus.FAIL,
            "Java not found",
            details,
            suggestions
        );
    }
}
