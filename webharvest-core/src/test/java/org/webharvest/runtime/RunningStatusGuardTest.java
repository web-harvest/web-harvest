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

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.mockito.Mockito.*;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.util.concurrent.Monitor;

public class RunningStatusGuardTest {

    private StatusHolder mockHolder;

    private RunningStatusGuard guard;

    @BeforeMethod
    public void setUp() throws Exception {
        mockHolder = mock(StatusHolder.class);
        guard = new RunningStatusGuard(new Monitor());

        // Use reflection to set the private field
        java.lang.reflect.Field field = RunningStatusGuard.class.getDeclaredField("statusHolder");
        field.setAccessible(true);
        field.set(guard, mockHolder);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        guard = null;
    }

    @Test
    public void isSatisfiedIfRunningStatus() throws Exception {
        when(mockHolder.getStatus()).thenReturn(ScraperState.RUNNING);

        final boolean result = guard.isSatisfied();
        assertTrue("Unexpected result.", result);
    }

    @Test
    public void isSatisfiedIfOtherStatus() throws Exception {
        when(mockHolder.getStatus()).thenReturn(ScraperState.PAUSED);

        final boolean result = guard.isSatisfied();
        assertFalse("Unexpected result.", result);
    }
}
