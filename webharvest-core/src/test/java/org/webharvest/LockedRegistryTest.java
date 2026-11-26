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
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
public class LockedRegistryTest {

    private static final String VALUE = "value";

    private Registry<String, String> delegate;

    private LockedRegistry<String, String> decorator;

    private AtomicInteger atomicCounter;

    @BeforeClass
    public void setUp() {
        delegate = new RegistryMock();
        decorator = new LockedRegistry<String, String>(delegate);
        atomicCounter = new AtomicInteger();
    }

    @AfterClass
    public void tearDown() {
        decorator = null;
        delegate = null;
        atomicCounter = null;
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testConstructorWithNullDelegate() {
         new LockedRegistry<String, String>(null);
    }

    @Test(threadPoolSize = 100, invocationCount = 100)
    public void test() throws Exception {
        final String key = Integer.toString(atomicCounter.incrementAndGet());
        decorator.bind(key, VALUE);
        final String value = decorator.lookup(key);
        assertNotNull("Null value", value);
        assertSame("Unexpected value", VALUE, value);
        decorator.unbind(key);
        final Set<String> entries = decorator.listBound();
        assertNotNull("Null bound", entries);
        assertFalse("Found previously unbound value", entries.contains(key));
    }

    @Test
    public void listBoundReturnsDetachedImmutableSnapshot() throws Exception {
        RegistryMock liveDelegate = new RegistryMock();
        LockedRegistry<String, String> locked = new LockedRegistry<String, String>(liveDelegate);
        locked.bind("snapshot-key", VALUE);

        Set<String> snapshot = locked.listBound();
        locked.unbind("snapshot-key");
        locked.bind("later-key", VALUE);

        assertTrue("Snapshot must retain the value present at capture time", snapshot.contains("snapshot-key"));
        assertFalse("Snapshot must not reflect later bindings", snapshot.contains("later-key"));
        boolean immutable = false;
        try {
            snapshot.add("external-key");
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        assertTrue("Snapshot must not allow caller mutation", immutable);
        assertFalse("Caller mutation must not affect the registry", locked.listBound().contains("external-key"));
    }

    private final class RegistryMock implements Registry<String, String> {

        private final Map<String, String> storage =
            new HashMap<String, String>();

        @Override
        public String lookup(final String name) {
            return storage.get(name);
        }

        @Override
        public void bind(final String name, final String value)
                throws AlreadyBoundException {
            storage.put(name, value);
        }

        @Override
        public void unbind(final String name) {
            storage.remove(name);
        }

        @Override
        public Set<String> listBound() {
            return storage.keySet();
        }

    }

}
