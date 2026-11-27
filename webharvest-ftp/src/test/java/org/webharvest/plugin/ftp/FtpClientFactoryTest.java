package org.webharvest.plugin.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.resource.ExecutionResourceAccess;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class FtpClientFactoryTest {

    @Test
    public void factoryRejectsLegacyPasvAddressTrustOverrideAndBoundsConnect() {
        String property = FTPClient.FTP_IP_ADDRESS_FROM_PASV_RESPONSE;
        String previous = System.getProperty(property);
        try {
            System.setProperty(property, "true");

            FTPClient legacyClient = new FTPClient();
            assertTrue(legacyClient.isIpAddressFromPasvResponse(),
                    "Test must prove the legacy system property enables the unsafe behavior");

            FTPClient safeClient = FtpClientFactory.create();
            assertFalse(safeClient.isIpAddressFromPasvResponse(),
                    "WebHarvest FTP clients must ignore the server-advertised PASV address");
            assertEquals(safeClient.getConnectTimeout(), 30_000,
                    "FTP connection establishment must have a finite timeout");
        } finally {
            if (previous == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previous);
            }
        }
    }

    @Test
    public void managedClientRegistersDisconnectAbortAndFinalLogoutClose() throws Exception {
        FTPClient client = mock(FTPClient.class);
        when(client.isConnected()).thenReturn(true);
        DynamicScopeContext context = mock(DynamicScopeContext.class);
        ExecutionResourceAccess access = mock(ExecutionResourceAccess.class);
        ExecutionResourceAccess.OperationResource resource =
            mock(ExecutionResourceAccess.OperationResource.class);
        ExecutionResourceAccess.Lease lease = mock(ExecutionResourceAccess.Lease.class);
        when(context.getResourceAccess()).thenReturn(access);
        when(resource.acquire()).thenReturn(lease);

        ArgumentCaptor<ExecutionResourceAccess.AbortAction> abort =
            ArgumentCaptor.forClass(ExecutionResourceAccess.AbortAction.class);
        ArgumentCaptor<ExecutionResourceAccess.CloseAction> close =
            ArgumentCaptor.forClass(ExecutionResourceAccess.CloseAction.class);
        when(access.registerOperationResource(anyString(), abort.capture(), close.capture()))
            .thenReturn(resource);

        FtpClientFactory.ManagedClient managed = FtpClientFactory.manage(client, context);

        abort.getValue().abort();
        verify(client).disconnect();
        verify(client, never()).logout();

        close.getValue().close();
        verify(client).logout();
        verify(client, org.mockito.Mockito.times(2)).disconnect();

        managed.close();
        InOrder order = inOrder(lease, resource);
        order.verify(lease).close();
        order.verify(resource).close();
    }

    @Test
    public void legacyClientIsClosedDirectlyWhenNoExecutionResourceAccessExists() {
        FTPClient client = mock(FTPClient.class);
        when(client.isConnected()).thenReturn(true);
        DynamicScopeContext context = mock(DynamicScopeContext.class);
        when(context.getResourceAccess()).thenReturn(null);

        FtpClientFactory.ManagedClient managed = FtpClientFactory.manage(client, context);
        managed.close();

        verify(client).logout();
        verify(client).disconnect();
    }
}
