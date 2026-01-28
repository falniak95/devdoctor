package com.falniak.devdoctor.commands;

import com.falniak.devdoctor.check.Check;
import com.falniak.devdoctor.check.CheckContext;
import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckRunner;
import com.falniak.devdoctor.check.CheckStatus;
import com.falniak.devdoctor.check.ComposeProjectInfoCheck;
import com.falniak.devdoctor.check.DefaultProcessExecutor;
import com.falniak.devdoctor.check.DockerCheck;
import com.falniak.devdoctor.check.GitCheck;
import com.falniak.devdoctor.check.JavaCheck;
import com.falniak.devdoctor.check.JavaProjectInfoCheck;
import com.falniak.devdoctor.check.NodeCheck;
import com.falniak.devdoctor.check.NodeProjectInfoCheck;
import com.falniak.devdoctor.check.ProcessExecutor;
import com.falniak.devdoctor.check.render.ConsoleRenderer;
import com.falniak.devdoctor.check.render.JsonRenderer;
import com.falniak.devdoctor.config.ConfigException;
import com.falniak.devdoctor.config.ConfigLoader;
import com.falniak.devdoctor.config.DevDoctorConfig;
import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectDetector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Command(
    name = "check",
    description = "Run environment and project checks"
)
public class CheckCommand implements java.util.concurrent.Callable<Integer> {

    @Option(
        names = {"--path", "-p"},
        description = "Path to the project directory (default: current directory)",
        defaultValue = "."
    )
    private String path;

    @Option(
        names = "--system-only",
        description = "Run only system-level checks"
    )
    private boolean systemOnly;

    @Option(
        names = "--project-only",
        description = "Run only project-level checks"
    )
    private boolean projectOnly;

    @Option(
        names = "--show-na",
        description = "Show not applicable checks in output"
    )
    private boolean showNa;

    @Option(
        names = "--verbose",
        description = "Show detailed output including details and suggestions"
    )
    private boolean verbose;

    @Option(
        names = "--config",
        description = "Path to the config file (default: .devdoctor.yml in project root)"
    )
    private String configPath;

    @Option(
        names = "--json",
        description = "Output results as JSON"
    )
    private boolean json;

    @Option(
        names = "--json-pretty",
        description = "Output results as pretty-printed JSON (implies --json)"
    )
    private boolean jsonPretty;

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
            Path loadedConfigPath = null;
            
            try {
                if (configPath != null) {
                    // Explicit config path provided
                    Path explicitPath = Paths.get(configPath).toAbsolutePath().normalize();
                    DevDoctorConfig loadedConfig = configLoader.loadFromExplicitPath(explicitPath);
                    config = Optional.of(loadedConfig);
                    loadedConfigPath = explicitPath;
                } else {
                    // Try to load from project root
                    config = configLoader.loadFromProjectRoot(detectionResult.root());
                    if (config.isPresent()) {
                        loadedConfigPath = configLoader.defaultConfigPath(detectionResult.root());
                    }
                }
            } catch (ConfigException e) {
                System.err.println("Error loading config: " + e.getMessage());
                return 2;
            }
            
            // Print config status (only in non-JSON mode)
            if (!json && !jsonPretty) {
                if (loadedConfigPath != null) {
                    System.out.println("Config: loaded from " + loadedConfigPath);
                } else {
                    System.out.println("Config: none");
                }
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
            
            // Build check list based on flags
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
            
            // Determine failed required checks
            Set<String> failedRequiredChecks = Set.of();
            if (config.isPresent() && !config.get().requireChecks().isEmpty()) {
                Set<String> requireSet = config.get().requireChecks();
                failedRequiredChecks = results.stream()
                    .filter(result -> requireSet.contains(result.id()))
                    .filter(result -> result.status() == CheckStatus.FAIL)
                    .map(CheckResult::id)
                    .collect(Collectors.toSet());
            }
            
            // Print output
            if (json || jsonPretty) {
                // JSON output mode
                JsonRenderer jsonRenderer = new JsonRenderer();
                jsonRenderer.render(
                    detectionResult,
                    results,
                    config,
                    loadedConfigPath,
                    jsonPretty
                );
            } else {
                // Console output mode
                ConsoleRenderer renderer = new ConsoleRenderer(showNa, verbose);
                renderer.render(detectionResult, results, failedRequiredChecks);
            }
            
            // Calculate exit code
            boolean hasFailures = results.stream()
                .anyMatch(r -> r.status() == CheckStatus.FAIL);
            boolean hasFailedRequired = !failedRequiredChecks.isEmpty();
            
            if (hasFailures || hasFailedRequired) {
                return 1;
            }
            return 0;
        } catch (Exception e) {
            // Unexpected exception during execution
            System.err.println("Unexpected error: " + e.getMessage());
            return 2;
        }
    }
    
    private List<Check> buildCheckList() {
        List<Check> checks = new ArrayList<>();
        
        if (!projectOnly) {
            // System checks
            checks.add(new JavaCheck());
            checks.add(new GitCheck());
            checks.add(new NodeCheck());
            checks.add(new DockerCheck());
        }
        
        if (!systemOnly) {
            // Project checks
            checks.add(new JavaProjectInfoCheck());
            checks.add(new NodeProjectInfoCheck());
            checks.add(new ComposeProjectInfoCheck());
        }
        
        return checks;
    }
}
