package org.webharvest.server.websocket;

import org.eclipse.jetty.ee9.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.time.Duration;

/**
 * WebSocket servlet for Visual Semantic Model
 */
public class VisualSemanticModelWebSocketServlet extends JettyWebSocketServlet {
    
    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofMinutes(10));
        factory.setMaxTextMessageSize(1048576); // 1MB - increased for large semantic models
        factory.setCreator((req, resp) -> new VisualSemanticModelWebSocketHandler());
    }
}






