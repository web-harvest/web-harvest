package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
 * Behavioral contracts for HttpPlugin binary MIME handling, generic methods,
 * header merge semantics, query appending, and configuration validation.
 */
public class WebRequestBranchContractTest {

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
        when(httpResponse.getBody()).thenReturn("text-body");
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getContentType()).thenReturn("text/plain");
        when(httpResponse.getCharset()).thenReturn("UTF-8");
        when(httpResponse.getHeaders()).thenReturn(Collections.emptyMap());
        when(httpService.get(anyString(), anyMap())).thenReturn(httpResponse);
        when(httpService.post(anyString(), anyString(), anyMap())).thenReturn(httpResponse);
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
    public void binaryMimeTypesReturnBytePayloadWithoutStringCorruption() throws Exception {
        byte[] payload = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A};
        when(httpResponse.getContentType()).thenReturn("image/png; charset=binary");
        when(httpResponse.getBodyAsBinary()).thenReturn(payload);
        when(configuration.getProperty("url")).thenReturn("https://example.com/a.png");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");

        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        assertTrue(result instanceof NodeVariable);
        Object wrapped = ((NodeVariable) result).getWrappedObject();
        assertTrue(wrapped instanceof byte[]);
        assertEquals((byte[]) wrapped, payload, "PNG payload must be preserved as bytes");
        verify(httpResponse).getBodyAsBinary();
        verify(httpResponse, never()).getBody();
    }

    @Test
    public void applicationPdfIsTreatedAsBinary() throws Exception {
        byte[] pdf = "%PDF-1.4".getBytes(StandardCharsets.US_ASCII);
        when(httpResponse.getContentType()).thenReturn("application/pdf");
        when(httpResponse.getBodyAsBinary()).thenReturn(pdf);
        when(configuration.getProperty("url")).thenReturn("https://example.com/doc.pdf");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");

        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        Object wrapped = ((NodeVariable) result).getWrappedObject();
        assertTrue(wrapped instanceof byte[]);
        assertEquals((byte[]) wrapped, pdf, "PDF payload must be preserved as bytes");
    }

    @Test
    public void applicationOctetStreamIsTreatedAsBinary() throws Exception {
        byte[] payload = new byte[] {1, 2, 3};
        when(httpResponse.getContentType()).thenReturn("application/octet-stream");
        when(httpResponse.getBodyAsBinary()).thenReturn(payload);
        when(configuration.getProperty("url")).thenReturn("https://example.com/blob");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");

        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        Variable blob = plugin.execute(context);
        Object wrapped = ((NodeVariable) blob).getWrappedObject();
        assertTrue(wrapped instanceof byte[]);
        assertEquals((byte[]) wrapped, payload, "octet-stream payload must be preserved as bytes");
    }

    @Test
    public void deleteUsesGenericRequestPath() throws Exception {
        when(configuration.getProperty("url")).thenReturn("https://api.example.com/item/1");
        when(configuration.getProperty("method", "GET")).thenReturn("DELETE");
        when(configuration.getProperty("body", "")).thenReturn("");

        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        plugin.execute(context);
        verify(httpService).request(eq("DELETE"), eq("https://api.example.com/item/1"), eq(""), anyMap());
    }

    @Test
    public void patchUsesGenericRequestPath() throws Exception {
        when(configuration.getProperty("url")).thenReturn("https://api.example.com/item/1");
        when(configuration.getProperty("method", "GET")).thenReturn("PATCH");
        when(configuration.getProperty("body", "")).thenReturn("{\"x\":1}");

        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        plugin.execute(context);
        verify(httpService).request(eq("PATCH"), eq("https://api.example.com/item/1"), eq("{\"x\":1}"), anyMap());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void unsupportedMethodFailsAtExecution() throws Exception {
        when(configuration.getProperty("url")).thenReturn("https://example.com/");
        when(configuration.getProperty("method", "GET")).thenReturn("FOO");
        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test
    public void duplicateHeadersMergeCaseInsensitivelyAndSkipInvalidChildren() throws Exception {
        PluginConfiguration h1 = mock(PluginConfiguration.class);
        when(h1.getElementName()).thenReturn("http-header");
        when(h1.getProperty("name")).thenReturn("Accept");
        when(h1.getProperty("_textContent", "")).thenReturn("text/html");

        PluginConfiguration h2 = mock(PluginConfiguration.class);
        when(h2.getElementName()).thenReturn("http-header");
        when(h2.getProperty("name")).thenReturn("accept");
        when(h2.getProperty("_textContent", "")).thenReturn("application/json");

        PluginConfiguration emptyName = mock(PluginConfiguration.class);
        when(emptyName.getElementName()).thenReturn("http-header");
        when(emptyName.getProperty("name")).thenReturn("  ");

        PluginConfiguration nullChild = null;
        PluginConfiguration unknown = mock(PluginConfiguration.class);
        when(unknown.getElementName()).thenReturn("other");
        PluginConfiguration nullElement = mock(PluginConfiguration.class);
        when(nullElement.getElementName()).thenReturn(null);

        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(
                Arrays.asList(h1, h2, emptyName, nullChild, unknown, nullElement));
        when(configuration.getProperty("url")).thenReturn("https://example.com/");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");

        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        plugin.execute(context);

        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(httpService).get(eq("https://example.com/"), headers.capture());
        assertEquals(headers.getValue().size(), 1);
        assertEquals(headers.getValue().get("Accept"), "text/html, application/json");
    }

    @Test
    public void queryParametersAppendWithAmpersandWhenUrlAlreadyHasQuery() throws Exception {
        PluginConfiguration param = mock(PluginConfiguration.class);
        when(param.getElementName()).thenReturn("http-param");
        when(param.getProperty("name")).thenReturn("page");
        when(param.getProperty("_textContent", "")).thenReturn("2");

        PluginConfiguration blank = mock(PluginConfiguration.class);
        when(blank.getElementName()).thenReturn("http-param");
        when(blank.getProperty("name")).thenReturn("");

        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(List.of(param, blank));
        when(configuration.getProperty("url")).thenReturn("https://example.com/search?q=web");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");

        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        plugin.execute(context);

        verify(httpService).get(eq("https://example.com/search?q=web&page=2"), anyMap());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void validateRejectsUnsupportedMethod() throws Exception {
        when(configuration.getProperty("url")).thenReturn("https://example.com/");
        when(configuration.getProperty("method", "GET")).thenReturn("CONNECT");
        HttpPlugin plugin = plugin();
        plugin.validateConfiguration(configuration);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void validateRejectsBlankMethodOverride() throws Exception {
        when(configuration.getProperty("url")).thenReturn("https://example.com/");
        when(configuration.getProperty("method", "GET")).thenReturn("   ");
        HttpPlugin plugin = plugin();
        plugin.validateConfiguration(configuration);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void validateRejectsMissingUrl() throws Exception {
        when(configuration.getProperty("url")).thenReturn(null);
        when(configuration.getProperty("method", "GET")).thenReturn("GET");
        HttpPlugin plugin = plugin();
        plugin.validateConfiguration(configuration);
    }
}
