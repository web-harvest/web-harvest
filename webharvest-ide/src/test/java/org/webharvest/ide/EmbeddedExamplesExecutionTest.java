package org.webharvest.ide;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.webharvest.definition.BufferConfigSource;
import org.webharvest.definition.Config;
import org.webharvest.definition.ConfigFactory;
import org.webharvest.definition.ConfigSource;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.Scraper;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.session.ScraperSessionImpl;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.plugin.services.HttpResponse;
import org.webharvest.plugin.services.HttpService;
import org.webharvest.plugin.services.HttpServiceException;

import java.util.UUID;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Integration tests that EXECUTE embedded IDE examples and verify
 * that variables are set correctly and contain expected values.
 */
public class EmbeddedExamplesExecutionTest {

    private Injector injector;
    private ConfigFactory configFactory;

    @BeforeClass
    public void setUp() {
        injector = Guice.createInjector(Modules.override(new ScraperModule(".")).with(new HttpStubModule()));
        configFactory = injector.getInstance(ConfigFactory.class);
    }

    @Test(description = "Test httpbin.org JSON example - should return actual JSON data", groups = "network")
    public void testHttpbinJsonExample() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                          "    <def var=\"jsonData\">\n" +
                          "        <http url=\"https://httpbin.org/json\"/>\n" +
                          "    </def>\n" +
                          "    <def var=\"xmlData\">\n" +
                          "        <json-to-xml>\n" +
                          "            <get var=\"jsonData\"/>\n" +
                          "        </json-to-xml>\n" +
                          "    </def>\n" +
                          "    <def var=\"title\">\n" +
                          "        <xpath expression=\"//slideshow/title/text()\">\n" +
                          "            <get var=\"xmlData\"/>\n" +
                          "        </xpath>\n" +
                          "    </def>\n" +
                          "</config>";

        DynamicScopeContext context = executeConfig(configXml);
        
        // Verify jsonData exists and contains JSON
        Variable jsonData = context.getVar("jsonData");
        assertNotNull(jsonData, "jsonData variable should be set");
        assertTrue(jsonData.toString().contains("slideshow"), 
                  "jsonData should contain 'slideshow' key");
        
        // Verify xmlData exists
        Variable xmlData = context.getVar("xmlData");
        assertNotNull(xmlData, "xmlData variable should be set");
        assertTrue(xmlData.toString().contains("<slideshow>"), 
                  "xmlData should contain XML elements");
        
