package com.falniak.devdoctor.fix;

import java.util.List;

/**
 * Represents a single fix action that can be applied.
 *
 * @param id The unique identifier of the action (typically matches check ID)
 * @param title A short title describing the action
 * @param description A detailed description of what the action does
 * @param risk The risk level of applying this action
 * @param commands List of commands to execute (may be empty)
 * @param applyable Whether this action can be automatically applied (requires non-empty commands and SAFE risk)
 */
public record FixAction(
    String id,
    String title,
    String description,
    Risk risk,
    List<String> commands,
    boolean applyable
) {
}
