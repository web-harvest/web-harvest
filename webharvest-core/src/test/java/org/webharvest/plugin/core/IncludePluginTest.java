package org.webharvest.plugin.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.definition.ConfigFactory;
import org.webharvest.definition.ConfigSourceFactory;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.DefaultPluginBodyExecutor;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.IncludeExecutionException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

public class IncludePluginTest {

    private static final String NAMESPACE = "http://org.webharvest/schema/2.1/core";

    private Injector injector;
    private ConfigFactory configFactory;
    private ConfigSourceFactory configSourceFactory;
    private ScraperContext context;
    private Path directory;

    @BeforeMethod
    public void setUp() throws IOException {
        directory = Files.createTempDirectory("webharvest-include-test");
        injector = Guice.createInjector(new ScraperModule(directory.toString()));
        configFactory = injector.getInstance(ConfigFactory.class);
        configSourceFactory = injector.getInstance(ConfigSourceFactory.class);
        context = injector.getInstance(ScraperContext.class);
    }

    @Test
    public void shouldExecuteIncludedConfigurationAndShareVariables() throws Exception {
        write("main.xml", config("<include path=\"included.xml\"/>"
                + "<def var=\"afterInclude\" value=\"done\"/>"));
        write("included.xml", config("<def var=\"fromInclude\" value=\"loaded\"/>"));

        Config main = load("main.xml");
        context.setConfig(main);

        Variable result = execute(main);

        assertNotNull(result);
        assertEquals(context.getVar("fromInclude").toString(), "loaded");
        assertEquals(context.getVar("afterInclude").toString(), "done");
        assertSame(context.getConfig(), main, "The parent config must be restored after include");
    }

    @Test
    public void shouldResolveNestedIncludesRelativeToEachIncludingFile() throws Exception {
        write("main.xml", config("<include path=\"sub/first.xml\"/>"));
        write("sub/first.xml", config("<include path=\"nested/second.xml\"/>"));
        write("sub/nested/second.xml", config("<def var=\"nestedValue\" value=\"resolved\"/>"));

        Config main = load("main.xml");
        context.setConfig(main);

        execute(main);

        assertEquals(context.getVar("nestedValue").toString(), "resolved");
        assertSame(context.getConfig(), main, "Nested includes must restore each parent config");
    }

    @Test
    public void shouldAbortContainingConfigurationAndRestoreParentOnMissingInclude() throws Exception {
        write("main.xml", config("<include path=\"missing.xml\"/>"
                + "<def var=\"mustNotRun\" value=\"incorrect\"/>"));

        Config main = load("main.xml");
        context.setConfig(main);

        try {
            execute(main);
            fail("A missing include must fail the containing configuration");
        } catch (RuntimeException failure) {
            assertTrue(hasIncludeFailure(failure), "Failure must retain the IncludeExecutionException marker");
        }

        assertNull(context.getVar("mustNotRun"), "Later siblings must not execute after a fatal include");
        assertSame(context.getConfig(), main, "Parent config must be restored after include failure");
    }

    @Test
    public void shouldResolveNestedIncludesRelativeToHttpConfigurationUrls() throws Exception {
        Map<String, String> resources = new HashMap<>();
        resources.put("/main.xml", config("<include path=\"parts/first.xml\"/>"));
        resources.put("/parts/first.xml", config("<include path=\"nested/second.xml\"/>"));
        resources.put("/parts/nested/second.xml", config("<def var=\"httpNestedValue\" value=\"resolved\"/>"));

        try (LocalHttpFixture fixture = new LocalHttpFixture(resources)) {
            Config main = load(fixture.url("/main.xml"));
            context.setConfig(main);

            execute(main);

            assertEquals(context.getVar("httpNestedValue").toString(), "resolved");
            assertSame(context.getConfig(), main, "HTTP include must restore the original parent config");
        }
    }

