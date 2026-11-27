package org.webharvest.plugin.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
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
    public void abortActionDisconnectsAnEstablishedControlConnection() throws Exception {
        org.mockftpserver.fake.FakeFtpServer server = new org.mockftpserver.fake.FakeFtpServer();
        server.setServerControlPort(0);
        server.addUserAccount(new org.mockftpserver.fake.UserAccount("user", "secret", "/"));
        server.start();
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
            client.connect("127.0.0.1", server.getServerControlPort());
            assertTrue(client.login("user", "secret"));
            assertTrue(client.isConnected());

            ArgumentCaptor<ExecutionResourceAccess.AbortAction> abort =
                ArgumentCaptor.forClass(ExecutionResourceAccess.AbortAction.class);
            verify(access).registerOperationResource(anyString(), abort.capture(), any());
            abort.getValue().abort();

            assertTrue(!client.isConnected(), "Owner abort must disconnect the FTP control socket");
            resource.close();
        } finally {
            server.stop();
        }
    }
}
