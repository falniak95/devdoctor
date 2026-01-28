package com.falniak.devdoctor.commands;

import com.falniak.devdoctor.check.Check;
import com.falniak.devdoctor.check.CheckContext;
import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckRunner;
import com.falniak.devdoctor.check.CheckStatus;
import com.falniak.devdoctor.check.ComposeProjectInfoCheck;
import com.falniak.devdoctor.check.DefaultProcessExecutor;
import com.falniak.devdoctor.check.DockerCheck;
import com.falniak.devdoctor.check.ExecResult;
import com.falniak.devdoctor.check.GitCheck;
import com.falniak.devdoctor.check.JavaCheck;
import com.falniak.devdoctor.check.JavaProjectInfoCheck;
import com.falniak.devdoctor.check.NodeCheck;
import com.falniak.devdoctor.check.NodeProjectInfoCheck;
import com.falniak.devdoctor.check.ProcessExecutor;
import com.falniak.devdoctor.check.requirements.GoRequirementCheck;
import com.falniak.devdoctor.check.requirements.JavaRequirementCheck;
import com.falniak.devdoctor.check.requirements.NodeRequirementCheck;
import com.falniak.devdoctor.check.requirements.PythonRequirementCheck;
import com.falniak.devdoctor.config.ConfigException;
import com.falniak.devdoctor.config.ConfigLoader;
import com.falniak.devdoctor.config.DevDoctorConfig;
import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectDetector;
import com.falniak.devdoctor.fix.FixAction;
import com.falniak.devdoctor.fix.FixPlan;
import com.falniak.devdoctor.fix.FixPlanner;
import com.falniak.devdoctor.fix.Risk;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Command(
    name = "fix",
    description = "Generate and optionally apply fixes for check failures"
)
public class FixCommand implements java.util.concurrent.Callable<Integer> {

    @Option(
        names = {"--path", "-p"},
        description = "Path to the project directory (default: current directory)",
        defaultValue = "."
    )
    private String path;

    @Option(
        names = "--apply",
        description = "Apply safe fixes automatically"
    )
    private boolean apply;

    @Option(
        names = "--yes",
        description = "Skip confirmation prompt when applying fixes"
    )
    private boolean yes;

    @Override
    public Integer call() {
        try {
            Path targetPath = Paths.get(path).toAbsolutePath().normalize();
            
            // Detect project
            ProjectDetector detector = new ProjectDetector();
            DetectionResult detectionResult = detector.detect(targetPath);
            
            // Load config
            ConfigLoader configLoader = new ConfigLoader();
            Optional<DevDoctorConfig> config;
            
            try {
                config = configLoader.loadFromProjectRoot(detectionResult.root());
            } catch (ConfigException e) {
                System.err.println("Error loading config: " + e.getMessage());
                return 2;
            }
            
            // Build context
            ProcessExecutor executor = new DefaultProcessExecutor();
            CheckContext context = new CheckContext(
                targetPath,
                detectionResult.root(),
                detectionResult.types(),
                detectionResult,
                executor
            );
            
            // Build check list
            List<Check> checks = buildCheckList();
            
            // Apply ignoreChecks filtering
            if (config.isPresent() && !config.get().ignoreChecks().isEmpty()) {
                Set<String> ignoreSet = config.get().ignoreChecks();
                checks = checks.stream()
                    .filter(check -> !ignoreSet.contains(check.id()))
                    .collect(Collectors.toList());
            }
            
            // Run checks
            CheckRunner runner = new CheckRunner();
            List<CheckResult> results = runner.runChecks(checks, context);
            
            // Generate fix plan
            FixPlanner planner = new FixPlanner();
            FixPlan plan = planner.plan(results, context);
            
            // Warn if --yes is used without --apply
            if (yes && !apply) {
                System.out.println("Note: --yes has no effect without --apply.");
            }
            
            // Display plan
            displayPlan(plan);
            
            // Apply fixes if requested
            if (apply) {
                return applyFixes(plan, executor, detectionResult.root());
            }
            
            return 0;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return 2;
        }
    }
    
    private List<Check> buildCheckList() {
        List<Check> checks = new ArrayList<>();
        
        // System checks
        checks.add(new JavaCheck());
        checks.add(new GitCheck());
        checks.add(new NodeCheck());
        checks.add(new DockerCheck());
        
        // Project checks
        checks.add(new JavaProjectInfoCheck());
        checks.add(new NodeProjectInfoCheck());
        checks.add(new ComposeProjectInfoCheck());
        checks.add(new NodeRequirementCheck());
        checks.add(new PythonRequirementCheck());
        checks.add(new GoRequirementCheck());
        checks.add(new JavaRequirementCheck());
        
        return checks;
    }
    
