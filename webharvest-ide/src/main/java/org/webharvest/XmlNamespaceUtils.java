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

package org.webharvest;

import net.sf.saxon.om.NamespaceResolver;
import org.apache.commons.lang.StringUtils;
import org.webharvest.utils.Stack;
import org.webharvest.utils.XmlUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.namespace.QName;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class XmlNamespaceUtils {

    @SuppressWarnings({"finally", "ReturnInsideFinallyBlock"})
    public static WHNamespaceResolver getNamespaceResolverFromBrokenXml(String xmlHeadFragment) {
        int openIndex = xmlHeadFragment.lastIndexOf('<');
        int closeIndex = xmlHeadFragment.lastIndexOf('>');

        if (openIndex > closeIndex) {
            if (xmlHeadFragment.endsWith("='")) {
                xmlHeadFragment += "'>";
            } else if (xmlHeadFragment.endsWith("=\"")) {
                xmlHeadFragment += "\">";
            } else {
                xmlHeadFragment += ">";
            }
        }

        final Map<String, Stack<String>> nsMap = new HashMap<String, Stack<String>>();
        try {
            XmlUtil.getSAXParserFactory(false, true).
                    newSAXParser().
                    parse(new InputSource(new StringReader(xmlHeadFragment)), new DefaultHandler() {
                        @Override public void startPrefixMapping(String prefix, String uri) throws SAXException {
                            Stack<String> stack = nsMap.get(prefix);
                            if (stack == null) {
                                stack = new Stack<String>();
                                nsMap.put(prefix, stack);
                            }
                            stack.push(uri);
                        }

                        @Override public void endPrefixMapping(String prefix) throws SAXException {
                            final Stack<String> stack = nsMap.get(prefix);
                            if (stack.size() > 1) {
                                stack.pop();
                            } else {
                                nsMap.remove(prefix);
                            }
                        }
                    });
        } finally {
            return new WHNamespaceResolver(nsMap);
        }
    }

    public static QName parseQName(String qName, NamespaceResolver nsResolver) {
        final String nsPrefix = qName.contains(":") ? StringUtils.substringBefore(qName, ":") : "";
        return new QName(
                nsResolver.getURIForPrefix(nsPrefix, true),
                StringUtils.isNotEmpty(nsPrefix) ? qName.substring(nsPrefix.length() + 1) : qName,
                nsPrefix);
    }

}
