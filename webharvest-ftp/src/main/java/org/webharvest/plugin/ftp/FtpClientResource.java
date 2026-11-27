/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.plugin.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.resource.ExecutionResourceAccess;

import java.io.IOException;
import java.util.UUID;

/**
 * Owns one FTP operation's client, cancellation boundary and resource lease.
 *
 * <p>Managed executions register the client before any network I/O. Owner
 * termination disconnects both control and active data sockets; final close
 * independently attempts logout and disconnect after the operation lease drains.
 * Legacy contexts without resource access retain local finally-based cleanup.</p>
 */
final class FtpClientResource implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FtpClientResource.class);

    private final FTPClient client;
    private final ExecutionResourceAccess.OperationResource operation;
    private final ExecutionResourceAccess.Lease lease;

    private FtpClientResource(FTPClient client,
                              ExecutionResourceAccess.OperationResource operation,
                              ExecutionResourceAccess.Lease lease) {
        this.client = client;
        this.operation = operation;
        this.lease = lease;
    }

    static FtpClientResource open(DynamicScopeContext context) {
        FTPClient client = FtpClientFactory.create();
        ExecutionResourceAccess access = context == null ? null : context.getResourceAccess();
        if (access == null) {
            return new FtpClientResource(client, null, null);
        }

        ExecutionResourceAccess.OperationResource operation = null;
        ExecutionResourceAccess.Lease lease = null;
        try {
            operation = access.registerOperationResource(
                "ftp/client/" + UUID.randomUUID(),
                () -> disconnect(client),
                () -> closeClient(client));
            lease = operation.acquire();
            return new FtpClientResource(client, operation, lease);
        } catch (RuntimeException | Error failure) {
            if (lease != null) {
                lease.close();
            }
            if (operation != null) {
                operation.close();
            } else {
                try {
                    closeClient(client);
                } catch (IOException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            throw failure;
        }
    }

    FTPClient getClient() {
        return client;
    }

    @Override
    public void close() {
        if (lease != null) {
            lease.close();
        }
        if (operation != null) {
            operation.close();
            return;
        }
        try {
            closeClient(client);
        } catch (IOException failure) {
            LOGGER.warn("Failed to close legacy FTP client", failure);
        }
    }

    private static void disconnect(FTPClient client) throws IOException {
        synchronized (client) {
            client.disconnect();
        }
    }

    static void closeClient(FTPClient client) throws IOException {
        synchronized (client) {
            IOException failure = null;
            if (client.isConnected()) {
                try {
                    client.logout();
                } catch (IOException logoutFailure) {
                    failure = logoutFailure;
                }
            }
            try {
                // Always attempt disconnect, even if logout failed.
                client.disconnect();
            } catch (IOException disconnectFailure) {
                if (failure == null) {
                    failure = disconnectFailure;
                } else if (failure != disconnectFailure) {
                    failure.addSuppressed(disconnectFailure);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