    @Test
    public void shouldRestoreParentConfigWhenNestedIncludeFails() throws Exception {
        write("main.xml", config("<include path=\"sub/first.xml\"/>"
                + "<def var=\"mustNotRunAfterNestedFailure\" value=\"incorrect\"/>"));
        write("sub/first.xml", config("<include path=\"missing.xml\"/>"));

        Config main = load("main.xml");
        context.setConfig(main);

        try {
            execute(main);
            fail("A nested missing include must abort the containing configuration");
        } catch (RuntimeException failure) {
            assertTrue(hasIncludeFailure(failure));
        }

        assertNull(context.getVar("mustNotRunAfterNestedFailure"));
        assertSame(context.getConfig(), main, "Every include frame must restore its parent Config");
    }

    @Test
    public void shouldRestoreParentConfigWhenIncludedExecutionIsInterrupted() throws Exception {
        CorePluginRegistry.getInstance().registerMapping(null, InterruptingPlugin.class, "interrupt-test");
        write("main.xml", config("<include path=\"included.xml\"/>"));
        write("included.xml", config("<interrupt-test/>"));

        Config main = load("main.xml");
        context.setConfig(main);

        try {
            execute(main);
            fail("The included child must propagate interruption");
        } catch (InterruptedException expected) {
            assertSame(context.getConfig(), main, "Parent Config must be restored before interruption escapes");
            assertTrue(Thread.currentThread().isInterrupted(), "Interruption status must be preserved");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void shouldRejectMalformedIncludedConfigurationAndRestoreParent() throws Exception {
        write("main.xml", config("<include path=\"malformed.xml\"/>"
                + "<def var=\"mustNotRunAfterMalformedInclude\" value=\"incorrect\"/>"));
        write("malformed.xml", "<config xmlns=\"" + NAMESPACE + "\"><def var=\"broken\"></config>");

        Config main = load("main.xml");
        context.setConfig(main);

        try {
            execute(main);
            fail("Malformed included XML must fail the containing configuration");
        } catch (RuntimeException failure) {
            assertTrue(hasIncludeFailure(failure));
        }

        assertNull(context.getVar("mustNotRunAfterMalformedInclude"));
        assertSame(context.getConfig(), main, "Parent Config must be restored after parse failure");
    }

    @Test
    public void shouldNotLetTryPluginSwallowFatalIncludeFailure() throws Exception {
        write("main.xml", config("<try><include path=\"missing.xml\"/></try>"
                + "<def var=\"afterTryFailure\" value=\"incorrect\"/>"));

        Config main = load("main.xml");
        context.setConfig(main);

        try {
            execute(main);
            fail("A fatal Include failure must bypass the <try> recovery path");
        } catch (RuntimeException failure) {
            assertTrue(hasIncludeFailure(failure));
        }

        assertNull(context.getVar("afterTryFailure"));
        assertSame(context.getConfig(), main);
    }

    @Test
    public void shouldNotLetFunctionCallSwallowFatalIncludeFailure() throws Exception {
        write("main.xml", config("<function name=\"loadMissing\"><include path=\"missing.xml\"/>"
                + "</function><call name=\"loadMissing\"/>"
                + "<def var=\"afterCallFailure\" value=\"incorrect\"/>"));

        Config main = load("main.xml");
        context.setConfig(main);

        try {
            execute(main);
            fail("A fatal Include failure must bypass the function-call recovery path");
        } catch (RuntimeException failure) {
            assertTrue(hasIncludeFailure(failure));
        }

        assertNull(context.getVar("afterCallFailure"));
        assertSame(context.getConfig(), main);
    }

    @Test
    public void shouldFailClearlyWhenConfigFactoryWasNotInjected() throws Exception {
        write("main.xml", config(""));
        write("included.xml", config("<def var=\\"includedValue\\" value=\\"loaded\\"/>"));
        Config main = load("main.xml");
        context.setConfig(main);

        IncludePlugin plugin = new IncludePlugin();
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "include", NAMESPACE, "include", Collections.singletonMap("path", "included.xml"));
        plugin.initialize(configuration);

        try {
            plugin.execute(context);
            fail("An Include plugin without Guice injection must fail explicitly");
        } catch (RuntimeException failure) {
            assertTrue(hasMessageContaining(failure, "ConfigFactory was not injected"),
                    "Failure must explain the missing dependency instead of exposing a NullPointerException");
        }
        assertSame(context.getConfig(), main, "The parent config must remain active");
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void shouldValidateRequiredPath() throws Exception {
        IncludePlugin plugin = new IncludePlugin();
        DefaultPluginConfiguration configuration =
                new DefaultPluginConfiguration("include", NAMESPACE, "include", Collections.emptyMap());

        plugin.validateConfiguration(configuration);
    }

    @Test
    public void shouldExposeIncludeMetadataAndSchema() {
        IncludePlugin plugin = new IncludePlugin();
        assertEquals(plugin.getMetadata().getId(), "include");
        assertEquals(plugin.getMetadata().getElementName(), "include");
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertEquals(schema.getRequiredAttributes(), new String[]{"path"});
    }

    private Config load(String name) {
        Config config = configFactory.create(configSourceFactory.create(directory.resolve(name).toFile()));
        config.reload();
        return config;
    }

    private Config load(URL url) {
        Config config = configFactory.create(configSourceFactory.create(url));
        config.reload();
        return config;
    }

    private Variable execute(Config config) throws InterruptedException {
        return new DefaultPluginBodyExecutor().executeChildren(
                config.getConfigurationTree().getConfiguration().getChildren(), context);
    }

    private void write(String name, String xml) throws IOException {
        Path file = directory.resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, xml.getBytes(StandardCharsets.UTF_8));
    }

    private String config(String children) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<config xmlns=\"" + NAMESPACE + "\">" + children + "</config>";
    }

