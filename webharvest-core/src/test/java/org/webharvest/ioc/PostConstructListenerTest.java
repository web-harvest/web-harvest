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

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import javax.annotation.PostConstruct;

import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;

public class PostConstructListenerTest {

    @Test
    public void invokesPublicNoArgHooks() {
        final Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bindListener(Matchers.any(), new PostConstructListener());

                bind(WithPostConstructHook.class).in(Singleton.class);
                bind(WithoutPostConstructHook.class).in(Singleton.class);
            }
        });

        injector.getInstance(WithoutPostConstructHook.class);

        final WithPostConstructHook withHook =
            injector.getInstance(WithPostConstructHook.class);
        assertTrue("Post construct hook has not been called",
                withHook.postConstructCalled);
    }

    /**
     * misplaced hook test case - method annotated with @PostConstruct
     * declares input parameters
     */
    @Test
    public void throwsExceptionForHookWithArgs() {
        final Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bindListener(Matchers.any(), new PostConstructListener());

                bind(HavingPostConstructWithArgs.class).in(Singleton.class);
            }
        });
        try {
            injector.getInstance(HavingPostConstructWithArgs.class);
            fail("ProvisionException expected");
        } catch (ProvisionException e) {
            //ok, expected
        }
    }

    /**
     * @PostConstruct method should not delcare checked exceptions.
     * In case of runtime exception occurred, it should be rethrown.
     */
    @Test
    public void rethrowsRuntimeException() {
        final Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bindListener(Matchers.any(), new PostConstructListener());

                bind(WithRuntimeExceptionThrown.class).in(Singleton.class);
            }
        });
        try {
            injector.getInstance(WithRuntimeExceptionThrown.class);
            fail("ProvisionException expected");
        } catch (ProvisionException e) {
            //ok, expected
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    private static class WithPostConstructHook {
        private boolean postConstructCalled = false;

        @PostConstruct
        public void shouldBeCalled() {
            postConstructCalled = true;
        }

        public void shouldNotBeCalled() {
            throw new AssertionError("This method should not be called");
        }
    }

    private static class WithoutPostConstructHook {
        public void shouldNotBeCalled() {
            throw new AssertionError("This method should not be called");
        }
    }

    private static class HavingPostConstructWithArgs {
        @PostConstruct
        public void methodWithParameters(final String param1,
                final String param2) {
            // should not be called
        }
    }

    private static class WithRuntimeExceptionThrown {
        @PostConstruct
        public void iWillThrowRuntimeException() {
            throw new IllegalArgumentException("TEST");
        }
    }
}
