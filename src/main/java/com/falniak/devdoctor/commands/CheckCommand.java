package com.falniak.devdoctor.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;

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
        System.out.println("Target path: " + targetPath);
        System.out.println("System-only: " + systemOnly);
        System.out.println("Project-only: " + projectOnly);
        System.out.println("Not implemented yet");
    }
}
