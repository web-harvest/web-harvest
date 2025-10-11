package org.webharvest.server.manager;

import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.SessionMetrics;
import org.webharvest.runtime.tracking.TokenTracker;
import org.webharvest.runtime.tracking.TokenUsage;

/**
 * Execution job with state management and session tracking.
 * 
 * <p>Enhanced in v2.2 to include:</p>
 * <ul>
 *   <li>ScraperSession for tracking</li>
 *   <li>TokenTracker for resource monitoring</li>
 *   <li>Session metadata</li>
 * </ul>
 * 
 * @since 2.2.0
 */
public class ExecutionJob {
    
    private final String id;
    private final String configName;
    private final String configXml;
    private volatile String status = "PENDING";
    private volatile int progress = 0;
    private volatile ScraperSession session;  // v2.2: Session tracking
    private volatile TokenTracker tokenTracker;  // v2.2: Token tracking
    
    public ExecutionJob(String id, String configName, String configXml) {
        this.id = id;
        this.configName = configName;
        this.configXml = configXml;
    }
    
    public String getId() {
        return id;
    }
    
    public String getConfigName() {
        return configName;
    }
    
    public String getConfigXml() {
        return configXml;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }
    
    // v2.2: Session management
    
    public ScraperSession getSession() {
        return session;
    }
    
    public void setSession(ScraperSession session) {
        this.session = session;
    }
    
    public TokenTracker getTokenTracker() {
        return tokenTracker;
    }
    
    public void setTokenTracker(TokenTracker tokenTracker) {
        this.tokenTracker = tokenTracker;
    }
    
    /**
     * Returns session metadata as formatted string for logging.
     */
    public String getSessionInfo() {
        if (session == null) {
            return "No session";
        }
        
        SessionMetrics metrics = session.getMetrics();
        return String.format("Session[id=%s, client=%s, duration=%s, elements=%d]",
            session.getSessionId().substring(0, 8) + "...",
            session.getClientId(),
            metrics.getDuration(),
            metrics.getProcessedElements());
    }
    
    /**
     * Returns token usage as formatted string for logging.
     */
    public String getTokenUsageInfo() {
        if (tokenTracker == null) {
            return "Tokens: not tracked";
        }
        
        TokenUsage usage = tokenTracker.getUsage();
        return String.format("Tokens[HTTP: %d req, %d bytes, CPU: %dms, Mem: %d bytes]",
            usage.getHttpRequests(),
            usage.getHttpBytes(),
            usage.getCpuTimeMillis(),
            usage.getMemoryPeakBytes());
    }
}

