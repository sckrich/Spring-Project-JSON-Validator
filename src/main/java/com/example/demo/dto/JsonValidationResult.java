package com.example.demo.dto;

import java.util.List;

/**
 * Represents the result of JSON schema validation
 * Contains validation status and list of error messages if validation failed
 */
public class JsonValidationResult {
    private boolean valid;
    private List<String> errors;
    
    /**
     * Represents the result of JSON schema validation
     * Contains validation status and list of error messages if validation failed
     */
    public JsonValidationResult(boolean valid) {
        this.valid = valid;
    }
    
    /**
     * Creates validation result with validity status and error list
     * @param valid true if JSON is valid against schema, false otherwise
     * @param errors list of validation error messages, can be null if valid is true
     */
    public JsonValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }
    
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}