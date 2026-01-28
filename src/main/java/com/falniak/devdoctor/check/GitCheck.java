package com.falniak.devdoctor.check;

import java.util.ArrayList;
import java.util.List;

/**
 * Check for Git availability.
 */
public class GitCheck implements Check {

    @Override
    public String id() {
        return "system.git";
    }

    @Override
    public CheckResult run(CheckContext ctx) {
        try {
            ExecResult result = ctx.processExecutor().exec(List.of("git", "--version"));
            if (result.exitCode() == 0) {
                String version = extractVersion(result.stdout());
                String summary = version != null ? "Git is available (" + version + ")" : "Git is available";
                return new CheckResult(
                    id(),
                    CheckStatus.PASS,
                    summary,
                    null,
                    List.of()
                );
            } else {
                return createFailResult("Git command returned non-zero exit code: " + result.exitCode());
            }
        } catch (Exception e) {
            return createFailResult("Git not found: " + e.getMessage());
        }
    }

    private String extractVersion(String output) {
        if (output == null || output.isEmpty()) {
            return null;
        }
        // Git output is typically "git version 2.x.x"
        String[] parts = output.trim().split("\\s+");
        if (parts.length >= 3) {
            return parts[2];
        }
        return null;
    }

    private CheckResult createFailResult(String details) {
        List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(new Suggestion(
            "Install Git from https://git-scm.com/downloads or your system package manager",
            List.of(),
            Risk.SAFE
        ));
        return new CheckResult(
            id(),
            CheckStatus.FAIL,
            "Git not found",
            details,
            suggestions
        );
    }
}
