package org.webharvest.server.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
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
        String executionId = getExecutionIdFromQuery(session);
        if (executionId != null) {
            LOG.info("WebSocket connected for execution: {}", executionId);
            executionManager.registerWebSocket(executionId, session);
        } else {
            LOG.warn("WebSocket connected without execution ID");
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
        String executionId = getExecutionIdFromQuery(session);
        if (executionId != null && LOG.isInfoEnabled()) {
            LOG.info("WebSocket closed for execution: " + executionId + " (code: " + statusCode + ", reason: " + reason + ")");
            executionManager.unregisterWebSocket(executionId);
        } else if (executionId != null) {
            executionManager.unregisterWebSocket(executionId);
        }
    }
    
    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        String executionId = getExecutionIdFromQuery(session);
        if (LOG.isErrorEnabled()) {
            LOG.error("WebSocket error for execution: " + executionId, error);
        }
    }
    
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

