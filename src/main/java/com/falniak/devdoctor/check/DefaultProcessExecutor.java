package com.falniak.devdoctor.check;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of ProcessExecutor using ProcessBuilder.
 */
public class DefaultProcessExecutor implements ProcessExecutor {

    private static final int TIMEOUT_SECONDS = 5;
    private final Path workingDirectory;

    /**
     * Creates a DefaultProcessExecutor with no specific working directory.
     */
    public DefaultProcessExecutor() {
        this.workingDirectory = null;
    }

    /**
     * Creates a DefaultProcessExecutor with a specific working directory.
     *
     * @param workingDirectory The working directory for executed commands
     */
    public DefaultProcessExecutor(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public ExecResult exec(List<String> command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);
        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory.toFile());
        }

        Process process = processBuilder.start();

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        try (BufferedReader stdoutReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
             BufferedReader stderrReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {

            // Read stdout in a separate thread to avoid blocking
            Thread stdoutThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = stdoutReader.readLine()) != null) {
                        if (stdout.length() > 0) {
                            stdout.append(System.lineSeparator());
                        }
                        stdout.append(line);
                    }
                } catch (Exception e) {
                    // Ignore read errors
                }
            });

            // Read stderr in a separate thread
            Thread stderrThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = stderrReader.readLine()) != null) {
                        if (stderr.length() > 0) {
                            stderr.append(System.lineSeparator());
                        }
                        stderr.append(line);
                    }
                } catch (Exception e) {
                    // Ignore read errors
                }
            });

            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new Exception("Process execution timed out after " + TIMEOUT_SECONDS + " seconds");
            }

            stdoutThread.join(1000);
            stderrThread.join(1000);

            int exitCode = process.exitValue();
            return new ExecResult(exitCode, stdout.toString(), stderr.toString());
        }
    }
}
