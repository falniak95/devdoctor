package com.falniak.devdoctor.report;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Summary information with status counts for JSON output.
 */
public record SummaryInfo(
    @JsonProperty("pass") int pass,
    @JsonProperty("warn") int warn,
    @JsonProperty("fail") int fail,
    @JsonProperty("info") int info,
    @JsonProperty("notApplicable") int notApplicable
) {
}
