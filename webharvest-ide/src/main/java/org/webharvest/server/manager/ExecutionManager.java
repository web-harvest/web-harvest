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
import org.webharvest.definition.IElementDef;
import org.webharvest.events.ScraperExecutionEndEvent;
import org.webharvest.ioc.HttpModule;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.web.HttpClientManager;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.KeyValuePair;

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
    private final Map<String, org.eclipse.jetty.websocket.api.Session> webSocketSessions;
    private final Injector injector;
    private final Harvest harvest;
    
    private ExecutionManager() {
        LOG.info("Initializing ExecutionManager...");
        
        this.executorService = Executors.newFixedThreadPool(5); // Max 5 concurrent executions
        this.activeExecutions = new ConcurrentHashMap<>();
        this.webSocketSessions = new ConcurrentHashMap<>();
        
        LOG.info("Creating Guice injector for Harvest API...");
        
        // Initialize Guice injector
        this.injector = Guice.createInjector(
            new ScraperModule("."),
            new HttpModule(HttpClientManager.ProxySettings.NO_PROXY_SET)
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
                sendUpdate(job.getId(), "FAILED", e.getMessage(), 0, null);
            } finally {
                activeExecutions.remove(job.getId());
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
                @Override
                public void onSuccess(List<IElementDef> elements) {
                    LOG.info("Configuration loaded with {} elements", elements.size());
                    sendLog(job.getId(), "SUCCESS", "Configuration loaded: " + elements.size() + " elements");
                }
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
            
            // Notify completion
            job.setStatus("COMPLETED");
            sendUpdate(job.getId(), "COMPLETED", "Execution completed successfully", 100, resultMap);
            
            sendLog(job.getId(), "SUCCESS", "Results sent to browser!");
            
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
     * Send log entry via WebSocket
     */
    private void sendLog(String executionId, String level, String message) {
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

