package org.webharvest.runtime.web;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for EasyX509TrustManager class
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public class EasyX509TrustManagerTest {

    private EasyX509TrustManager trustManager;

    @BeforeMethod
    public void setUp() throws Exception {
        trustManager = new EasyX509TrustManager(null);
    }

    @Test
    public void testDefaultConfiguration() {
        assertTrue(trustManager.isAllowSelfSigned());
        assertTrue(!trustManager.isAllowExpired());
        assertTrue(trustManager.isAllowInvalidHostname());
    }

    @Test
    public void testCustomConfiguration() throws Exception {
        EasyX509TrustManager customTrustManager = new EasyX509TrustManager(
                null, false, true, false);
        
        assertTrue(!customTrustManager.isAllowSelfSigned());
        assertTrue(customTrustManager.isAllowExpired());
        assertTrue(!customTrustManager.isAllowInvalidHostname());
    }

    @Test
    public void testSettersAndGetters() {
        trustManager.setAllowSelfSigned(false);
        trustManager.setAllowExpired(true);
        trustManager.setAllowInvalidHostname(false);

        assertTrue(!trustManager.isAllowSelfSigned());
        assertTrue(trustManager.isAllowExpired());
        assertTrue(!trustManager.isAllowInvalidHostname());
    }

    @Test
    public void testGetAcceptedIssuers() {
        X509Certificate[] issuers = trustManager.getAcceptedIssuers();
        
        assertNotNull(issuers);
        assertEquals(issuers.length, 0);
    }

    @Test
    public void testCheckClientTrustedWithNullCertificates() throws Exception {
        // This should not throw an exception in easy mode
        trustManager.checkClientTrusted(null, "RSA");
    }

    @Test
    public void testCheckClientTrustedWithEmptyCertificates() throws Exception {
        // This should not throw an exception in easy mode
        trustManager.checkClientTrusted(new X509Certificate[0], "RSA");
    }

    @Test
    public void testCheckServerTrustedWithNullCertificates() throws Exception {
        // This should not throw an exception in easy mode
        trustManager.checkServerTrusted(null, "RSA");
    }

    @Test
    public void testCheckServerTrustedWithEmptyCertificates() throws Exception {
        // This should not throw an exception in easy mode
        trustManager.checkServerTrusted(new X509Certificate[0], "RSA");
    }

    @Test
    public void testCheckClientTrustedWithValidCertificates() throws Exception {
        // Create a mock certificate array
        X509Certificate[] certificates = new X509Certificate[1];
        // Note: In a real test, you would create actual X509Certificate instances
        // For now, we test that the method doesn't throw an exception
        
        // This should not throw an exception in easy mode
        trustManager.checkClientTrusted(certificates, "RSA");
    }

    @Test
    public void testCheckServerTrustedWithValidCertificates() throws Exception {
        // Create a mock certificate array
        X509Certificate[] certificates = new X509Certificate[1];
        // Note: In a real test, you would create actual X509Certificate instances
        // For now, we test that the method doesn't throw an exception
        
        // This should not throw an exception in easy mode
        trustManager.checkServerTrusted(certificates, "RSA");
    }

    @Test
    public void testConstructorWithKeyStore() throws Exception {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(null, null);
        
        EasyX509TrustManager trustManagerWithKeystore = new EasyX509TrustManager(keystore);
        
        assertNotNull(trustManagerWithKeystore);
        assertTrue(trustManagerWithKeystore.isAllowSelfSigned());
        assertTrue(!trustManagerWithKeystore.isAllowExpired());
        assertTrue(trustManagerWithKeystore.isAllowInvalidHostname());
    }

    @Test
    public void testConstructorWithKeyStoreAndCustomSettings() throws Exception {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(null, null);
        
        EasyX509TrustManager trustManagerWithKeystore = new EasyX509TrustManager(
                keystore, false, true, false);
        
        assertNotNull(trustManagerWithKeystore);
        assertTrue(!trustManagerWithKeystore.isAllowSelfSigned());
        assertTrue(trustManagerWithKeystore.isAllowExpired());
        assertTrue(!trustManagerWithKeystore.isAllowInvalidHostname());
    }
}
