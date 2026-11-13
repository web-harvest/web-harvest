package org.webharvest.plugin.services;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.testng.annotations.Test;

/**
 * Behavioral contracts for redirect-safe default header injection.
 */
public class HeaderPreservingInterceptorBranchContractTest {

    @Test
    public void nullDefaultHeadersBecomeEmptyMapAndProcessIsNoOp() throws Exception {
        HeaderPreservingInterceptor interceptor = new HeaderPreservingInterceptor(null);
        assertTrue(interceptor.getDefaultHeaders().isEmpty());

        HttpRequest request = mock(HttpRequest.class);
        interceptor.process(request, null, mock(HttpContext.class));
        verify(request, never()).addHeader(anyString(), anyString());
    }

    @Test
    public void emptyDefaultHeadersSkipProcessing() throws Exception {
        HeaderPreservingInterceptor interceptor =
                new HeaderPreservingInterceptor(Collections.emptyMap());
        HttpRequest request = mock(HttpRequest.class);
        interceptor.process(request, null, mock(HttpContext.class));
        verifyNoInteractions(request);
    }

    @Test
    public void processAddsMissingHeadersWithoutOverridingExistingOnes() throws Exception {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("Referer", "https://example.com/login");
        defaults.put("X-API-Key", "secret");
        HeaderPreservingInterceptor interceptor = new HeaderPreservingInterceptor(defaults);

        HttpRequest request = mock(HttpRequest.class);
        when(request.containsHeader("Referer")).thenReturn(true);
        when(request.containsHeader("X-API-Key")).thenReturn(false);
        when(request.getRequestUri()).thenReturn("/dashboard");

        interceptor.process(request, null, mock(HttpContext.class));

        verify(request, never()).addHeader(eq("Referer"), anyString());
        verify(request).addHeader("X-API-Key", "secret");

        Map<String, String> copy = interceptor.getDefaultHeaders();
        copy.clear();
        assertEquals(interceptor.getDefaultHeaders().get("X-API-Key"), "secret",
                "getDefaultHeaders must return a defensive copy");
    }

    @Test
    public void noArgConstructorStartsWithEmptyDefaults() {
        assertTrue(new HeaderPreservingInterceptor().getDefaultHeaders().isEmpty());
    }
}
