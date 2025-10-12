package org.webharvest.server.manager;

import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.SessionMetrics;
import org.webharvest.runtime.tracking.TokenTracker;
import org.webharvest.runtime.tracking.TokenUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Execution job with state management and session tracking.
 * 
 * <p>Enhanced in v2.2 to include:</p>
 * <ul>
 *   <li>ScraperSession for tracking</li>
 *   <li>TokenTracker for resource monitoring</li>
 *   <li>Session metadata</li>
 *   <li>Per-job log storage (Apple HIG - each tab has own logs)</li>
 *   <li>Per-job results storage</li>
 * </ul>
 * 
 * @author Robert Bala
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
    
    // v2.2: Per-job storage (Apple HIG - each document has own state)
    private final List<LogEntry> logs = new CopyOnWriteArrayList<>();
    private final Map<String, String> results = new ConcurrentHashMap<>();
    private volatile String lastMessage = "";
    
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
    
    // v2.2: Log storage (Apple HIG - each tab keeps its own logs)
    
    public void addLog(String level, String message) {
        logs.add(new LogEntry(level, message));
    }
    
    public List<LogEntry> getAllLogs() {
        return new ArrayList<>(logs);
    }
    
    public int getLogCount() {
        return logs.size();
    }
    
    // v2.2: Results storage
    
    public void setResults(Map<String, String> results) {
        this.results.clear();
        if (results != null) {
            this.results.putAll(results);
        }
    }
    
    public Map<String, String> getResults() {
        return new ConcurrentHashMap<>(results);
    }
    
    public void setLastMessage(String message) {
        this.lastMessage = message;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }
}

