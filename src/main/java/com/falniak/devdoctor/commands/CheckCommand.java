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
import com.falniak.devdoctor.check.Suggestion;
import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectDetector;
import com.falniak.devdoctor.detect.ProjectType;
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
        printResults(detectionResult, results);
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
    
    private void printResults(DetectionResult detectionResult, List<CheckResult> results) {
        System.out.println("Project root: " + detectionResult.root());
        System.out.println("Detected types:");
        
        if (detectionResult.types().isEmpty()) {
            System.out.println("  None");
        } else {
            for (ProjectType type : detectionResult.types()) {
                System.out.println("  " + type);
            }
        }
        
        System.out.println();
        
        for (CheckResult result : results) {
            String statusStr = "[" + result.status() + "]";
            System.out.println(statusStr + " " + result.id() + " - " + result.summary());
            
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
