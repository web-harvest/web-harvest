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

package org.webharvest.ioc;

import static org.testng.AssertJUnit.*;

import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class ScopeAttributeHolderTest {

    private static final String KEY = "key";

    private static final String VALUE = "value";

    private ScopeAttributeHolder holder;

    @BeforeMethod
    public void setUp() {
        holder = new ScopeAttributeHolder();
    }

    @AfterMethod
    public void tearDown() {
        holder = null;
    }

    @Test
    public void testWriteReadOperations() {
        assertFalse(holder.hasAttribute(KEY));
        holder.putAttribute(KEY, VALUE);
        assertTrue(holder.hasAttribute(KEY));
        final Object value = holder.getAttribute(KEY);
        assertNotNull(value);
        assertSame(VALUE, value);
        final Map<Object, Object> attributes =
            (Map<Object, Object>) holder.getAttributeLock();
        assertNotNull(attributes);
        assertFalse(attributes.isEmpty());
        assertTrue(attributes.containsValue(VALUE));
    }


}
