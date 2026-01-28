package com.falniak.devdoctor.check;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a list of checks and collects their results.
 */
public class CheckRunner {

    /**
     * Runs the given checks in order and returns their results.
     *
     * @param checks The list of checks to run
     * @param context The context to pass to each check
     * @return List of check results in the same order as the input checks
     */
    public List<CheckResult> runChecks(List<Check> checks, CheckContext context) {
        List<CheckResult> results = new ArrayList<>();
        for (Check check : checks) {
            CheckResult result = check.run(context);
            results.add(result);
        }
        return results;
    }
}