        // Verify title was extracted
        Variable title = context.getVar("title");
        assertNotNull(title, "title variable should be set");
        assertTrue(title.toString().length() > 0, 
                  "title should not be empty");
    }

    @Test(description = "Test XQuery example with parameters", groups = "network")
    public void testXQueryWithParameters() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                          "    <def var=\"result\">\n" +
                          "        <xquery>\n" +
                          "            <xq-param name=\"num\" type=\"integer\">42</xq-param>\n" +
                          "            <xq-expression>\n" +
                          "                <![CDATA[\n" +
                          "                declare variable $num as xs:integer external;\n" +
                          "                <result>{$num * 2}</result>\n" +
                          "                ]]>\n" +
                          "            </xq-expression>\n" +
                          "        </xquery>\n" +
                          "    </def>\n" +
                          "</config>";

        DynamicScopeContext context = executeConfig(configXml);
        
        // Verify result exists and contains expected value
        Variable result = context.getVar("result");
        assertNotNull(result, "result variable should be set");
        assertTrue(result.toString().contains("84"), 
                  "result should contain 42*2=84, got: " + result.toString());
    }

    @Test(description = "Test simple HTTP + XPath extraction")
    public void testHttpXPathExtraction() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                          "    <def var=\"html\">\n" +
                          "        <http url=\"https://example.com\"/>\n" +
                          "    </def>\n" +
                          "    <def var=\"page\">\n" +
                          "        <html-to-xml>\n" +
                          "            <get var=\"html\"/>\n" +
                          "        </html-to-xml>\n" +
                          "    </def>\n" +
                          "    <def var=\"title\">\n" +
                          "        <xpath expression=\"//title/text()\">\n" +
                          "            <get var=\"page\"/>\n" +
                          "        </xpath>\n" +
                          "    </def>\n" +
                          "    <def var=\"heading\">\n" +
                          "        <xpath expression=\"//h1/text()\">\n" +
                          "            <get var=\"page\"/>\n" +
                          "        </xpath>\n" +
                          "    </def>\n" +
                          "</config>";

        DynamicScopeContext context = executeConfig(configXml);
        
        // Verify ALL variables are set
        Variable html = context.getVar("html");
        assertNotNull(html, "html variable should be set");
        assertTrue(html.toString().contains("Example Domain"), 
                  "html should contain 'Example Domain'");
        
        Variable page = context.getVar("page");
        assertNotNull(page, "page variable should be set");
        assertTrue(page.toString().contains("<html"), 
                  "page should contain XML representation");
        
        Variable title = context.getVar("title");
        assertNotNull(title, "title variable should be set");
        assertTrue(title.toString().contains("Example Domain"), 
                  "title should be 'Example Domain'");
        
        Variable heading = context.getVar("heading");
        assertNotNull(heading, "heading variable should be set");
        assertTrue(heading.toString().contains("Example Domain"), 
                  "heading should contain 'Example Domain'");
    }

    @Test(description = "Test that loop creates _index variable")
    public void testLoopIndexVariable() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                          "    <def var=\"last_index\">-1</def>\n" +
                          "    <loop maxloops=\"5\">\n" +
                          "        <def var=\"last_index\">${_index}</def>\n" +
                          "    </loop>\n" +
                          "</config>";

        DynamicScopeContext context = executeConfig(configXml);
        
        Variable lastIndex = context.getVar("last_index");
        assertNotNull(lastIndex, "last_index variable should be set");
        assertEquals(lastIndex.toString().trim(), "4", 
                    "last_index should be 4 (0-based, 5 iterations)");
    }

    @Test(description = "Test that multiple variables are all visible in context")
    public void testMultipleVariablesVisible() throws Exception {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                          "    <def var=\"var1\">Value 1</def>\n" +
                          "    <def var=\"var2\">Value 2</def>\n" +
                          "    <def var=\"var3\">Value 3</def>\n" +
                          "    <def var=\"var4\">Value 4</def>\n" +
                          "    <def var=\"var5\">Value 5</def>\n" +
                          "</config>";

        DynamicScopeContext context = executeConfig(configXml);
        
        // Verify ALL 5 variables are visible
        for (int i = 1; i <= 5; i++) {
            String varName = "var" + i;
            Variable var = context.getVar(varName);
            assertNotNull(var, varName + " should be set");
            assertEquals(var.toString().trim(), "Value " + i, 
                        varName + " should have correct value");
        }
        
        // Count total variables (including configResult)
        int totalVars = 0;
        for (org.webharvest.utils.KeyValuePair<Variable> pair : context) {
            if (!"sys".equals(pair.getKey()) && !"http".equals(pair.getKey())) {
                totalVars++;
            }
        }
        
        assertTrue(totalVars >= 5, 
                  "Should have at least 5 user variables, found: " + totalVars);
    }

    /**
     * Helper method to execute config and return context with variables.
     */
    private DynamicScopeContext executeConfig(String configXml) throws Exception {
        ConfigSource configSource = new BufferConfigSource(configXml);
        Config config = configFactory.create(configSource);
        config.reload();
        
        DynamicScopeContext context = new ScraperContext();
        context.setConfig(config);
        
        String sessionId = UUID.randomUUID().toString();
        EventBus eventBus = new EventBus();
        ScraperSessionImpl session = new ScraperSessionImpl(sessionId, "test-client", config, eventBus);
        context.setEventBus(eventBus);
        context.setSession(session);
        session.markRunning(context);

        Scraper scraper = new Scraper(eventBus);
        scraper.execute(context);
        session.markCompleted();

        return context;
    }

    private static final class HttpStubModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(HttpService.class).toInstance(new StubHttpService());
        }

        private static final class StubHttpService implements HttpService {
            private final Map<String, StubResponse> responses;

            private StubHttpService() {
                Map<String, StubResponse> map = new HashMap<>();
                String slideshowJson = "{" +
                        "\"slideshow\": {" +
                        "  \"title\": \"Sample Slide Deck\"," +
                        "  \"author\": \"Yours Truly\"," +
                        "  \"date\": \"date of publication\"," +
                        "  \"slides\": [{\"title\": \"Wake up to WonderWidgets!\"}]" +
                        "}}";
                map.put("https://httpbin.org/json", StubResponse.json(slideshowJson));
                map.put("https://example.com", StubResponse.html("<html><head><title>Example Domain</title></head>" +
                        "<body><h1>Example Domain</h1><p>Example content</p></body></html>"));
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
                if (response == null) {
                    throw new HttpServiceException("No stub response for " + method + " " + url);
                }
                return response;
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

            static StubResponse json(String body) {
                return new StubResponse(200, body, "application/json");
            }

            static StubResponse html(String body) {
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
                return body.getBytes();
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
                return "UTF-8";
            }
        }
    }
}

