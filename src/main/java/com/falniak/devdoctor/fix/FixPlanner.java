package com.falniak.devdoctor.fix;

import com.falniak.devdoctor.check.CheckContext;
import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckStatus;
import com.falniak.devdoctor.check.Suggestion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Plans fix actions based on check results.
 */
public class FixPlanner {

    /**
     * Generates a fix plan from check results.
     *
     * @param results The check results to analyze
     * @param ctx The check context (for additional information if needed)
     * @return A fix plan containing actions to resolve failures
     */
    public FixPlan plan(List<CheckResult> results, CheckContext ctx) {
        List<FixAction> actions = new ArrayList<>();
        Set<String> processedCheckIds = new HashSet<>();

        for (CheckResult result : results) {
            // Only process FAIL results
            if (result.status() != CheckStatus.FAIL) {
                continue;
            }

            // Avoid duplicates
            if (processedCheckIds.contains(result.id())) {
                continue;
            }

            String checkId = result.id();

            // Handle system.docker failures
            if ("system.docker".equals(checkId)) {
                FixAction action = createDockerAction(result);
                if (action != null) {
                    actions.add(action);
                    processedCheckIds.add(checkId);
                }
            }
            // Handle project.*.requirements failures
            else if (checkId.startsWith("project.") && checkId.endsWith(".requirements")) {
                FixAction action = createRequirementAction(result);
                if (action != null) {
                    actions.add(action);
                    processedCheckIds.add(checkId);
                }
            }
        }

        return new FixPlan(actions);
    }

    private FixAction createDockerAction(CheckResult result) {
        List<String> commands = new ArrayList<>();
        String description = "Docker is not installed or not available. Install Docker to enable container-based workflows.";

        // Extract commands from suggestions
        for (Suggestion suggestion : result.suggestions()) {
            if (suggestion.commands() != null && !suggestion.commands().isEmpty()) {
                commands.addAll(suggestion.commands());
            }
        }

        // If no commands found, add a generic suggestion
        if (commands.isEmpty()) {
            // Check if Windows and add winget command
            String osName = System.getProperty("os.name", "").toLowerCase();
            if (osName.contains("windows")) {
                commands.add("winget install -e --id Docker.DockerDesktop");
            }
        }

        // CAUTION actions are never applyable (safety rule)
        boolean applyable = false;

        return new FixAction(
            result.id(),
            "Install Docker",
            description,
            Risk.CAUTION,
            commands,
            applyable
        );
    }

    private FixAction createRequirementAction(CheckResult result) {
        List<String> commands = new ArrayList<>();
        
        // Extract the project type from check ID (e.g., "project.node.requirements" -> "Node.js")
        String projectType = extractProjectType(result.id());
        String title = "Align " + projectType + " version";
        String description = result.summary() != null ? result.summary() : 
            String.format("The installed %s version does not match the project requirements.", projectType);

        // Extract commands from suggestions
        for (Suggestion suggestion : result.suggestions()) {
            if (suggestion.commands() != null && !suggestion.commands().isEmpty()) {
                commands.addAll(suggestion.commands());
            }
        }

        // CAUTION actions are never applyable (safety rule)
        boolean applyable = false;

        return new FixAction(
            result.id(),
            title,
            description,
            Risk.CAUTION,
            commands,
            applyable
        );
    }

    private String extractProjectType(String checkId) {
        // Extract project type from check ID like "project.node.requirements"
        if (checkId.contains("node")) {
            return "Node.js";
        } else if (checkId.contains("java")) {
            return "Java";
        } else if (checkId.contains("python")) {
            return "Python";
        } else if (checkId.contains("go")) {
            return "Go";
        }
        return "runtime";
    }
}
