package com.falniak.devdoctor.check;

/**
 * Interface for checks that can be executed.
 */
public interface Check {
    /**
     * Returns the unique identifier of this check.
     *
     * @return The check ID
     */
    String id();

    /**
     * Runs this check with the given context.
     *
     * @param ctx The check context
     * @return The check result
     */
    CheckResult run(CheckContext ctx);
}
