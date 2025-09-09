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
import org.webharvest.utils.Stack;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: awajda
 * Date: 11/21/11
 * Time: 12:55 AM
 */

public class WHNamespaceResolver implements NamespaceResolver {
    private final Map<String, Stack<String>> nsMap;

    public WHNamespaceResolver(Map<String, Stack<String>> nsMap) {
        this.nsMap = nsMap;
    }

    @Override
    public String getURIForPrefix(String prefix, boolean useDefault) {
        if (useDefault || prefix.length() > 0) {
            final Stack<String> stack = nsMap.get(prefix);
            return stack == null ? null : stack.peek();
        } else {
            return null;
        }
    }

    @Override
    public Iterator iteratePrefixes() {
        return nsMap.keySet().iterator();
    }

    public boolean isEmpty() {
        return nsMap.isEmpty();
    }

    public void addPrefix(final String prefix, final String uri) {
        nsMap.put(prefix, new Stack<String>() {{
            push(uri);
        }});
    }
}
