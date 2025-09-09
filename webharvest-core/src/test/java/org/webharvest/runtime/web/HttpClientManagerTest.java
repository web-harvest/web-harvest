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

import static org.testng.AssertJUnit.assertEquals;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.web.HttpClientManager.ProxySettings;

public class HttpClientManagerTest {

    private static final String CHARSET = "utf-8";

    private HttpClientManager manager;

    @BeforeMethod
    public void setUp() {
        this.manager = new HttpClientManager(ProxySettings.NO_PROXY_SET);
    }

    @AfterMethod
    public void tearDown() {
        this.manager = null;
    }

    @Test
    public void createGetMethodNoHttpParams() throws URIException {
        final GetMethod get = manager.createGetMethod("http://sourceforge.net/",
                new HashMap<String, HttpParamInfo>(), CHARSET, false);

        assertEquals("Unexpected URI created",
                "http://sourceforge.net/", get.getURI().getURI());
    }

    @Test
    public void createGetMethodWithHttpParams() throws URIException {
        final Map<String, HttpParamInfo> params =
            new LinkedHashMap<String, HttpParamInfo>();
        params.put("param1", new HttpParamInfo("param1", false, null, null,
                new NodeVariable("param1Value")));
        params.put("param2", new HttpParamInfo("param2", false, null, null,
                new NodeVariable("param2Value")));

        final GetMethod get = manager.createGetMethod("http://sourceforge.net/",
                params, CHARSET, false);

        assertEquals("Unexpected URI created",
                "http://sourceforge.net/?param1=param1Value&param2=param2Value",
                get.getURI().getURI());
    }
}
