package org.webharvest.server.websocket;

import org.eclipse.jetty.ee9.websocket.api.Session;
import org.eclipse.jetty.ee9.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.server.manager.ExecutionManager;

/**
 * WebSocket handler for real-time execution updates
 */
@WebSocket
public class ExecutionWebSocketHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExecutionWebSocketHandler.class);
    private final ExecutionManager executionManager = ExecutionManager.getInstance();
    
    @OnWebSocketConnect
    public void onConnect(Session session) {
        String executionId = getExecutionIdFromPath(session);
        LOG.info("🔌 WebSocket onConnect called! URI: {}", session.getUpgradeRequest().getRequestURI());

        // Support legacy query-based connection (?id=123)
        if (executionId == null || executionId.isEmpty()) {
            executionId = getExecutionIdFromQuery(session);
            if (executionId != null && LOG.isInfoEnabled()) {
                LOG.info("✓ Execution ID resolved from query parameter: {}", executionId);
            }
        }

        if (executionId != null && !executionId.isEmpty()) {
            LOG.info("✓ WebSocket connected for execution: {}", executionId);
            executionManager.registerWebSocket(executionId, session);
        } else {
            LOG.warn("⚠ WebSocket connected WITHOUT execution ID! Path: {}", 
                session.getUpgradeRequest().getRequestURI().getPath());
        }
    }
    
    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("WebSocket message received: {}", message);
        }
        // Handle client messages (pause, resume, stop commands)
        // TODO: Parse JSON and handle commands
    }
    
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        String executionId = getExecutionIdFromPath(session);
        if ((executionId == null || executionId.isEmpty())) {
            executionId = getExecutionIdFromQuery(session);
        }
        if (executionId != null && LOG.isInfoEnabled()) {
            LOG.info("WebSocket closed for execution: " + executionId + " (code: " + statusCode + ", reason: " + reason + ")");
            executionManager.unregisterWebSocket(executionId);
        } else if (executionId != null) {
            executionManager.unregisterWebSocket(executionId);
        }
    }
    
    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        String executionId = getExecutionIdFromPath(session);
        if ((executionId == null || executionId.isEmpty())) {
            executionId = getExecutionIdFromQuery(session);
        }
        if (LOG.isErrorEnabled()) {
            LOG.error("WebSocket error for execution: " + executionId, error);
        }
    }
    
    /**
     * Extract execution ID from URL path
     * Expected: /ws/execution/{executionId}
     */
    private String getExecutionIdFromPath(Session session) {
        try {
            String path = session.getUpgradeRequest().getRequestURI().getPath();
            LOG.debug("Extracting execution ID from path: {}", path);
            
            // Path format: /ws/execution/{executionId}
            if (path != null && path.startsWith("/ws/execution")) {
                // Handle both /ws/execution/<id> and /ws/execution
                if (path.length() == "/ws/execution".length()) {
                    return null; // No ID segment
                }

                if (path.startsWith("/ws/execution/")) {
                    String executionId = path.substring("/ws/execution/".length());
                    LOG.info("✓ Extracted execution ID: {}", executionId);
                    return executionId;
                }
            }
            
            LOG.warn("⚠ Could not extract execution ID from path: {}", path);
        } catch (Exception e) {
            LOG.error("Failed to extract execution ID from path", e);
        }
        return null;
    }
    
    /**
     * Legacy: extract from query (deprecated)
     */
    private String getExecutionIdFromQuery(Session session) {
        try {
            String query = session.getUpgradeRequest().getRequestURI().getQuery();
            if (query != null && query.startsWith("id=")) {
                return query.substring(3);
            }
        } catch (Exception e) {
            LOG.error("Failed to extract execution ID from query", e);
        }
        return null;
    }
}

