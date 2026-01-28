package com.falniak.devdoctor.check;

import java.util.List;

/**
 * Represents a suggestion for resolving a check issue.
 *
 * @param message The suggestion message
 * @param commands List of commands that can be executed (may be empty)
 * @param risk The risk level of executing these commands
 */
public record Suggestion(
    String message,
    List<String> commands,
    Risk risk
) {
}
