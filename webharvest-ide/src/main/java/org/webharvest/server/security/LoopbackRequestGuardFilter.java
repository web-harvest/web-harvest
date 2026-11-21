package org.webharvest.server.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee9.servlet.FilterHolder;
import org.eclipse.jetty.ee9.servlet.ServletContextHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Locale;

/**
 * Restricts the embedded IDE's privileged HTTP and WebSocket endpoints to
 * requests addressed to the local listener and initiated by an explicitly
 * trusted local UI origin.
 *
 * <p>Loopback binding is the primary network boundary. This filter adds a
 * browser-origin boundary so that a hostile website cannot use the user's
 * browser as an unauthenticated client of the local execution API.</p>
 */
public final class LoopbackRequestGuardFilter implements Filter {

    private static final int VITE_DEV_SERVER_PORT = 3000;

    /**
     * Installs the same guard for every privileged HTTP API and WebSocket route.
     */
    public static void install(ServletContextHandler context) {
        context.addFilter(new FilterHolder(new LoopbackRequestGuardFilter()),
                "/api/*", EnumSet.of(DispatcherType.REQUEST));
        context.addFilter(new FilterHolder(new LoopbackRequestGuardFilter()),
                "/ws/*", EnumSet.of(DispatcherType.REQUEST));
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // No configuration required.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)
                || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!isLoopbackConnection(httpRequest)
                || !isLoopbackHost(httpRequest.getServerName())
                || httpRequest.getServerPort() != httpRequest.getLocalPort()) {
            reject(httpResponse);
            return;
        }

        String origin = getSingleOrigin(httpRequest);
        boolean websocketUpgrade = "websocket".equalsIgnoreCase(
                httpRequest.getHeader("Upgrade"));

        if (origin == null) {
            if (websocketUpgrade || isStateChangingMethod(httpRequest.getMethod())) {
                reject(httpResponse);
                return;
            }
        } else if (!isTrustedLocalOrigin(origin, httpRequest)) {
            reject(httpResponse);
            return;
        }

        chain.doFilter(request, response);
    }

    private static boolean isLoopbackConnection(HttpServletRequest request) {
        String localAddress = request.getLocalAddr();
        if (localAddress == null || localAddress.isEmpty()) {
            return false;
        }
        try {
            return InetAddress.getByName(localAddress).isLoopbackAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isLoopbackHost(String host) {
        if (host == null) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return "localhost".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "::1".equals(normalized)
                || "0:0:0:0:0:0:0:1".equals(normalized);
    }

    /**
     * Returns the only Origin header value, or null when no Origin is present.
     * Multiple Origin headers are treated as malformed and rejected by returning
     * a sentinel that cannot pass URI validation.
     */
    private static String getSingleOrigin(HttpServletRequest request) {
        Enumeration<String> origins = request.getHeaders("Origin");
        if (origins == null || !origins.hasMoreElements()) {
            return null;
        }
        String origin = origins.nextElement();
        if (origins.hasMoreElements()) {
            return "multiple-origins";
        }
        return origin;
    }

    private static boolean isTrustedLocalOrigin(String origin,
                                                HttpServletRequest request) {
        try {
            URI uri = new URI(origin);
            if (uri.getHost() == null || uri.getRawUserInfo() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !"http".equalsIgnoreCase(uri.getScheme())
                    || !isLoopbackHost(uri.getHost())) {
                return false;
            }

            int originPort = uri.getPort() == -1 ? 80 : uri.getPort();
            return originPort == request.getServerPort()
                    || originPort == VITE_DEV_SERVER_PORT;
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean isStateChangingMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("Forbidden");
    }

    @Override
    public void destroy() {
        // No resources to release.
    }
}
