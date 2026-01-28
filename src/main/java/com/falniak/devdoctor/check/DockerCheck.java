package com.falniak.devdoctor.check;

import java.util.ArrayList;
import java.util.List;

/**
 * Check for Docker availability.
 */
public class DockerCheck implements Check {

    @Override
    public String id() {
        return "system.docker";
    }

    @Override
    public CheckResult run(CheckContext ctx) {
        try {
            ExecResult result = ctx.processExecutor().exec(List.of("docker", "--version"));
            if (result.exitCode() == 0) {
                String version = extractVersion(result.stdout());
                String summary = version != null ? "Docker is available (" + version + ")" : "Docker is available";
                return new CheckResult(
                    id(),
                    CheckStatus.PASS,
                    summary,
                    null,
                    List.of()
                );
            } else {
                return createFailResult("Docker command returned non-zero exit code: " + result.exitCode());
            }
        } catch (Exception e) {
            return createFailResult("Docker not found: " + e.getMessage());
        }
    }

    private String extractVersion(String output) {
        if (output == null || output.isEmpty()) {
            return null;
        }
        // Docker output is typically "Docker version 20.x.x, build ..."
        String[] parts = output.trim().split("\\s+");
        if (parts.length >= 3) {
            return parts[2].replace(",", "");
        }
        return null;
    }

    private CheckResult createFailResult(String details) {
        List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(new Suggestion(
            "Install Docker from https://www.docker.com/get-started or your system package manager",
            List.of(),
            Risk.SAFE
        ));
        return new CheckResult(
            id(),
            CheckStatus.FAIL,
            "Docker not found",
            details,
            suggestions
        );
    }
}
