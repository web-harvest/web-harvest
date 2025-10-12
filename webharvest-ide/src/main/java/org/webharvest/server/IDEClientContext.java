package org.webharvest.server;

import java.util.UUID;

/**
 * IDE-specific client context with generated ID at startup.
 * 
 * <p>Each IDE instance gets a unique client ID that persists
 * for the lifetime of the IDE process. This enables:</p>
 * <ul>
 *   <li>Session tracking per IDE instance</li>
 *   <li>Resource usage monitoring</li>
 *   <li>Multi-IDE scenarios (future)</li>
 * </ul>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class IDEClientContext {
    
    private static final String IDE_CLIENT_ID = "IDE-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String IDE_VERSION = "2.2.0";
    private static final long START_TIME = System.currentTimeMillis();
    
    /**
     * Returns the unique client ID for this IDE instance.
     * 
     * @return the client ID (constant for IDE lifetime)
     */
    public static String getClientId() {
        return IDE_CLIENT_ID;
    }
    
    /**
     * Returns the IDE version.
     * 
     * @return the version string
     */
    public static String getVersion() {
        return IDE_VERSION;
    }
    
    /**
     * Returns when the IDE was started.
     * 
     * @return startup timestamp in milliseconds
     */
    public static long getStartTime() {
        return START_TIME;
    }
    
    /**
     * Returns IDE uptime in milliseconds.
     * 
     * @return uptime in milliseconds
     */
    public static long getUptime() {
        return System.currentTimeMillis() - START_TIME;
    }
    
    /**
     * Returns formatted client info for logging.
     * 
     * @return formatted string with client ID and version
     */
    public static String getClientInfo() {
        return String.format("IDE Client: %s (v%s, uptime: %dmin)",
            IDE_CLIENT_ID,
            IDE_VERSION,
            getUptime() / 60000);
    }
}


