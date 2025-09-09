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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.TransformationException;
import org.webharvest.Transformer;

/**
 * {@link SchemaResolverPostProcessor} implementation capable of transforming
 * specified XML schema resources which could be e.g. a paths to these resources
 * or some resource object. Given XML schema resources are transformed to
 * {@link SchemaSource}s using appropriate {@link Transformer} allowing to this
 * transformation.
 *
 * Correctly transformed XML schemas are registered in the post processed
 * {@link SchemaResolver}. If transformation process of particular XML schema
 * resource failed, then this situation will be logged and processing of next
 * resources will be continued.
 *
 * @see Transformer
 * @see SchemaSource
 *
 * @author mczapiewski
 * @since 2.1-SNAPSHOT
 * @version %I%, %G%
 *
 * @param <T>
 *            type of schema resources which are going to be processed by this
 *            component
 */
public final class SchemaResourcesPostProcessor<T> implements
        SchemaResolverPostProcessor {

    private static final Logger LOG = LoggerFactory
            .getLogger(SchemaResourcesPostProcessor.class);

    private final Transformer<T, SchemaSource> transformer;
    private final T[] schemaResources;

    /**
     * Default class constructor accepting array of XML schema resources and
     * reference to {@link Transformer} instance which allows to convert these
     * resources to {@link SchemaSource} objects. All parameters are required to
     * be not {@code null} and an {@link IllegalArgumentException} will be
     * thrown when it does not.
     *
     * @param transformer
     *            a reference to {@link Transformer} instance allowing to
     *            convert XML schema resources to {@link SchemaSource} objects
     * @param schemaResources
     *            array of XML schema resources
     */
    public SchemaResourcesPostProcessor(
            final Transformer<T, SchemaSource> transformer,
            T... schemaResources) {
        if (transformer == null) {
            throw new IllegalArgumentException("Transformer must not be null.");
        }
        if (schemaResources == null) {
            throw new IllegalArgumentException(
                    "Schema resources must not be null.");
        }
        this.transformer = transformer;
        this.schemaResources = schemaResources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcess(final SchemaResolver resolver) {
        for (T schemaResource : schemaResources) {
            try {
                resolver.registerSchemaSource(transformer
                        .transform(schemaResource));
            } catch (TransformationException e) {
                LOG.error("Transformation of {} resource failed.",
                        schemaResource);
            }
        }
    }

}
