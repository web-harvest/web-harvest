package org.webharvest.plugin.ftp;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates FTP clients with explicit transport bounds and safe PASV handling.
 *
 * <p>Timeout defaults are intentionally finite: 10 seconds to connect and
 * 30 seconds for control-channel reads and data-channel reads. They bound
 * otherwise unbounded legacy operations; deployments with slower servers
 * may need a later per-plugin configuration surface.</p>
 */
final class FtpClientFactory {

    static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    static final int CONTROL_TIMEOUT_MILLIS = 30_000;
    static final int DATA_TIMEOUT_MILLIS = 30_000;

    private FtpClientFactory() {
    }

    static FTPClient create() {
        ManagedFtpClient client = new ManagedFtpClient();
        client.setIpAddressFromPasvResponse(false);
        client.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        client.setDefaultTimeout(CONTROL_TIMEOUT_MILLIS);
        client.setDataTimeout(Duration.ofMillis(DATA_TIMEOUT_MILLIS));
        return client;
    }

    /**
     * Commons Net keeps the active data socket local to its blocking transfer
     * method. Closing only the control socket is not sufficient to guarantee
     * that a blocked data read/write is released, so retain every opened data
     * socket until this one-operation client is disconnected.
     */
    static final class ManagedFtpClient extends FTPClient {

        private final Set<Socket> activeDataSockets = ConcurrentHashMap.newKeySet();

        @Override
        protected Socket _openDataConnection_(String command, String argument) throws IOException {
            Socket socket = super._openDataConnection_(command, argument);
            if (socket != null) {
                trackDataSocket(socket);
            }
            return socket;
        }

        void trackDataSocket(Socket socket) {
            if (socket != null) {
                activeDataSockets.add(socket);
            }
        }

        @Override
        public void disconnect() throws IOException {
            IOException failure = null;
            for (Socket socket : activeDataSockets) {
                try {
                    socket.close();
                } catch (IOException closeFailure) {
                    if (failure == null) {
                        failure = closeFailure;
                    } else if (failure != closeFailure) {
                        failure.addSuppressed(closeFailure);
                    }
                }
            }
            activeDataSockets.clear();

            try {
                super.disconnect();
            } catch (IOException disconnectFailure) {
                if (failure != null && failure != disconnectFailure) {
                    disconnectFailure.addSuppressed(failure);
                }
                throw disconnectFailure;
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
