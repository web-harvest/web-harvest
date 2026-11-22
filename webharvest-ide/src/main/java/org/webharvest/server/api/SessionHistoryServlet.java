package org.webharvest.server.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.server.manager.ExecutionJob;
import org.webharvest.server.manager.ExecutionManager;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.SessionMetrics;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API endpoint for retrieving session execution history.
 * 
 * <h2>Endpoints:</h2>
 * <ul>
 *   <li><b>GET /api/sessions/history</b> - Returns list of executed sessions</li>
 * </ul>
 * 
 * <h2>Query Parameters:</h2>
 * <ul>
 *   <li><b>limit</b> - Maximum number of sessions to return (default: 20, max: 100)</li>
 *   <li><b>status</b> - Filter by session status (COMPLETED, FAILED, RUNNING, etc.)</li>
 *   <li><b>offset</b> - Skip first N sessions for pagination (default: 0)</li>
 * </ul>
 * 
 * <h2>Response Format:</h2>
 * <pre>{@code
 * {
 *   "sessions": [
 *     {
 *       "sessionId": "abc123...",
 *       "timestamp": "2025-10-13T13:45:00Z",
 *       "status": "COMPLETED",
 *       "duration": 150,
 *       "configName": "test.xml",
 *       "elementsProcessed": 25,
 *       "processingRate": 166.67
 *     }
 *   ],
 *   "total": 42,
 *   "limit": 20,
 *   "offset": 0
 * }
 * }</pre>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Get last 10 sessions
 * GET /api/sessions/history?limit=10
 * 
 * // Get only completed sessions
 * GET /api/sessions/history?status=COMPLETED
 * 
 * // Pagination (get next page)
 * GET /api/sessions/history?limit=20&offset=20
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @see ExecutionManager
 * @see ScraperSession
 */
public class SessionHistoryServlet extends HttpServlet {
    
    private static final Logger LOG = LoggerFactory.getLogger(SessionHistoryServlet.class);
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter ISO_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));
    
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_OFFSET = 0;
    
    @Override
    public void init() throws ServletException {
        super.init();
        LOG.info("SessionHistoryServlet initialized");
    }
    
    /**
     * GET /api/sessions/history - Returns session execution history
     * 
     * @param request HTTP request with optional query parameters
     * @param response HTTP response containing JSON session list
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // Parse query parameters
            int limit = parseIntParam(request, "limit", DEFAULT_LIMIT);
            int offset = parseIntParam(request, "offset", DEFAULT_OFFSET);
            String statusFilter = request.getParameter("status");
            
            // Validate limit
            if (limit < 1) {
                limit = DEFAULT_LIMIT;
            }
            if (limit > MAX_LIMIT) {
                limit = MAX_LIMIT;
            }
            
            // Validate offset
            if (offset < 0) {
                offset = DEFAULT_OFFSET;
            }
            
            LOG.debug("Session history request: limit=" + limit + ", offset=" + offset + ", status=" + statusFilter);
            
            // Get execution history from ExecutionManager
            ExecutionManager manager = ExecutionManager.getInstance();
            List<ExecutionJob> allJobs = manager.getCompletedExecutions();
            
            // Filter by status if specified
            if (statusFilter != null && !statusFilter.isEmpty()) {
                allJobs = allJobs.stream()
                    .filter(job -> job.getSession() != null && 
                                   statusFilter.equalsIgnoreCase(job.getSession().getStatus().name()))
                    .collect(Collectors.toList());
            }
            
            int total = allJobs.size();
            
            // Apply pagination
            List<ExecutionJob> pagedJobs = allJobs.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
            
            // Build JSON response
            JsonObject responseJson = new JsonObject();
            JsonArray sessionsArray = new JsonArray();
            
            for (ExecutionJob job : pagedJobs) {
                JsonObject sessionJson = buildSessionJson(job);
                sessionsArray.add(sessionJson);
            }
            
            responseJson.add("sessions", sessionsArray);
            responseJson.addProperty("total", total);
            responseJson.addProperty("limit", limit);
            responseJson.addProperty("offset", offset);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(GSON.toJson(responseJson));
            
            LOG.debug("Returned " + pagedJobs.size() + " sessions (total: " + total + 
                     ", offset: " + offset + ", limit: " + limit + ")");
            
        } catch (Exception e) {
            LOG.error("Error retrieving session history", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("error", "Failed to retrieve session history");
            errorJson.addProperty("message", e.getMessage());
            response.getWriter().write(GSON.toJson(errorJson));
        }
    }
    
    /**
     * Builds JSON object for a single session.
     * 
     * @param job the execution job
     * @return JSON representation of the session
     */
    private JsonObject buildSessionJson(ExecutionJob job) {
        JsonObject json = new JsonObject();
        
        // Basic job info
        json.addProperty("sessionId", job.getId());
        json.addProperty("configName", job.getConfigName());
        json.addProperty("status", job.getStatus());
        
        // Session details
        ScraperSession session = job.getSession();
        if (session != null) {
            SessionMetrics metrics = session.getMetrics();
            
            // Timestamp (use session start time or current time as fallback)
            Instant timestamp = metrics.getStartTime() != null ? 
                metrics.getStartTime() : Instant.now();
            json.addProperty("timestamp", ISO_FORMATTER.format(timestamp));
            
            // Metrics
            json.addProperty("duration", metrics.getDuration().toMillis());
            json.addProperty("elementsProcessed", metrics.getProcessedElements());
            json.addProperty("processingRate", metrics.getProcessingRate());
            
            // Status from session
            json.addProperty("status", session.getStatus().name());
        } else {
            // Fallback if no session (shouldn't happen in normal flow)
            json.addProperty("timestamp", ISO_FORMATTER.format(Instant.now()));
            json.addProperty("duration", 0);
            json.addProperty("elementsProcessed", 0);
            json.addProperty("processingRate", 0.0);
        }
        
        return json;
    }
    
    /**
     * Parses integer parameter from request.
     * 
     * @param request HTTP request
     * @param paramName parameter name
     * @param defaultValue default value if parameter missing or invalid
     * @return parsed integer value
     */
    private int parseIntParam(HttpServletRequest request, String paramName, int defaultValue) {
        String paramValue = request.getParameter(paramName);
        if (paramValue == null || paramValue.isEmpty()) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(paramValue);
        } catch (NumberFormatException e) {
            LOG.warn("Invalid " + paramName + " parameter: " + paramValue + 
                    ", using default: " + defaultValue);
            return defaultValue;
        }
    }
}

