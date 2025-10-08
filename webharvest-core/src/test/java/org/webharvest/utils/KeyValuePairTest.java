/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.utils;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Test for KeyValuePair utility class.
 */
public class KeyValuePairTest {

    @Test
    public void testConstructorAndGetters() {
        String key = "testKey";
        String value = "testValue";
        
        KeyValuePair<String> pair = new KeyValuePair<>(key, value);
        
        assertEquals(pair.getKey(), key);
        assertEquals(pair.getValue(), value);
    }

    @Test
    public void testWithIntegerValue() {
        String key = "number";
        Integer value = 42;
        
        KeyValuePair<Integer> pair = new KeyValuePair<>(key, value);
        
        assertEquals(pair.getKey(), key);
        assertEquals(pair.getValue(), value);
    }

    @Test
    public void testWithNullValue() {
        String key = "nullValue";
        String value = null;
        
        KeyValuePair<String> pair = new KeyValuePair<>(key, value);
        
        assertEquals(pair.getKey(), key);
        assertNull(pair.getValue());
    }

    @Test
    public void testWithNullKey() {
        String key = null;
        String value = "testValue";
        
        KeyValuePair<String> pair = new KeyValuePair<>(key, value);
        
        assertNull(pair.getKey());
        assertEquals(pair.getValue(), value);
    }

    @Test
    public void testWithCustomObject() {
        String key = "object";
        Object value = new Object();
        
        KeyValuePair<Object> pair = new KeyValuePair<>(key, value);
        
        assertEquals(pair.getKey(), key);
        assertEquals(pair.getValue(), value);
    }
}















