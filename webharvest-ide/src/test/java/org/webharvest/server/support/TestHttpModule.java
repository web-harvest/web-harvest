package org.webharvest.server.support;

import com.google.inject.AbstractModule;
import org.webharvest.plugin.services.HttpResponse;
import org.webharvest.plugin.services.HttpService;
import org.webharvest.plugin.services.HttpServiceException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Guice module used by IDE integration tests to stub HTTP requests.
 * Ensures tests do not rely on external network connectivity.
 */
public final class TestHttpModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(HttpService.class).toInstance(new StubHttpService());
    }

    private static final class StubHttpService implements HttpService {

        private final Map<String, StubResponse> responses;

        private StubHttpService() {
            Map<String, StubResponse> map = new HashMap<>();

            map.put("https://jsonplaceholder.typicode.com/users/1",
                    StubResponse.json("{\"id\":1,\"name\":\"Leanne Graham\",\"email\":\"leanne@example.org\"}"));
            map.put("https://httpbin.org/get",
                    StubResponse.json("{\"url\":\"https://httpbin.org/get\",\"args\":{}}"));
            map.put("https://httpbin.org/json",
                    StubResponse.json("{\"slideshow\":{\"title\":\"Sample Slide Deck\",\"author\":\"Yours Truly\",\"slides\":[{\"title\":\"Wake up to WonderWidgets!\"}]}}"));
            map.put("https://httpbin.org/html",
                    StubResponse.html("<!DOCTYPE html><html><head><title>Herman Melville - Moby-Dick</title></head><body><h1>Example Domain</h1></body></html>"));
            map.put("https://example.com",
                    StubResponse.html("<!DOCTYPE html><html><head><title>Example Domain</title></head><body><h1>Example Domain</h1><p>Example content</p></body></html>"));

            this.responses = Collections.unmodifiableMap(map);
        }

        @Override
        public HttpResponse get(String url, Map<String, String> headers) throws HttpServiceException {
            return respond("GET", url);
        }

        @Override
        public HttpResponse post(String url, String body, Map<String, String> headers) throws HttpServiceException {
            return respond("POST", url);
        }

        @Override
        public HttpResponse request(String method, String url, String body, Map<String, String> headers) throws HttpServiceException {
            return respond(method, url);
        }

        private HttpResponse respond(String method, String url) throws HttpServiceException {
            StubResponse response = responses.get(url);
            if (response != null) {
                return response;
            }

            // Default stubbed JSON response for any unsupported URL.
            String json = "{\"stub\":true,\"method\":\"" + method + "\",\"url\":\"" + url + "\"}";
            return StubResponse.json(json);
        }
    }

    private static final class StubResponse implements HttpResponse {
        private final int status;
        private final String body;
        private final String contentType;

        private StubResponse(int status, String body, String contentType) {
            this.status = status;
            this.body = body;
            this.contentType = contentType;
        }

        private static StubResponse json(String body) {
            return new StubResponse(200, body, "application/json");
        }

        private static StubResponse html(String body) {
            return new StubResponse(200, body, "text/html");
        }

        @Override
        public int getStatusCode() {
            return status;
        }

        @Override
        public String getBody() {
            return body;
        }

        @Override
        public byte[] getBodyAsBinary() {
            return body.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Map<String, String> getHeaders() {
            return Collections.emptyMap();
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getCharset() {
            return StandardCharsets.UTF_8.name();
        }
    }
}


