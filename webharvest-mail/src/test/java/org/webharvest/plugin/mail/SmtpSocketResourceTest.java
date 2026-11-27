package org.webharvest.plugin.mail;

import java.net.Socket;
import java.net.SocketException;

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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/** Contract tests for execution-owned SMTP sockets. */
public class SmtpSocketResourceTest {

    @Test
    public void socketFactoryTracksAndClosesEveryCreatedSocket() throws Exception {
        TrackingSmtpSocketFactory factory = new TrackingSmtpSocketFactory();
        Socket first = factory.createSocket();
        Socket second = factory.createSocket();

        assertFalse(first.isClosed());
        assertFalse(second.isClosed());
        factory.closeAll();

        assertTrue(first.isClosed());
        assertTrue(second.isClosed());
        factory.closeAll();
    }

    @Test
    public void socketFactoryRejectsSocketCreationAfterAbort() throws Exception {
        TrackingSmtpSocketFactory factory = new TrackingSmtpSocketFactory();
        factory.closeAll();

        try {
            factory.createSocket();
            throw new AssertionError("A terminated SMTP operation must reject late socket creation");
        } catch (SocketException expected) {
            assertTrue(expected.getMessage().contains("already aborted"));
        }
    }

    @Test
    public void ownerAbortClosesSocketsAndLeasePrecedesFinalResourceClose() throws Exception {
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

        TrackingSmtpSocketFactory factory = new TrackingSmtpSocketFactory();
        Socket socket = factory.createSocket();
        SmtpSocketResource resource = SmtpSocketResource.open(context, factory);

        ArgumentCaptor<ExecutionResourceAccess.AbortAction> abort =
            ArgumentCaptor.forClass(ExecutionResourceAccess.AbortAction.class);
        ArgumentCaptor<ExecutionResourceAccess.CloseAction> close =
            ArgumentCaptor.forClass(ExecutionResourceAccess.CloseAction.class);
        verify(access).registerOperationResource(anyString(), abort.capture(), close.capture());
        assertNotNull(abort.getValue());
        assertNotNull(close.getValue());

        abort.getValue().abort();
        assertTrue(socket.isClosed());
        try {
            factory.createSocket();
            throw new AssertionError("Abort must close socket admission");
        } catch (SocketException expected) {
            assertTrue(expected.getMessage().contains("already aborted"));
        }

        resource.close();
        InOrder order = inOrder(lease, operation);
        order.verify(lease).close();
        order.verify(operation).close();
    }

    @Test
    public void legacyContextClosesSocketsWithoutRuntimeResourceAccess() throws Exception {
        TrackingSmtpSocketFactory factory = new TrackingSmtpSocketFactory();
        Socket socket = factory.createSocket();
        SmtpSocketResource resource = SmtpSocketResource.open(null, factory);

        resource.close();

        assertTrue(socket.isClosed());
    }
}
