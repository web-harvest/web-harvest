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

package org.webharvest.definition.validation;

/**
 * Interface to be implemented by the web harvest XML schema sources resolvers.
 * Facilitates configuration of resolvers, allowing to register resolver post
 * processors and to register instances of {@link SchemaSource}s.
 *
 * @see SchemaResolverPostProcessor
 * @see SchemaSource
 *
 * @author Maciej Czapiewski (mc5122)
 * @since 2.1-SNAPSHOT
 * @version %I%, %G%
 */
public interface SchemaResolver {

    /**
     * Adds new {@link SchemaResolverPostProcessor} which is going to be applied
     * on the current resolver instance on the resolver refresh.
     *
     * @param postProcessor
     *            new instance of {@link SchemaResolverPostProcessor}; must not
     *            be {code null}
     */
    void addPostProcessor(SchemaResolverPostProcessor postProcessor);

    /**
     * Refresh the current resolver instance, causing all XML schema sources to
     * be reinitialized. Also, all previously registered
     * {@link SchemaResolverPostProcessor}s are invoked.
     */
    void refresh();

    /**
     * Register provided {@link SchemaSource} which cannot be {@code null}
     *
     * @param schemaSource
     *            an instance of {@link SchemaSource} which is going to be
     *            registered
     */
    void registerSchemaSource(SchemaSource schemaSource);

}
