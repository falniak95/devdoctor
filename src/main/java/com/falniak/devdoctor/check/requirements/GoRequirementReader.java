package com.falniak.devdoctor.check.requirements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads Go version requirements from go.mod file.
 */
public class GoRequirementReader {

    /**
     * Reads the Go version requirement from go.mod.
     *
     * @param projectRoot The project root directory
     * @return Optional Requirement if found, empty otherwise
     */
    public Optional<Requirement> read(Path projectRoot) {
        Path goModPath = projectRoot.resolve("go.mod");
        if (!Files.exists(goModPath) || !Files.isRegularFile(goModPath)) {
            return Optional.empty();
        }

        try {
            // Read go.mod and find the line starting with "go "
            for (String line : Files.readAllLines(goModPath)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("go ")) {
                    String version = trimmed.substring(3).trim();
                    Integer major = VersionParser.parseGoVersion(trimmed);
                    return Optional.of(new Requirement("go", "go.mod", version, major));
                }
            }
        } catch (IOException e) {
            // Return empty on any error
        }

        return Optional.empty();
    }
}
