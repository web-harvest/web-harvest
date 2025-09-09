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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Implementation of {@link SchemaFactory} and {@link SchemaResolver} interfaces
 * which realizes Abstract Factory and Singleton design patterns. It support
 * multiple refresh operations. Each time, resolver refresh operation is
 * invoked, new {@link Set} of {@link SchemaSource}s is created, all previously
 * registered {@link SchemaResolverPostProcessor}s are triggered and finally
 * using {@link XMLConstants#W3C_XML_SCHEMA_NS_URI} schema language new instance
 * of {@link Schema} is created.
 *
 * @author mczapiewski
 * @since 2.1-SNAPSHOT
 * @version %I%, %G%
 */
public final class SchemaFactoryImpl implements SchemaFactory, SchemaResolver {

    private static final Logger LOG = LoggerFactory
            .getLogger(SchemaFactoryImpl.class);

    /**
     * Reference to singleton instance.
     */
    public static final SchemaFactoryImpl INSTANCE;

    /**
     * Static block instantiating {@link SchemaFactoryImpl}.
     */
    static {
        INSTANCE = new SchemaFactoryImpl();
    }

    private final javax.xml.validation.SchemaFactory schemaFactory =
        javax.xml.validation.SchemaFactory.newInstance(
                XMLConstants.W3C_XML_SCHEMA_NS_URI);
    private final List<SchemaResolverPostProcessor> postProcessors =
        new ArrayList<SchemaResolverPostProcessor>();

    /** Synchronization monitor for the "refresh" operation */
    private final Object refreshMonitor = new Object();

    private Set<SchemaSource> schemaSources = new HashSet<SchemaSource>();
    private Schema schema;

    /**
     * Private class constructor which is a required part of Singleton design
     * pattern.
     */
    private SchemaFactoryImpl() {
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerSchemaSource(final SchemaSource schemaSource) {
        if (schemaSource == null) {
            throw new IllegalArgumentException(
                    "Schema source must not be null.");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Register schema source for system id {}", schemaSource
                    .getSource().getSystemId());
        }
        schemaSources.add(schemaSource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPostProcessor(
            final SchemaResolverPostProcessor postProcessor) {
        if (postProcessor == null) {
            throw new IllegalArgumentException(
                    "PostProcessor must not be null.");
        }
        postProcessors.add(postProcessor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return schema;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        synchronized (this.refreshMonitor) {
            schemaSources = new HashSet<SchemaSource>();
            invokePostProcessors();
            createSchema();
        }
    }

    /**
     * Helper method invoking all post processors.
     */
    private void invokePostProcessors() {
        for (SchemaResolverPostProcessor postProcessor : postProcessors) {
            postProcessor.postProcess(this);
        }
    }

    /**
     * Helper method creating new {@link Schema} instance for schema sources
     * resolved by post processors. If this operation failed a
     * {@link RuntimeException} is thrown.
     */
    private void createSchema() {
        try {
            schema = schemaFactory.newSchema(
                    getSources().toArray(new Source[] {}));
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method transforming schema sources resolved by post processors to
     * {@link Source}s and returning them.
     *
     * @return {@link Source}s retrieved from schema sources which has been
     *         resolved by post processors
     */
    private Set<Source> getSources() {
        final Set<Source> sources = new HashSet<Source>();
        for (SchemaSource schemaSource : schemaSources) {
            sources.add(schemaSource.getSource());
        }
        return sources;
    }

}
