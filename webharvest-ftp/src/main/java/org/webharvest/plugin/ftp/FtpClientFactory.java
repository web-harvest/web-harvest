package org.webharvest.plugin.ftp;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.resource.ExecutionResourceAccess;

/**
 * Creates FTP clients with safe PASV handling and execution-owned lifecycle.
 *
 * <p>Never trust an address advertised by an FTP server in its PASV response.
 * Commons Net 3.9.0+ defaults to this policy, but setting it explicitly also
 * prevents a legacy system property from re-enabling the unsafe behavior.</p>
 *
 * <p>Managed clients are registered before connect so execution termination can
 * close the control socket during login or transfer. Connect is bounded because
 * no FTPClient exists yet that can reliably cancel a socket connection attempt.</p>
 */
final class FtpClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(FtpClientFactory.class);
    private static final int CONNECT_TIMEOUT_MILLIS = 30_000;

    private FtpClientFactory() {
    }

    static FTPClient create() {
        FTPClient client = new FTPClient();
        client.setIpAddressFromPasvResponse(false);
        client.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        return client;
    }

    static ManagedClient open(DynamicScopeContext context) {
        FTPClient client = create();
        ExecutionResourceAccess access = context.getResourceAccess();
        if (access == null) {
            return new ManagedClient(client, null, null);
        }

        ExecutionResourceAccess.OperationResource resource = null;
        ExecutionResourceAccess.Lease lease = null;
        try {
            resource = access.registerOperationResource(
                "ftp/client/" + UUID.randomUUID(),
                () -> abort(client),
                () -> close(client));
            lease = resource.acquire();
            return new ManagedClient(client, resource, lease);
        } catch (RuntimeException | Error failure) {
            if (lease != null) {
                try {
                    lease.close();
                } catch (RuntimeException | Error cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            if (resource != null) {
                try {
                    resource.close();
                } catch (RuntimeException | Error cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            } else {
                try {
                    close(client);
                } catch (IOException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }

    private static void abort(FTPClient client) throws IOException {
        // Do not issue QUIT during abort: closing the control socket is the
        // best-effort way to release a blocked FTP command/data transfer.
        client.disconnect();
    }

    private static void close(FTPClient client) throws IOException {
        IOException failure = null;
        if (client.isConnected()) {
            try {
                client.logout();
            } catch (IOException logoutFailure) {
                failure = logoutFailure;
            }
        }
        try {
            // Always release the socket, including a failed/partial connect.
            client.disconnect();
        } catch (IOException disconnectFailure) {
            if (failure == null) {
                failure = disconnectFailure;
            } else {
                failure.addSuppressed(disconnectFailure);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static void closeQuietly(FTPClient client) {
        try {
            close(client);
        } catch (IOException failure) {
            LOGGER.warn("Failed to close FTP connection", failure);
        }
    }

    static final class ManagedClient implements AutoCloseable {
        private final FTPClient client;
        private final ExecutionResourceAccess.OperationResource resource;
        private final ExecutionResourceAccess.Lease lease;

        private ManagedClient(FTPClient client, ExecutionResourceAccess.OperationResource resource,
                              ExecutionResourceAccess.Lease lease) {
            this.client = client;
            this.resource = resource;
            this.lease = lease;
        }

        FTPClient getClient() {
            return client;
        }

        @Override
        public void close() {
            if (resource == null) {
                closeQuietly(client);
                return;
            }
            try {
                lease.close();
            } catch (RuntimeException failure) {
                LOGGER.warn("Failed to release FTP operation lease", failure);
            } finally {
                try {
                    resource.close();
                } catch (RuntimeException failure) {
                    LOGGER.warn("Failed to close FTP operation resource", failure);
                }
            }
        }
    }
}
