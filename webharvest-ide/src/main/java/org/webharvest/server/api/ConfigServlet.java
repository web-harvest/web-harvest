package org.webharvest.server.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.definition.BufferConfigSource;
import org.webharvest.definition.Config;
import org.webharvest.definition.ConfigFactory;
import org.webharvest.definition.ConfigSource;
import org.webharvest.ioc.ScraperModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.io.IOException;

/**
 * REST API for configuration operations
 * 
 * Endpoints:
 * POST /api/config/validate - Validate XML configuration
 * POST /api/config/save - Save configuration
 * GET /api/config/list - List saved configurations
 */
public class ConfigServlet extends HttpServlet {
    
    private static final Logger LOG = LoggerFactory.getLogger(ConfigServlet.class);
    private final Gson gson = new Gson();
    private final ConfigFactory configFactory;
    
    public ConfigServlet() {
        LOG.info("Initializing ConfigServlet...");
        Injector injector = Guice.createInjector(
            new ScraperModule(".")
        );
        this.configFactory = injector.getInstance(ConfigFactory.class);
        LOG.info("ConfigServlet initialized successfully");
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        
        if ("/validate".equals(pathInfo)) {
            validateConfig(req, resp);
        } else if ("/save".equals(pathInfo)) {
            saveConfig(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        
        if ("/list".equals(pathInfo)) {
            listConfigs(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    private void validateConfig(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        try {
            // Read XML config from request
            String xmlContent = req.getReader().lines()
                    .reduce("", (acc, line) -> acc + line + "\n");
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Validating configuration: {} bytes", xmlContent.length());
            }
            
            // Validate by trying to parse configuration (v2.2 API)
            ConfigSource configSource = new BufferConfigSource(xmlContent);
            Config config = configFactory.create(configSource);
            config.reload(); // Load and validate the configuration tree
            
            // If no exception thrown, config is valid
            JsonObject response = new JsonObject();
            response.addProperty("valid", true);
            response.addProperty("message", "Configuration is valid");
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Configuration validated successfully");
            }
            
            sendJsonResponse(resp, HttpServletResponse.SC_OK, response);
            
        } catch (Exception e) {
            LOG.error("Configuration validation failed", e);
            
            JsonObject response = new JsonObject();
            response.addProperty("valid", false);
            response.addProperty("message", e.getMessage());
            response.addProperty("error", e.getClass().getSimpleName());
            
            sendJsonResponse(resp, HttpServletResponse.SC_BAD_REQUEST, response);
        }
    }
    
    private void saveConfig(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        // TODO: Implement config saving to filesystem
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("message", "Not implemented yet");
        
        sendJsonResponse(resp, HttpServletResponse.SC_NOT_IMPLEMENTED, response);
    }
    
    private void listConfigs(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        // TODO: Implement listing saved configs
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("message", "Not implemented yet");
        
        sendJsonResponse(resp, HttpServletResponse.SC_NOT_IMPLEMENTED, response);
    }
    
    private void sendJsonResponse(HttpServletResponse resp, int status, JsonObject data) 
            throws IOException {
        
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.getWriter().write(gson.toJson(data));
    }
}

