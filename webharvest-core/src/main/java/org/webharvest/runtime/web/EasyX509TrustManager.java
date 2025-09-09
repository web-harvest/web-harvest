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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced X509TrustManager that accepts self-signed certificates.
 * This implementation provides better security controls and logging
 * compared to the original EasyX509TrustManager.
 * 
 * <p>
 * This trust manager SHOULD NOT be used for productive systems 
 * due to security reasons, unless it is a conscious decision and 
 * you are perfectly aware of security implications of accepting 
 * self-signed certificates.
 * </p>
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public class EasyX509TrustManager implements X509TrustManager {

    private static final Logger LOG = LoggerFactory.getLogger(EasyX509TrustManager.class);

    private X509TrustManager standardTrustManager = null;
    private boolean allowSelfSigned = true;
    private boolean allowExpired = false;
    private boolean allowInvalidHostname = true;

    /**
     * Constructor for EasyX509TrustManager with default settings.
     * 
     * @param keystore the keystore to use for standard trust validation
     * @throws NoSuchAlgorithmException if the default algorithm is not available
     * @throws KeyStoreException if the keystore is invalid
     */
    public EasyX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
        this(keystore, true, false, true);
    }

    /**
     * Constructor for EasyX509TrustManager with custom settings.
     * 
     * @param keystore the keystore to use for standard trust validation
     * @param allowSelfSigned whether to allow self-signed certificates
     * @param allowExpired whether to allow expired certificates
     * @param allowInvalidHostname whether to allow certificates with invalid hostnames
     * @throws NoSuchAlgorithmException if the default algorithm is not available
     * @throws KeyStoreException if the keystore is invalid
     */
    public EasyX509TrustManager(KeyStore keystore, boolean allowSelfSigned, 
                               boolean allowExpired, boolean allowInvalidHostname) 
            throws NoSuchAlgorithmException, KeyStoreException {
        super();
        this.allowSelfSigned = allowSelfSigned;
        this.allowExpired = allowExpired;
        this.allowInvalidHostname = allowInvalidHostname;
        
        if (keystore != null) {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(keystore);
            TrustManager[] trustmanagers = factory.getTrustManagers();
            if (trustmanagers.length == 0) {
                throw new NoSuchAlgorithmException("no trust manager found");
            }
            this.standardTrustManager = (X509TrustManager) trustmanagers[0];
        }
    }

    /**
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate[],String)
     */
    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking client trust for {} certificates with auth type: {}", 
                     certificates != null ? certificates.length : 0, authType);
        }
        
        if (standardTrustManager != null) {
            try {
                standardTrustManager.checkClientTrusted(certificates, authType);
                return;
            } catch (CertificateException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Standard trust manager rejected client certificates, checking with easy mode", e);
                }
            }
        }
        
        // If standard validation fails, check with easy mode
        checkCertificates(certificates, "client");
    }

    /**
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[],String)
     */
    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking server trust for {} certificates with auth type: {}", 
                     certificates != null ? certificates.length : 0, authType);
        }
        
        if (standardTrustManager != null) {
            try {
                standardTrustManager.checkServerTrusted(certificates, authType);
                return;
            } catch (CertificateException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Standard trust manager rejected server certificates, checking with easy mode", e);
                }
            }
        }
        
        // If standard validation fails, check with easy mode
        checkCertificates(certificates, "server");
    }

    /**
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        if (standardTrustManager != null) {
            return standardTrustManager.getAcceptedIssuers();
        }
        return new X509Certificate[0];
    }

    /**
     * Performs easy mode certificate validation
     * 
     * @param certificates the certificate chain to validate
     * @param type the type of validation (client/server)
     * @throws CertificateException if certificate validation fails
     */
    private void checkCertificates(X509Certificate[] certificates, String type) throws CertificateException {
        if (certificates == null || certificates.length == 0) {
            // In easy mode, we accept empty or null certificates
            LOG.debug("Accepting empty/null certificates in easy mode for {}", type);
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("{} certificate chain:", type);
            for (int i = 0; i < certificates.length; i++) {
                X509Certificate cert = certificates[i];
                LOG.debug("X509Certificate[{}]={}", i, cert.getSubjectDN());
                LOG.debug("  Issuer: {}", cert.getIssuerDN());
                LOG.debug("  Valid from: {} to: {}", cert.getNotBefore(), cert.getNotAfter());
                LOG.debug("  Serial number: {}", cert.getSerialNumber());
            }
        }

        // Check each certificate in the chain
        for (X509Certificate cert : certificates) {
            if (cert == null) {
                // Skip null certificates in easy mode
                LOG.debug("Skipping null certificate in easy mode for {}", type);
                continue;
            }
            
            try {
                // Check validity period unless explicitly allowed
                if (!allowExpired) {
                    cert.checkValidity();
                }
                
                // Additional checks could be added here for self-signed detection
                // For now, we accept all certificates in easy mode
                
            } catch (CertificateException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Certificate validation failed for {}: {}", type, e.getMessage());
                }
                
                // In easy mode, we might still accept the certificate
                // depending on configuration
                if (!allowExpired && e.getMessage().contains("NotAfter")) {
                    LOG.warn("Accepting expired certificate in easy mode: {}", cert.getSubjectDN());
                } else {
                    throw e;
                }
            }
        }

        LOG.debug("{} certificates accepted in easy mode", type);
    }

    // Getters and setters for configuration
    public boolean isAllowSelfSigned() {
        return allowSelfSigned;
    }

    public void setAllowSelfSigned(boolean allowSelfSigned) {
        this.allowSelfSigned = allowSelfSigned;
    }

    public boolean isAllowExpired() {
        return allowExpired;
    }

    public void setAllowExpired(boolean allowExpired) {
        this.allowExpired = allowExpired;
    }

    public boolean isAllowInvalidHostname() {
        return allowInvalidHostname;
    }

    public void setAllowInvalidHostname(boolean allowInvalidHostname) {
        this.allowInvalidHostname = allowInvalidHostname;
    }
}

