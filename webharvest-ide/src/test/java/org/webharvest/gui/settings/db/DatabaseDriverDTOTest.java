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

package org.webharvest.gui.settings.db;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DatabaseDriverDTOTest {

    private DatabaseDriverDTO x;
    private DatabaseDriverDTO y;
    private DatabaseDriverDTO z;
    private DatabaseDriverDTO notX;

    @BeforeMethod
    public void setUp() {
        this.x = new DatabaseDriverDTO("/tmp/somewhere");
        this.y = new DatabaseDriverDTO("/tmp/somewhere");
        this.z = new DatabaseDriverDTO("/tmp/somewhere");

        this.notX = new DatabaseDriverDTO("/tmp/somewhere/else");
    }

    @AfterMethod
    public void tearDown() {
        this.x = null;
        this.y = null;
        this.z = null;

        this.notX = null;
    }


    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullLocationDisallowed() {
        new DatabaseDriverDTO(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationDisallowed() {
        new DatabaseDriverDTO("");
    }

    @Test
    public void equalsIsReflexive() {
        assertEquals("Equals is not reflexive", x, x);
    }

    @Test
    public void equalsIsSymmetric() {
        assertTrue("Equals is not symmetric", x.equals(y) && y.equals(x));
    }

    @Test
    public void equalsIsTransitive() {
        assertTrue("Equals is not transitive",
                x.equals(y) && y.equals(z) && x.equals(z));
    }

    @Test
    public void equalsIsCautious() {
        assertFalse("Comparision with null should always return false",
                x.equals(null));
    }

    @Test
    public void equalsIsTypeAware() {
        assertFalse("Comparison with different type should always return false",
                x.equals(this));
    }

    @Test
    public void notEqualForDifferentLocations() {
        assertFalse("Location is not considered by equals", x.equals(notX));
    }

    @Test
    public void hashCodeIsRepeatable() {
        final int hashCode = x.hashCode();
        for (int i = 0; i < 10; i++) {
            assertEquals("Hash code is not repeatable", hashCode, x.hashCode());
        }
    }

    @Test
    public void hashCodeConsistentWithEquals() {
        assertFalse("Expected different hash codes for not equal objects",
                x.hashCode() == notX.hashCode());
    }
}
