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
import org.webharvest.exception.PluginException;

import static org.testng.Assert.*;

/**
 * Test for ClassLoaderUtil utility class.
 */
public class ClassLoaderUtilTest {

    @Test
    public void testGetPluginClassWithValidClass() throws Exception {
        // Test loading a class that exists in the classpath
        Class<?> clazz = ClassLoaderUtil.getPluginClass("java.lang.String");
        assertNotNull(clazz);
        assertEquals(clazz.getName(), "java.lang.String");
    }

    @Test(expectedExceptions = PluginException.class)
    public void testGetPluginClassWithInvalidClass() throws Exception {
        // Test loading a class that doesn't exist
        ClassLoaderUtil.getPluginClass("com.nonexistent.Class");
    }

    @Test(expectedExceptions = PluginException.class)
    public void testGetPluginClassWithNullClassName() throws Exception {
        // Test with null class name
        ClassLoaderUtil.getPluginClass(null);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testGetPluginClassWithEmptyClassName() throws Exception {
        // Test with empty class name
        ClassLoaderUtil.getPluginClass("");
    }

    @Test
    public void testGetPluginClassWithSystemClass() throws Exception {
        // Test loading a system class
        Class<?> clazz = ClassLoaderUtil.getPluginClass("java.util.ArrayList");
        assertNotNull(clazz);
        assertEquals(clazz.getName(), "java.util.ArrayList");
    }

    @Test
    public void testGetPluginClassWithWebHarvestClass() throws Exception {
        // Test loading a Web-Harvest class
        Class<?> clazz = ClassLoaderUtil.getPluginClass("org.webharvest.utils.CommonUtil");
        assertNotNull(clazz);
        assertEquals(clazz.getName(), "org.webharvest.utils.CommonUtil");
    }
}

























