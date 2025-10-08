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

import static org.testng.Assert.*;

import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import org.webharvest.definition.FileConfigSource.FileLocation;

import org.testng.annotations.*;



import org.webharvest.definition.ConfigSource.Location;

public class FileConfigSourceTest {

    private File configFile;

    private FileConfigSource source;

    // @RegularMock - converted to mock() in @BeforeMethod
    private ConfigLocationVisitor mockVisitor;

    @BeforeMethod
    public void setUp() throws Exception {
        mockVisitor = mock(ConfigLocationVisitor.class);
        configFile = File.createTempFile("foo", "tmp");
        source = new FileConfigSource(configFile);
    }

    @AfterMethod
    public void testTearDown() {
        source = null;
        configFile.delete();
        configFile = null;
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testCreateWithoutFile() {
        new FileConfigSource((File) null);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testCreateWithoutLocation() {
        new FileConfigSource((FileLocation) null);
    }

    @Test
    public void testGetLocation() {
        final Location location = source.getLocation();
        assertNotNull(location);
        assertEquals(location.toString(), configFile.getAbsolutePath());
    }

    @Test
    public void testGetReader() throws IOException {
        final Reader reader = source.getReader();
        assertNotNull(reader);
        assertTrue((reader instanceof FileReader));
    }

    @Test
    public void testVisitWithVisitableLocation() throws IOException {
        final FileLocation mockLocation = new FileLocation(configFile);
        mockVisitor.visit(mockLocation);
        
        new FileConfigSource(mockLocation).visit(mockVisitor);
    }

}
