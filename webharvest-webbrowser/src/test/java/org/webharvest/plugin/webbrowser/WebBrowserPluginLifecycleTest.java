package org.webharvest.plugin.webbrowser;

import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.google.common.eventbus.EventBus;
import org.webharvest.definition.Config;
import org.webharvest.definition.CorePluginsRegistrar;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.resource.LedgerExecutionResourceCoordinator;
import org.webharvest.runtime.resource.ResourceLedger;
import org.webharvest.runtime.resource.LedgerExecutionResourceContext;
import org.webharvest.runtime.resource.ExecutionResourceAccess;
import org.webharvest.runtime.session.ExecutionOwner;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.session.ScraperSessionImpl;
import org.webharvest.runtime.variables.Variable;

import java.io.IOException;
import java.time.Duration;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

/**
 * End-to-end lifecycle tests for the HtmlUnit-backed WebBrowser plugin.
 * All requests target an in-process HTTP server; the suite does not use the public Internet.
 */
public class WebBrowserPluginLifecycleTest {

    private static final String NAMESPACE = "http://org.webharvest/schema/2.1/core";
    private static final String PAGE =
            "<!doctype html><html><head><title>before</title></head>"
                    + "<body><div id='result'>initial</div>"
                    + "<script>document.body.setAttribute('data-script', 'ran');</script>"
                    + "</body></html>";

    private static HttpServer server;
    private static String baseUrl;

    private DynamicScopeContext context;

