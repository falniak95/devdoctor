package com.falniak.devdoctor.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.falniak.devdoctor.check.Risk;

import java.util.List;

/**
 * Suggestion DTO for JSON output.
 */
public record SuggestionDto(
    @JsonProperty("message") String message,
    @JsonProperty("commands") List<String> commands,
    @JsonProperty("risk") String risk
) {
}
