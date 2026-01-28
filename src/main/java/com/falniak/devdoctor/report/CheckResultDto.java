package com.falniak.devdoctor.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckStatus;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Check result DTO for JSON output.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckResultDto(
    @JsonProperty("id") String id,
    @JsonProperty("status") String status,
    @JsonProperty("summary") String summary,
    @JsonProperty("details") String details,
    @JsonProperty("suggestions") List<SuggestionDto> suggestions
) {
    /**
     * Creates a CheckResultDto from a CheckResult.
     */
    public static CheckResultDto from(CheckResult result) {
        List<SuggestionDto> suggestions = result.suggestions() != null
            ? result.suggestions().stream()
                .map(s -> new SuggestionDto(
                    s.message(),
                    s.commands(),
                    s.risk().name()
                ))
                .collect(Collectors.toList())
            : List.of();

        return new CheckResultDto(
            result.id(),
            result.status().name(),
            result.summary(),
            result.details(),
            suggestions.isEmpty() ? null : suggestions
        );
    }
}
