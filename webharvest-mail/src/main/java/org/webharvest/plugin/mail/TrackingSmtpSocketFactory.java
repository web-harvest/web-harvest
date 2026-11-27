/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.plugin.mail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.SocketFactory;

/**
 * Tracks the sockets created for one SMTP send operation.
 *
 * <p>JavaMail's static {@code Transport.send} hides the SMTPTransport instance
 * from the plugin. Its SocketFetcher does, however, accept a SocketFactory
 * instance in the session properties. This factory captures the underlying
 * sockets so the execution owner can close them without reflection or global
 * transport state.</p>
 */
final class TrackingSmtpSocketFactory extends SocketFactory {

    private final Object lock = new Object();
    private final Set<Socket> sockets = new HashSet<>();
    private boolean closed;

    @Override
    public Socket createSocket() throws IOException {
        return register(new Socket());
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return createAndConnect(new InetSocketAddress(host, port), null);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort)
            throws IOException {
        return createAndConnect(new InetSocketAddress(host, port),
            new InetSocketAddress(localAddress, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return createAndConnect(new InetSocketAddress(host, port), null);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        return createAndConnect(new InetSocketAddress(address, port),
            new InetSocketAddress(localAddress, localPort));
    }

    private Socket createAndConnect(InetSocketAddress remoteAddress,
                                     InetSocketAddress localAddress) throws IOException {
        Socket socket = register(new Socket());
        try {
            if (localAddress != null) {
                socket.bind(localAddress);
            }
            socket.connect(remoteAddress);
            return socket;
        } catch (IOException connectionFailure) {
            try {
                socket.close();
            } catch (IOException closeFailure) {
                connectionFailure.addSuppressed(closeFailure);
            }
            throw connectionFailure;
        }
    }

    private Socket register(Socket socket) throws IOException {
        synchronized (lock) {
            if (closed) {
                SocketException aborted = new SocketException("SMTP operation is already aborted");
                try {
                    socket.close();
                } catch (IOException closeFailure) {
                    aborted.addSuppressed(closeFailure);
                }
                throw aborted;
            }
            sockets.add(socket);
            return socket;
        }
    }

    /**
     * Permanently closes this operation's socket admission and all sockets
     * already created. Safe to call from abort and final-close callbacks.
     */
    void closeAll() throws IOException {
        List<Socket> toClose;
        synchronized (lock) {
            closed = true;
            toClose = new ArrayList<>(sockets);
            sockets.clear();
        }

        IOException failure = null;
        for (Socket socket : toClose) {
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
        if (failure != null) {
            throw failure;
        }
    }
}
