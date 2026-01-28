package com.falniak.devdoctor.fix;

import java.util.List;

/**
 * Represents a plan of fix actions to resolve check failures.
 *
 * @param actions List of fix actions to apply
 */
public record FixPlan(
    List<FixAction> actions
) {
}
