package com.falniak.devdoctor.check;

import java.util.ArrayList;
import java.util.List;

/**
 * Check for Node.js availability.
 */
public class NodeCheck implements Check {

    @Override
    public String id() {
        return "system.node";
    }

    @Override
    public CheckResult run(CheckContext ctx) {
        try {
            ExecResult result = ctx.processExecutor().exec(List.of("node", "-v"));
            if (result.exitCode() == 0) {
                String version = result.stdout().trim();
                String summary = version.isEmpty() ? "Node.js is available" : "Node.js is available (" + version + ")";
                return new CheckResult(
                    id(),
                    CheckStatus.PASS,
                    summary,
                    null,
                    List.of()
                );
            } else {
                return createFailResult("Node.js command returned non-zero exit code: " + result.exitCode());
            }
        } catch (Exception e) {
            return createFailResult("Node.js not found: " + e.getMessage());
        }
    }

    private CheckResult createFailResult(String details) {
        List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(new Suggestion(
            "Install Node.js from https://nodejs.org/ or your system package manager",
            List.of(),
            Risk.SAFE
        ));
        return new CheckResult(
            id(),
            CheckStatus.FAIL,
            "Node.js not found",
            details,
            suggestions
        );
    }
}
