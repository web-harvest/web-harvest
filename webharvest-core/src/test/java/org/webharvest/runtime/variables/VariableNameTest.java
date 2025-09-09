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

package org.webharvest.runtime.variables;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.*;
import org.webharvest.exception.VariableException;

public class VariableNameTest {

    private static final String VALID_NAME = "dummy";

    @Test(expectedExceptions=VariableException.class)
    public void testCreateWithNullName() {
        new VariableName(null);
    }

    @Test(expectedExceptions=VariableException.class)
    public void testCreateWithEmptyName() {
        new VariableName("");
    }

    @Test(expectedExceptions=VariableException.class)
    public void testCreateWithSeparatedName() {
        new VariableName("new value");
    }

    @Test(expectedExceptions=VariableException.class)
    public void testCreateWithNameSeparatedByMinus() {
        new VariableName("new-value");
    }

    @Test(expectedExceptions=VariableException.class)
    public void testCreateWithVarNameStartingWithNumber() {
        new VariableName("1SSSS");
    }

    @Test
    public void testCreateWithVarNameStartsWithUpper() {
        new VariableName("Dummy");
    }

    @Test
    public void testCreateWithVarNameWithCammelCase() {
        new VariableName("dummyVar");
    }

    @Test
    public void testCreateWithVarNameEndingWithNumber() {
        new VariableName("dummy1");
    }

    @Test
    public void testCreateWithVarNameWithNumberInMiddle() {
        new VariableName("dum12my");
    }

    @Test
    public void testGetValue() {
        final String value = new VariableName(VALID_NAME).getValue();
        assertNotNull(value);
        assertSame(VALID_NAME, value);
    }

}
