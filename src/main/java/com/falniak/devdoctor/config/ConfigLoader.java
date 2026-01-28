package com.falniak.devdoctor.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads DevDoctor configuration from YAML files.
 */
public class ConfigLoader {
    
    private final Yaml yaml;
    
    public ConfigLoader() {
        this.yaml = new Yaml();
    }
    
    /**
     * Loads configuration from the project root directory.
     * Looks for .devdoctor.yml in the given project root.
     *
     * @param projectRoot The project root directory
     * @return Optional containing the config if found and valid, empty if file doesn't exist
     * @throws ConfigException if the file exists but is invalid
     */
    public Optional<DevDoctorConfig> loadFromProjectRoot(Path projectRoot) {
        Path configPath = defaultConfigPath(projectRoot);
        if (!Files.exists(configPath) || !Files.isRegularFile(configPath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(loadFromExplicitPath(configPath));
        } catch (ConfigException e) {
            throw new ConfigException("Failed to load config from project root: " + e.getMessage(), e);
        }
    }
    
    /**
     * Loads configuration from an explicit file path.
     *
     * @param configPath The path to the config file
     * @return The loaded configuration
     * @throws ConfigException if the file doesn't exist or is invalid
     */
    public DevDoctorConfig loadFromExplicitPath(Path configPath) throws ConfigException {
        if (!Files.exists(configPath)) {
            throw new ConfigException("Config file not found: " + configPath);
        }
        if (!Files.isRegularFile(configPath)) {
            throw new ConfigException("Config path is not a regular file: " + configPath);
        }
        
        try {
            String content = Files.readString(configPath);
            return parseYaml(content, configPath);
        } catch (IOException e) {
            throw new ConfigException("Failed to read config file: " + configPath + " - " + e.getMessage(), e);
        } catch (ConfigException e) {
            throw new ConfigException("Invalid config file " + configPath + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Returns the default config file path for a project root.
     *
     * @param projectRoot The project root directory
     * @return The path to .devdoctor.yml in the project root
     */
    public Path defaultConfigPath(Path projectRoot) {
        return projectRoot.resolve(".devdoctor.yml");
    }
    
    private DevDoctorConfig parseYaml(String content, Path configPath) throws ConfigException {
        try {
            Object parsed = yaml.load(content);
            if (parsed == null) {
                // Empty YAML file
                return new DevDoctorConfig(Set.of(), Set.of(), List.of());
            }
            
            if (!(parsed instanceof Map)) {
                throw new ConfigException("Config must be a YAML mapping (key-value pairs)");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = (Map<String, Object>) parsed;
            
            Set<String> ignoreChecks = parseStringSet(configMap, "ignore_checks", configPath);
            Set<String> requireChecks = parseStringSet(configMap, "require_checks", configPath);
            List<Integer> ports = parseIntegerList(configMap, "ports", configPath);
            
            return new DevDoctorConfig(ignoreChecks, requireChecks, ports);
        } catch (ClassCastException e) {
            throw new ConfigException("Invalid YAML structure: " + e.getMessage(), e);
        }
    }
    
    private Set<String> parseStringSet(Map<String, Object> configMap, String key, Path configPath) throws ConfigException {
        Object value = configMap.get(key);
        if (value == null) {
            return Set.of();
        }
        
        if (!(value instanceof List)) {
            throw new ConfigException("'" + key + "' must be a sequence (list) of strings");
        }
        
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) value;
        Set<String> result = new HashSet<>();
        
        for (Object item : list) {
            if (item == null) {
                throw new ConfigException("'" + key + "' contains null values");
            }
            if (!(item instanceof String)) {
                throw new ConfigException("'" + key + "' must contain only strings, found: " + item.getClass().getSimpleName());
            }
            result.add((String) item);
        }
        
        return result;
    }
    
    private List<Integer> parseIntegerList(Map<String, Object> configMap, String key, Path configPath) throws ConfigException {
        Object value = configMap.get(key);
        if (value == null) {
            return List.of();
        }
        
        if (!(value instanceof List)) {
            throw new ConfigException("'ports' must be a sequence (list) of integers");
        }
        
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) value;
        List<Integer> result = new ArrayList<>();
        
        for (Object item : list) {
            if (item == null) {
                throw new ConfigException("'ports' contains null values");
            }
            
            Integer intValue = null;
            if (item instanceof Integer) {
                intValue = (Integer) item;
            } else if (item instanceof String) {
                try {
                    intValue = Integer.parseInt((String) item);
                } catch (NumberFormatException e) {
                    throw new ConfigException("'ports' contains non-integer string: " + item);
                }
            } else {
                throw new ConfigException("'ports' must contain only integers or integer strings, found: " + item.getClass().getSimpleName());
            }
            
            result.add(intValue);
        }
        
        return result;
    }
}
