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

import org.webharvest.definition.ConfigLocationVisitor.VisitableLocation;
import org.webharvest.definition.URLConfigSource.URLLocation;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.testng.annotations.*;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.webharvest.definition.ConfigSource.Location;

public class AbstractConfigSourceTest extends UnitilsTestNG {

    private MockConfigSource source;

    @RegularMock
    private VisitableLocation mockLocation;

    @BeforeMethod
    public void setUp() throws Exception {
        source = new MockConfigSource();
    }

    @AfterMethod
    public void testTearDown() {
        source = null;
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testIncludeWithoutLocation() throws IOException {
        source.include(null);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testIncludeWithDefaultLocation() throws IOException {
        source.include(ConfigSource.UNDEFINED_LOCATION);
    }

    @Test
    public void testIncludeWithURLLocation() throws Exception {
        source.setLocation(new URLLocation(new URL("http://sf.net")));
        final ConfigSource included = source.include(
                new Location() {

                    @Override
                    public String toString() {
                        return "/home/";
                    }

                });
        assertNotNull(included);
    }

    @Test(expectedExceptions=IllegalStateException.class)
    public void testIncludeWithUnsupportedLocation() throws Exception {
        mockLocation.accept(isA(IncludeVisitor.class));
        expectLastCall();
        source.setLocation(mockLocation);
        EasyMockUnitils.replay();
        source.include(
                new Location() {

                    @Override
                    public String toString() {
                        return "/home/";
                    }

                });
    }

    private class MockConfigSource extends AbstractConfigSource {

        private VisitableLocation location;

        @Override
        public Location getLocation() {
            return location;
        }

        public void setLocation(final VisitableLocation location) {
            this.location = location;
        }

        @Override
        public Reader getReader() throws IOException {
            return null;
        }

        @Override
        protected void visit(ConfigLocationVisitor visitor) throws IOException {
            location.accept(visitor);
        }

    }

}
