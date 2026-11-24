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

package org.webharvest.ioc;

import static org.testng.AssertJUnit.assertSame;

import static org.mockito.Mockito.*;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;




import com.google.inject.OutOfScopeException;

public class AttributeHolderScopeTest {

    private AttributeHolderScope<AttributeHolder> scope;

    // @RegularMock - converted to mock() in @BeforeMethod
    private AttributeHolder mockHolder;

    // @RegularMock - converted to mock() in @BeforeMethod
    private AttributeHolder mockOtherHolder;

    @BeforeMethod
    public void setUp() {
        mockHolder = mock(AttributeHolder.class);
        mockOtherHolder = mock(AttributeHolder.class);
        this.scope = new AttributeHolderScope<AttributeHolder>();
    }

    @AfterMethod
    public void tearDown() {
        this.scope = null;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void cannotEnterWithNullHolder() {
        scope.enter(null);
    }

    @Test(expectedExceptions = OutOfScopeException.class)
    public void cannotExitWhenNotInScope() {
        scope.exit();
    }

    @Test(expectedExceptions = OutOfScopeException.class)
    public void cannotGetHolderWhenNotInScope() {
        scope.get();
    }

    @Test
    public void enterScopeOnce() {
        
        scope.enter(mockHolder);
        assertSame("Unexpected scope's attribute holder",
                mockHolder, scope.get());
        scope.exit();
    }

    @Test
    public void removesEmptyThreadScopeAndCanEnterAgain() {
        scope.enter(mockHolder);
        scope.exit();

        try {
            scope.get();
            fail("Scope must be empty after the final exit");
        } catch (OutOfScopeException expected) {
            // Expected; the empty ThreadLocal stack is removed by the scope.
        }

        scope.enter(mockOtherHolder);
        assertSame(mockOtherHolder, scope.get());
        scope.exit();
    }

    @Test
    public void supportsNestedScopes() {
        
        scope.enter(mockHolder);
        assertSame("Unexpected scope's attribute holder",
                mockHolder, scope.get());
        scope.enter(mockOtherHolder);
        assertSame("Unexpected scope's attribute holder",
                mockOtherHolder, scope.get());
        scope.exit();
        assertSame("Unexpected scope's attribute holder",
                mockHolder, scope.get());
        scope.exit();
    }
}
