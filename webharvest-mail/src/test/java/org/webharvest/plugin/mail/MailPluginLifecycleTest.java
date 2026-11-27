package org.webharvest.plugin.mail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.resource.ExecutionResourceAccess;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

/** End-to-end cancellation contract for SMTP DATA in progress. */
public class MailPluginLifecycleTest {

    @Test
    public void ownerAbortClosesSmtpSocketWhileAwaitingFinalDataReply() throws Exception {
        ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        CountDownLatch dataReceived = new CountDownLatch(1);
        CountDownLatch clientSocketClosed = new CountDownLatch(1);
        CountDownLatch resourceRegistered = new CountDownLatch(1);
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        AtomicReference<Socket> acceptedSocket = new AtomicReference<>();
        Thread serverThread = new Thread(() -> serveBlockedSmtp(
            serverSocket, dataReceived, clientSocketClosed, serverFailure, acceptedSocket),
            "smtp-abort-test-server");
        serverThread.start();

        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            DynamicScopeContext context = mock(DynamicScopeContext.class);
            ExecutionResourceAccess access = mock(ExecutionResourceAccess.class);
            ExecutionResourceAccess.OperationResource operation =
                mock(ExecutionResourceAccess.OperationResource.class);
            ExecutionResourceAccess.Lease lease = mock(ExecutionResourceAccess.Lease.class);
            when(context.getResourceAccess()).thenReturn(access);
            when(context.getCharset()).thenReturn("UTF-8");
            when(access.registerOperationResource(anyString(),
                    any(ExecutionResourceAccess.AbortAction.class),
                    any(ExecutionResourceAccess.CloseAction.class))).thenAnswer(invocation -> {
                resourceRegistered.countDown();
                return operation;
            });
            when(operation.acquire()).thenReturn(lease);

            MailPlugin plugin = new TestMailPlugin("lifecycle cancellation test body");
            DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "mail", "http://org.webharvest/schema/2.1/core", "mail");
            configuration.setProperty("smtp-host", "127.0.0.1");
            configuration.setProperty("smtp-port", Integer.toString(serverSocket.getLocalPort()));
            configuration.setProperty("from", "sender@example.com");
            configuration.setProperty("to", "recipient@example.com");
            configuration.setProperty("subject", "SMTP lifecycle test");
            plugin.initialize(configuration);

            Future<Variable> send = worker.submit(() -> plugin.execute(context));
            assertTrue(resourceRegistered.await(3, TimeUnit.SECONDS),
                "SMTP socket resource must be registered before send");
            assertTrue(dataReceived.await(5, TimeUnit.SECONDS),
                "SMTP server must receive DATA before owner abort");

            org.mockito.ArgumentCaptor<ExecutionResourceAccess.AbortAction> abort =
                org.mockito.ArgumentCaptor.forClass(ExecutionResourceAccess.AbortAction.class);
            org.mockito.ArgumentCaptor<ExecutionResourceAccess.CloseAction> close =
                org.mockito.ArgumentCaptor.forClass(ExecutionResourceAccess.CloseAction.class);
            verify(access).registerOperationResource(anyString(), abort.capture(), close.capture());

            abort.getValue().abort();

            try {
                send.get(5, TimeUnit.SECONDS);
                throw new AssertionError("Aborted SMTP send must not report delivery success");
            } catch (ExecutionException expected) {
                // JavaMail reports the broken SMTP socket; remote delivery remains unknown.
            }
            assertTrue(clientSocketClosed.await(3, TimeUnit.SECONDS),
                "Owner abort must close the actual SMTP socket");
            close.getValue().close();
            verify(lease).close();
            verify(operation).close();
        } finally {
            worker.shutdownNow();
            serverSocket.close();
            Socket accepted = acceptedSocket.get();
            if (accepted != null) {
                accepted.close();
            }
            serverThread.join(3000);
        }
        if (serverFailure.get() != null && !(serverFailure.get() instanceof java.net.SocketException)) {
            throw new AssertionError("SMTP test server failed", serverFailure.get());
        }
    }

    private static void serveBlockedSmtp(ServerSocket serverSocket, CountDownLatch dataReceived,
                                         CountDownLatch clientSocketClosed,
                                         AtomicReference<Throwable> serverFailure,
                                         AtomicReference<Socket> acceptedSocket) {
        try (Socket socket = serverSocket.accept()) {
            acceptedSocket.set(socket);
            serveSession(socket, dataReceived, clientSocketClosed);
        } catch (Throwable failure) {
            if (!serverSocket.isClosed()) {
                serverFailure.compareAndSet(null, failure);
            }
        }
    }

    private static void serveSession(Socket socket, CountDownLatch dataReceived,
                                     CountDownLatch clientSocketClosed) throws java.io.IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                 socket.getInputStream(), StandardCharsets.US_ASCII));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                 socket.getOutputStream(), StandardCharsets.US_ASCII))) {
            reply(writer, "220 test SMTP ready");
            String command;
            while ((command = reader.readLine()) != null) {
                String verb = command.split(" ", 2)[0].toUpperCase(java.util.Locale.ROOT);
                if ("EHLO".equals(verb)) {
                    reply(writer, "250-test.local");
                    reply(writer, "250 OK");
                } else if ("HELO".equals(verb) || "MAIL".equals(verb) || "RCPT".equals(verb)) {
                    reply(writer, "250 OK");
                } else if ("DATA".equals(verb)) {
                    reply(writer, "354 end with <CRLF>.<CRLF>");
                    String dataLine;
                    while ((dataLine = reader.readLine()) != null && !".".equals(dataLine)) {
                        // Consume the complete MIME payload but do not acknowledge it.
                    }
                    if (dataLine == null) {
                        throw new java.io.IOException("SMTP client closed before DATA terminator");
                    }
                    dataReceived.countDown();

                    // The client is blocked waiting for the final 250 reply.
                    // Owner abort must close this socket and release that read.
                    if (reader.readLine() == null) {
                        clientSocketClosed.countDown();
                    }
                    return;
                } else if ("QUIT".equals(verb)) {
                    reply(writer, "221 bye");
                    return;
                } else {
                    reply(writer, "250 OK");
                }
            }
            clientSocketClosed.countDown();
        }
    }

    private static void reply(BufferedWriter writer, String line) throws java.io.IOException {
        writer.write(line);
        writer.write("\r\n");
        writer.flush();
    }

    private static final class TestMailPlugin extends MailPlugin {
        private final String body;

        private TestMailPlugin(String body) {
            this.body = body;
        }

        @Override
        protected Variable executeBody(DynamicScopeContext context) {
            return new NodeVariable(body);
        }
    }
}
