package com.falniak.devdoctor.config;

import java.util.List;
import java.util.Set;

/**
 * Configuration for DevDoctor project checks.
 *
 * @param ignoreChecks Set of check IDs to skip during execution
 * @param requireChecks Set of check IDs that must pass
 * @param ports List of port numbers (stored for future use)
 */
public record DevDoctorConfig(
    Set<String> ignoreChecks,
    Set<String> requireChecks,
    List<Integer> ports
) {
    /**
     * Creates a config with empty defaults for missing fields.
     */
    public DevDoctorConfig {
        if (ignoreChecks == null) {
            ignoreChecks = Set.of();
        }
        if (requireChecks == null) {
            requireChecks = Set.of();
        }
        if (ports == null) {
            ports = List.of();
        }
    }
}
