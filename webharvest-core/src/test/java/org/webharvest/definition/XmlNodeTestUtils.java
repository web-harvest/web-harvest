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

package org.webharvest.definition;


public class XmlNodeTestUtils {

    public static final String NAMESPACE_10 = "xmlns='http://org.webharvest/schema/1.0/config' " +
            "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
            "xsi:schemaLocation='http://org.webharvest/schema/1.0/config config.xsd'";

    public static final String NAMESPACE_21 = "xmlns='http://org.webharvest/schema/2.1/core' " +
            "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
            "xsi:schemaLocation='http://org.webharvest/schema/2.1/core wh-core-2.1-loose.xsd'";

    /**
     * Instantiates {@link XmlNode} for given part of XML file and namespace. It
     * firstly wrappes given part of XML file by {@code <config>} element with
     * namespace attributes, then instantes {@link XmlNode} for whole
     * {@code <config>} element and finally returns first child of config node
     * which is a {@link XmlNode} for given part of XML file.
     */
    public static XmlNode createXmlNode(final String xmlPart, final String namespace) {
        final StringBuilder builder = new StringBuilder().append("<config ").
            append(namespace).append(">").append(xmlPart).append("</config>");
        final XmlNode configNode = new SAXConfigParser().parse(
                new BufferConfigSource(builder.toString())).getNode();

        return (XmlNode) configNode.getElementList().get(0);
    }

}
