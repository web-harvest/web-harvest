package org.webharvest.server.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.server.manager.ExecutionManager;
import org.webharvest.server.manager.ExecutionJob;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * REST API endpoint for execution control (pause, resume, stop) and info.
 * 
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li><b>GET /api/execution/{id}/info</b> - Get execution info</li>
 *   <li><b>POST /api/execution/{id}/pause</b> - Pause running execution</li>
 *   <li><b>POST /api/execution/{id}/resume</b> - Resume paused execution</li>
 *   <li><b>POST /api/execution/{id}/stop</b> - Stop execution (cannot resume)</li>
 * </ul>
 * 
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Pause execution
 * POST /api/execution/abc-123/pause
 * Response: {"success": true, "status": "PAUSED"}
 * 
 * // Resume execution
 * POST /api/execution/abc-123/resume
 * Response: {"success": true, "status": "RUNNING"}
 * 
 * // Stop execution
 * POST /api/execution/abc-123/stop
 * Response: {"success": true, "status": "STOPPED"}
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @see ExecutionManager
 */
public class ExecutionControlServlet extends HttpServlet {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExecutionControlServlet.class);
    private static final Gson GSON = new Gson();
    
    @Override
    public void init() throws ServletException {
        super.init();
        LOG.info("ExecutionControlServlet initialized");
    }
    
    /**
     * GET /api/execution/{id}/info - Get execution info
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            sendError(response, "Missing execution ID");
            return;
        }
        
        String[] parts = pathInfo.substring(1).split("/");
        String executionId = parts[0];
        
        ExecutionManager manager = ExecutionManager.getInstance();
        ExecutionJob job = manager.getExecutionJob(executionId);
        
        if (job == null) {
            sendError(response, "Execution not found: " + executionId);
            return;
        }
        
        // Build info response
        JsonObject info = new JsonObject();
        info.addProperty("executionId", job.getId());
        info.addProperty("configName", job.getConfigName());
        info.addProperty("status", job.getStatus());
        info.addProperty("progress", job.getProgress());
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(GSON.toJson(info));
    }
    
    /**
     * POST /api/execution/{id}/pause - Pause execution
     * POST /api/execution/{id}/resume - Resume execution
     * POST /api/execution/{id}/stop - Stop execution
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            String pathInfo = request.getPathInfo();
            
            if (pathInfo == null || pathInfo.length() < 2) {
                sendError(response, "Invalid path: " + pathInfo);
                return;
            }
            
            // Parse path: /{executionId}/pause|resume|stop
            String[] parts = pathInfo.substring(1).split("/");
            if (parts.length != 2) {
                sendError(response, "Invalid path format. Expected: /{executionId}/{action}");
                return;
            }
            
            String executionId = parts[0];
            String action = parts[1];
            
            LOG.info("Control request: {} for execution: {}", action, executionId);
            
            ExecutionManager manager = ExecutionManager.getInstance();
            boolean success = false;
            String newStatus = null;
            
            switch (action.toLowerCase()) {
                case "pause":
                    success = manager.pause(executionId);
                    newStatus = "PAUSED";
                    break;
                    
                case "resume":
                    success = manager.resume(executionId);
                    newStatus = "RUNNING";
                    break;
                    
                case "stop":
                    success = manager.stop(executionId);
                    newStatus = "STOPPED";
                    break;
                    
                default:
                    sendError(response, "Unknown action: " + action + ". Use: pause, resume, or stop");
                    return;
            }
            
            if (success) {
                JsonObject result = new JsonObject();
                result.addProperty("success", true);
                result.addProperty("executionId", executionId);
                result.addProperty("action", action);
                result.addProperty("status", newStatus);
                
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(GSON.toJson(result));
                
                LOG.info("Control request successful: {} → {}", action, newStatus);
            } else {
                sendError(response, "Failed to " + action + " execution. " +
                         "Execution may not exist or is not in a valid state.");
            }
            
        } catch (Exception e) {
            LOG.error("Error processing control request", e);
            sendError(response, "Internal error: " + e.getMessage());
        }
    }
    
    private void sendError(HttpServletResponse response, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write(GSON.toJson(error));
    }
}