    @BeforeClass
    public void startServer() throws IOException {
        CorePluginsRegistrar.registerCorePlugins();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/page", exchange -> respond(exchange, 200, PAGE));
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/page");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/missing", exchange -> respond(exchange, 404, "missing"));
        server.createContext("/session-a", exchange -> respond(exchange, 200,
                "<html><head><title>session-a</title></head><body>A</body></html>"));
        server.createContext("/session-b", exchange -> respond(exchange, 200,
                "<html><head><title>session-b</title></head><body>B</body></html>"));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterClass(alwaysRun = true)
    public void stopServer() {
        WebBrowserPlugin.closeAllPages();
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeMethod
    public void setUp() {
        WebBrowserPlugin.closeAllPages();
        context = new ScraperContext();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        WebBrowserPlugin.closeAllPages();
    }

    @Test
    public void loadExecuteScriptGetContentAndCloseNamedPage() throws Exception {
        Variable loaded = execute("load", "main", baseUrl + "/page", true, 1, null);
        assertNotNull(loaded);
        assertTrue(loaded.toString().contains("before"));

        Variable scriptResult = execute(
                "execute-script", "main", null, true, 1,
                "document.getElementById('result').textContent = 'executed';"
                        + "document.title = 'after'; document.title");
        assertEquals(scriptResult.toString(), "after");

        Variable content = execute("get-content", "main", null, true, 0, null);
        assertTrue(content.toString().contains("executed"));
        assertTrue(content.toString().contains("<title>after</title>"));

        execute("close", "main", null, true, 0, null);
        expectRuntimeException(
                () -> execute("get-content", "main", null, true, 0, null));
    }

    @Test
    public void loadDefaultPageWithJavaScriptDisabledAndCloseAll() throws Exception {
        execute("load", null, baseUrl + "/page", false, 0, null);

        Variable content = execute("get-content", null, null, false, 0, null);
        assertTrue(content.toString().contains("<title>before</title>"));
        assertFalse(content.toString().contains("data-script=\"ran\""));

        WebBrowserPlugin.closeAllPages();
        expectRuntimeException(
                () -> execute("get-content", null, null, false, 0, null));
    }

    @Test
    public void loadFollowsRedirectAndReportsHttpFailure() throws Exception {
        Variable redirected = execute("load", "redirect", baseUrl + "/redirect", false, 0, null);
        assertTrue(redirected.toString().contains("<title>before</title>"));
        assertTrue(execute("get-content", "redirect", null, false, 0, null)
                .toString().contains("<title>before</title>"));

        RuntimeException failure = expectRuntimeException(
                () -> execute("load", "missing", baseUrl + "/missing", false, 0, null));
        assertTrue(failure.getMessage().contains("Error in webbrowser plugin"));
        assertNotNull(failure.getCause());
    }

    @Test
    public void executeScriptRequiresExistingPageAndNonEmptyBody() throws Exception {
        RuntimeException missingPage = expectRuntimeException(
                () -> execute("execute-script", "absent", null, true, 0, "1 + 1"));
        assertTrue(missingPage.getCause().getMessage().contains("not found"));

        execute("load", "script", baseUrl + "/page", true, 0, null);
        RuntimeException emptyBody = expectRuntimeException(
                () -> execute("execute-script", "script", null, true, 0, ""));
        assertTrue(emptyBody.getCause().getMessage().contains("JavaScript code is required"));
    }

    @Test
    public void executeScriptReturnsWhenPageReportsJavaScriptError() throws Exception {
        execute("load", "script-error", baseUrl + "/page", true, 0, null);
        Variable result = execute("execute-script", "script-error", null, true, 0,
                "throw new Error('script failure')");
        assertNotNull(result);
    }

    @Test
    public void replacingNamedAndDefaultPagesClosesPreviousClients() throws Exception {
        execute("load", "replace", baseUrl + "/page", false, 0, null);
        execute("load", "replace", baseUrl + "/page", true, 0, null);
        execute("close", "replace", null, true, 0, null);

        execute("load", null, baseUrl + "/page", false, 0, null);
        execute("load", null, baseUrl + "/page", true, 0, null);
        execute("close", null, null, true, 0, null);
    }

    @Test
    public void browserPagesAreIsolatedAndCleanedUpPerSession() throws Exception {
        EventBus eventBus = new EventBus();
        ScraperSessionImpl sessionA =
                new ScraperSessionImpl("webbrowser-session-a", null, mock(Config.class), eventBus);
        ScraperSessionImpl sessionB =
                new ScraperSessionImpl("webbrowser-session-b", null, mock(Config.class), eventBus);
        DynamicScopeContext contextA = new ScraperContext();
        DynamicScopeContext contextB = new ScraperContext();
        contextA.setEventBus(eventBus);
        contextA.setSession(sessionA);
        contextB.setEventBus(eventBus);
        contextB.setSession(sessionB);
        sessionA.markRunning(contextA);
        sessionB.markRunning(contextB);

        try {
            execute(contextA, "load", "main", baseUrl + "/session-a", false, 0, null);
            execute(contextB, "load", "main", baseUrl + "/session-b", false, 0, null);
            assertTrue(execute(contextA, "get-content", "main", null, false, 0, null)
                    .toString().contains("<title>session-a</title>"));
            assertTrue(execute(contextB, "get-content", "main", null, false, 0, null)
                    .toString().contains("<title>session-b</title>"));

            execute(contextA, "close", "main", null, false, 0, null);
            expectRuntimeException(
                    () -> execute(contextA, "get-content", "main", null, false, 0, null));
            assertTrue(execute(contextB, "get-content", "main", null, false, 0, null)
                    .toString().contains("<title>session-b</title>"));

            // Default pages must be isolated by the same session boundary.
            execute(contextA, "load", null, baseUrl + "/session-a", false, 0, null);
            execute(contextB, "load", null, baseUrl + "/session-b", false, 0, null);
            assertTrue(execute(contextA, "get-content", null, null, false, 0, null)
                    .toString().contains("<title>session-a</title>"));
            assertTrue(execute(contextB, "get-content", null, null, false, 0, null)
                    .toString().contains("<title>session-b</title>"));

            execute(contextA, "close", null, null, false, 0, null);
            expectRuntimeException(
                    () -> execute(contextA, "get-content", null, null, false, 0, null));
            assertTrue(execute(contextB, "get-content", null, null, false, 0, null)
                    .toString().contains("<title>session-b</title>"));

            // A terminal event closes every browser owned by A, but leaves B untouched.
            execute(contextA, "load", null, baseUrl + "/session-a", false, 0, null);
            sessionA.markCompleted();
            expectRuntimeException(
                    () -> execute(contextA, "get-content", "main", null, false, 0, null));
            expectRuntimeException(
                    () -> execute(contextA, "get-content", null, null, false, 0, null));
            assertTrue(execute(contextB, "get-content", "main", null, false, 0, null)
                    .toString().contains("<title>session-b</title>"));
            assertTrue(execute(contextB, "get-content", null, null, false, 0, null)
                    .toString().contains("<title>session-b</title>"));
        } finally {
            if (!sessionA.getStatus().isTerminal()) {
                sessionA.markFailed(new RuntimeException("Test cleanup"));
            }
            if (!sessionB.getStatus().isTerminal()) {
                sessionB.markFailed(new RuntimeException("Test cleanup"));
            }
        }
    }

    @Test
    public void managedBrowserStateIsRetiredByExecutionResourceLifecycle() throws Exception {
        String runtimeId = "webbrowser-managed-runtime";
        String sessionId = "webbrowser-managed-session";
        ExecutionOwner owner = new ExecutionOwner(runtimeId, sessionId, 1);
        EventBus eventBus = new EventBus();
        ScraperSessionImpl session = new ScraperSessionImpl(
            sessionId, null, mock(Config.class), eventBus, owner);
        LedgerExecutionResourceCoordinator resources = new LedgerExecutionResourceCoordinator(
            new ResourceLedger(runtimeId), owner);
        DynamicScopeContext managedContext = new ScraperContext();
        managedContext.setEventBus(eventBus);
        managedContext.setSession(session);
        managedContext.setExecutionOwner(owner);
        managedContext.setResourceAccess(resources.getAccess());
        session.markRunning(managedContext);

        try {
            execute(managedContext, "load", "managed", baseUrl + "/session-a", false, 0, null);
            assertTrue(execute(managedContext, "get-content", "managed", null, false, 0, null)
                .toString().contains("<title>session-a</title>"));

            resources.requestTermination();
            java.util.List<LedgerExecutionResourceContext.DrainReport> reports =
                resources.markOwnerQuiescentAndDrain(Duration.ofSeconds(2));
            assertEquals(reports.size(), 1);
            assertEquals(reports.get(0).getState(), ExecutionResourceAccess.ResourceState.CLOSED);

            expectRuntimeException(
                () -> execute(managedContext, "get-content", "managed", null, false, 0, null));
        } finally {
            if (!session.getStatus().isTerminal()) {
                session.markFailed(new RuntimeException("Managed browser test cleanup"));
            }
            WebBrowserPlugin.closeAllPages();
        }
    }

    @Test
    public void terminatingOneManagedBrowserSessionDoesNotCloseAnother() throws Exception {
        String runtimeId = "webbrowser-multi-session-runtime";
        EventBus eventBus = new EventBus();
        ResourceLedger ledger = new ResourceLedger(runtimeId);
        ExecutionOwner ownerA = new ExecutionOwner(runtimeId, "webbrowser-managed-a", 1);
        ExecutionOwner ownerB = new ExecutionOwner(runtimeId, "webbrowser-managed-b", 2);
        ScraperSessionImpl sessionA = new ScraperSessionImpl(
            ownerA.getSessionId(), null, mock(Config.class), eventBus, ownerA);
        ScraperSessionImpl sessionB = new ScraperSessionImpl(
            ownerB.getSessionId(), null, mock(Config.class), eventBus, ownerB);
        LedgerExecutionResourceCoordinator resourcesA =
            new LedgerExecutionResourceCoordinator(ledger, ownerA);
        LedgerExecutionResourceCoordinator resourcesB =
            new LedgerExecutionResourceCoordinator(ledger, ownerB);
        DynamicScopeContext contextA = new ScraperContext();
        DynamicScopeContext contextB = new ScraperContext();
        contextA.setEventBus(eventBus);
        contextA.setSession(sessionA);
        contextA.setExecutionOwner(ownerA);
        contextA.setResourceAccess(resourcesA.getAccess());
        contextB.setEventBus(eventBus);
        contextB.setSession(sessionB);
        contextB.setExecutionOwner(ownerB);
        contextB.setResourceAccess(resourcesB.getAccess());
        sessionA.markRunning(contextA);
        sessionB.markRunning(contextB);

        try {
            execute(contextA, "load", "main", baseUrl + "/session-a", false, 0, null);
            execute(contextB, "load", "main", baseUrl + "/session-b", false, 0, null);

            resourcesA.requestTermination();
            java.util.List<LedgerExecutionResourceContext.DrainReport> reportsA =
                resourcesA.markOwnerQuiescentAndDrain(Duration.ofSeconds(2));
            assertEquals(reportsA.size(), 1);
            assertEquals(reportsA.get(0).getState(), ExecutionResourceAccess.ResourceState.CLOSED);

            expectRuntimeException(
                () -> execute(contextA, "get-content", "main", null, false, 0, null));
            assertTrue(execute(contextB, "get-content", "main", null, false, 0, null)
                .toString().contains("<title>session-b</title>"));
        } finally {
            if (!sessionA.getStatus().isTerminal()) {
                resourcesA.requestTermination();
                resourcesA.markOwnerQuiescentAndDrain(Duration.ofSeconds(2));
                sessionA.markFailed(new RuntimeException("Managed browser session A cleanup"));
            }
            if (!sessionB.getStatus().isTerminal()) {
                resourcesB.requestTermination();
                resourcesB.markOwnerQuiescentAndDrain(Duration.ofSeconds(2));
                sessionB.markFailed(new RuntimeException("Managed browser session B cleanup"));
            }
            WebBrowserPlugin.closeAllPages();
        }
    }

    private RuntimeException expectRuntimeException(ThrowingRunnable action) {
        try {
            action.run();
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            return expected;
        } catch (Exception other) {
            throw new AssertionError("Expected RuntimeException but got " + other, other);
        }
        throw new AssertionError("Expected RuntimeException");
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private Variable execute(
            String action,
            String page,
            String url,
            boolean javascriptEnabled,
            int wait,
            String script) throws Exception {
        return execute(context, action, page, url, javascriptEnabled, wait, script);
    }

    private Variable execute(
            DynamicScopeContext executionContext,
            String action,
            String page,
            String url,
            boolean javascriptEnabled,
            int wait,
            String script) throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("action", action);
        properties.put("javascript-enabled", Boolean.toString(javascriptEnabled));
        properties.put("wait", Integer.toString(wait));
        if (page != null) {
            properties.put("page", page);
        }
        if (url != null) {
            properties.put("url", url);
        }

        DefaultPluginConfiguration configuration =
                new DefaultPluginConfiguration("web-browser", NAMESPACE, "web-browser", properties);
        if (script != null) {
            DefaultPluginConfiguration text = new DefaultPluginConfiguration("text", NAMESPACE, "text");
            text.setProperty("_textContent", script);
            configuration.setChildren(Collections.singletonList(text));
        }

        WebBrowserPlugin plugin = new WebBrowserPlugin();
        plugin.initialize(configuration);
        return plugin.execute(executionContext);
    }

    private static void respond(
            com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
