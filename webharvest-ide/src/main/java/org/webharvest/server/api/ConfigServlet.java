package org.webharvest.server.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import org.webharvest.server.validation.SchemaValidator;
import org.webharvest.server.validation.ValidationError;
import org.webharvest.server.validation.ValidationResult;
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
    private final SchemaValidator schemaValidator;
    
    public ConfigServlet() {
        LOG.info("Initializing ConfigServlet with SchemaValidator...");
        Injector injector = Guice.createInjector(
            new ScraperModule("target")
        );
        this.configFactory = injector.getInstance(ConfigFactory.class);
        this.schemaValidator = new SchemaValidator();
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
            
            LOG.info("🔍 Validating configuration: {} bytes", xmlContent.length());
            
            if (xmlContent == null || xmlContent.trim().isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("valid", false);
                response.addProperty("message", "Empty configuration");
                sendJsonResponse(resp, HttpServletResponse.SC_OK, response);
                return;
            }
            
            // STEP 1: Schema validation (attributes, plugins, context)
            ValidationResult schemaValidation = schemaValidator.validate(xmlContent);
            
            if (!schemaValidation.isValid()) {
                LOG.warn("❌ Schema validation failed with {} errors", schemaValidation.getErrors().size());
                
                JsonObject response = new JsonObject();
                response.addProperty("valid", false);
                
                // Build detailed error message
                StringBuilder errorMsg = new StringBuilder("Validation errors:\n");
                JsonArray errorsArray = new JsonArray();
                
                for (ValidationError error : schemaValidation.getErrors()) {
                    errorMsg.append("  - ").append(error.getMessage()).append("\n");
                    
                    JsonObject errorObj = new JsonObject();
                    errorObj.addProperty("message", error.getMessage());
                    errorObj.addProperty("line", error.getLine());
                    errorObj.addProperty("plugin", error.getPlugin());
                    errorObj.addProperty("type", error.getType());
                    errorsArray.add(errorObj);
                }
                
                response.addProperty("message", errorMsg.toString());
                response.add("errors", errorsArray);
                
                sendJsonResponse(resp, HttpServletResponse.SC_OK, response);
                return;
            }
            
            // STEP 2: Try to parse with ConfigFactory (structural validation)
            try {
                ConfigSource configSource = new BufferConfigSource(xmlContent);
                Config config = configFactory.create(configSource);
                config.reload();
            } catch (Exception e) {
                LOG.error("❌ ConfigFactory validation failed: {}", e.getMessage());
                
                JsonObject response = new JsonObject();
                response.addProperty("valid", false);
                response.addProperty("message", "Configuration parse error: " + e.getMessage());
                response.addProperty("error", e.getClass().getSimpleName());
                
                JsonArray errorsArray = new JsonArray();
                JsonObject errorObj = new JsonObject();
                errorObj.addProperty("message", e.getMessage());
                errorObj.addProperty("line", 0);
                errorObj.addProperty("plugin", "config");
                errorObj.addProperty("type", "PARSE_ERROR");
                errorsArray.add(errorObj);
                response.add("errors", errorsArray);
                
                sendJsonResponse(resp, HttpServletResponse.SC_OK, response);
                return;
            }
            
            // If both validations pass, config is valid
            JsonObject response = new JsonObject();
            response.addProperty("valid", true);
            response.addProperty("message", "Configuration is valid");
            
            LOG.info("✅ Configuration validated successfully");
            
            sendJsonResponse(resp, HttpServletResponse.SC_OK, response);
            
        } catch (Exception e) {
            LOG.error("❌ Validation system error: {}", e.getMessage(), e);
            
            JsonObject response = new JsonObject();
            response.addProperty("valid", false);
            response.addProperty("message", "Validation system error: " + e.getMessage());
            response.addProperty("error", e.getClass().getSimpleName());
            
            sendJsonResponse(resp, HttpServletResponse.SC_OK, response);
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

