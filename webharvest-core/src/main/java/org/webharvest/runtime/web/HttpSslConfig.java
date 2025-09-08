/*  Copyright (c) 2006-2007, Vladimir Nikic
    All rights reserved.

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

    You can contact Vladimir Nikic by sending e-mail to
    nikic_vladimir@yahoo.com. Please include the word "Web-Harvest" in the
    subject line.
*/
package org.webharvest.runtime.web;

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

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Configuration class for HTTP SSL/TLS settings.
 * Provides comprehensive SSL configuration including support for self-signed certificates,
 * custom keystores, truststores, and various SSL protocols.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public class HttpSslConfig {

    private static final Logger LOG = LoggerFactory.getLogger(HttpSslConfig.class);

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

    private SslMode sslMode = SslMode.STRICT;
    private String keystorePath;
    private String keystorePassword;
    private String keystoreType = "JKS";
    private String truststorePath;
    private String truststorePassword;
    private String truststoreType = "JKS";
    private String sslProtocol = "TLS";
    private boolean verifyHostname = true;
    private boolean allowSelfSigned = false;
    private boolean allowAllHostnames = false;

    /**
     * Default constructor with strict SSL mode
     */
    public HttpSslConfig() {
        this(SslMode.STRICT);
    }

    /**
     * Constructor with specified SSL mode
     * 
     * @param sslMode SSL configuration mode
     */
    public HttpSslConfig(SslMode sslMode) {
        this.sslMode = sslMode;
        if (sslMode == SslMode.EASY) {
            this.allowSelfSigned = true;
            this.verifyHostname = false;
        }
    }

    /**
     * Creates SSL context based on current configuration
     * 
     * @return SSLContext configured according to current settings
     * @throws Exception if SSL context creation fails
     */
    public SSLContext createSSLContext() throws Exception {
        switch (sslMode) {
            case STRICT:
                return createStrictSSLContext();
            case EASY:
                return createEasySSLContext();
            case CUSTOM:
                return createCustomSSLContext();
            case DISABLED:
                return null;
            default:
                throw new IllegalArgumentException("Unknown SSL mode: " + sslMode);
        }
    }

    /**
     * Creates protocol socket factory based on current configuration
     * 
     * @return ProtocolSocketFactory for HTTP client
     * @throws Exception if factory creation fails
     */
    public ProtocolSocketFactory createProtocolSocketFactory() throws Exception {
        switch (sslMode) {
            case STRICT:
                return new org.apache.commons.httpclient.contrib.ssl.StrictSSLProtocolSocketFactory(verifyHostname);
            case EASY:
                return new org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory();
            case CUSTOM:
                return new org.apache.commons.httpclient.contrib.ssl.AuthSSLProtocolSocketFactory(
                    keystorePath != null ? new java.net.URL("file://" + keystorePath) : null,
                    keystorePassword,
                    truststorePath != null ? new java.net.URL("file://" + truststorePath) : null,
                    truststorePassword);
            case DISABLED:
                return null;
            default:
                throw new IllegalArgumentException("Unknown SSL mode: " + sslMode);
        }
    }

    /**
     * Registers HTTPS protocol with current SSL configuration
     * 
     * @param port HTTPS port (default 443)
     */
    public void registerHttpsProtocol(int port) {
        try {
            ProtocolSocketFactory factory = createProtocolSocketFactory();
            if (factory != null) {
                Protocol.registerProtocol("https", new Protocol("https", factory, port));
                LOG.info("Registered HTTPS protocol with SSL mode: {}", sslMode);
            }
        } catch (Exception e) {
            LOG.error("Failed to register HTTPS protocol", e);
            throw new RuntimeException("Failed to register HTTPS protocol", e);
        }
    }

    /**
     * Registers HTTPS protocol with default port 443
     */
    public void registerHttpsProtocol() {
        registerHttpsProtocol(443);
    }

    private SSLContext createStrictSSLContext() throws Exception {
        SSLContext context = SSLContext.getInstance(sslProtocol);
        context.init(null, null, null);
        return context;
    }

    private SSLContext createEasySSLContext() throws Exception {
        SSLContext context = SSLContext.getInstance(sslProtocol);
        context.init(null, new TrustManager[]{new EasyX509TrustManager(null)}, null);
        return context;
    }

    private SSLContext createCustomSSLContext() throws Exception {
        KeyManager[] keyManagers = createKeyManagers();
        TrustManager[] trustManagers = createTrustManagers();
        
        SSLContext context = SSLContext.getInstance(sslProtocol);
        context.init(keyManagers, trustManagers, null);
        return context;
    }

    private KeyManager[] createKeyManagers() throws Exception {
        if (StringUtils.isBlank(keystorePath)) {
            return null;
        }

        KeyStore keystore = loadKeyStore(keystorePath, keystorePassword, keystoreType);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keystore, keystorePassword != null ? keystorePassword.toCharArray() : null);
        return kmf.getKeyManagers();
    }

    private TrustManager[] createTrustManagers() throws Exception {
        if (StringUtils.isBlank(truststorePath)) {
            return new TrustManager[]{new EasyX509TrustManager(null)};
        }

        KeyStore truststore = loadKeyStore(truststorePath, truststorePassword, truststoreType);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(truststore);
        return tmf.getTrustManagers();
    }

    private KeyStore loadKeyStore(String path, String password, String type) throws Exception {
        KeyStore keystore = KeyStore.getInstance(type);
        InputStream is = null;
        
        try {
            if (path.startsWith("classpath:")) {
                String resourcePath = path.substring("classpath:".length());
                is = getClass().getClassLoader().getResourceAsStream(resourcePath);
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

    // Getters and setters
    public SslMode getSslMode() {
        return sslMode;
    }

    public void setSslMode(SslMode sslMode) {
        this.sslMode = sslMode;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public String getTruststoreType() {
        return truststoreType;
    }

    public void setTruststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public boolean isVerifyHostname() {
        return verifyHostname;
    }

    public void setVerifyHostname(boolean verifyHostname) {
        this.verifyHostname = verifyHostname;
    }

    public boolean isAllowSelfSigned() {
        return allowSelfSigned;
    }

    public void setAllowSelfSigned(boolean allowSelfSigned) {
        this.allowSelfSigned = allowSelfSigned;
    }

    public boolean isAllowAllHostnames() {
        return allowAllHostnames;
    }

    public void setAllowAllHostnames(boolean allowAllHostnames) {
        this.allowAllHostnames = allowAllHostnames;
    }

    /**
     * Builder pattern for HttpSslConfig
     */
    public static class Builder {
        private HttpSslConfig config;

        public Builder() {
            this.config = new HttpSslConfig();
        }

        public Builder(SslMode sslMode) {
            this.config = new HttpSslConfig(sslMode);
        }

        public Builder setSslMode(SslMode sslMode) {
            config.setSslMode(sslMode);
            return this;
        }

        public Builder setKeystore(String path, String password) {
            config.setKeystorePath(path);
            config.setKeystorePassword(password);
            return this;
        }

        public Builder setKeystore(String path, String password, String type) {
            config.setKeystorePath(path);
            config.setKeystorePassword(password);
            config.setKeystoreType(type);
            return this;
        }

        public Builder setTruststore(String path, String password) {
            config.setTruststorePath(path);
            config.setTruststorePassword(password);
            return this;
        }

        public Builder setTruststore(String path, String password, String type) {
            config.setTruststorePath(path);
            config.setTruststorePassword(password);
            config.setTruststoreType(type);
            return this;
        }

        public Builder setSslProtocol(String protocol) {
            config.setSslProtocol(protocol);
            return this;
        }

        public Builder setVerifyHostname(boolean verify) {
            config.setVerifyHostname(verify);
            return this;
        }

        public Builder setAllowSelfSigned(boolean allow) {
            config.setAllowSelfSigned(allow);
            return this;
        }

        public Builder setAllowAllHostnames(boolean allow) {
            config.setAllowAllHostnames(allow);
            return this;
        }

        public HttpSslConfig build() {
            return config;
        }
    }
}
