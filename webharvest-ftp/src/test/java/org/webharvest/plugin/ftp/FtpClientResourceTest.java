package org.webharvest.plugin.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
import org.webharvest.runtime.resource.ExecutionResourceAccess;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/** Contract tests for the FTP operation resource adapter. */
public class FtpClientResourceTest {

    @Test
    public void managedClientIsRegisteredAndLeasePrecedesOperationClose() {
        DynamicScopeContext context = mock(DynamicScopeContext.class);
        ExecutionResourceAccess access = mock(ExecutionResourceAccess.class);
        ExecutionResourceAccess.OperationResource operation =
            mock(ExecutionResourceAccess.OperationResource.class);
        ExecutionResourceAccess.Lease lease = mock(ExecutionResourceAccess.Lease.class);
        when(context.getResourceAccess()).thenReturn(access);
        when(access.registerOperationResource(anyString(),
                any(ExecutionResourceAccess.AbortAction.class),
                any(ExecutionResourceAccess.CloseAction.class))).thenReturn(operation);
        when(operation.acquire()).thenReturn(lease);

        FtpClientResource resource = FtpClientResource.open(context);
        FTPClient client = resource.getClient();

        assertNotNull(client);
        assertEquals(client.getConnectTimeout(), FtpClientFactory.CONNECT_TIMEOUT_MILLIS);
        assertEquals(client.getDefaultTimeout(), FtpClientFactory.CONTROL_TIMEOUT_MILLIS);
        assertEquals(client.getDataTimeout().toMillis(), (long) FtpClientFactory.DATA_TIMEOUT_MILLIS);

        ArgumentCaptor<ExecutionResourceAccess.AbortAction> abort =
            ArgumentCaptor.forClass(ExecutionResourceAccess.AbortAction.class);
        ArgumentCaptor<ExecutionResourceAccess.CloseAction> close =
            ArgumentCaptor.forClass(ExecutionResourceAccess.CloseAction.class);
        verify(access).registerOperationResource(anyString(), abort.capture(), close.capture());
        assertNotNull(abort.getValue(), "Managed FTP operations require an abort action");
        assertNotNull(close.getValue(), "Managed FTP operations require a final close action");

        resource.close();

        InOrder order = inOrder(lease, operation);
        order.verify(lease).close();
        order.verify(operation).close();
    }

    @Test
    public void abortActionClosesBlockedDataSocketAndUnblocksTransfer() throws Exception {
        ServerSocket controlServer = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress());
        ServerSocket dataServer = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress());
        CountDownLatch dataAccepted = new CountDownLatch(1);
        CountDownLatch dataClosed = new CountDownLatch(1);
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        Thread serverThread = new Thread(() -> serveBlockedRetrieve(
            controlServer, dataServer, dataAccepted, dataClosed, serverFailure), "ftp-abort-test-server");
        serverThread.start();

        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            DynamicScopeContext context = mock(DynamicScopeContext.class);
            ExecutionResourceAccess access = mock(ExecutionResourceAccess.class);
            ExecutionResourceAccess.OperationResource operation =
                mock(ExecutionResourceAccess.OperationResource.class);
            ExecutionResourceAccess.Lease lease = mock(ExecutionResourceAccess.Lease.class);
            when(context.getResourceAccess()).thenReturn(access);
            when(access.registerOperationResource(anyString(),
                    any(ExecutionResourceAccess.AbortAction.class),
                    any(ExecutionResourceAccess.CloseAction.class))).thenReturn(operation);
            when(operation.acquire()).thenReturn(lease);

            FtpClientResource resource = FtpClientResource.open(context);
            FTPClient client = resource.getClient();
            client.connect("127.0.0.1", controlServer.getLocalPort());
            assertTrue(client.login("user", "secret"));
            assertTrue(client.setFileType(FTPClient.BINARY_FILE_TYPE));

            Future<Boolean> transfer = worker.submit(
                () -> client.retrieveFile("blocked.bin", new ByteArrayOutputStream()));
            assertTrue(dataAccepted.await(3, TimeUnit.SECONDS),
                "The server must observe the active data connection before abort");

            ArgumentCaptor<ExecutionResourceAccess.AbortAction> abort =
                ArgumentCaptor.forClass(ExecutionResourceAccess.AbortAction.class);
            verify(access).registerOperationResource(anyString(), abort.capture(), any());
            abort.getValue().abort();

            try {
                transfer.get(3, TimeUnit.SECONDS);
                throw new AssertionError("Aborted FTP transfer must not report success");
            } catch (ExecutionException expected) {
                // Closing the tracked data and control sockets terminates the blocked read.
            }
            assertTrue(dataClosed.await(3, TimeUnit.SECONDS),
                "Owner abort must close the data socket, not only the FTP control socket");
            assertTrue(!client.isConnected(), "Owner abort must disconnect the control socket");
            resource.close();
        } finally {
            worker.shutdownNow();
            controlServer.close();
            dataServer.close();
            serverThread.join(3000);
        }
        if (serverFailure.get() != null && !(serverFailure.get() instanceof java.net.SocketException)) {
            throw new AssertionError("FTP test server failed", serverFailure.get());
        }
    }

    private static void serveBlockedRetrieve(ServerSocket controlServer, ServerSocket dataServer,
                                             CountDownLatch dataAccepted, CountDownLatch dataClosed,
                                             AtomicReference<Throwable> serverFailure) {
        try (Socket control = controlServer.accept();
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                 control.getInputStream(), StandardCharsets.US_ASCII));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                 control.getOutputStream(), StandardCharsets.US_ASCII))) {
            reply(writer, "220 test FTP ready");
            String command;
            while ((command = reader.readLine()) != null) {
                String verb = command.split(" ", 2)[0];
                if ("USER".equals(verb)) {
                    reply(writer, "331 password required");
                } else if ("PASS".equals(verb)) {
                    reply(writer, "230 logged in");
                } else if ("TYPE".equals(verb)) {
                    reply(writer, "200 binary type set");
                } else if ("PASV".equals(verb)) {
                    int port = dataServer.getLocalPort();
                    reply(writer, "227 Entering Passive Mode (127,0,0,1,"
                        + (port / 256) + "," + (port % 256) + ")");
                } else if ("RETR".equals(verb)) {
                    reply(writer, "150 opening data connection");
                    try (Socket data = dataServer.accept()) {
                        dataAccepted.countDown();
                        while (data.getInputStream().read() != -1) {
                            // Deliberately keep the data channel open until owner abort closes it.
                        }
                    }
                    dataClosed.countDown();
                    reply(writer, "226 transfer complete");
                } else if ("QUIT".equals(verb)) {
                    reply(writer, "221 goodbye");
                    return;
                } else {
                    reply(writer, "200 command accepted");
                }
            }
        } catch (Throwable failure) {
            if (!controlServer.isClosed()) {
                serverFailure.compareAndSet(null, failure);
            }
        }
    }

    private static void reply(BufferedWriter writer, String line) throws java.io.IOException {
        writer.write(line);
        writer.write("\r\n");
        writer.flush();
    }}
