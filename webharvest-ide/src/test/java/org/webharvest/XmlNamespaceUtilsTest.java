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
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.webharvest.XmlNamespaceUtils.getNamespaceResolverFromBrokenXml;

public class XmlNamespaceUtilsTest {

    @Test
    public void testGetNamespaceResolverFromBrokenXml_root() throws Exception {
        assertEquals(getNamespaceResolverFromBrokenXml("<root xmlns:a='foo'").getURIForPrefix("a", false), "foo");
        assertEquals(getNamespaceResolverFromBrokenXml("<root xmlns:a='bar' ").getURIForPrefix("a", false), "bar");
        assertEquals(getNamespaceResolverFromBrokenXml("<root xmlns:a='baz' b='").getURIForPrefix("a", false), "baz");
        assertEquals(getNamespaceResolverFromBrokenXml("<root xmlns:a='qux' b=\"").getURIForPrefix("a", false), "qux");
    }

    @Test
    public void testGetNamespaceResolverFromBrokenXml_in_scope() throws Exception {
        final NamespaceResolver nsResolver = getNamespaceResolverFromBrokenXml("" +
                "<root xmlns:a='aaa' xmlns:b='bbb'>" +
                "<foo xmlns:b='bbb2' xmlns:c='ccc'>" +
                "<bar");
        assertEquals(nsResolver.getURIForPrefix("a", false), "aaa");
        assertEquals(nsResolver.getURIForPrefix("b", false), "bbb2");
        assertEquals(nsResolver.getURIForPrefix("c", false), "ccc");
    }

    @Test
    public void testGetNamespaceResolverFromBrokenXml_out_scope() throws Exception {
        final NamespaceResolver nsResolver = getNamespaceResolverFromBrokenXml("" +
                "<root xmlns:a='aaa' xmlns:b='bbb'>" +
                "<foo xmlns:b='bbb2'></foo>" +
                "<bar");
        assertEquals(nsResolver.getURIForPrefix("a", false), "aaa");
        assertEquals(nsResolver.getURIForPrefix("b", false), "bbb");
        assertNull(nsResolver.getURIForPrefix("c", false));
    }

    @Test
    public void testGetNamespaceResolverFromBrokenXml_default_ns() throws Exception {
        assertNull(getNamespaceResolverFromBrokenXml("<root>").getURIForPrefix("", true));
        assertNull(getNamespaceResolverFromBrokenXml("<root xmlns='xxx'>").getURIForPrefix("", false));
        assertEquals(getNamespaceResolverFromBrokenXml("<root xmlns='xxx'>").getURIForPrefix("", true), "xxx");
    }

    @Test
    public void testGetNamespaceResolverFromBrokenXml_in_scope_with_default() throws Exception {
        final NamespaceResolver nsResolver = getNamespaceResolverFromBrokenXml("" +
                "<root xmlns='xxx'>" +
                "<foo xmlns='yyy'>" +
                "<bar");
        assertEquals(nsResolver.getURIForPrefix("", true), "yyy");
    }

    @Test
    public void testGetNamespaceResolverFromBrokenXml_out_scope_with_default() throws Exception {
        final NamespaceResolver nsResolver = getNamespaceResolverFromBrokenXml("" +
                "<root xmlns='xxx'>" +
                "<foo xmlns='yyy'></foo>" +
                "<bar");
        assertEquals(nsResolver.getURIForPrefix("", true), "xxx");
    }
}
