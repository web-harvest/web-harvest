package org.webharvest.plugin.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.services.HttpService;
import org.webharvest.plugin.services.HttpServiceImpl;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Deterministic loopback test for HttpPlugin dependency injection and execution.
 */
public class HttpPluginWithDependencyInjectionTest {

    private static final String JSON_BODY = "{\"ok\":true,\"path\":\"/get\"}";
    private static final String HTML_BODY = "<html><body><h1>fixture</h1></body></html>";

    private Injector injector;
    private DynamicScopeContext context;
    private HttpPlugin httpPlugin;
    private HttpServer server;
    private String baseUrl;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/get", exchange ->
                respond(exchange, JSON_BODY, "application/json; charset=UTF-8"));
        server.createContext("/html", exchange ->
                respond(exchange, HTML_BODY, "text/html; charset=UTF-8"));
        server.createContext("/status/200", exchange ->
                respond(exchange, "status-ok", "text/plain; charset=UTF-8"));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        File targetDirectory = new File("target");
        if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
            throw new IOException("Could not create test working directory: " + targetDirectory);
        }

        injector = Guice.createInjector(new ScraperModule(targetDirectory.getPath()));
        context = injector.getInstance(ScraperContext.class);
        context.setConfig(mock(Config.class));
        context.setScriptingLanguage(org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        httpPlugin = injector.getInstance(HttpPlugin.class);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (injector != null) {
            HttpService service = injector.getInstance(HttpService.class);
            if (service instanceof HttpServiceImpl) {
                ((HttpServiceImpl) service).close();
            }
        }
        if (server != null) {
            server.stop(0);
        }
    }

    private void respond(HttpExchange exchange, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    @Test
    public void testHttpPluginSimpleGet() throws Exception {
        assertNotNull(httpPlugin, "Guice must provide HttpPlugin with its dependencies");

        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
                "HttpPlugin", "http://org.webharvest/schema/2.1/core", "http");
        config.setProperty("url", baseUrl + "/get");

        httpPlugin.initialize(config);
        Variable result = httpPlugin.execute(context);

        assertNotNull(result, "HTTP response should not be null");
        assertEquals(result.toString(), JSON_BODY,
                "HttpPlugin must return the exact body supplied by the loopback server");
    }

    @Test
    public void testHttpPluginGetHtml() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
                "HttpPlugin", "http://org.webharvest/schema/2.1/core", "http");
        config.setProperty("url", baseUrl + "/html");

        httpPlugin.initialize(config);
        Variable result = httpPlugin.execute(context);

        assertNotNull(result, "HTTP response should not be null");
        assertEquals(result.toString(), HTML_BODY,
                "HttpPlugin must return the exact HTML body supplied by the loopback server");
    }

    @Test
    public void testHttpPluginWithVariableUrl() throws Exception {
        context.setLocalVar("baseUrl",
                new org.webharvest.runtime.variables.NodeVariable(baseUrl));

        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
                "HttpPlugin", "http://org.webharvest/schema/2.1/core", "http");
        config.setProperty("url", "$" + "{baseUrl}/status/200");

        httpPlugin.initialize(config);
        Variable result = httpPlugin.execute(context);

        assertNotNull(result, "HTTP response should not be null");
        assertEquals(result.toString(), "status-ok",
                "HttpPlugin must resolve the context variable before issuing the request");
    }
}
