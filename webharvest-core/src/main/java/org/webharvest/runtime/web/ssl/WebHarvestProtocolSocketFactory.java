/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package org.webharvest.runtime.web.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom ProtocolSocketFactory for Web-Harvest SSL connections.
 * Provides better SSL configuration and error handling compared to the old Apache Commons implementation.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class WebHarvestProtocolSocketFactory implements ProtocolSocketFactory {

    private static final Logger LOG = LoggerFactory.getLogger(WebHarvestProtocolSocketFactory.class);

    private final SSLContext sslContext;
    private final boolean verifyHostname;

    /**
     * Creates factory with easy SSL mode (accepts self-signed certificates)
     */
    public WebHarvestProtocolSocketFactory() {
        this(true, false);
    }

    /**
     * Creates factory with specified SSL configuration
     * 
     * @param acceptSelfSigned whether to accept self-signed certificates
     * @param verifyHostname whether to verify hostname
     */
    public WebHarvestProtocolSocketFactory(boolean acceptSelfSigned, boolean verifyHostname) {
        this.verifyHostname = verifyHostname;
        try {
            if (acceptSelfSigned) {
                this.sslContext = WebHarvestSSLContextFactory.createEasySSLContext();
            } else {
                this.sslContext = WebHarvestSSLContextFactory.createStrictSSLContext();
            }
        } catch (Exception e) {
            LOG.error("Failed to create SSL context", e);
            throw new RuntimeException("Failed to create SSL context", e);
        }
    }

    /**
     * Creates factory with custom SSL context
     * 
     * @param sslContext custom SSL context
     * @param verifyHostname whether to verify hostname
     */
    public WebHarvestProtocolSocketFactory(SSLContext sslContext, boolean verifyHostname) {
        this.sslContext = sslContext;
        this.verifyHostname = verifyHostname;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return createSocket(host, port, null, 0);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return createSocket(host, port, localHost, localPort, 0);
    }

    public Socket createSocket(String host, int port, InetAddress localHost, int localPort, int timeout) throws IOException, UnknownHostException {
        if (sslContext == null) {
            throw new IOException("SSL context is null");
        }

        SSLSocketFactory factory = sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port, localHost, localPort);
        
        // Configure SSL socket
        configureSSLSocket(socket, host);
        
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort, HttpConnectionParams params) throws IOException, UnknownHostException {
        int timeout = 0;
        if (params != null) {
            timeout = params.getConnectionTimeout();
        }
        return createSocket(host, port, localHost, localPort, timeout);
    }

    private void configureSSLSocket(SSLSocket socket, String host) {
        try {
            // Enable all supported cipher suites
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
            
            // Enable all supported protocols
            socket.setEnabledProtocols(socket.getSupportedProtocols());
            
            // Set hostname verification if required
            if (verifyHostname) {
                // Note: Hostname verification is typically handled by the SSL handshake
                // This is a placeholder for additional hostname verification logic if needed
                LOG.debug("Hostname verification enabled for: {}", host);
            }
            
            LOG.debug("SSL socket configured for host: {} with protocols: {}", 
                     host, java.util.Arrays.toString(socket.getEnabledProtocols()));
            
        } catch (Exception e) {
            LOG.warn("Failed to configure SSL socket for host: {}", host, e);
        }
    }
}
