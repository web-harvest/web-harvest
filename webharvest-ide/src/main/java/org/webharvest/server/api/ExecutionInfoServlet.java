package org.webharvest.server.api;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.server.manager.ExecutionJob;
import org.webharvest.server.manager.ExecutionManager;
import org.webharvest.server.manager.LogEntry;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.SessionMetrics;
import org.webharvest.runtime.tracking.TokenUsage;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for retrieving execution job information (logs, results, session info).
 * 
 * <p>Endpoints:</p>
 * <ul>
 *   <li>GET /api/execution/{id}/logs - Get all logs for execution</li>
 *   <li>GET /api/execution/{id}/results - Get results for execution</li>
 *   <li>GET /api/execution/{id}/session - Get session metadata</li>
 *   <li>GET /api/execution/{id}/info - Get complete execution info</li>
 * </ul>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class ExecutionInfoServlet extends HttpServlet {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExecutionInfoServlet.class);
    private final Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            sendError(resp, 400, "Missing execution ID in path");
            return;
        }
        
        // Parse path: /{executionId}/{endpoint}
        String[] parts = pathInfo.substring(1).split("/");
        if (parts.length < 1) {
            sendError(resp, 400, "Invalid path format");
            return;
        }
        
        String executionId = parts[0];
        String endpoint = parts.length > 1 ? parts[1] : "info";
        
        ExecutionManager manager = ExecutionManager.getInstance();
        ExecutionJob job = manager.getExecutionJob(executionId);
        
        if (job == null) {
            sendError(resp, 404, "Execution not found: " + executionId);
            return;
        }
        
        // Route to appropriate endpoint
        switch (endpoint) {
            case "logs":
                sendLogs(resp, job);
                break;
            case "results":
                sendResults(resp, job);
                break;
            case "session":
                sendSessionInfo(resp, job);
                break;
            case "info":
                sendFullInfo(resp, job);
                break;
            default:
                sendError(resp, 404, "Unknown endpoint: " + endpoint);
        }
    }
    
    private void sendLogs(HttpServletResponse resp, ExecutionJob job) throws IOException {
        List<LogEntry> logs = job.getAllLogs();
        
        // Convert to simple map format for JSON
        List<Map<String, Object>> logsJson = logs.stream()
            .map(log -> {
                Map<String, Object> m = new HashMap<>();
                m.put("timestamp", log.getTimestamp());
                m.put("level", log.getLevel());
                m.put("message", log.getMessage());
                return m;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("executionId", job.getId());
        response.put("logs", logsJson);
        response.put("logCount", logs.size());
        
        sendJson(resp, response);
    }
    
    private void sendResults(HttpServletResponse resp, ExecutionJob job) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("executionId", job.getId());
        response.put("status", job.getStatus());
        response.put("results", job.getResults());
        response.put("resultCount", job.getResults().size());
        
        sendJson(resp, response);
    }
    
    private void sendSessionInfo(HttpServletResponse resp, ExecutionJob job) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("executionId", job.getId());
        
        ScraperSession session = job.getSession();
        if (session != null) {
            SessionMetrics metrics = session.getMetrics();
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("sessionId", session.getSessionId());
            sessionData.put("clientId", session.getClientId());
            sessionData.put("status", session.getStatus().toString());
            sessionData.put("startTime", metrics.getStartTime().toEpochMilli());
            sessionData.put("duration", metrics.getDuration().toMillis());
            sessionData.put("elementsProcessed", metrics.getProcessedElements());
            sessionData.put("processingRate", metrics.getProcessingRate());
            
            if (metrics.getEndTime() != null) {
                sessionData.put("endTime", metrics.getEndTime().toEpochMilli());
            }
            
            response.put("session", sessionData);
        } else {
            response.put("session", null);
        }
        
        // Token usage
        if (job.getTokenTracker() != null) {
            TokenUsage usage = job.getTokenTracker().getUsage();
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("httpRequests", usage.getHttpRequests());
            tokenData.put("httpBytes", usage.getHttpBytes());
            tokenData.put("cpuTimeMs", usage.getCpuTimeMillis());
            tokenData.put("memoryPeakBytes", usage.getMemoryPeakBytes());
            response.put("tokens", tokenData);
        } else {
            response.put("tokens", null);
        }
        
        sendJson(resp, response);
    }
    
    private void sendFullInfo(HttpServletResponse resp, ExecutionJob job) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("executionId", job.getId());
        response.put("configName", job.getConfigName());
        response.put("status", job.getStatus());
        response.put("progress", job.getProgress());
        response.put("lastMessage", job.getLastMessage());
        response.put("logCount", job.getLogCount());
        response.put("resultCount", job.getResults().size());
        
        // Session info summary
        if (job.getSession() != null) {
            response.put("sessionInfo", job.getSessionInfo());
        }
        
        // Token usage summary
        if (job.getTokenTracker() != null) {
            response.put("tokenUsage", job.getTokenUsageInfo());
        }
        
        sendJson(resp, response);
    }
    
    private void sendJson(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(data));
    }
    
    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        sendJson(resp, error);
    }
}

