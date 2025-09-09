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

package org.webharvest.runtime.web;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.webharvest.runtime.web.HttpClientManager.ProxySettings;

public class ProxySettingsTest {

    private static final String PROXY_HOST = "myproxy.com";
    private static final int PROXY_PORT = 8081;
    private static final String PROXY_AUTH_USERNAME = "frenchbulldog";
    private static final String PROXY_AUTH_PASSWD = "ehS9A!a@nf;!";
    private static final String PROXY_AUTH_NTHOST = "windowsproxyhost";
    private static final String PROXY_AUTH_NTDOMAIN = "PROXY_DOMAIN";

    private HttpClient httpClient;

    @BeforeMethod
    public void setUp() {
        this.httpClient = new HttpClient();
    }

    @AfterMethod
    public void tearDown() {
        this.httpClient = null;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void builderDisallowsNullHost() {
        new ProxySettings.Builder(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void builderDisallowsEmptyHost() {
        new ProxySettings.Builder("");
    }

    @Test
    public void appliesHostSettings() {
        new ProxySettings.Builder(PROXY_HOST)
            .build()
            .apply(httpClient);

        assertEquals("Unexpected proxy host",
                PROXY_HOST, httpClient.getHostConfiguration().getProxyHost());
    }

    @Test
    public void appliesDefaultPortWhenOnlyHostSet() {
        new ProxySettings.Builder(PROXY_HOST)
            .build()
            .apply(httpClient);

        assertEquals("Unexpected proxy port",
                80, httpClient.getHostConfiguration().getProxyPort());
    }

    @Test
    public void appliesPortSettings() {
        new ProxySettings.Builder(PROXY_HOST)
            .setProxyPort(PROXY_PORT)
            .build()
            .apply(httpClient);

        assertEquals("Unexpected proxy port",
                PROXY_PORT, httpClient.getHostConfiguration().getProxyPort());
    }

    @Test
    public void appliesUsernameAndPasswdCredentials() {
        new ProxySettings.Builder(PROXY_HOST)
            .setProxyCredentialsUsername(PROXY_AUTH_USERNAME)
            .setProxyCredentialsPassword(PROXY_AUTH_PASSWD)
            .build()
            .apply(httpClient);

        final Credentials credentials = httpClient.getState()
            .getProxyCredentials(AuthScope.ANY);

        assertNotNull("Credentials not applied", credentials);
        assertTrue("Unexpected credentials type",
                credentials instanceof UsernamePasswordCredentials);
        assertEquals("Unexpected proxy username",
                PROXY_AUTH_USERNAME,
                ((UsernamePasswordCredentials) credentials).getUserName());
        assertEquals("Unexpected proxy password",
                PROXY_AUTH_PASSWD,
                ((UsernamePasswordCredentials) credentials).getPassword());
    }

    @Test
    public void appliesNTCredentials() {
        new ProxySettings.Builder(PROXY_HOST)
            .setProxyCredentialsUsername(PROXY_AUTH_USERNAME)
            .setProxyCredentialsPassword(PROXY_AUTH_PASSWD)
            .setProxyCredentialsNTHost(PROXY_AUTH_NTHOST)
            .setProxyCredentialsNTDomain(PROXY_AUTH_NTDOMAIN)
            .build()
            .apply(httpClient);

        final Credentials credentials = httpClient.getState()
            .getProxyCredentials(AuthScope.ANY);

        assertNotNull("Credentials not applied", credentials);
        assertTrue("Unexpected credentials type",
                credentials instanceof NTCredentials);
        assertEquals("Unexpected proxy username",
                PROXY_AUTH_USERNAME,
                ((NTCredentials) credentials).getUserName());
        assertEquals("Unexpected proxy password",
                PROXY_AUTH_PASSWD,
                ((NTCredentials) credentials).getPassword());
        assertEquals("Unexpected proxy NT host",
                PROXY_AUTH_NTHOST,
                ((NTCredentials) credentials).getHost());
        assertEquals("Unexpected proxy NT domain",
                PROXY_AUTH_NTDOMAIN,
                ((NTCredentials) credentials).getDomain());
    }

    @Test
    public void doesNotModifyHttpClientWhenNoProxySet() {
        ProxySettings.NO_PROXY_SET.apply(httpClient);
        assertNull("Expected proxy host not to be set",
                httpClient.getHostConfiguration().getProxyHost());
        assertNull("Expected proxy credentials not to be set",
                httpClient.getState().getProxyCredentials(AuthScope.ANY));
    }
}
