package com.falniak.devdoctor.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void testLoadValidYamlWithAllFields() throws IOException {
        String yaml = """
            ignore_checks:
              - system.java
              - system.docker
            require_checks:
              - system.git
            ports:
              - 8080
              - 3000
            """;
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        DevDoctorConfig config = loader.loadFromExplicitPath(configFile);
        
        assertEquals(Set.of("system.java", "system.docker"), config.ignoreChecks());
        assertEquals(Set.of("system.git"), config.requireChecks());
        assertEquals(List.of(8080, 3000), config.ports());
    }

    @Test
    void testLoadValidYamlWithMissingFields() throws IOException {
        String yaml = """
            ignore_checks:
              - system.java
            """;
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        DevDoctorConfig config = loader.loadFromExplicitPath(configFile);
        
        assertEquals(Set.of("system.java"), config.ignoreChecks());
        assertEquals(Set.of(), config.requireChecks());
        assertEquals(List.of(), config.ports());
    }

    @Test
    void testLoadEmptyYaml() throws IOException {
        String yaml = "";
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        DevDoctorConfig config = loader.loadFromExplicitPath(configFile);
        
        assertEquals(Set.of(), config.ignoreChecks());
        assertEquals(Set.of(), config.requireChecks());
        assertEquals(List.of(), config.ports());
    }

    @Test
    void testLoadFromProjectRootFindsFile() throws IOException {
        String yaml = """
            ignore_checks:
              - system.java
            """;
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        Optional<DevDoctorConfig> config = loader.loadFromProjectRoot(tempDir);
        
        assertTrue(config.isPresent());
        assertEquals(Set.of("system.java"), config.get().ignoreChecks());
    }

    @Test
    void testLoadFromProjectRootReturnsEmptyWhenMissing() {
        ConfigLoader loader = new ConfigLoader();
        Optional<DevDoctorConfig> config = loader.loadFromProjectRoot(tempDir);
        
        assertFalse(config.isPresent());
    }

    @Test
    void testLoadFromExplicitPathWithValidPath() throws IOException {
        String yaml = """
            require_checks:
              - system.git
            """;
        
        Path configFile = tempDir.resolve("custom-config.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        DevDoctorConfig config = loader.loadFromExplicitPath(configFile);
        
        assertEquals(Set.of("system.git"), config.requireChecks());
    }

    @Test
    void testLoadFromExplicitPathThrowsWhenFileMissing() {
        Path configFile = tempDir.resolve("nonexistent.yml");
        
        ConfigLoader loader = new ConfigLoader();
        ConfigException exception = assertThrows(ConfigException.class, () -> {
            loader.loadFromExplicitPath(configFile);
        });
        
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void testInvalidYamlStructureThrowsException() throws IOException {
        // Use a list instead of a mapping - should throw exception
        String yaml = """
            - item1
            - item2
            """;
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        ConfigException exception = assertThrows(ConfigException.class, () -> {
            loader.loadFromExplicitPath(configFile);
        });
        
        assertTrue(exception.getMessage().contains("mapping") || exception.getMessage().contains("Invalid"));
    }

    @Test
    void testIgnoreChecksMustBeSequence() throws IOException {
        String yaml = """
            ignore_checks: "not a list"
            """;
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        ConfigException exception = assertThrows(ConfigException.class, () -> {
            loader.loadFromExplicitPath(configFile);
        });
        
        assertTrue(exception.getMessage().contains("must be a sequence"));
    }

    @Test
    void testIgnoreChecksMustContainStrings() throws IOException {
        String yaml = """
            ignore_checks:
              - system.java
              - 123
            """;
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        ConfigException exception = assertThrows(ConfigException.class, () -> {
            loader.loadFromExplicitPath(configFile);
        });
        
        assertTrue(exception.getMessage().contains("must contain only strings"));
    }

    @Test
    void testPortsMustBeSequence() throws IOException {
        String yaml = """
            ports: 8080
            """;
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        ConfigException exception = assertThrows(ConfigException.class, () -> {
            loader.loadFromExplicitPath(configFile);
        });
        
        assertTrue(exception.getMessage().contains("must be a sequence"));
    }

    @Test
    void testPortsAsStringNumbersAreParsed() throws IOException {
        String yaml = """
            ports:
              - "8080"
              - "3000"
            """;
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        DevDoctorConfig config = loader.loadFromExplicitPath(configFile);
        
        assertEquals(List.of(8080, 3000), config.ports());
    }

    @Test
    void testPortsAsIntegersAreParsed() throws IOException {
        String yaml = """
            ports:
              - 8080
              - 3000
            """;
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        DevDoctorConfig config = loader.loadFromExplicitPath(configFile);
        
        assertEquals(List.of(8080, 3000), config.ports());
    }

    @Test
    void testPortsInvalidStringThrowsException() throws IOException {
        String yaml = """
            ports:
              - "not-a-number"
            """;
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        ConfigException exception = assertThrows(ConfigException.class, () -> {
            loader.loadFromExplicitPath(configFile);
        });
        
        assertTrue(exception.getMessage().contains("non-integer string"));
    }

    @Test
    void testPortsInvalidTypeThrowsException() throws IOException {
        String yaml = """
            ports:
              - true
            """;
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        ConfigException exception = assertThrows(ConfigException.class, () -> {
            loader.loadFromExplicitPath(configFile);
        });
        
        assertTrue(exception.getMessage().contains("must contain only integers"));
    }

    @Test
    void testDefaultConfigPath() {
        ConfigLoader loader = new ConfigLoader();
        Path configPath = loader.defaultConfigPath(tempDir);
        
        assertEquals(tempDir.resolve(".devdoctor.yml"), configPath);
    }

    @Test
    void testLoadFromProjectRootThrowsWhenFileInvalid() throws IOException {
        String yaml = """
            ignore_checks: "invalid"
            """;
        
        Path configFile = tempDir.resolve(".devdoctor.yml");
        Files.writeString(configFile, yaml);
        
        ConfigLoader loader = new ConfigLoader();
        ConfigException exception = assertThrows(ConfigException.class, () -> {
            loader.loadFromProjectRoot(tempDir);
        });
        
        assertTrue(exception.getMessage().contains("Failed to load config from project root"));
    }
}
