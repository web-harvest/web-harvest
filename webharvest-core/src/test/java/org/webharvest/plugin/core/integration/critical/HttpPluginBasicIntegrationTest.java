package org.webharvest.plugin.core.integration.critical;

import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.*;

/** Deterministic critical HttpPlugin integration tests using a loopback HTTP fixture. */
public class HttpPluginBasicIntegrationTest extends XmlBasedIntegrationTestBase {
    private HttpServer startServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String body;
            String type;
            if ("/html".equals(path)) {
                body = "<html><body>WebHarvest local fixture</body></html>";
                type = "text/html; charset=UTF-8";
            } else if ("/json".equals(path)) {
                body = "{\"fixture\":\"webharvest\"}";
                type = "application/json; charset=UTF-8";
            } else {
                body = "OK";
                type = "text/plain; charset=UTF-8";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", type);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        return server;
    }

    @Test
    public void testBasicHttpGet() throws Exception {
        HttpServer server = startServer();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/html";
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "    <def var=\"content\"><http url=\"" + url + "\"/></def>\n</config>";
            DynamicScopeContext context = executeXmlString(xml);
            Variable content = context.getVar("content");
            assertNotNull(content, "HTTP response should be stored in variable");
            assertTrue(content.toString().contains("WebHarvest local fixture"), "Response should contain local HTML fixture");
        } finally { server.stop(0); }
    }

    @Test
    public void testHttpWithVariableUrl() throws Exception {
        HttpServer server = startServer();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/json";
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "    <def var=\"url\">" + url + "</def>\n" +
                "    <def var=\"response\"><http url=\"${url}\"/></def>\n</config>";
            DynamicScopeContext context = executeXmlString(xml);
            Variable response = context.getVar("response");
            assertNotNull(response, "HTTP response should be stored in variable");
            assertTrue(response.toString().contains("webharvest"), "Response should contain local JSON fixture");
        } finally { server.stop(0); }
    }

    @Test
    public void testHttpSimpleUrl() throws Exception {
        HttpServer server = startServer();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/status/200";
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "    <def var=\"response\"><http url=\"" + url + "\"/></def>\n</config>";
            DynamicScopeContext context = executeXmlString(xml);
            assertNotNull(context.getVar("response"), "HTTP response variable should exist");
        } finally { server.stop(0); }
    }
}
