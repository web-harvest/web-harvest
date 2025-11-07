package org.webharvest.server.validation;

import java.util.List;

/**
 * Result of validation operation
 */
public class ValidationResult {
    private final boolean valid;
    private final List<ValidationError> errors;
    
    public ValidationResult(boolean valid, List<ValidationError> errors) {
        this.valid = valid;
        this.errors = errors;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<ValidationError> getErrors() {
        return errors;
    }
}







