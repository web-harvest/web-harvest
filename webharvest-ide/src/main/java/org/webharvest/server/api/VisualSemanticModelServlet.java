/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * [BSD 3-Clause License text...]
 */

package org.webharvest.server.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API Servlet for Visual Semantic Model
 * 
 * Provides access to the Visual Semantic Model for AI agents (Cursor AI, etc.)
 * 
 * Endpoints:
 * - GET /api/visual/model - Get current Visual Semantic Model (JSON for LLM)
 * - GET /api/visual/model/actions - Get schema of available actions
 * - POST /api/visual/model/actions - Execute action from agent (addNode, updateNode, etc.)
 * 
 * The model is cached in memory and updated via WebSocket from the frontend.
 * 
 * @author WebHarvest Team
 * @since 2.3.0
 */
@WebServlet(name = "VisualSemanticModelServlet", urlPatterns = {
    "/api/visual/model",
    "/api/visual/model/*"
})
public class VisualSemanticModelServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(VisualSemanticModelServlet.class);
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    
    // Cache modelu (aktualizowany przez WebSocket)
    private static volatile JsonObject cachedModel = null;
    private static volatile long lastUpdateTime = 0;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        String requestURI = req.getRequestURI();
        
        // Set CORS headers
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        // Handle OPTIONS preflight
        if ("OPTIONS".equals(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        // GET /api/visual/model/actions - zwróć schema akcji
        if (requestURI.endsWith("/actions") && pathInfo != null && pathInfo.endsWith("/actions")) {
            handleGetActionsSchema(req, resp);
            return;
        }
        
        // GET /api/visual/model - zwróć cached model
        handleGetModel(req, resp);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        // Set CORS headers
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        // Handle OPTIONS preflight
        if ("OPTIONS".equals(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        String pathInfo = req.getPathInfo();
        String requestURI = req.getRequestURI();
        
        // POST /api/visual/model/actions - wykonaj akcję
        if (requestURI.endsWith("/actions") && pathInfo != null && pathInfo.endsWith("/actions")) {
            handlePostAction(req, resp);
            return;
        }
        
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid endpoint");
    }
    
    private boolean isJsonContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String mediaType = contentType.split(";", 2)[0].trim();
        return "application/json".equalsIgnoreCase(mediaType);
    }

    /**
     * GET /api/visual/model - zwróć cached model
     */
    private void handleGetModel(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        if (cachedModel == null) {
            JsonObject response = new JsonObject();
            response.addProperty("error", "Model not available yet. Frontend has not sent any updates.");
            response.addProperty("timestamp", System.currentTimeMillis());
            response.addProperty("lastUpdateTime", lastUpdateTime);
            
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            resp.getWriter().write(GSON.toJson(response));
            return;
        }
        
        // Return cached model
        JsonObject response = new JsonObject();
        response.add("model", cachedModel);
        response.addProperty("timestamp", System.currentTimeMillis());
        response.addProperty("lastUpdateTime", lastUpdateTime);
        response.addProperty("age", System.currentTimeMillis() - lastUpdateTime);
        
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(GSON.toJson(response));
    }
    
    /**
     * GET /api/visual/model/actions - zwróć schema akcji
     */
    private void handleGetActionsSchema(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        JsonObject schema = new JsonObject();
        
        // Add action schemas (from VisualSemanticModelForLLM.availableActions)
        JsonObject actions = new JsonObject();
        
        // addNode
        JsonObject addNode = new JsonObject();
        addNode.addProperty("description", "Add a new node to the workflow");
        JsonObject addNodeParams = new JsonObject();
        addNodeParams.addProperty("pluginType", "string - The type of plugin to add");
        addNodeParams.addProperty("position", "Point (optional) - { x: number, y: number }");
        addNodeParams.addProperty("parentId", "string (optional) - ID of parent node");
        addNodeParams.addProperty("attributes", "Record<string, string> (optional) - Initial attributes");
        addNode.add("parameters", addNodeParams);
        actions.add("addNode", addNode);
        
        // updateNode
        JsonObject updateNode = new JsonObject();
        updateNode.addProperty("description", "Update attributes of an existing node");
        JsonObject updateNodeParams = new JsonObject();
        updateNodeParams.addProperty("nodeId", "string - ID of the node to update");
        updateNodeParams.addProperty("attributes", "Record<string, string> - Attributes to update");
        updateNode.add("parameters", updateNodeParams);
        actions.add("updateNode", updateNode);
        
        // removeNode
        JsonObject removeNode = new JsonObject();
        removeNode.addProperty("description", "Remove a node from the workflow");
        JsonObject removeNodeParams = new JsonObject();
        removeNodeParams.addProperty("nodeId", "string - ID of the node to remove");
        removeNode.add("parameters", removeNodeParams);
        actions.add("removeNode", removeNode);
        
        // connectNodes
        JsonObject connectNodes = new JsonObject();
        connectNodes.addProperty("description", "Create a connection between two nodes");
        JsonObject connectNodesParams = new JsonObject();
        connectNodesParams.addProperty("fromNodeId", "string - ID of the source node");
        connectNodesParams.addProperty("toNodeId", "string - ID of the target node");
        connectNodesParams.addProperty("type", "ConnectionType (optional) - sequential | conditional | error");
        connectNodes.add("parameters", connectNodesParams);
        actions.add("connectNodes", connectNodes);
        
        // disconnectNodes
        JsonObject disconnectNodes = new JsonObject();
        disconnectNodes.addProperty("description", "Remove a connection between two nodes");
        JsonObject disconnectNodesParams = new JsonObject();
        disconnectNodesParams.addProperty("fromNodeId", "string - ID of the source node");
        disconnectNodesParams.addProperty("toNodeId", "string - ID of the target node");
        disconnectNodes.add("parameters", disconnectNodesParams);
        actions.add("disconnectNodes", disconnectNodes);
        
        // moveNode
        JsonObject moveNode = new JsonObject();
        moveNode.addProperty("description", "Move a node to a new position on the canvas");
        JsonObject moveNodeParams = new JsonObject();
        moveNodeParams.addProperty("nodeId", "string - ID of the node to move");
        moveNodeParams.addProperty("position", "Point - { x: number, y: number }");
        moveNode.add("parameters", moveNodeParams);
        actions.add("moveNode", moveNode);
        
        // loadConfig
        JsonObject loadConfig = new JsonObject();
        loadConfig.addProperty("description", "Load XML configuration - parses XML and loads all nodes into the visual designer");
        JsonObject loadConfigParams = new JsonObject();
        loadConfigParams.addProperty("xml", "string - XML configuration string (must include <config> root element)");
        loadConfig.add("parameters", loadConfigParams);
        actions.add("loadConfig", loadConfig);
        
        schema.add("availableActions", actions);
        
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(GSON.toJson(schema));
    }
    
    /**
     * POST /api/visual/model/actions - wykonaj akcję
     */
    private void handlePostAction(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        try {
            // Parse request body
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                body.append(line);
            }
            
            JsonObject request = JsonParser.parseString(body.toString()).getAsJsonObject();
            String actionType = request.get("type").getAsString();
            JsonObject parameters = request.getAsJsonObject("parameters");
            
            // Forward action to WebSocket handler for execution
            // (WebSocket handler will execute action and return result)
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("error", "Actions must be executed via WebSocket. Connect to /ws/visual/model?clientType=agent");
            response.addProperty("actionType", actionType);
            
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(GSON.toJson(response));
            
        } catch (Exception e) {
            LOG.error("Error handling action request", e);
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("error", e.getMessage());
            
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(GSON.toJson(response));
        }
    }
    
    /**
     * Metoda do aktualizacji cache (wywoływana przez WebSocket handler)
     */
    public static void updateModelCache(JsonObject model) {
        cachedModel = model;
        lastUpdateTime = System.currentTimeMillis();
        LOG.debug("Model cache updated at {}", lastUpdateTime);
    }
    
    /**
     * Pobierz cached model (dla WebSocket handler)
     */
    public static JsonObject getCachedModel() {
        return cachedModel;
    }
    
    /**
     * Pobierz czas ostatniej aktualizacji
     */
    public static long getLastUpdateTime() {
        return lastUpdateTime;
    }
}


