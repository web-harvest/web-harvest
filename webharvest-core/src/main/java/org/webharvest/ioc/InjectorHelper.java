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

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Guice static injector helper. This class is handy when it comes to get
 * reference to Guice's {@link Injector} for a class that is not managed or
 * instantiated by Guice (eg. legacy code or Guice module type listener
 * classes). Please do not overuse it as its a kind of trick rather then good
 * approach to codding style.
 *
 * <pre>
 * public class MyModule extends AbstractModule {
 *   protected void configure() {
 *     // (...)
 *     requestStaticInjection(InjectorHelper.class);
 *     // (...)
 *   }
 * }
 * </pre>
 *
 * @author Robert Bala
 * @author Piotr Dyraga
 * @since 2.1.0-SNAPSHOT
 * @see Injector
 */
public final class InjectorHelper {

    @Inject
    private static Injector injector;

    /**
     * Class do nothing constructor preventing instantiation.
     */
    private InjectorHelper() {
        // Do nothing constructor
    }

    /**
     * Gets reference to the current {@link Injector}.
     *
     * @return reference to Guice's or {@code null} if the helper has not been
     *         installed properly.
     */
    public static Injector getInjector() {
        return injector;
    }

}
