package com.falniak.devdoctor.check;

import java.util.List;

/**
 * Interface for executing system processes.
 */
public interface ProcessExecutor {
    /**
     * Executes a command and returns the result.
     *
     * @param command The command to execute as a list of strings
     * @return The execution result
     * @throws Exception if the process execution fails
     */
    ExecResult exec(List<String> command) throws Exception;
}
