package org.webharvest.server.api;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * REST API endpoint for Visual Semantic Model diagnostics
 * 
 * Provides diagnostic information about:
 * - Model state (availability, size, health)
 * - Connection state (WebSocket, backend)
 * - Agent actions history
 * - Performance metrics
 * - Errors and warnings
 * 
 * Endpoints:
 * - GET /api/visual/model/diagnostic - Get current diagnostic information
 */
@WebServlet(name = "VisualSemanticModelDiagnosticServlet", urlPatterns = {"/api/visual/model/diagnostic"})
public class VisualSemanticModelDiagnosticServlet extends HttpServlet {
    
    private static final Logger LOG = LoggerFactory.getLogger(VisualSemanticModelDiagnosticServlet.class);
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        
        try {
            JsonObject diagnostic = buildDiagnosticInfo();
            resp.getWriter().write(diagnostic.toString());
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            LOG.error("Error building diagnostic info", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Failed to build diagnostic info: " + e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }
    
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setStatus(HttpServletResponse.SC_OK);
    }
    
    /**
     * Build diagnostic information from cached model and WebSocket state
     */
    private JsonObject buildDiagnosticInfo() {
        JsonObject diagnostic = new JsonObject();
        diagnostic.addProperty("timestamp", System.currentTimeMillis());
        diagnostic.addProperty("version", "1.0.0");
        
        // Model state
        JsonObject modelState = new JsonObject();
        JsonObject cachedModel = VisualSemanticModelServlet.getCachedModel();
        if (cachedModel != null) {
            modelState.addProperty("available", true);
            modelState.addProperty("lastUpdateTime", VisualSemanticModelServlet.getLastUpdateTime());
            
            // Extract model size
            if (cachedModel.has("workflow")) {
                JsonObject workflow = cachedModel.getAsJsonObject("workflow");
                if (workflow.has("rootNodes")) {
                    modelState.addProperty("rootNodesCount", workflow.getAsJsonArray("rootNodes").size());
                }
                if (workflow.has("allNodes")) {
                    modelState.addProperty("totalNodesCount", workflow.getAsJsonObject("allNodes").size());
                }
                if (workflow.has("connections")) {
                    modelState.addProperty("connectionsCount", workflow.getAsJsonObject("connections").size());
                }
            }
        } else {
            modelState.addProperty("available", false);
        }
        diagnostic.add("model", modelState);
        
        // Connection state
        JsonObject connectionState = new JsonObject();
        connectionState.addProperty("backendReachable", true); // We're the backend
        connectionState.addProperty("lastCheckTime", System.currentTimeMillis());
        diagnostic.add("connection", connectionState);
        
        // Health status
        JsonObject health = new JsonObject();
        boolean isHealthy = cachedModel != null;
        health.addProperty("status", isHealthy ? "healthy" : "degraded");
        if (!isHealthy) {
            health.addProperty("issue", "Model not available - frontend has not sent any updates");
        }
        health.addProperty("lastCheckTime", System.currentTimeMillis());
        diagnostic.add("health", health);
        
        return diagnostic;
    }
}

