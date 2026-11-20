package org.webharvest.server.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jetty.ee9.websocket.api.Session;
import org.eclipse.jetty.ee9.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.server.api.VisualSemanticModelServlet;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * WebSocket handler for Visual Semantic Model real-time communication
 * 
 * Handles:
 * - Frontend connections (send model updates)
 * - Agent connections (receive model updates, send actions)
 * - Broadcast model updates to all agents
 * - Forward actions from agents to frontend (via WebSocket message)
 */
@WebSocket
public class VisualSemanticModelWebSocketHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(VisualSemanticModelWebSocketHandler.class);
    private static final Gson GSON = new GsonBuilder().create();
    
    // Separate sessions for agents and frontend
    private static final Map<String, Session> agentSessions = new ConcurrentHashMap<>();
    private static final Map<String, Session> frontendSessions = new ConcurrentHashMap<>();
    
    // Buffer actions when frontend is not connected (dla LLM)
    private static final Queue<JsonObject> pendingActions = new ConcurrentLinkedQueue<>();
    
    @OnWebSocketConnect
    public void onConnect(Session session) {
        String clientType = getClientType(session);
        String sessionId = session.getRemoteAddress().toString();
        
        LOG.info("Visual Semantic Model WebSocket connected: clientType={}, sessionId={}", clientType, sessionId);
        
        if ("agent".equals(clientType)) {
            agentSessions.put(sessionId, session);
            // Send current model snapshot to agent
            sendModelSnapshot(session);
        } else if ("frontend".equals(clientType)) {
            frontendSessions.put(sessionId, session);
            LOG.info("Frontend connected - ready to receive model updates");
            
            // Send any pending actions that were buffered while frontend was disconnected
            flushPendingActions();
        } else {
            LOG.warn("Unknown client type: {}", clientType);
        }
    }
    
    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            JsonObject msg = JsonParser.parseString(message).getAsJsonObject();
            String type = msg.get("type").getAsString();
            
            String clientType = getClientType(session);
            
            if ("modelUpdate".equals(type)) {
                // From frontend - update cache and broadcast to agents
                if (!"frontend".equals(clientType)) {
                    LOG.warn("modelUpdate received from non-frontend client: {}", clientType);
                    return;
                }
                
                JsonObject model = msg.getAsJsonObject("model");
                VisualSemanticModelServlet.updateModelCache(model);
                broadcastToAgents(model);
                
            } else if ("action".equals(type)) {
                // From agent - forward to frontend for execution
                if (!"agent".equals(clientType)) {
                    LOG.warn("action received from non-agent client: {}", clientType);
                    return;
                }
                
                JsonObject action = msg.getAsJsonObject("action");
                forwardActionToFrontend(action, session);
                
            } else if ("actionResult".equals(type)) {
                // From frontend - forward result back to agent
                if (!"frontend".equals(clientType)) {
                    LOG.warn("actionResult received from non-frontend client: {}", clientType);
                    return;
                }
                
                // Find the agent session that sent the original action
                // For now, forward to all agents (in production, track action IDs)
                forwardActionResultToAgents(msg);
                
            } else {
                LOG.warn("Unknown message type: {}", type);
            }
            
        } catch (Exception e) {
            LOG.error("Error handling WebSocket message", e);
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }
    
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        String sessionId = session.getRemoteAddress().toString();
        agentSessions.remove(sessionId);
        frontendSessions.remove(sessionId);
        LOG.info("Visual Semantic Model WebSocket closed: sessionId=" + sessionId + ", code=" + statusCode + ", reason=" + reason);
    }
    
    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        LOG.error("Visual Semantic Model WebSocket error", error);
    }
    
    /**
     * Get client type from query parameter
     */
    private String getClientType(Session session) {
        try {
            String query = session.getUpgradeRequest().getRequestURI().getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("clientType=")) {
                        return param.substring("clientType=".length());
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to get client type from query", e);
            return "unknown";
        }
        return "unknown";
    }
    
    /**
     * Send current model snapshot to agent
     */
    private void sendModelSnapshot(Session session) {
        JsonObject cachedModel = VisualSemanticModelServlet.getCachedModel();
        if (cachedModel == null) {
            JsonObject response = new JsonObject();
            response.addProperty("type", "error");
            response.addProperty("error", "Model not available yet. Frontend has not sent any updates.");
            sendMessage(session, response);
            return;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("type", "modelUpdate");
        response.add("model", cachedModel);
        response.addProperty("timestamp", System.currentTimeMillis());
        response.addProperty("lastUpdateTime", VisualSemanticModelServlet.getLastUpdateTime());
        
        sendMessage(session, response);
    }
    
    /**
     * Broadcast model update to all agents
     */
    public static void broadcastToAgents(JsonObject model) {
        JsonObject update = new JsonObject();
        update.addProperty("type", "modelUpdate");
        update.add("model", model);
        update.addProperty("timestamp", System.currentTimeMillis());
        
        String message = GSON.toJson(update);
        int sent = 0;
        
        for (Session session : agentSessions.values()) {
            try {
                if (session.isOpen()) {
                    session.getRemote().sendString(message);
                    sent++;
                }
            } catch (IOException e) {
                LOG.error("Failed to send update to agent", e);
            }
        }
        
        if (sent > 0) {
            LOG.debug("Broadcast model update to {} agents", sent);
        }
    }
    
    /**
     * Forward action from agent to frontend
     */
    private void forwardActionToFrontend(JsonObject action, Session agentSession) {
        LOG.info("Action received from agent: {}", action);
        
        // Forward action to all connected frontend sessions
        JsonObject message = new JsonObject();
        message.addProperty("type", "action");
        message.add("action", action);
        
        String messageJson = GSON.toJson(message);
        boolean sent = false;
        
        for (Session frontendSession : frontendSessions.values()) {
            try {
                if (frontendSession.isOpen()) {
                    frontendSession.getRemote().sendString(messageJson);
                    sent = true;
                    LOG.info("Action forwarded to frontend: sessionId={}", frontendSession.getRemoteAddress());
                }
            } catch (IOException e) {
                LOG.error("Failed to forward action to frontend", e);
            }
        }
        
        if (!sent) {
            // No frontend connected - buffer action for later (dla LLM)
            pendingActions.offer(action);
            LOG.info("No frontend connected - buffering action ({} pending)", pendingActions.size());
            
            JsonObject response = new JsonObject();
            response.addProperty("type", "actionResult");
            response.addProperty("success", false);
            response.addProperty("error", "No frontend connected. Action buffered and will be executed when frontend connects.");
            response.add("action", action);
            sendMessage(agentSession, response);
        } else {
            // Action forwarded - frontend will execute it and send result back
            // We'll handle the result in onMessage when frontend sends it
            LOG.info("Action forwarded to frontend. Waiting for execution result...");
        }
    }
    
    /**
     * Send message to session
     */
    private void sendMessage(Session session, JsonObject message) {
        try {
            if (session.isOpen()) {
                session.getRemote().sendString(GSON.toJson(message));
            }
        } catch (IOException e) {
            LOG.error("Failed to send message", e);
        }
    }
    
    /**
     * Forward action result from frontend to agents
     */
    private void forwardActionResultToAgents(JsonObject actionResult) {
        String messageJson = GSON.toJson(actionResult);
        int sent = 0;
        
        for (Session agentSession : agentSessions.values()) {
            try {
                if (agentSession.isOpen()) {
                    agentSession.getRemote().sendString(messageJson);
                    sent++;
                }
            } catch (IOException e) {
                LOG.error("Failed to forward action result to agent", e);
            }
        }
        
        if (sent > 0) {
            LOG.info("Action result forwarded to {} agents", sent);
        }
    }
    
    /**
     * Send error message
     */
    private void sendError(Session session, String error) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "error");
        response.addProperty("error", error);
        sendMessage(session, response);
    }
    
    /**
     * Flush pending actions to frontend (called when frontend connects)
     */
    private void flushPendingActions() {
        if (pendingActions.isEmpty()) {
            return;
        }
        
        LOG.info("Flushing {} pending actions to frontend", pendingActions.size());
        
        JsonObject message = new JsonObject();
        message.addProperty("type", "action");
        
        int flushed = 0;
        while (!pendingActions.isEmpty()) {
            JsonObject action = pendingActions.poll();
            if (action == null) {
                break;
            }
            
            message.add("action", action);
            String messageJson = GSON.toJson(message);
            
            for (Session frontendSession : frontendSessions.values()) {
                try {
                    if (frontendSession.isOpen()) {
                        frontendSession.getRemote().sendString(messageJson);
                        flushed++;
                    }
                } catch (IOException e) {
                    LOG.error("Failed to flush pending action to frontend", e);
                    // Put action back in queue if failed
                    pendingActions.offer(action);
                }
            }
        }
        
        if (flushed > 0) {
            LOG.info("Flushed {} pending actions to frontend", flushed);
        }
    }
}

