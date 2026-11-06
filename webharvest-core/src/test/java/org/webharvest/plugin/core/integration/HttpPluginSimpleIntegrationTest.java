package org.webharvest.plugin.core.integration;

import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.*;

/** Deterministic HttpPlugin integration tests using a loopback HTTP fixture. */
public class HttpPluginSimpleIntegrationTest extends XmlBasedIntegrationTestBase {
    private HttpServer startServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = "/json".equals(exchange.getRequestURI().getPath())
                ? "{\"fixture\":\"webharvest\"}" : "OK";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        return server;
    }

    @Test
    public void testHttpPluginDirectCall() throws Exception {
        HttpServer server = startServer();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/json";
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "    <http url=\"" + url + "\"/>\n</config>";
            DynamicScopeContext context = executeXmlString(xml);
            Variable result = context.getVar("configResult");
            assertNotNull(result, "HTTP result should exist in configResult");
            assertTrue(result.toString().contains("webharvest"), "Response should contain local fixture payload");
        } finally { server.stop(0); }
    }

    @Test
    public void testHttpPluginViaDefVar() throws Exception {
        HttpServer server = startServer();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/json";
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "    <def var=\"url\">" + url + "</def>\n" +
                "    <def var=\"response\"><http url=\"${url}\"/></def>\n</config>";
            DynamicScopeContext context = executeXmlString(xml);
            Variable response = context.getVar("response");
            assertNotNull(response, "HTTP response should be stored in response variable");
            assertTrue(response.toString().contains("webharvest"), "Response should contain local fixture payload");
        } finally { server.stop(0); }
    }
}
