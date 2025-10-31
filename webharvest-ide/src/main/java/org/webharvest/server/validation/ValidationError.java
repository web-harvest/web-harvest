package org.webharvest.server.validation;

/**
 * Single validation error
 */
public class ValidationError {
    private final String message;
    private final int line;
    private final String plugin;
    private final String type;
    
    public ValidationError(String message, int line, String plugin, String type) {
        this.message = message;
        this.line = line;
        this.plugin = plugin;
        this.type = type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public int getLine() {
        return line;
    }
    
    public String getPlugin() {
        return plugin;
    }
    
    public String getType() {
        return type;
    }
}





