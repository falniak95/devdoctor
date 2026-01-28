package com.falniak.devdoctor;

import com.falniak.devdoctor.commands.CheckCommand;
import com.falniak.devdoctor.commands.DetectCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "devdoctor",
    description = "Project-aware CLI tool that helps developers diagnose environment and setup issues",
    subcommands = {DetectCommand.class, CheckCommand.class},
    mixinStandardHelpOptions = true
)
public class DevDoctor implements Runnable {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new DevDoctor()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        // Show help when no subcommand is provided
        CommandLine.usage(this, System.out);
    }
}
