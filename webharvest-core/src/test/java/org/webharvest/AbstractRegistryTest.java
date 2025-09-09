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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertSame;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AbstractRegistryTest {

    private static final String KEY = "my registry key";
    private static final String VALUE = "my registry value";

    private class MockRegistry extends AbstractRegistry<String, String> {
    }

    private MockRegistry registry;

    @BeforeMethod
    public void setUp() {
        this.registry = new MockRegistry();
    }

    @Test
    public void lookupBound() throws Exception {
        registry.bind(KEY, VALUE);
        final String result = registry.lookup(KEY);
        assertSame("Unexpected value found", VALUE, result);
    }

    @Test
    public void lookupNotBound() {
        final String result = registry.lookup(KEY);
        assertNull("Expectected value not to be bound", result);
    }

    @Test
    public void lookupUnbound() throws Exception {
        registry.bind(KEY, VALUE);
        registry.unbind(KEY);
        final String result = registry.lookup(KEY);
        assertNull("Expectected value not to be bound", result);
    }

    @Test(expectedExceptions = AlreadyBoundException.class)
    public void doNotBoundTwice() throws Exception {
        registry.bind(KEY, VALUE);
        registry.bind(KEY, "another value 123!@#");
    }

    @Test
    public void listAllBound() throws Exception {
        registry.bind(KEY, VALUE);
        registry.bind("anotherkey", "anothervalue");

        final Set<String> expected = new HashSet<String>(Arrays.asList(
                KEY, "anotherkey"));
        final Set<String> result = registry.listBound();

        assertEquals("Unexpected bound list", expected, result);
    }
}
