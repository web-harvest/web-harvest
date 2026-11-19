package org.webharvest.server.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Restricts browser-facing IDE APIs to the local browser origin.
 *
 * The embedded IDE is a desktop application, not a remotely accessible server.
 * This guard complements the loopback-only connector: it also rejects
 * cross-origin browser requests and Host-header/DNS-rebinding attempts.
 */
public final class LocalOriginGuardFilter implements Filter {

    private static final Set<String> LOOPBACK_HOSTS =
            Set.of("localhost", "127.0.0.1", "::1");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String host = httpRequest.getHeader("Host");
        String origin = httpRequest.getHeader("Origin");
        boolean websocket = isWebSocketUpgrade(httpRequest);

        boolean allowed = isLoopbackHost(host, httpRequest.getServerPort())
                && (origin != null
                    ? isSameLocalOrigin(origin, host, httpRequest.getServerPort())
                    : !websocket
                        && isSafeReadMethod(httpRequest.getMethod())
                        && "same-origin".equals(httpRequest.getHeader("Sec-Fetch-Site")));

        if (!allowed) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Local IDE origin required");
            return;
        }

        chain.doFilter(request, response);
    }

    private static boolean isWebSocketUpgrade(HttpServletRequest request) {
        return "websocket".equalsIgnoreCase(request.getHeader("Upgrade"));
    }

    private static boolean isSafeReadMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }

    static boolean isLoopbackHost(String hostHeader, int serverPort) {
        URI host = parseAuthority(hostHeader);
        return host != null
                && host.getUserInfo() == null
                && host.getHost() != null
                && LOOPBACK_HOSTS.contains(normalizeHost(host.getHost()))
                && effectivePort(host) == serverPort;
    }

    static boolean isSameLocalOrigin(String originHeader, String hostHeader, int serverPort) {
        URI origin = parseAuthority(originHeader);
        URI host = parseAuthority(hostHeader);
        if (origin == null || host == null
                || origin.getUserInfo() != null
                || origin.getHost() == null
                || host.getHost() == null
                || !"http".equalsIgnoreCase(origin.getScheme())
                || !LOOPBACK_HOSTS.contains(normalizeHost(origin.getHost()))) {
            return false;
        }

        return normalizeHost(origin.getHost()).equals(normalizeHost(host.getHost()))
                && effectivePort(origin) == serverPort
                && effectivePort(host) == serverPort;
    }

    private static URI parseAuthority(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI uri = value.startsWith("http://") || value.startsWith("https://")
                    ? new URI(value)
                    : new URI("http://" + value);
            if (uri.getRawPath() != null && !uri.getRawPath().isEmpty()
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return null;
            }
            return uri;
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private static int effectivePort(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort()
                : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
    }

    private static String normalizeHost(String host) {
        return host.toLowerCase(Locale.ROOT);
    }
}
