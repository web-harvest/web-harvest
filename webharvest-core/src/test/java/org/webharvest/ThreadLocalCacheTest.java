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

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ThreadLocalCacheTest {

    private ThreadLocalCache<String, Object> cache;

    @BeforeMethod
    public void setUp() {
        this.cache = new ThreadLocalCache<String, Object>();
    }

    @Test
    public void doNotContainNotCachedObject() {
        assertFalse("Contains not cached object", cache.contains("NOT_CACHED"));
    }

    @Test
    public void doNotLookupNotCachedObject() {
        assertNull("Lookup returns not cached object",
                cache.lookup("NOT_CACHED"));
    }

    @Test
    public void containsCachedObject() {
        final Object cachedObject = new Object();
        cache.put("mykey", cachedObject);
        assertTrue("Cache should contain previously stored object",
                cache.contains("mykey"));
    }

    @Test
    public void lookupsCachedObject() {
        final Object cachedObject = new Object();
        cache.put("mykey", cachedObject);
        assertSame("Cache should be able to lookup previously stored object",
                cachedObject, cache.lookup("mykey"));
    }

    @Test
    public void objectIsNotAvailableAfterInvalidation() {
        final Object cachedObject = new Object();
        cache.put("mykey", cachedObject);
        cache.invalidate("mykey");
        assertNull("Object should not be available after its invalidation",
                cache.lookup("mykey"));
    }
}
