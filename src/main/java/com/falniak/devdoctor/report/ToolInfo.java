package com.falniak.devdoctor.report;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Tool information for JSON output.
 */
public record ToolInfo(
    @JsonProperty("name") String name,
    @JsonProperty("version") String version
) {
}
