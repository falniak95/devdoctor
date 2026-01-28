package com.falniak.devdoctor.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Configuration information for JSON output.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConfigInfo(
    @JsonProperty("path") String path,
    @JsonProperty("ignoreChecks") Set<String> ignoreChecks,
    @JsonProperty("requireChecks") Set<String> requireChecks
) {
    public ConfigInfo(Path path, Set<String> ignoreChecks, Set<String> requireChecks) {
        this(path != null ? path.toString() : null, ignoreChecks, requireChecks);
    }
}
