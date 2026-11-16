/*
 * Copyright (c) 2006-2025 the original author or authors.
 * Distributed under the BSD-style license included with this project.
 */
package org.webharvest.plugin.integration;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;
import org.webharvest.plugin.services.HttpResponse;
import org.webharvest.plugin.services.HttpService;
import org.webharvest.plugin.services.HttpServiceException;

import static org.testng.Assert.*;

public class HttpPluginRequestContractIntegrationTest extends XmlBasedIntegrationTestBase {

    private RecordingHttpService recordingService;

    @Override
    protected HttpService createHttpService() {
        recordingService = new RecordingHttpService();
        return recordingService;
    }

    @Test(groups = {"integration", "http-plugins"})
    public void sendsPostBodyHeadersAndEncodedParameters() throws Exception {
        executeXmlString("<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
                + "<http url=\"https://service.test/items\" method=\"POST\" body=\"payload\">"
                + "<http-header name=\"X-Trace-Id\">trace-123</http-header>"
                + "<http-param name=\"q\">hello world</http-param>"
                + "<http-param name=\"tag\">a&amp;b</http-param>"
                + "</http></config>");

        assertEquals(recordingService.method, "POST");
        assertEquals(recordingService.url,
                "https://service.test/items?q=hello+world&tag=a%26b");
        assertEquals(recordingService.body, "payload");
        assertEquals(recordingService.headers.get("X-Trace-Id"), "trace-123");
    }

    @Test(groups = {"integration", "http-plugins"})
    public void sendsCustomMethodsThroughRequestContract() throws Exception {
        executeXmlString("<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
                + "<http url=\"https://service.test/resource\" method=\"PATCH\" body=\"update\"/>"
                + "</config>");

        assertEquals(recordingService.method, "PATCH");
        assertEquals(recordingService.url, "https://service.test/resource");
        assertEquals(recordingService.body, "update");
    }

    private static final class RecordingHttpService implements HttpService {
        private String method;
        private String url;
        private String body;
        private Map<String, String> headers = Collections.emptyMap();

        @Override
        public HttpResponse get(String url, Map<String, String> headers) {
            return record("GET", url, "", headers);
        }

        @Override
        public HttpResponse post(String url, String body, Map<String, String> headers) {
            return record("POST", url, body, headers);
        }

        @Override
        public HttpResponse request(String method, String url, String body, Map<String, String> headers) {
            return record(method, url, body, headers);
        }

        private HttpResponse record(String method, String url, String body, Map<String, String> headers) {
            this.method = method;
            this.url = url;
            this.body = body;
            this.headers = new HashMap<>(headers);
            return new HttpResponse() {
                @Override public int getStatusCode() { return 200; }
                @Override public String getBody() { return "recorded"; }
                @Override public byte[] getBodyAsBinary() { return "recorded".getBytes(StandardCharsets.UTF_8); }
                @Override public Map<String, String> getHeaders() { return Collections.emptyMap(); }
                @Override public String getHeader(String name) { return null; }
                @Override public String getContentType() { return "text/plain"; }
                @Override public String getCharset() { return "UTF-8"; }
            };
        }
    }
}
