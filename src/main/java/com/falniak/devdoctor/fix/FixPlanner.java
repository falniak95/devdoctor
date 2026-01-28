package com.falniak.devdoctor.fix;

import com.falniak.devdoctor.check.CheckContext;
import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckStatus;
import com.falniak.devdoctor.check.Suggestion;
import com.falniak.devdoctor.detect.ProjectType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
                FixAction action = createRequirementAction(result, ctx);
                if (action != null) {
                    actions.add(action);
                    processedCheckIds.add(checkId);
                }
            }
        }

        // Check for Node dependencies (node_modules missing)
        if (ctx.projectTypes().contains(ProjectType.NODE)) {
            Path nodeModulesPath = ctx.projectRoot().resolve("node_modules");
            if (!Files.exists(nodeModulesPath) || !Files.isDirectory(nodeModulesPath)) {
                // Check if package-lock.json exists
                Path packageLockPath = ctx.projectRoot().resolve("package-lock.json");
                List<String> commands;
                String description;
                
                if (Files.exists(packageLockPath) && Files.isRegularFile(packageLockPath)) {
                    commands = List.of("npm", "ci");
                    description = "node_modules/ directory is missing. Run 'npm ci' to install dependencies from package-lock.json.";
                } else {
                    commands = List.of("npm", "install");
                    description = "node_modules/ directory is missing. Run 'npm install' to install dependencies.";
                }
                
                FixAction nodeDepsAction = new FixAction(
                    "project.node.dependencies",
                    "Install Node.js dependencies",
                    description,
                    Risk.SAFE,
                    commands,
                    true  // SAFE actions with commands are applyable
                );
                actions.add(nodeDepsAction);
            }
        }

        return new FixPlan(actions);
    }

    private FixAction createDockerAction(CheckResult result) {
        List<String> commands = new ArrayList<>();
        String description = "Docker is not installed or not available. Install Docker Desktop to enable container-based workflows.";

        // Extract commands from suggestions
        for (Suggestion suggestion : result.suggestions()) {
            if (suggestion.commands() != null && !suggestion.commands().isEmpty()) {
                commands.addAll(suggestion.commands());
            }
        }

        // Always add winget command on Windows
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("windows")) {
            // Check if winget command is not already present
            boolean hasWinget = commands.stream()
                .anyMatch(cmd -> cmd.contains("winget") && cmd.contains("Docker.DockerDesktop"));
            if (!hasWinget) {
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

    private FixAction createRequirementAction(CheckResult result, CheckContext ctx) {
        String checkId = result.id();
        
        if (checkId.contains("java")) {
            return createJavaRequirementAction(result);
        } else if (checkId.contains("node")) {
            return createNodeRequirementAction(result, ctx);
        } else if (checkId.contains("python")) {
            return createPythonRequirementAction(result);
        } else {
            // Generic fallback for other requirement types
            return createGenericRequirementAction(result);
        }
    }

    private FixAction createJavaRequirementAction(CheckResult result) {
        List<String> commands = new ArrayList<>();
        String description;
        
        // Extract version info from summary (format: "Required: X (source: Y), Local: Z")
        String summary = result.summary();
        if (summary != null && summary.contains("Required:") && summary.contains("Local:")) {
            Pattern pattern = Pattern.compile("Required:\\s*([^\\s(]+)\\s*\\(source:\\s*([^)]+)\\),\\s*Local:\\s*(.+)");
            Matcher matcher = pattern.matcher(summary);
            if (matcher.find()) {
                String required = matcher.group(1).trim();
                String source = matcher.group(2).trim();
                String local = matcher.group(3).trim();
                
                // Extract major version for JDK installation
                Integer majorVersion = extractMajorVersion(required);
                if (majorVersion != null) {
                    String osName = System.getProperty("os.name", "").toLowerCase();
                    if (osName.contains("windows")) {
                        commands.add(String.format("winget install -e --id EclipseAdoptium.Temurin.%d.JDK", majorVersion));
                    }
                }
                
                description = String.format("Java version mismatch. Required: %s (from %s), Local: %s. Set JAVA_HOME or install JDK %s.",
                    required, source, local, required);
            } else {
                description = summary;
            }
        } else {
            description = summary != null ? summary : "Java version mismatch. Set JAVA_HOME or install the required JDK version.";
        }

        // Extract commands from suggestions
        for (Suggestion suggestion : result.suggestions()) {
            if (suggestion.commands() != null && !suggestion.commands().isEmpty()) {
                commands.addAll(suggestion.commands());
            }
        }

        // CAUTION actions are never applyable (safety rule)
        return new FixAction(
            result.id(),
            "Align Java version",
            description,
            Risk.CAUTION,
            commands,
            false
        );
    }

    private FixAction createNodeRequirementAction(CheckResult result, CheckContext ctx) {
        List<String> commands = new ArrayList<>();
        String description;
        
        // Check if .nvmrc or .node-version exists
        Path projectRoot = ctx.projectRoot();
        boolean hasNvmrc = Files.exists(projectRoot.resolve(".nvmrc")) && 
                          Files.isRegularFile(projectRoot.resolve(".nvmrc"));
        boolean hasNodeVersion = Files.exists(projectRoot.resolve(".node-version")) && 
                                Files.isRegularFile(projectRoot.resolve(".node-version"));
        
        // Extract version info from summary
        String summary = result.summary();
        String required = null;
        String source = null;
        String local = null;
        
        if (summary != null && summary.contains("Required:") && summary.contains("Local:")) {
            Pattern pattern = Pattern.compile("Required:\\s*([^\\s(]+)\\s*\\(source:\\s*([^)]+)\\),\\s*Local:\\s*(.+)");
            Matcher matcher = pattern.matcher(summary);
            if (matcher.find()) {
                required = matcher.group(1).trim();
                source = matcher.group(2).trim();
                local = matcher.group(3).trim();
            }
        }
        
        if (hasNvmrc || hasNodeVersion) {
            // Prefer nvm use when version file exists
            commands.add("nvm use");
            String sourceFile = hasNvmrc ? ".nvmrc" : ".node-version";
            if (required != null && local != null) {
                description = String.format("Node.js version mismatch. Required: %s (from %s), Local: %s. Run 'nvm use' to switch to the required version.",
                    required, sourceFile, local);
            } else {
                description = String.format("Node.js version mismatch. Run 'nvm use' to switch to the version specified in %s.", sourceFile);
            }
        } else {
            // Extract major version and suggest nvm install
            if (required != null) {
                Integer majorVersion = extractMajorVersion(required);
                if (majorVersion != null) {
                    commands.add(String.format("nvm install %d", majorVersion));
                    commands.add(String.format("nvm use %d", majorVersion));
                }
            }
            
            if (required != null && local != null) {
                description = String.format("Node.js version mismatch. Required: %s, Local: %s. Install and use Node.js %s via nvm.",
                    required, local, required);
            } else {
                description = summary != null ? summary : "Node.js version mismatch. Install and use the required Node.js version via nvm.";
            }
        }

        // Extract commands from suggestions (may override our commands)
        for (Suggestion suggestion : result.suggestions()) {
            if (suggestion.commands() != null && !suggestion.commands().isEmpty()) {
                // Prepend suggestion commands (they may be more specific)
                commands.addAll(0, suggestion.commands());
            }
        }

        // CAUTION actions are never applyable (safety rule)
        return new FixAction(
            result.id(),
            "Align Node.js version",
            description,
            Risk.CAUTION,
            commands,
            false
        );
    }

    private FixAction createPythonRequirementAction(CheckResult result) {
        List<String> commands = new ArrayList<>();
        String description;
        
        // Extract version info from summary
        String summary = result.summary();
        if (summary != null && summary.contains("Required:") && summary.contains("Local:")) {
            Pattern pattern = Pattern.compile("Required:\\s*([^\\s(]+)\\s*\\(source:\\s*([^)]+)\\),\\s*Local:\\s*(.+)");
            Matcher matcher = pattern.matcher(summary);
            if (matcher.find()) {
                String required = matcher.group(1).trim();
                String source = matcher.group(2).trim();
                String local = matcher.group(3).trim();
                
                description = String.format("Python version mismatch. Required: %s (from %s), Local: %s. Use pyenv or Python launcher to install/switch to the required version.",
                    required, source, local);
                
                // Add pyenv command
                commands.add(String.format("pyenv install %s", required));
                
                // Add Windows Python launcher command
                String osName = System.getProperty("os.name", "").toLowerCase();
                if (osName.contains("windows")) {
                    // Extract major.minor for py launcher (e.g., "3.11" -> "py -3.11")
                    Pattern pythonPattern = Pattern.compile("(\\d+)\\.(\\d+)");
                    Matcher pythonMatcher = pythonPattern.matcher(required);
                    if (pythonMatcher.find()) {
                        commands.add(String.format("py -%s.%s", pythonMatcher.group(1), pythonMatcher.group(2)));
                    }
                }
            } else {
                description = summary;
            }
        } else {
            description = summary != null ? summary : "Python version mismatch. Use pyenv or Python launcher to install/switch to the required version.";
        }

        // Extract commands from suggestions
        for (Suggestion suggestion : result.suggestions()) {
            if (suggestion.commands() != null && !suggestion.commands().isEmpty()) {
                commands.addAll(suggestion.commands());
            }
        }

        // CAUTION actions are never applyable (safety rule)
        return new FixAction(
            result.id(),
            "Align Python version",
            description,
            Risk.CAUTION,
            commands,
            false
        );
    }

    private FixAction createGenericRequirementAction(CheckResult result) {
        List<String> commands = new ArrayList<>();
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
        return new FixAction(
            result.id(),
            title,
            description,
            Risk.CAUTION,
            commands,
            false
        );
    }

    private Integer extractMajorVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return null;
        }
        // Try to extract major version (first number)
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(version.trim());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
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
