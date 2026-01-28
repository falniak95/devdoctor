package com.falniak.devdoctor.check.requirements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads Node.js version requirements from project files.
 * Priority order: .nvmrc, .node-version, package.json engines.node
 */
public class NodeRequirementReader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Reads the Node.js version requirement from the project root.
     *
     * @param projectRoot The project root directory
     * @return Optional Requirement if found, empty otherwise
     */
    public Optional<Requirement> read(Path projectRoot) {
        // Try .nvmrc first
        Path nvmrcPath = projectRoot.resolve(".nvmrc");
        if (Files.exists(nvmrcPath) && Files.isRegularFile(nvmrcPath)) {
            try {
                String content = Files.readString(nvmrcPath).trim();
                if (!content.isEmpty()) {
                    Integer major = VersionParser.parseNodeVersion(content);
                    return Optional.of(new Requirement("node", ".nvmrc", content, major));
                }
            } catch (IOException e) {
                // Continue to next source
            }
        }

        // Try .node-version
        Path nodeVersionPath = projectRoot.resolve(".node-version");
        if (Files.exists(nodeVersionPath) && Files.isRegularFile(nodeVersionPath)) {
            try {
                String content = Files.readString(nodeVersionPath).trim();
                if (!content.isEmpty()) {
                    Integer major = VersionParser.parseNodeVersion(content);
                    return Optional.of(new Requirement("node", ".node-version", content, major));
                }
            } catch (IOException e) {
                // Continue to next source
            }
        }

        // Try package.json engines.node
        Path packageJsonPath = projectRoot.resolve("package.json");
        if (Files.exists(packageJsonPath) && Files.isRegularFile(packageJsonPath)) {
            try {
                String content = Files.readString(packageJsonPath);
                JsonNode root = OBJECT_MAPPER.readTree(content);
                JsonNode engines = root.get("engines");
                if (engines != null && engines.isObject()) {
                    JsonNode nodeVersion = engines.get("node");
                    if (nodeVersion != null && nodeVersion.isTextual()) {
                        String version = nodeVersion.asText();
                        Integer major = VersionParser.parseNodeVersion(version);
                        return Optional.of(new Requirement("node", "package.json", version, major));
                    }
                }
            } catch (IOException e) {
                // Continue (file might be invalid JSON)
            }
        }

        return Optional.empty();
    }
}
