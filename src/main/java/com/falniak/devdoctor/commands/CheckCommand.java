package com.falniak.devdoctor.commands;

import com.falniak.devdoctor.check.Check;
import com.falniak.devdoctor.check.CheckContext;
import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckRunner;
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
import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectDetector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Command(
    name = "check",
    description = "Run environment and project checks"
)
public class CheckCommand implements Runnable {

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

    @Override
    public void run() {
        Path targetPath = Paths.get(path).toAbsolutePath().normalize();
        
        // Detect project
        ProjectDetector detector = new ProjectDetector();
        DetectionResult detectionResult = detector.detect(targetPath);
        
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
        
        // Run checks
        CheckRunner runner = new CheckRunner();
        List<CheckResult> results = runner.runChecks(checks, context);
        
        // Print output
        ConsoleRenderer renderer = new ConsoleRenderer(showNa, verbose);
        renderer.render(detectionResult, results);
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
