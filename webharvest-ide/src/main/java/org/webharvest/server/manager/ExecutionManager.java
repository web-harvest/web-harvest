package org.webharvest.server.manager;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.Harvest;
import org.webharvest.Harvester;
import org.webharvest.HarvestLoadCallback;
import org.webharvest.definition.BufferConfigSource;
import org.webharvest.definition.ConfigSource;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.KeyValuePair;
import org.webharvest.server.IDEClientContext;
import org.webharvest.runtime.tracking.TokenTracker;
import org.webharvest.runtime.tracking.TokenTrackerImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Manages concurrent scraper executions
 * 
 * Features:
 * - Thread pool for concurrent executions (max 5)
 * - Pause/Resume/Stop control
 * - Progress tracking
 * - WebSocket notifications
 */
public class ExecutionManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExecutionManager.class);
    private static final ExecutionManager INSTANCE = new ExecutionManager();
    
    private final ExecutorService executorService;
    private final Map<String, ExecutionJob> activeExecutions;
    private final Map<String, ExecutionJob> completedExecutions;  // v2.2: Keep completed jobs
    private final Map<String, org.eclipse.jetty.websocket.api.Session> webSocketSessions;
    private final Injector injector;
    private final Harvest harvest;
    
    private ExecutionManager() {
        LOG.info("Initializing ExecutionManager...");
        
        this.executorService = Executors.newFixedThreadPool(5); // Max 5 concurrent executions
        this.activeExecutions = new ConcurrentHashMap<>();
        this.completedExecutions = new ConcurrentHashMap<>();  // v2.2: Store completed jobs
        this.webSocketSessions = new ConcurrentHashMap<>();
        
        LOG.info("Creating Guice injector for Harvest API...");
        
        // Initialize Guice injector
        // Note: HttpModule removed during HttpComponents 4.x migration
        this.injector = Guice.createInjector(
            new ScraperModule(".")
        );
        this.harvest = injector.getInstance(Harvest.class);
        
        LOG.info("ExecutionManager initialized successfully");
        LOG.info("Thread pool size: 5, max concurrent executions: 5");
    }
    
    public static ExecutionManager getInstance() {
        return INSTANCE;
    }
    
    public void submit(ExecutionRequest request) {
        ExecutionJob job = new ExecutionJob(
                request.getExecutionId(),
                request.getConfigName(),
                request.getConfigXml()
        );
        
        activeExecutions.put(job.getId(), job);
        LOG.info("Submitting execution: {} ({})", job.getId(), job.getConfigName());
        
        executorService.submit(() -> {
            try {
                executeJob(job);
            } catch (Exception e) {
                LOG.error("Execution failed: {}", job.getId(), e);
                job.addLog("ERROR", "Execution failed: " + e.getMessage());
                sendUpdate(job.getId(), "FAILED", e.getMessage(), 0, null);
            } finally {
                // v2.2: Move to completed instead of removing (Apple HIG - preserve state)
                activeExecutions.remove(job.getId());
                completedExecutions.put(job.getId(), job);
                LOG.info("Job {} moved to completed storage (total completed: {})", 
                    job.getId(), completedExecutions.size());
            }
        });
    }
    
    private void executeJob(ExecutionJob job) {
        try {
            LOG.info("Starting execution: {}", job.getId());
            
            // Notify start
            job.setStatus("RUNNING");
            sendLog(job.getId(), "INFO", "Starting execution: " + job.getConfigName());
            sendLog(job.getId(), "INFO", "Creating configuration source...");
            sendUpdate(job.getId(), "STARTED", "Initializing scraper...", 0, null);
            
            // Create config source from XML string
            sendLog(job.getId(), "INFO", "Parsing XML configuration...");
            ConfigSource configSource = new BufferConfigSource(job.getConfigXml());
            
            // Create load callback
            HarvestLoadCallback loadCallback = new HarvestLoadCallback() {
                // v2.2.x: IElementDef removed - onSuccess() method removed
                // @Override
                // public void onSuccess(List<IElementDef> elements) {
                //     LOG.info("Configuration loaded with {} elements", elements.size());
                //     sendLog(job.getId(), "SUCCESS", "Configuration loaded: " + elements.size() + " elements");
                // }
            };
            
            // Get harvester instance
            sendLog(job.getId(), "INFO", "Creating harvester instance...");
            Harvester harvester = harvest.getHarvester(configSource, loadCallback);
            sendLog(job.getId(), "INFO", "Harvester created, starting execution...");
            
            // Execute with simple callback
            DynamicScopeContext context = harvester.execute(new Harvester.ContextInitCallback() {
                @Override
                public void onSuccess(DynamicScopeContext ctx) {
                    LOG.info("Execution starting for: {}", job.getId());
                    sendLog(job.getId(), "INFO", "Configuration loaded, executing...");
                }
            });
            
            LOG.info("Execution completed: {}", job.getId());
            
            // Extract variables from completed context
            // NOTE: With our ConfigPlugin fix, variables are now in ROOT scope, not nested!
            Map<String, String> resultMap = new ConcurrentHashMap<>();
            extractVariables(context, resultMap);
            
            LOG.info("Final result map has {} variables", resultMap.size());
            
            sendLog(job.getId(), "SUCCESS", "Execution finished!");
            sendLog(job.getId(), "SUCCESS", "Extracted " + resultMap.size() + " variables");
            sendLog(job.getId(), "INFO", "Preparing results for display...");
            
            // v2.2: Store results in job (Apple HIG - preserve state)
            job.setResults(resultMap);
            job.setLastMessage("Execution completed successfully");
            
            // Notify completion
            job.setStatus("COMPLETED");
            sendUpdate(job.getId(), "COMPLETED", "Execution completed successfully", 100, resultMap);
            
            sendLog(job.getId(), "SUCCESS", "Results sent to browser!");
            
            // v2.2: Log session summary
            if (job.getSession() != null) {
                sendLog(job.getId(), "INFO", "═══ SESSION SUMMARY ═══");
                sendLog(job.getId(), "INFO", job.getSessionInfo());
                sendLog(job.getId(), "INFO", job.getTokenUsageInfo());
                sendLog(job.getId(), "INFO", "═══════════════════════");
            }
            
        } catch (Exception e) {
            LOG.error("Execution error: {}", job.getId(), e);
            sendLog(job.getId(), "ERROR", "Execution failed: " + e.getMessage());
            sendLog(job.getId(), "ERROR", "Exception: " + e.getClass().getSimpleName());
            job.setStatus("FAILED");
            sendUpdate(job.getId(), "FAILED", e.getMessage(), 0, null);
        }
    }
    
    /**
     * Extract variables from context and add to result map
     */
    private void extractVariables(DynamicScopeContext context, Map<String, String> resultMap) {
        int varCount = 0;
        
        LOG.info("=== Extracting variables from context ===");
        
        // Method 1: Try iterator first
        int iteratorCount = 0;
        for (KeyValuePair<Variable> pair : context) {
            String key = pair.getKey();
            iteratorCount++;
            LOG.debug("  Iterator found: {} (#{})", key, iteratorCount);
            
            // Skip system variables
            if ("sys".equals(key) || "http".equals(key)) {
                LOG.debug("    → Skipping system variable");
                continue;
            }
            
            Variable value = pair.getValue();
            String valueStr = value != null ? value.toString() : "null";
            
            // Limit size
            if (valueStr.length() > 5000) {
                valueStr = valueStr.substring(0, 5000) + "...(truncated)";
            }
            
            resultMap.put(key, valueStr);
            varCount++;
            LOG.info("  ✓ Variable: {} = {} chars", key, valueStr.length());
        }
        
        LOG.info("Iterator found {} total variables, {} user variables", iteratorCount, varCount);
        
        // Method 2: Probe for common variable names
        String[] commonVars = {
            // HTTP/HTML
            "url", "html", "page", "response", "content", "body",
            // Parsing
            "title", "heading", "quotes", "authors", "items", "data",
            // XML/JSON
            "xmlContent", "jsonData", "xmlData", "xml", "json",
            // Wikipedia example
            "topic", "intro", "configResult",
            // Quotes example  
            "quote", "author",
            // Generic
            "result", "output", "text", "value"
        };
        
        LOG.info("Probing {} common variable names...", commonVars.length);
        int probeCount = 0;
        
        for (String varName : commonVars) {
            if (!resultMap.containsKey(varName)) {
                Variable var = context.getVar(varName);
                if (var != null) {
                    String valueStr = var.toString();
                    
                    // Skip if empty
                    if (valueStr == null || valueStr.trim().isEmpty()) {
                        LOG.debug("  Probe '{}': empty, skipping", varName);
                        continue;
                    }
                    
                    // Limit size
                    if (valueStr.length() > 5000) {
                        valueStr = valueStr.substring(0, 5000) + "...(truncated)";
                    }
                    
                    resultMap.put(varName, valueStr);
                    varCount++;
                    probeCount++;
                    LOG.info("  ✓ Probed variable: {} = {} chars", varName, valueStr.length());
                }
            }
        }
        
        LOG.info("Probe found {} additional variables", probeCount);
        LOG.info("Extracted {} total variables (excluding system vars)", varCount);
    }
    
    /**
     * Send log entry via WebSocket AND store in job (v2.2: Apple HIG - preserve logs)
     */
    private void sendLog(String executionId, String level, String message) {
        // v2.2: Store log in job first (Apple HIG - each tab keeps own logs)
        ExecutionJob job = getJob(executionId);
        if (job != null) {
            job.addLog(level, message);
        }
        
        // Send via WebSocket for real-time display
        org.eclipse.jetty.websocket.api.Session session = webSocketSessions.get(executionId);
        if (session != null && session.isOpen()) {
            try {
                String json = String.format(
                    "{\"type\":\"log\",\"level\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}",
                    level, escapeJson(message), System.currentTimeMillis()
                );
                session.getRemote().sendString(json);
            } catch (Exception e) {
                LOG.error("Failed to send log message", e);
            }
        }
    }
    
    /**
     * Get job from active or completed storage.
     */
    private ExecutionJob getJob(String executionId) {
        ExecutionJob job = activeExecutions.get(executionId);
        if (job == null) {
            job = completedExecutions.get(executionId);
        }
        return job;
    }
    
    /**
     * Get job info for API endpoint (v2.2).
     */
    public ExecutionJob getExecutionJob(String executionId) {
        return getJob(executionId);
    }
    
    public void pause(String executionId) {
        ExecutionJob job = activeExecutions.get(executionId);
        if (job != null) {
            LOG.info("Pausing execution: {}", executionId);
            // TODO: Implement pause functionality with Harvester API
            job.setStatus("PAUSED");
            sendUpdate(executionId, "PAUSED", "Pause not yet implemented", job.getProgress(), null);
        }
    }
    
    public void resume(String executionId) {
        ExecutionJob job = activeExecutions.get(executionId);
        if (job != null) {
            LOG.info("Resuming execution: {}", executionId);
            // TODO: Implement resume functionality with Harvester API
            job.setStatus("RUNNING");
            sendUpdate(executionId, "RUNNING", "Resume not yet implemented", job.getProgress(), null);
        }
    }
    
    public void stop(String executionId) {
        ExecutionJob job = activeExecutions.get(executionId);
        if (job != null) {
            LOG.info("Stopping execution: {}", executionId);
            // TODO: Implement stop functionality with Harvester API
            job.setStatus("STOPPED");
            sendUpdate(executionId, "STOPPED", "Stop not yet implemented", 0, null);
        }
    }
    
    public void registerWebSocket(String executionId, org.eclipse.jetty.websocket.api.Session session) {
        LOG.debug("Registering WebSocket for execution: {}", executionId);
        webSocketSessions.put(executionId, session);
    }
    
    public void unregisterWebSocket(String executionId) {
        LOG.debug("Unregistering WebSocket for execution: {}", executionId);
        webSocketSessions.remove(executionId);
    }
    
    private void sendUpdate(String executionId, String status, String message, int progress, Map<String, String> results) {
        org.eclipse.jetty.websocket.api.Session session = webSocketSessions.get(executionId);
        if (session != null && session.isOpen()) {
            try {
                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append("\"status\":\"").append(status).append("\",");
                json.append("\"message\":\"").append(escapeJson(message)).append("\",");
                json.append("\"progress\":").append(progress);
                
                if (results != null && !results.isEmpty()) {
                    LOG.info("Sending {} results via WebSocket", results.size());
                    json.append(",\"results\":{");
                    boolean first = true;
                    for (Map.Entry<String, String> entry : results.entrySet()) {
                        if (!first) json.append(",");
                        String key = escapeJson(entry.getKey());
                        String value = escapeJson(entry.getValue());
                        json.append("\"").append(key).append("\":");
                        json.append("\"").append(value).append("\"");
                        first = false;
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("  Result var: {} = {} chars", key, value.length());
                        }
                    }
                    json.append("}");
                } else {
                    LOG.warn("No results to send for execution: {}", executionId);
                }
                
                // v2.2: Add session info on completion
                ExecutionJob job = getJob(executionId);
                if (job != null && ("COMPLETED".equals(status) || "FAILED".equals(status))) {
                    if (job.getSession() != null) {
                        LOG.info("Adding session info to WebSocket message");
                        json.append(",\"sessionInfo\":{");
                        org.webharvest.runtime.session.ScraperSession scraperSession = job.getSession();
                        org.webharvest.runtime.session.SessionMetrics metrics = scraperSession.getMetrics();
                        
                        json.append("\"sessionId\":\"").append(scraperSession.getSessionId()).append("\",");
                        json.append("\"clientId\":\"").append(scraperSession.getClientId() != null ? scraperSession.getClientId() : "").append("\",");
                        json.append("\"status\":\"").append(scraperSession.getStatus()).append("\",");
                        json.append("\"duration\":").append(metrics.getDuration().toMillis()).append(",");
                        json.append("\"elementsProcessed\":").append(metrics.getProcessedElements()).append(",");
                        json.append("\"processingRate\":").append(metrics.getProcessingRate());
                        json.append("}");
                    }
                    
                    if (job.getTokenTracker() != null) {
                        LOG.info("Adding token usage to WebSocket message");
                        json.append(",\"tokenUsage\":{");
                        org.webharvest.runtime.tracking.TokenUsage usage = job.getTokenTracker().getUsage();
                        
                        json.append("\"httpRequests\":").append(usage.getHttpRequests()).append(",");
                        json.append("\"httpBytes\":").append(usage.getHttpBytes()).append(",");
                        json.append("\"cpuTimeMs\":").append(usage.getCpuTimeMillis()).append(",");
                        json.append("\"memoryPeakBytes\":").append(usage.getMemoryPeakBytes());
                        json.append("}");
                    }
                }
                
                json.append("}");
                
                String jsonStr = json.toString();
                LOG.info("Sending WebSocket message ({} bytes): {}", jsonStr.length(), 
                         jsonStr.length() > 200 ? jsonStr.substring(0, 200) + "..." : jsonStr);
                
                session.getRemote().sendString(jsonStr);
                LOG.info("WebSocket message sent successfully for {}: {}", executionId, status);
                
            } catch (Exception e) {
                LOG.error("Failed to send WebSocket update for: {}", executionId, e);
            }
        } else {
            LOG.warn("No active WebSocket session for execution: {}", executionId);
        }
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    public void shutdown() {
        LOG.info("Shutting down ExecutionManager...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}

