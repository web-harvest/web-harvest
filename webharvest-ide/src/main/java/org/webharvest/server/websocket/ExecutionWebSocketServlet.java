package org.webharvest.server.websocket;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee9.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.time.Duration;

/**
 * WebSocket servlet for execution updates
 */
public class ExecutionWebSocketServlet extends JettyWebSocketServlet {
    
    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofMinutes(10));
        factory.setMaxTextMessageSize(65536);
        factory.setCreator((req, resp) -> new ExecutionWebSocketHandler());
    }
}

