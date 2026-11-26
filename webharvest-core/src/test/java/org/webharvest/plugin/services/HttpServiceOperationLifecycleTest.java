package org.webharvest.plugin.services;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.webharvest.runtime.resource.ExecutionResourceAccess;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Deterministic loopback tests for request-scoped HTTP cancellation and timeout.
 */
public class HttpServiceOperationLifecycleTest {

    private HttpServer server;
    private ExecutorService serverExecutor;
    private String baseUrl;
    private final CountDownLatch blockedRequestStarted = new CountDownLatch(1);
    private final CountDownLatch releaseBlockedRequest = new CountDownLatch(1);

    @BeforeClass(alwaysRun = true)
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverExecutor = Executors.newCachedThreadPool();
        server.setExecutor(serverExecutor);
        server.createContext("/ok", exchange -> {
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/delayed-headers", exchange -> {
            try {
                Thread.sleep(600);
                byte[] body = "late".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(body);
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
                // A timed-out client may close the connection before headers arrive.
            }
        });
        server.createContext("/blocked", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            blockedRequestStarted.countDown();
            try (OutputStream output = exchange.getResponseBody()) {
                releaseBlockedRequest.await(5, TimeUnit.SECONDS);
                output.write("released".getBytes(StandardCharsets.UTF_8));
            } catch (Exception ignored) {
                // Client cancellation is expected to close the response stream.
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterClass(alwaysRun = true)
    public void stopServer() {
        releaseBlockedRequest.countDown();
        if (server != null) {
            server.stop(0);
        }
        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
        }
    }

    @Test
    public void successfulRequestReleasesItsLeaseBeforeClosingOperation() throws Exception {
        TrackingResourceAccess access = new TrackingResourceAccess();
        try (HttpServiceImpl service = new HttpServiceImpl()) {
            HttpResponse response = service.request("GET", baseUrl + "/ok", null, Map.of(),
                Duration.ofSeconds(2), access);
            assertEquals(response.getBody(), "ok");
            assertTrue(access.leaseReleased.get(), "Request lease must be released");
            assertTrue(access.operationClosed.get(), "Operation resource must be closed");
            assertTrue(access.leaseCloseOrder.get() < access.operationCloseOrder.get(),
                "The operation must close only after its lease is released");
        }
    }

    @Test
    public void ownerAbortCancelsOnlyTheActiveRequestAndReleasesItsLease() throws Exception {
        TrackingResourceAccess access = new TrackingResourceAccess();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (HttpServiceImpl service = new HttpServiceImpl()) {
            Future<HttpResponse> request = executor.submit(() -> service.request(
                "GET", baseUrl + "/blocked", null, Map.of(), Duration.ofSeconds(5), access));

            assertTrue(blockedRequestStarted.await(3, TimeUnit.SECONDS),
                "Loopback server must receive the request before cancellation");

            TrackingResourceAccess unaffectedAccess = new TrackingResourceAccess();
            Future<HttpResponse> unaffectedRequest = executor.submit(() -> service.request(
                "GET", baseUrl + "/ok", null, Map.of(), Duration.ofSeconds(5), unaffectedAccess));
            assertEquals(unaffectedRequest.get(5, TimeUnit.SECONDS).getBody(), "ok",
                "A separate request on the shared client must remain usable");

            access.abortActiveRequest();

            try {
                request.get(3, TimeUnit.SECONDS);
                fail("Cancelled HTTP request must not produce a successful response");
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof HttpServiceException
                        || expected.getCause() instanceof RuntimeException,
                    "Cancellation must surface as a request failure");
            }

            assertTrue(access.abortInvoked.get(), "Resource abort callback must be invoked");
            assertTrue(access.leaseReleased.get(), "Cancelled request lease must be released");
            assertTrue(access.operationClosed.get(), "Cancelled operation must be closed");
            assertTrue(access.leaseCloseOrder.get() < access.operationCloseOrder.get(),
                "Cancelled operation must close only after its lease is released");
            assertTrue(unaffectedAccess.leaseReleased.get());
            assertTrue(unaffectedAccess.operationClosed.get());
        } finally {
            releaseBlockedRequest.countDown();
            executor.shutdownNow();
            executor.awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    @Test
    public void responseTimeoutBoundsAStalledResponse() throws Exception {
        try (HttpServiceImpl service = new HttpServiceImpl()) {
            try {
                service.request("GET", baseUrl + "/delayed-headers", null, Map.of(),
                    Duration.ofMillis(150), null);
                fail("A response that exceeds the configured timeout must fail");
            } catch (HttpServiceException expected) {
                assertTrue(expected.getCause() != null,
                    "Timeout failure should retain the underlying I/O cause");
            }
        }
    }

    private static final class TrackingResourceAccess implements ExecutionResourceAccess {
        private final AtomicReference<AbortAction> abortAction = new AtomicReference<>();
        private final AtomicBoolean abortInvoked = new AtomicBoolean();
        private final AtomicBoolean leaseReleased = new AtomicBoolean();
        private final AtomicBoolean operationClosed = new AtomicBoolean();
        private final AtomicInteger closeSequence = new AtomicInteger();
        private final AtomicInteger leaseCloseOrder = new AtomicInteger();
        private final AtomicInteger operationCloseOrder = new AtomicInteger();

        @Override
        public org.webharvest.runtime.session.ExecutionOwner getOwner() {
            return null;
        }

        @Override
        public ResourceHandle registerSessionResource(String resourceId,
                                                       AbortAction abortAction,
                                                       CloseAction closeAction) {
            throw new UnsupportedOperationException("Session resources are not used by this test");
        }

        @Override
        public OperationResource registerOperationResource(String resourceId,
                                                            AbortAction abort,
                                                            CloseAction close) {
            abortAction.set(() -> {
                abortInvoked.set(true);
                abort.abort();
            });
            return new OperationResource() {
                @Override public String getResourceId() { return resourceId; }
                @Override public Lease acquire() {
                    return () -> {
                        leaseReleased.set(true);
                        leaseCloseOrder.set(closeSequence.incrementAndGet());
                    };
                }
                @Override public ResourceState getState() { return ResourceState.OPEN; }
                @Override public int getActiveLeaseCount() { return leaseReleased.get() ? 0 : 1; }
                @Override public Throwable getAbortFailure() { return null; }
                @Override public Throwable getCloseFailure() { return null; }
                @Override public void close() {
                    operationClosed.set(true);
                    operationCloseOrder.set(closeSequence.incrementAndGet());
                }
            };
        }

        private void abortActiveRequest() throws Exception {
            AbortAction action = abortAction.get();
            if (action == null) {
                throw new IllegalStateException("Request has not registered its abort action");
            }
            action.abort();
        }
    }
}
