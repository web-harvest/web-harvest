package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.lang.reflect.Field;
import java.util.Collections;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.services.HttpResponse;
import org.webharvest.plugin.services.HttpService;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Binary MIME families (video/audio/archive) and HEAD/OPTIONS request routing.
 */
public class WebRequestMimeMethodBranchContractTest {

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
        when(httpResponse.getStatusCode()).thenReturn(200);
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

    private void stubBinaryResponse(String contentType, byte[] payload) {
        when(httpResponse.getContentType()).thenReturn(contentType);
        when(httpResponse.getBodyAsBinary()).thenReturn(payload);
        when(configuration.getProperty("url")).thenReturn("https://example.com/resource");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");
    }

    @DataProvider(name = "binaryMimeTypes")
    public Object[][] binaryMimeTypes() {
        return new Object[][] {
            {"video/mp4; charset=binary", new byte[] {0x00, 0x00, 0x00, 0x18}},
            {"audio/mpeg", new byte[] {(byte) 0xFF, (byte) 0xFB}},
            {"application/zip", new byte[] {0x50, 0x4B, 0x03, 0x04}},
            {"application/gzip", new byte[] {0x1F, (byte) 0x8B}},
            {"application/x-tar", new byte[] {0x75, 0x73, 0x74, 0x61, 0x72}}
        };
    }

    @Test(dataProvider = "binaryMimeTypes")
    public void binaryFamiliesPreserveBytePayload(String contentType, byte[] payload) throws Exception {
        stubBinaryResponse(contentType, payload);
        HttpPlugin plugin = plugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        assertTrue(result instanceof NodeVariable);
        assertEquals(((NodeVariable) result).getWrappedObject(), payload);
        verify(httpResponse).getBodyAsBinary();
        verify(httpResponse, never()).getBody();
    }

    @Test
    public void headAndOptionsUseGenericRequestPath() throws Exception {
        when(httpResponse.getContentType()).thenReturn("text/plain");
        when(httpResponse.getBody()).thenReturn("ok");
        when(configuration.getProperty("url")).thenReturn("https://example.com/");
        when(configuration.getProperty("body", "")).thenReturn("");

        for (String method : new String[] {"HEAD", "OPTIONS"}) {
            when(configuration.getProperty("method", "GET")).thenReturn(method);
            HttpPlugin plugin = plugin();
            plugin.initialize(configuration);
            plugin.execute(context);
            verify(httpService).request(eq(method), eq("https://example.com/"), eq(""), anyMap());
            reset(httpService);
            when(httpService.request(anyString(), anyString(), anyString(), anyMap())).thenReturn(httpResponse);
        }
    }
}
