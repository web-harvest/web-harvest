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

package org.webharvest.runtime;

import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.webharvest.runtime.scripting.ScriptingLanguage;

public class NestedContextFactoryTest extends UnitilsTestNG {

    @RegularMock
    private DynamicScopeContext mockDelegate;

    // instance obtained from factory (under the test)
    private DynamicScopeContext nestedContext;

    @BeforeMethod
    public void setUp() {
        this.nestedContext = NestedContextFactory.create(mockDelegate);
    }

    @AfterMethod
    public void tearDown() {
        this.nestedContext = null;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void doesNotAcceptNullParentContext() {
        NestedContextFactory.create(null);
    }

    @Test
    public void delegateNotShadowedCalls() {
        final String varName = "varName";
        final String varValue = "var Value 123";

        mockDelegate.setLocalVar(same(varName), same(varValue));
        expectLastCall();

        EasyMockUnitils.replay();
        nestedContext.setLocalVar(varName, varValue);
    }

    @Test
    public void shadowCharsetOperations() {
        EasyMockUnitils.replay();

        assertNull("Expected null for not set charset",
                nestedContext.getCharset());
        nestedContext.setCharset("UTF-8");
        assertEquals("Unexpected charset", "UTF-8", nestedContext.getCharset());
    }

    @Test
    public void shadowScriptingLanguageOperations() {
        EasyMockUnitils.replay();

        assertNull("Expected null for not set scripting language",
                nestedContext.getScriptingLanguage());
        nestedContext.setScriptingLanguage(ScriptingLanguage.JAVASCRIPT);
        assertEquals("Unexpected scripting language",
                ScriptingLanguage.JAVASCRIPT,
                nestedContext.getScriptingLanguage());
    }
}
