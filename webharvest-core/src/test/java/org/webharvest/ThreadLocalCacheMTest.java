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

import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Multithreaded test is declared inside separate class class to overcome
 * unitils's 'before method' interceptor problems.
 * (We have to use 'before class' interceptor for multithreaded tests,
 * otherwise we can get unexpected results, since the cache reference may
 * change between test method invocations)
 */
public class ThreadLocalCacheMTest {

    private ThreadLocalCache<String, Object> cache;

    @BeforeClass
    public void setUp() {
        this.cache = new ThreadLocalCache<String, Object>();
    }

    @Test(invocationCount = 100, threadPoolSize = 10)
    public void cacheWorksLocallyPerThread() throws Exception {
        final Long cachedObject = new Long(Thread.currentThread().getId());
        cache.put("mykey", cachedObject);
        assertTrue("Cache should contain previously stored object",
                cache.contains("mykey"));
        assertSame("Cache should be able to lookup previously stored object",
                cachedObject, cache.lookup("mykey"));
    }
}
