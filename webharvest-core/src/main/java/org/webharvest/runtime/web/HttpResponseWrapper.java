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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;
import org.webharvest.utils.KeyValuePair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class defines http server response.
 * 
 * <h3>Bug #48 Fix (v2.2.1): Use getResponseBodyAsStream</h3>
 * <p>
 * <strong>Issue:</strong> Used {@code getResponseBody()} which buffers entire response in memory,
 * causing WARNING for large responses and potential OutOfMemoryError.
 * </p>
 * <p>
 * <strong>Fix:</strong> Changed to use {@code getResponseBodyAsStream()} which returns a stream,
 * allowing efficient processing of large responses without buffering everything in memory.
 * </p>
 * 
 * @author Web-Harvest Team
 * @version 2.2.1
 */
public class HttpResponseWrapper {

    private String charset;
    private String mimeType;
    private KeyValuePair<String> headers[];
    private int statusCode;
    private String statusText;
    private HttpMethodBase httpMethod;

    /**
     * Constructor - defines response result based on specified HttpMethodBase instance.
     *
     * @param method Http method object
     */
    @SuppressWarnings({"unchecked"})
    public HttpResponseWrapper(HttpMethodBase method) {
        this.httpMethod = method;

        Header[] headerArray = method.getResponseHeaders();
        if (headerArray != null) {
            headers = new KeyValuePair[headerArray.length];
            for (int i = 0; i < headerArray.length; i++) {
                String currName = headerArray[i].getName();
                String currValue = headerArray[i].getValue();
                headers[i] = new KeyValuePair<String>(currName, currValue);
                if ("content-type".equalsIgnoreCase(currName)) {
                    int index = currValue.indexOf(';');
                    this.mimeType = index > 0 ? currValue.substring(0, index) : currValue;
                }
            }
        }

        this.charset = method.getResponseCharSet();
        this.statusCode = method.getStatusCode();
        this.statusText = method.getStatusText();

    }

    public long getContentLength() {
        return httpMethod.getResponseContentLength();
    }

    public String getCharset() {
        return this.charset;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    /**
     * @return byte array
     * @deprecated Left only for backward compatibility. Use {@link #readBodyAsArray()} or {@link #getBodyAsInputStream()}
     */
    @Deprecated
    public byte[] getBody() {
        return readBodyAsArray();
    }

    /**
     * Read response body as byte array.
     * 
     * <p><strong>Warning:</strong> This loads the entire response into memory.
     * For large responses, use {@link #getBodyAsInputStream()} instead.</p>
     * 
     * <p>Bug #48 Fix: Now uses getResponseBodyAsStream() internally to read data,
     * but still returns byte array for backward compatibility.</p>
     * 
     * @return Response body as byte array
     */
    public byte[] readBodyAsArray() {
        try {
            // Bug #48 Fix: Use stream instead of getResponseBody()
            InputStream stream = httpMethod.getResponseBodyAsStream();
            if (stream == null) {
                return new byte[0];
            }
            
            // Read stream into byte array
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int bytesRead;
            while ((bytesRead = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get response body as InputStream for efficient large file processing.
     * 
     * <p>Bug #48 Fix: Returns the actual response stream instead of buffering
     * entire response in memory. This prevents OutOfMemoryError for large responses.</p>
     * 
     * <p><strong>Important:</strong> The stream should be consumed only once.
     * For multiple reads, use {@link #readBodyAsArray()} instead.</p>
     * 
     * @return InputStream for response body
     */
    public InputStream getBodyAsInputStream() {
        try {
            // Bug #48 Fix: Return actual stream from HttpMethod
            // This allows streaming large responses without buffering in memory
            InputStream stream = httpMethod.getResponseBodyAsStream();
            return stream != null ? stream : new ByteArrayInputStream(new byte[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        httpMethod.releaseConnection();
    }

    public KeyValuePair<String>[] getHeaders() {
        return this.headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

}