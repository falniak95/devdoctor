package com.falniak.devdoctor.commands;

import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectDetector;
import com.falniak.devdoctor.detect.ProjectType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;

@Command(
    name = "detect",
    description = "Detect project type and capabilities"
)
public class DetectCommand implements Runnable {

    @Option(
        names = {"--path", "-p"},
        description = "Path to the project directory (default: current directory)",
        defaultValue = "."
    )
    private String path;

    @Override
    public void run() {
        Path targetPath = Paths.get(path).toAbsolutePath().normalize();
        ProjectDetector detector = new ProjectDetector();
        DetectionResult result = detector.detect(targetPath);

        System.out.println("Project root: " + result.root());
        System.out.println("Detected project types:");
        
        if (result.types().isEmpty()) {
            System.out.println("  None");
        } else {
            for (ProjectType type : result.types()) {
                System.out.println("  - " + type.displayName());
            }
        }
    }
}
