/*
 Copyright (c) 2006-2007, Vladimir Nikic
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

package org.webharvest.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CommonUtilTest {

    @Test
    public void testReadStringFromUrlUsesDeclaredCharsetAndUtf8Fallback() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/latin1", exchange -> {
            byte[] body = "café".getBytes(StandardCharsets.ISO_8859_1);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=ISO-8859-1");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/utf8", exchange -> {
            byte[] body = "Zażółć gęślą jaźń".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            Assert.assertEquals(
                    CommonUtil.readStringFromUrl(new URL(baseUrl + "/latin1")), "café");
            Assert.assertEquals(
                    CommonUtil.readStringFromUrl(new URL(baseUrl + "/utf8")), "Zażółć gęślą jaźń");
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testIsFullUrl() throws Exception {
        // HTTP and HTTPS
        Assert.assertTrue(CommonUtil.isFullUrl("http://somehost:8080/downloads/somefile.jar"));
        Assert.assertTrue(CommonUtil.isFullUrl("https://myusername@somehost/index.html"));
        // File
        Assert.assertTrue(CommonUtil.isFullUrl("file:///somedir/some%25file.txt"));
        Assert.assertTrue(CommonUtil.isFullUrl("file:/C:/Documents and Settings"));
        Assert.assertTrue(CommonUtil.isFullUrl("file://///somehost/someshare/afile.txt"));
        // JAR
        Assert.assertTrue(CommonUtil.isFullUrl("ftp://myusername:mypassword@somehost/pub/downloads/somefile.tgz"));
        // FTP
        Assert.assertTrue(CommonUtil.isFullUrl("jar:file://my/path/to/images.jar!/path/in/classpath/image.png"));
    }
}
