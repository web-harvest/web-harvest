package org.webharvest.server.manager;

/**
 * Represents a single log entry with timestamp and level.
 * 
 * <p>Used to store execution logs per job for later retrieval.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class LogEntry {
    
    private final long timestamp;
    private final String level;
    private final String message;
    
    public LogEntry(String level, String message) {
        this.timestamp = System.currentTimeMillis();
        this.level = level;
        this.message = message;
    }
    
    public LogEntry(long timestamp, String level, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getLevel() {
        return level;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * Converts to JSON string for WebSocket transmission.
     */
    public String toJson() {
        return String.format(
            "{\"type\":\"log\",\"level\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}",
            level, 
            escapeJson(message), 
            timestamp
        );
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", 
            new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(timestamp)),
            level, 
            message);
    }
}


