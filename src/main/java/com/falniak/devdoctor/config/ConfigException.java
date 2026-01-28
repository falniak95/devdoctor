package com.falniak.devdoctor.config;

/**
 * Exception thrown when configuration loading or validation fails.
 */
public class ConfigException extends RuntimeException {
    
    public ConfigException(String message) {
        super(message);
    }
    
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
