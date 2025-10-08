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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.exception.BaseException;
import org.webharvest.exception.UserException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.variables.NodeVariable;

import static org.testng.Assert.*;

/**
 * Test for SystemUtilities utility class.
 */
public class SystemUtilitiesTest {

    private SystemUtilities systemUtilities;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
        systemUtilities = new SystemUtilities(context);
    }

    @Test
    public void testConstants() {
        assertEquals(SystemUtilities.lf.toString(), "\n");
        assertEquals(SystemUtilities.tab.toString(), "\t");
        assertEquals(SystemUtilities.cr.toString(), "\r");
        assertEquals(SystemUtilities.space.toString(), " ");
        assertEquals(SystemUtilities.quot.toString(), "\"");
        assertEquals(SystemUtilities.apos.toString(), "\'");
        assertEquals(SystemUtilities.backspace.toString(), "\b");
    }

    @Test
    public void testIsVariableDefined() {
        assertFalse(systemUtilities.isVariableDefined("nonexistent"));
        
        context.setLocalVar("testVar", CommonUtil.createVariable("testValue"));
        assertTrue(systemUtilities.isVariableDefined("testVar"));
    }

    @Test
    public void testGetVar() {
        assertNull(systemUtilities.getVar("nonexistent"));
        
        Variable testVar = CommonUtil.createVariable("testValue");
        context.setLocalVar("testVar", testVar);
        assertEquals(systemUtilities.getVar("testVar"), testVar);
    }

    @Test
    public void testDefineVariable() {
        systemUtilities.defineVariable("testVar", "testValue");
        assertEquals(systemUtilities.getVar("testVar").toString(), "testValue");
    }

    @Test
    public void testDefineVariableWithOverwrite() {
        systemUtilities.defineVariable("testVar", "initialValue");
        assertEquals(systemUtilities.getVar("testVar").toString(), "initialValue");
        
        systemUtilities.defineVariable("testVar", "newValue", false);
        assertEquals(systemUtilities.getVar("testVar").toString(), "initialValue");
        
        systemUtilities.defineVariable("testVar", "newValue", true);
        assertEquals(systemUtilities.getVar("testVar").toString(), "newValue");
    }

    @Test
    public void testDatetime() {
        String result = systemUtilities.datetime("yyyy-MM-dd");
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test(expectedExceptions = BaseException.class)
    public void testDatetimeWithNullFormat() {
        systemUtilities.datetime(null);
    }

    @Test
    public void testDate() {
        String result = systemUtilities.date();
        assertNotNull(result);
        assertTrue(result.matches("\\d{8}"));
    }

    @Test
    public void testTime() {
        String result = systemUtilities.time();
        assertNotNull(result);
        assertTrue(result.matches("\\d{6}"));
    }

    @Test
    public void testEscapeXml() {
        String result = systemUtilities.escapeXml("test & \"quoted\" <tag>");
        assertEquals(result, "test &amp; &quot;quoted&quot; &lt;tag&gt;");
    }

    @Test(expectedExceptions = BaseException.class)
    public void testEscapeXmlWithNull() {
        systemUtilities.escapeXml(null);
    }

    @Test
    public void testFullUrl() {
        String result = systemUtilities.fullUrl("http://example.com/page", "image.jpg");
        assertEquals(result, "http://example.com/image.jpg");
    }

    @Test(expectedExceptions = BaseException.class)
    public void testFullUrlWithNull() {
        systemUtilities.fullUrl(null, "link");
    }

    @Test
    public void testGetFilename() {
        assertEquals(systemUtilities.getFilename("/path/to/file.txt"), "file.txt");
        assertEquals(systemUtilities.getFilename("C:\\path\\to\\file.txt"), "file.txt");
        assertEquals(systemUtilities.getFilename("file.txt"), "file.txt");
        assertEquals(systemUtilities.getFilename("/path/to/"), "");
    }

    @Test(expectedExceptions = UserException.class)
    public void testError() {
        systemUtilities.error("Test error message");
    }

    @Test
    public void testRecordCount() {
        assertEquals(systemUtilities.recordCount(), 0);
    }
}
