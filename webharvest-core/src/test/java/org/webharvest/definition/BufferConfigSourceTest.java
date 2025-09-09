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

package org.webharvest.definition;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.webharvest.definition.ConfigLocationVisitor.VisitableLocation;

public class BufferConfigSourceTest extends UnitilsTestNG {

    private static final String CONTENT = "blah, blah";

    @RegularMock
    private ConfigLocationVisitor mockVisitor;

    @RegularMock
    private VisitableLocation mockVisitableLocation;

    private BufferConfigSource source;

    @BeforeMethod
    public void setUp() throws Exception {
        source = new BufferConfigSource(CONTENT);
    }

    @AfterMethod
    public void testTearDown() {
        source = null;
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testCreateWithoutContent() {
        new BufferConfigSource(null);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testCreateWithoutContentButWithLocation() {
        new BufferConfigSource(null, ConfigSource.UNDEFINED_LOCATION);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testCreateWithContentButWithoutLocation() {
        new BufferConfigSource(CONTENT, null);
    }

    @Test
    public void testGetLocation() {
        assertNotNull(source.getLocation());
        assertEquals(source.getLocation(), ConfigSource.UNDEFINED_LOCATION);
    }

    @Test
    public void testGetReader() throws IOException {
        final Reader reader = source.getReader();
        assertNotNull(reader);
        assertTrue((reader instanceof StringReader));
    }

    @Test
    public void testVisitWithUnvisitableLocation() throws IOException {
        EasyMockUnitils.replay();
        source.visit(mockVisitor);
    }

    @Test
    public void testVisitWithVisitableLocation() throws IOException {
        mockVisitableLocation.accept(mockVisitor);
        expectLastCall();
        EasyMockUnitils.replay();
        new BufferConfigSource(CONTENT,
                mockVisitableLocation).visit(mockVisitor);
    }

}
