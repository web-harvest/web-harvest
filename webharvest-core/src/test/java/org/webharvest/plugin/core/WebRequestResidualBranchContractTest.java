package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.services.HttpResponse;
import org.webharvest.plugin.services.HttpService;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Residual HttpPlugin contracts: header replace/skip-empty merge, null MIME,
 * empty query values, and TRACE routing.
 */
public class WebRequestResidualBranchContractTest {

    @Mock
    private DynamicScopeContext context;

    @Mock
    private HttpService httpService;

    @Mock
    private PluginConfiguration configuration;

    @Mock
    private HttpResponse httpResponse;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(Collections.emptyList());
        when(httpResponse.getBody()).thenReturn("ok");
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getContentType()).thenReturn("text/plain");
        when(httpResponse.getCharset()).thenReturn("UTF-8");
        when(httpResponse.getHeaders()).thenReturn(Collections.emptyMap());
        when(httpService.get(anyString(), anyMap())).thenReturn(httpResponse);
        when(httpService.request(anyString(), anyString(), anyString(), anyMap())).thenReturn(httpResponse);
    }

    private HttpPlugin plugin() throws Exception {
        HttpPlugin plugin = new HttpPlugin();
        Field field = HttpPlugin.class.getDeclaredField("httpService");
        field.setAccessible(true);
        field.set(plugin, httpService);
        return plugin;
    }

    @Test
    public void emptyExistingHeaderValueIsReplacedRatherThanCommaMerged() throws Exception {
        PluginConfiguration first = mock(PluginConfiguration.class);
        when(first.getElementName()).thenReturn("http-header");
        when(first.getProperty("name")).thenReturn("X-Token");
        when(first.getProperty("_textContent", "")).thenReturn("");

        PluginConfiguration second = mock(PluginConfiguration.class);
        when(second.getElementName()).thenReturn("http-header");
        when(second.getProperty("name")).thenReturn("x-token");
        when(second.getProperty("_textContent", "")).thenReturn("abc");

        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Arrays.asList(first, second));
        when(configuration.getProperty("url")).thenReturn("https://example.com/");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");

        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        plugin.execute(context);

        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(httpService).get(eq("https://example.com/"), headers.capture());
        assertEquals(headers.getValue().get("X-Token"), "abc");
    }

    @Test
    public void emptyDuplicateHeaderValueDoesNotAppendCommaFragment() throws Exception {
        PluginConfiguration first = mock(PluginConfiguration.class);
        when(first.getElementName()).thenReturn("http-header");
        when(first.getProperty("name")).thenReturn("Accept");
        when(first.getProperty("_textContent", "")).thenReturn("text/plain");

        PluginConfiguration second = mock(PluginConfiguration.class);
        when(second.getElementName()).thenReturn("http-header");
        when(second.getProperty("name")).thenReturn("Accept");
        when(second.getProperty("_textContent", "")).thenReturn("");

        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Arrays.asList(first, second));
        when(configuration.getProperty("url")).thenReturn("https://example.com/");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");

        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        plugin.execute(context);

        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(httpService).get(eq("https://example.com/"), headers.capture());
        assertEquals(headers.getValue().get("Accept"), "text/plain");
    }

    @Test
    public void nullContentTypeDefaultsToTextBodyPath() throws Exception {
        when(httpResponse.getContentType()).thenReturn(null);
        when(httpResponse.getBody()).thenReturn("plain");
        when(configuration.getProperty("url")).thenReturn("https://example.com/");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");

        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        assertEquals(result.toString(), "plain");
        verify(httpResponse).getBody();
        verify(httpResponse, never()).getBodyAsBinary();
    }

    @Test
    public void queryParameterWithBlankValueStillAppendsKey() throws Exception {
        PluginConfiguration param = mock(PluginConfiguration.class);
        when(param.getElementName()).thenReturn("http-param");
        when(param.getProperty("name")).thenReturn("flag");
        when(param.getProperty("_textContent", "")).thenReturn(null);

        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(param));
        when(configuration.getProperty("url")).thenReturn("https://example.com/path");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");

        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        plugin.execute(context);

        verify(httpService).get(eq("https://example.com/path?flag="), anyMap());
    }

    @Test
    public void traceUsesGenericRequestPath() throws Exception {
        when(configuration.getProperty("url")).thenReturn("https://example.com/");
        when(configuration.getProperty("method", "GET")).thenReturn("TRACE");
        when(configuration.getProperty("body", "")).thenReturn("");

        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        verify(httpService).request(eq("TRACE"), eq("https://example.com/"), eq(""), anyMap());
        assertTrue(result instanceof NodeVariable);
    }
}
