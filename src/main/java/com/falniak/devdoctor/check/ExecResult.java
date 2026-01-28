package com.falniak.devdoctor.check;

/**
 * Represents the result of executing a process.
 *
 * @param exitCode The exit code of the process
 * @param stdout The standard output
 * @param stderr The standard error output
 */
public record ExecResult(
    int exitCode,
    String stdout,
    String stderr
) {
}