    public static final class InterruptingPlugin extends AbstractPlugin {
        @Override
        public PluginMetadata getMetadata() {
            return new EmptyPlugin().getMetadata();
        }

        @Override
        protected void doInitialize(PluginConfiguration configuration) {
            // No initialization required by this test plugin.
        }

        @Override
        protected Variable doExecute(org.webharvest.runtime.DynamicScopeContext executionContext)
                throws InterruptedException {
            throw new InterruptedException("intentional Include interruption test");
        }

        @Override
        protected void doValidateConfiguration(PluginConfiguration configuration) {
            // No additional configuration required by this test plugin.
        }
    }

    private static final class LocalHttpFixture implements AutoCloseable {
        private final ServerSocket server;
        private final Map<String, String> resources;
        private final Thread worker;

        private LocalHttpFixture(Map<String, String> resources) throws IOException {
            this.resources = new HashMap<>(resources);
            server = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
            worker = new Thread(this::serve, "include-http-fixture");
            worker.setDaemon(true);
            worker.start();
        }

        private URL url(String path) throws IOException {
            return new URL("http://127.0.0.1:" + server.getLocalPort() + path);
        }

        private void serve() {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    String requestLine = reader.readLine();
                    String requestPath = requestLine == null ? "/" : requestLine.split(" ")[1];
                    String header;
                    while ((header = reader.readLine()) != null && !header.isEmpty()) {
                        // Consume request headers before writing the response.
                    }
                    String body = resources.get(requestPath);
                    byte[] payload = (body == null ? "Not found" : body).getBytes(StandardCharsets.UTF_8);
                    String status = body == null ? "404 Not Found" : "200 OK";
                    OutputStream output = socket.getOutputStream();
                    output.write(("HTTP/1.1 " + status + "\r\n"
                            + "Content-Type: application/xml; charset=UTF-8\r\n"
                            + "Content-Length: " + payload.length + "\r\n"
                            + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    output.write(payload);
                    output.flush();
                } catch (IOException e) {
                    if (!server.isClosed()) {
                        throw new IllegalStateException("Local Include HTTP fixture failed", e);
                    }
                }
            }
        }

        @Override
        public void close() throws Exception {
            server.close();
            worker.join(1000L);
        }
    }

    private boolean hasMessageContaining(Throwable failure, String expected) {
        Throwable current = failure;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(expected)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean hasIncludeFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof IncludeExecutionException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
