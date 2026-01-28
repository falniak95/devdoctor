package com.falniak.devdoctor.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.util.List;

/**
 * Project information for JSON output.
 */
public record ProjectInfo(
    @JsonProperty("root") String root,
    @JsonProperty("types") List<String> types
) {
    public ProjectInfo(Path root, List<String> types) {
        this(root.toString(), types);
    }
}
