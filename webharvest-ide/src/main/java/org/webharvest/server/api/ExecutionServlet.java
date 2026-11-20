package org.webharvest.server.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.server.manager.ExecutionManager;
import org.webharvest.server.manager.ExecutionRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * REST API for scraper execution
 * 
 * Endpoints:
 * POST /api/execute - Start new execution
 * POST /api/execute/{id}/pause - Pause execution
 * POST /api/execute/{id}/resume - Resume execution
 * POST /api/execute/{id}/stop - Stop execution
 * GET /api/execute/{id}/status - Get execution status
 */
public class ExecutionServlet extends HttpServlet {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExecutionServlet.class);
    private final Gson gson = new Gson();
    private final ExecutionManager executionManager;
    
    public ExecutionServlet() {
        LOG.info("Initializing ExecutionServlet...");
        this.executionManager = ExecutionManager.getInstance();
        LOG.info("ExecutionServlet initialized successfully");
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || "/".equals(pathInfo)) {
            // Start new execution
            startExecution(req, resp);
        } else {
            // Parse execution ID and action
            String[] parts = pathInfo.split("/");
            if (parts.length >= 3) {
                String executionId = parts[1];
                String action = parts[2];
                
                switch (action) {
                    case "pause":
                        pauseExecution(executionId, resp);
                        break;
                    case "resume":
                        resumeExecution(executionId, resp);
                        break;
                    case "stop":
                        stopExecution(executionId, resp);
                        break;
                    default:
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        
        if (pathInfo != null && pathInfo.matches("/[^/]+/status")) {
            String executionId = pathInfo.split("/")[1];
            getExecutionStatus(executionId, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    private void startExecution(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        try {
            String contentType = req.getContentType();
            String mediaType = contentType == null ? "" : contentType.split(";", 2)[0].trim();
            if (!"application/json".equalsIgnoreCase(mediaType)) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Content-Type must be application/json");
                sendJsonResponse(resp, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, error);
                return;
            }

            // Read request body
            String requestBody = req.getReader().lines()
                    .reduce("", (acc, line) -> acc + line + "\n");
            
            JsonObject requestJson = gson.fromJson(requestBody, JsonObject.class);
            
            if (requestJson == null || !requestJson.has("configXml")) {
                throw new IllegalArgumentException("Missing required parameter: configXml");
            }
            
            String configXml = requestJson.get("configXml").getAsString();
            String configName = requestJson.has("configName") 
                    ? requestJson.get("configName").getAsString() 
                    : "config.xml";
            
            LOG.info("Starting execution for: {}", configName);
            
            // Create execution request
            String executionId = UUID.randomUUID().toString();
            ExecutionRequest execReq = new ExecutionRequest(executionId, configName, configXml);
            
            // Return execution ID IMMEDIATELY (frontend needs it to connect WebSocket!)
            JsonObject response = new JsonObject();
            response.addProperty("executionId", executionId);
            response.addProperty("status", "QUEUED");
            response.addProperty("message", "Execution queued");
            
            sendJsonResponse(resp, HttpServletResponse.SC_OK, response);
            LOG.info("Returned executionId to client: {}", executionId);
            
            // Submit with DELAYED start (allows WebSocket to connect first)
            // ExecutionManager will wait for WebSocket before starting
            executionManager.submitWithWebSocketWait(execReq);
            
        } catch (Exception e) {
            LOG.error("Failed to start execution", e);
            
            JsonObject response = new JsonObject();
            response.addProperty("error", e.getMessage());
            
            sendJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
        }
    }
    
    private void pauseExecution(String executionId, HttpServletResponse resp) 
            throws IOException {
        
        try {
            executionManager.pause(executionId);
            
            JsonObject response = new JsonObject();
            response.addProperty("executionId", executionId);
            response.addProperty("status", "PAUSED");
            
            sendJsonResponse(resp, HttpServletResponse.SC_OK, response);
            
        } catch (Exception e) {
            LOG.error("Failed to pause execution: {}", executionId, e);
            
            JsonObject response = new JsonObject();
            response.addProperty("error", e.getMessage());
            
            sendJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
        }
    }
    
    private void resumeExecution(String executionId, HttpServletResponse resp) 
            throws IOException {
        
        try {
            executionManager.resume(executionId);
            
            JsonObject response = new JsonObject();
            response.addProperty("executionId", executionId);
            response.addProperty("status", "RUNNING");
            
            sendJsonResponse(resp, HttpServletResponse.SC_OK, response);
            
        } catch (Exception e) {
            LOG.error("Failed to resume execution: {}", executionId, e);
            
            JsonObject response = new JsonObject();
            response.addProperty("error", e.getMessage());
            
            sendJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
        }
    }
    
    private void stopExecution(String executionId, HttpServletResponse resp) 
            throws IOException {
        
        try {
            executionManager.stop(executionId);
            
            JsonObject response = new JsonObject();
            response.addProperty("executionId", executionId);
            response.addProperty("status", "STOPPED");
            
            sendJsonResponse(resp, HttpServletResponse.SC_OK, response);
            
        } catch (Exception e) {
            LOG.error("Failed to stop execution: {}", executionId, e);
            
            JsonObject response = new JsonObject();
            response.addProperty("error", e.getMessage());
            
            sendJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
        }
    }
    
    private void getExecutionStatus(String executionId, HttpServletResponse resp) 
            throws IOException {
        
        JsonObject response = new JsonObject();
        response.addProperty("executionId", executionId);
        response.addProperty("status", "UNKNOWN");
        response.addProperty("message", "Status tracking not implemented yet");
        
        sendJsonResponse(resp, HttpServletResponse.SC_OK, response);
    }
    
    private void sendJsonResponse(HttpServletResponse resp, int status, JsonObject data) 
            throws IOException {
        
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(data));
    }
}

