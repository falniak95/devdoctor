package com.falniak.devdoctor.check.render;

import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckStatus;
import com.falniak.devdoctor.check.Suggestion;
import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders check results to the console with formatting and filtering options.
 */
public class ConsoleRenderer {
    private final boolean showNa;
    private final boolean verbose;

    public ConsoleRenderer(boolean showNa, boolean verbose) {
        this.showNa = showNa;
        this.verbose = verbose;
    }

    /**
     * Renders the detection result and check results to the console.
     *
     * @param detectionResult The project detection result
     * @param results The list of check results to render
     * @param failedRequiredChecks Set of required check IDs that failed
     */
    public void render(DetectionResult detectionResult, List<CheckResult> results, Set<String> failedRequiredChecks) {
        // Print header
        System.out.println("Project root: " + detectionResult.root());
        System.out.println("Detected project types:");
        
        if (detectionResult.types().isEmpty()) {
            System.out.println("  None");
        } else {
            for (ProjectType type : detectionResult.types()) {
                System.out.println("  - " + type.displayName());
            }
        }
        
        System.out.println();

        // Separate system and project checks
        List<CheckResult> systemChecks = new ArrayList<>();
        List<CheckResult> projectChecks = new ArrayList<>();
        
        for (CheckResult result : results) {
            if (result.id().startsWith("system.")) {
                systemChecks.add(result);
            } else if (result.id().startsWith("project.")) {
                projectChecks.add(result);
            } else {
                // Fallback: treat as system check if no prefix matches
                systemChecks.add(result);
            }
        }

        // Print system checks (only if there are visible checks)
        boolean hasVisibleSystemChecks = hasVisibleChecks(systemChecks);
        if (hasVisibleSystemChecks) {
            System.out.println("System checks");
            printCheckGroup(systemChecks);
        }

        // Print project checks (only if there are visible checks)
        boolean hasVisibleProjectChecks = hasVisibleChecks(projectChecks);
        if (hasVisibleProjectChecks) {
            System.out.println("Project checks");
            printCheckGroup(projectChecks);
        }

        // Print summary
        printSummary(results, failedRequiredChecks);
    }
    
    /**
     * Renders the detection result and check results to the console.
     * Convenience method for backward compatibility.
     *
     * @param detectionResult The project detection result
     * @param results The list of check results to render
     */
    public void render(DetectionResult detectionResult, List<CheckResult> results) {
        render(detectionResult, results, Set.of());
    }

    private boolean hasVisibleChecks(List<CheckResult> checks) {
        for (CheckResult result : checks) {
            if (result.status() != CheckStatus.NOT_APPLICABLE || showNa) {
                return true;
            }
        }
        return false;
    }

    private void printCheckGroup(List<CheckResult> checks) {
        for (CheckResult result : checks) {
            // Filter out NOT_APPLICABLE unless showNa is true
            if (result.status() == CheckStatus.NOT_APPLICABLE && !showNa) {
                continue;
            }

            // Print one-line summary
            String statusStr = "[" + result.status() + "]";
            System.out.println(statusStr + " " + result.id() + "  " + result.summary());

            // Print details and suggestions if verbose
            if (verbose) {
                if (result.details() != null && !result.details().isEmpty()) {
                    System.out.println("  " + result.details());
                }

                if (result.suggestions() != null && !result.suggestions().isEmpty()) {
                    for (Suggestion suggestion : result.suggestions()) {
                        if (!suggestion.commands().isEmpty()) {
                            for (String command : suggestion.commands()) {
                                System.out.println("  > " + command);
                            }
                        }
                        if (suggestion.message() != null && !suggestion.message().isEmpty()) {
                            System.out.println("  " + suggestion.message());
                        }
                    }
                }
            }
        }
    }

    private void printSummary(List<CheckResult> results, Set<String> failedRequiredChecks) {
        Map<CheckStatus, Integer> counts = new HashMap<>();
        
        // Initialize all statuses to 0
        for (CheckStatus status : CheckStatus.values()) {
            counts.put(status, 0);
        }

        // Count all results (including filtered ones)
        for (CheckResult result : results) {
            counts.put(result.status(), counts.get(result.status()) + 1);
        }

        // Build summary string (exclude NOT_APPLICABLE for cleaner output)
        List<String> parts = new ArrayList<>();
        parts.add("PASS=" + counts.get(CheckStatus.PASS));
        parts.add("WARN=" + counts.get(CheckStatus.WARN));
        parts.add("FAIL=" + counts.get(CheckStatus.FAIL));
        parts.add("INFO=" + counts.get(CheckStatus.INFO));

        String summaryLine = "Summary: " + String.join(" ", parts);
        
        // Append required checks failure message if applicable
        if (!failedRequiredChecks.isEmpty()) {
            String failedIds = String.join(", ", failedRequiredChecks);
            summaryLine += " Required checks failed: " + failedIds;
        }
        
        System.out.println(summaryLine);

        // Print next steps if any FAIL or WARN exists (but not in verbose mode)
        if (!verbose && (counts.get(CheckStatus.FAIL) > 0 || counts.get(CheckStatus.WARN) > 0)) {
            if (counts.get(CheckStatus.FAIL) > 0) {
                System.out.println("Next steps: re-run with --verbose to see details and suggestions.");
            } else {
                System.out.println("Next steps: re-run with --verbose to see recommendations.");
            }
        }
    }
}