    private void displayPlan(FixPlan plan) {
        if (plan.actions().isEmpty()) {
            System.out.println("No fixes needed. All checks passed.");
            return;
        }
        
        // Count actions by risk
        long safeCount = plan.actions().stream()
            .filter(action -> action.risk() == Risk.SAFE)
            .count();
        long cautionCount = plan.actions().stream()
            .filter(action -> action.risk() == Risk.CAUTION)
            .count();
        
        System.out.println("Fix Plan:");
        System.out.println("=========");
        System.out.println();
        System.out.println("Fix plan summary: SAFE=" + safeCount + " CAUTION=" + cautionCount);
        System.out.println();
        
        // Separate system and project fixes
        List<FixAction> systemActions = plan.actions().stream()
            .filter(action -> action.id().startsWith("system."))
            .collect(Collectors.toList());
        List<FixAction> projectActions = plan.actions().stream()
            .filter(action -> !action.id().startsWith("system."))
            .collect(Collectors.toList());
        
        // Display system fixes
        if (!systemActions.isEmpty()) {
            System.out.println("System fixes:");
            System.out.println("------------");
            for (FixAction action : systemActions) {
                displayAction(action);
            }
            System.out.println();
        }
        
        // Display project fixes
        if (!projectActions.isEmpty()) {
            System.out.println("Project fixes:");
            System.out.println("-------------");
            for (FixAction action : projectActions) {
                displayAction(action);
            }
        }
    }
    
    private void displayAction(FixAction action) {
        System.out.println();
        System.out.println("[" + action.risk() + "] " + action.title());
        System.out.println("  ID: " + action.id());
        System.out.println("  Description: " + action.description());
        
        if (!action.commands().isEmpty()) {
            System.out.println("  Commands:");
            for (String command : action.commands()) {
                System.out.println("    - " + command);
            }
        }
        
        // Display status based on risk level
        if (action.risk() == Risk.CAUTION) {
            System.out.println("  Status: Suggestion only (not applied by DevDoctor)");
        } else if (action.applyable()) {
            System.out.println("  Status: Can be applied automatically");
        } else {
            System.out.println("  Status: Manual action required");
        }
    }
    
    private int applyFixes(FixPlan plan, ProcessExecutor executor, Path projectRoot) {
        // Filter to only SAFE and applyable actions
        List<FixAction> safeActions = plan.actions().stream()
            .filter(action -> action.risk() == Risk.SAFE && action.applyable())
            .collect(Collectors.toList());
        
        if (safeActions.isEmpty()) {
            System.out.println();
            System.out.println("No SAFE actions to apply.");
            return 0;
        }
        
        // Prompt user unless --yes
        if (!yes) {
            System.out.println();
            System.out.print("Apply " + safeActions.size() + " safe fix(es)? (y/n): ");
            if (!confirm()) {
                System.out.println("Cancelled.");
                return 0;
            }
        }
        
        System.out.println();
        System.out.println("Applying fixes:");
        System.out.println("==============");
        
        // Create executor with project root as working directory
        ProcessExecutor projectExecutor = new DefaultProcessExecutor(projectRoot);
        
        int applied = 0;
        int skipped = 0;
        
        for (FixAction action : safeActions) {
            try {
                // Execute each command
                boolean allSucceeded = true;
                for (String command : action.commands()) {
                    List<String> commandParts = parseCommand(command);
                    ExecResult result = projectExecutor.exec(commandParts);
                    if (result.exitCode() != 0) {
                        System.err.println("  Command failed: " + command);
                        System.err.println("  Error: " + result.stderr());
                        allSucceeded = false;
                        break;
                    }
                }
                
                if (allSucceeded) {
                    System.out.println("  Applied: " + action.title());
                    applied++;
                } else {
                    System.out.println("  Skipped: " + action.title() + " (command execution failed)");
                    skipped++;
                }
            } catch (Exception e) {
                System.err.println("  Error applying " + action.title() + ": " + e.getMessage());
                System.out.println("  Skipped: " + action.title());
                skipped++;
            }
        }
        
        // Print summary
        System.out.println();
        System.out.println("Summary: " + applied + " applied, " + skipped + " skipped");
        
        // Also show CAUTION actions that were not applied
        List<FixAction> cautionActions = plan.actions().stream()
            .filter(action -> action.risk() == Risk.CAUTION)
            .collect(Collectors.toList());
        
        if (!cautionActions.isEmpty()) {
            System.out.println();
            System.out.println("Note: " + cautionActions.size() + " CAUTION action(s) were not applied:");
            for (FixAction action : cautionActions) {
                System.out.println("  - " + action.title());
            }
        }
        
        return skipped > 0 ? 1 : 0;
    }
    
    private boolean confirm() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line = reader.readLine();
            if (line == null) {
                return false;
            }
            line = line.trim().toLowerCase();
            return line.equals("y") || line.equals("yes");
        } catch (Exception e) {
            return false;
        }
    }
    
    private List<String> parseCommand(String command) {
        // Simple command parsing - split by spaces, handle quoted strings
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (char c : command.toCharArray()) {
            if (c == '"' || c == '\'') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts;
    }
}
