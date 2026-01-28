package com.falniak.devdoctor.check;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fake ProcessExecutor for testing that allows configuring command results.
 */
public class FakeProcessExecutor implements ProcessExecutor {

    private final Map<String, ExecResult> commandResults = new HashMap<>();
    private final Map<String, Exception> commandExceptions = new HashMap<>();

    /**
     * Configures a command to return a specific result.
     *
     * @param command The command (first element of the command list)
     * @param result The result to return
     */
    public void setResult(String command, ExecResult result) {
        commandResults.put(command, result);
    }

    /**
     * Configures a command to throw an exception.
     *
     * @param command The command (first element of the command list)
     * @param exception The exception to throw
     */
    public void setException(String command, Exception exception) {
        commandExceptions.put(command, exception);
    }

    @Override
    public ExecResult exec(List<String> command) throws Exception {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Command cannot be empty");
        }

        String commandKey = command.get(0);

        if (commandExceptions.containsKey(commandKey)) {
            throw commandExceptions.get(commandKey);
        }

        if (commandResults.containsKey(commandKey)) {
            return commandResults.get(commandKey);
        }

        // Default: return failure
        return new ExecResult(1, "", "Command not configured: " + commandKey);
    }
}
