package com.falniak.devdoctor.commands;

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
        System.out.println("Target path: " + targetPath);
        System.out.println("Not implemented yet");
    }
}
