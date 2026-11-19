package org.webharvest.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class LocalOriginGuardFilterTest {

    @Test
    public void acceptsLoopbackHostAndMatchingOrigin() {
        assertTrue(LocalOriginGuardFilter.isLoopbackHost("localhost:8080", 8080));
        assertTrue(LocalOriginGuardFilter.isLoopbackHost("127.0.0.1:8080", 8080));
        assertTrue(LocalOriginGuardFilter.isSameLocalOrigin(
                "http://localhost:8080", "localhost:8080", 8080));
        assertTrue(LocalOriginGuardFilter.isSameLocalOrigin(
                "http://127.0.0.1:8080", "127.0.0.1:8080", 8080));
    }

    @Test
    public void rejectsRemoteHostsAndDnsRebindingAuthorities() {
        assertFalse(LocalOriginGuardFilter.isLoopbackHost("attacker.example:8080", 8080));
        assertFalse(LocalOriginGuardFilter.isLoopbackHost("localhost:8081", 8080));
        assertFalse(LocalOriginGuardFilter.isLoopbackHost("localhost.evil.example:8080", 8080));
        assertFalse(LocalOriginGuardFilter.isSameLocalOrigin(
                "http://attacker.example:8080", "localhost:8080", 8080));
        assertFalse(LocalOriginGuardFilter.isSameLocalOrigin(
                "http://localhost:8081", "localhost:8080", 8080));
        assertFalse(LocalOriginGuardFilter.isSameLocalOrigin(
                "null", "localhost:8080", 8080));
    }

    @Test
    public void rejectsNonHttpAndMalformedOrigins() {
        assertFalse(LocalOriginGuardFilter.isSameLocalOrigin(
                "https://localhost:8080", "localhost:8080", 8080));
        assertFalse(LocalOriginGuardFilter.isSameLocalOrigin(
                "http://localhost:8080/path", "localhost:8080", 8080));
        assertFalse(LocalOriginGuardFilter.isSameLocalOrigin(
                "http://user@localhost:8080", "localhost:8080", 8080));
    }

    @Test
    public void rejectsCrossOriginMutationBeforeCallingServlet() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "127.0.0.1:8080");
        headers.put("Origin", "http://attacker.example:8080");
        AtomicInteger status = new AtomicInteger();
        AtomicBoolean chained = new AtomicBoolean();

        new LocalOriginGuardFilter().doFilter(request(headers, "POST", 8080),
                response(status), (req, resp) -> chained.set(true));

        assertEquals(status.get(), HttpServletResponse.SC_FORBIDDEN);
        assertFalse(chained.get());
    }

    @Test
    public void allowsSameOriginMutationAndSameOriginFetchRead() throws Exception {
        AtomicInteger status = new AtomicInteger();
        AtomicBoolean mutationChained = new AtomicBoolean();
        Map<String, String> mutationHeaders = new HashMap<>();
        mutationHeaders.put("Host", "127.0.0.1:8080");
        mutationHeaders.put("Origin", "http://127.0.0.1:8080");

        new LocalOriginGuardFilter().doFilter(request(mutationHeaders, "POST", 8080),
                response(status), (req, resp) -> mutationChained.set(true));

        assertTrue(mutationChained.get());

        AtomicBoolean readChained = new AtomicBoolean();
        Map<String, String> readHeaders = new HashMap<>();
        readHeaders.put("Host", "127.0.0.1:8080");
        readHeaders.put("Sec-Fetch-Site", "same-origin");

        new LocalOriginGuardFilter().doFilter(request(readHeaders, "GET", 8080),
                response(new AtomicInteger()), (req, resp) -> readChained.set(true));

        assertTrue(readChained.get());
    }

    private HttpServletRequest request(Map<String, String> headers, String method, int port) {
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (proxy, called, args) -> {
                    if ("getHeader".equals(called.getName())) {
                        return headers.get(args[0]);
                    }
                    if ("getMethod".equals(called.getName())) {
                        return method;
                    }
                    if ("getServerPort".equals(called.getName())) {
                        return port;
                    }
                    return null;
                });
    }

    private HttpServletResponse response(AtomicInteger status) {
        return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{HttpServletResponse.class}, (proxy, called, args) -> {
                    if ("sendError".equals(called.getName())) {
                        status.set((Integer) args[0]);
                        return null;
                    }
                    return null;
                });
    }
}
