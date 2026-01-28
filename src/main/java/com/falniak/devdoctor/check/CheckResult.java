package com.falniak.devdoctor.check;

import java.util.List;

/**
 * Represents the result of executing a check.
 *
 * @param id The unique identifier of the check
 * @param status The status of the check
 * @param summary A brief summary of the check result
 * @param details Additional details (nullable)
 * @param suggestions List of suggestions for resolving issues (may be empty)
 */
public record CheckResult(
    String id,
    CheckStatus status,
    String summary,
    String details,
    List<Suggestion> suggestions
) {
}
