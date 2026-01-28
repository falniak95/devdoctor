package com.falniak.devdoctor.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Top-level check report for JSON output.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckReport(
    @JsonProperty("tool") ToolInfo tool,
    @JsonProperty("project") ProjectInfo project,
    @JsonProperty("config") ConfigInfo config,
    @JsonProperty("summary") SummaryInfo summary,
    @JsonProperty("checks") List<CheckResultDto> checks
) {
}
