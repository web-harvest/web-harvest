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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating SSL contexts with various security configurations.
 * Provides comprehensive SSL configuration including support for self-signed certificates,
 * custom keystores, truststores, and various SSL protocols.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class WebHarvestSSLContextFactory {

    private static final Logger LOG = LoggerFactory.getLogger(WebHarvestSSLContextFactory.class);

    /**
     * SSL configuration modes
     */
    public enum SslMode {
        /** Strict SSL validation - only trusted certificates */
        STRICT,
        /** Easy SSL - accepts self-signed certificates */
        EASY,
        /** Custom SSL - uses provided keystore/truststore */
        CUSTOM,
        /** Disabled SSL - no SSL/TLS encryption */
        DISABLED
    }

    /**
     * Creates SSL context based on configuration
     * 
     * @param sslMode SSL mode
     * @param keystorePath Path to keystore (optional)
     * @param keystorePassword Keystore password (optional)
     * @param keystoreType Keystore type (default: JKS)
     * @param truststorePath Path to truststore (optional)
     * @param truststorePassword Truststore password (optional)
     * @param truststoreType Truststore type (default: JKS)
     * @param sslProtocol SSL protocol (default: TLS)
     * @return SSLContext configured according to settings
     * @throws Exception if SSL context creation fails
     */
    public static SSLContext createSSLContext(SslMode sslMode, 
                                            String keystorePath, String keystorePassword, String keystoreType,
                                            String truststorePath, String truststorePassword, String truststoreType,
                                            String sslProtocol) throws Exception {
        
        if (sslMode == SslMode.DISABLED) {
            return null;
        }

        if (StringUtils.isBlank(sslProtocol)) {
            sslProtocol = "TLS";
        }
        if (StringUtils.isBlank(keystoreType)) {
            keystoreType = "JKS";
        }
        if (StringUtils.isBlank(truststoreType)) {
            truststoreType = "JKS";
        }

        SSLContext context = SSLContext.getInstance(sslProtocol);
        
        switch (sslMode) {
            case STRICT:
                context.init(null, null, null);
                break;
            case EASY:
                context.init(null, new TrustManager[]{new WebHarvestTrustManager(true)}, null);
                break;
            case CUSTOM:
                KeyManager[] keyManagers = createKeyManagers(keystorePath, keystorePassword, keystoreType);
                TrustManager[] trustManagers = createTrustManagers(truststorePath, truststorePassword, truststoreType);
                context.init(keyManagers, trustManagers, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown SSL mode: " + sslMode);
        }
        
        return context;
    }

    /**
     * Creates SSL context with easy mode (accepts self-signed certificates)
     * 
     * @return SSLContext in easy mode
     * @throws Exception if SSL context creation fails
     */
    public static SSLContext createEasySSLContext() throws Exception {
        return createSSLContext(SslMode.EASY, null, null, null, null, null, null, "TLS");
    }

    /**
     * Creates SSL context with strict mode (only trusted certificates)
     * 
     * @return SSLContext in strict mode
     * @throws Exception if SSL context creation fails
     */
    public static SSLContext createStrictSSLContext() throws Exception {
        return createSSLContext(SslMode.STRICT, null, null, null, null, null, null, "TLS");
    }

    private static KeyManager[] createKeyManagers(String keystorePath, String keystorePassword, String keystoreType) throws Exception {
        if (StringUtils.isBlank(keystorePath)) {
            return null;
        }

        KeyStore keystore = loadKeyStore(keystorePath, keystorePassword, keystoreType);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keystore, keystorePassword != null ? keystorePassword.toCharArray() : null);
        return kmf.getKeyManagers();
    }

    private static TrustManager[] createTrustManagers(String truststorePath, String truststorePassword, String truststoreType) throws Exception {
        if (StringUtils.isBlank(truststorePath)) {
            return new TrustManager[]{new WebHarvestTrustManager(true)};
        }

        KeyStore truststore = loadKeyStore(truststorePath, truststorePassword, truststoreType);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(truststore);
        return tmf.getTrustManagers();
    }

    private static KeyStore loadKeyStore(String path, String password, String type) throws Exception {
        KeyStore keystore = KeyStore.getInstance(type);
        InputStream is = null;
        
        try {
            if (path.startsWith("classpath:")) {
                String resourcePath = path.substring("classpath:".length());
                is = WebHarvestSSLContextFactory.class.getClassLoader().getResourceAsStream(resourcePath);
                if (is == null) {
                    throw new IOException("Keystore resource not found: " + resourcePath);
                }
            } else if (path.startsWith("file:")) {
                is = new FileInputStream(new File(new URL(path).getFile()));
            } else {
                is = new FileInputStream(new File(path));
            }
            
            keystore.load(is, password != null ? password.toCharArray() : null);
            return keystore;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOG.warn("Failed to close keystore input stream", e);
                }
            }
        }
    }

    /**
     * Custom TrustManager that can accept self-signed certificates
     * 
     * @author Robert Bala
     * @since 2.1.0
     */
    public static class WebHarvestTrustManager implements X509TrustManager {
        
        private final boolean acceptSelfSigned;
        
        public WebHarvestTrustManager(boolean acceptSelfSigned) {
            this.acceptSelfSigned = acceptSelfSigned;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (acceptSelfSigned) {
                // In easy mode, accept all client certificates
                return;
            }
            // In strict mode, use default validation
            throw new CertificateException("Client certificate validation not supported in strict mode");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (acceptSelfSigned) {
                // In easy mode, accept all server certificates including self-signed
                return;
            }
            // In strict mode, use default validation
            throw new CertificateException("Server certificate validation not supported in strict mode");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
