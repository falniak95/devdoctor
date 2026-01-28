package com.falniak.devdoctor.check.render;

import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckStatus;
import com.falniak.devdoctor.config.DevDoctorConfig;
import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectType;
import com.falniak.devdoctor.report.CheckReport;
import com.falniak.devdoctor.report.CheckResultDto;
import com.falniak.devdoctor.report.ConfigInfo;
import com.falniak.devdoctor.report.ProjectInfo;
import com.falniak.devdoctor.report.SummaryInfo;
import com.falniak.devdoctor.report.ToolInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Renders check results to JSON format.
 */
public class JsonRenderer {
    private static final String TOOL_NAME = "devdoctor";
    private static final String TOOL_VERSION = "1.0-SNAPSHOT";

    /**
     * Renders the detection result and check results to JSON.
     *
     * @param detectionResult The project detection result
     * @param results The list of check results to render (includes all checks, not filtered)
     * @param config Optional configuration
     * @param configPath Path to the config file if loaded, null otherwise
     * @param pretty Whether to pretty-print the JSON
     */
    public void render(
        DetectionResult detectionResult,
        List<CheckResult> results,
        Optional<DevDoctorConfig> config,
        Path configPath,
        boolean pretty
    ) {
        try {
            CheckReport report = buildReport(detectionResult, results, config, configPath);
            
            ObjectMapper mapper = new ObjectMapper();
            if (pretty) {
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
            }
            
            String json = mapper.writeValueAsString(report);
            System.out.println(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to render JSON output", e);
        }
    }

    private CheckReport buildReport(
        DetectionResult detectionResult,
        List<CheckResult> results,
        Optional<DevDoctorConfig> config,
        Path configPath
    ) {
        // Build tool info
        ToolInfo tool = new ToolInfo(TOOL_NAME, TOOL_VERSION);

        // Build project info
        List<String> projectTypes = detectionResult.types().stream()
            .map(ProjectType::displayName)
            .collect(Collectors.toList());
        ProjectInfo project = new ProjectInfo(detectionResult.root(), projectTypes);

        // Build config info
        ConfigInfo configInfo = null;
        if (config.isPresent() || configPath != null) {
            DevDoctorConfig cfg = config.orElse(new DevDoctorConfig(null, null, null));
            configInfo = new ConfigInfo(
                configPath,
                cfg.ignoreChecks(),
                cfg.requireChecks()
            );
        }

        // Build summary
        Map<CheckStatus, Integer> counts = new HashMap<>();
        for (CheckStatus status : CheckStatus.values()) {
            counts.put(status, 0);
        }
        for (CheckResult result : results) {
            counts.put(result.status(), counts.get(result.status()) + 1);
        }
        SummaryInfo summary = new SummaryInfo(
            counts.get(CheckStatus.PASS),
            counts.get(CheckStatus.WARN),
            counts.get(CheckStatus.FAIL),
            counts.get(CheckStatus.INFO),
            counts.get(CheckStatus.NOT_APPLICABLE)
        );

        // Build check results (include all, no filtering)
        List<CheckResultDto> checkDtos = results.stream()
            .map(CheckResultDto::from)
            .collect(Collectors.toList());

        return new CheckReport(tool, project, configInfo, summary, checkDtos);
    }
}
