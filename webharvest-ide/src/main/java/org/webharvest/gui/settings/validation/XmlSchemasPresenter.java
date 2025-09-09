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

package org.webharvest.gui.settings.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.webharvest.definition.validation.SchemaComponentFactory;
import org.webharvest.definition.validation.SchemaResolver;
import org.webharvest.gui.Settings;
import org.webharvest.gui.settings.SettingsAware;
import org.webharvest.gui.settings.validation.XmlSchemasView.Presenter;

/**
 * Default implementation of MVP's {@link XmlSchemasView.Presenter} interface
 * which also realizes {@link SettingsAware} interface in order to
 * react on {@link Settings} load/update action.
 *
 * @see XmlSchemasView.Presenter
 *
 * @author mczapiewski
 * @since 2.1-SNAPSHOT
 * @version %I%, %G%
 */
public final class XmlSchemasPresenter implements Presenter, SettingsAware {

    private Set<XmlSchemaDTO> schemaDTOs = new HashSet<XmlSchemaDTO>();

    private final SchemaResolver schemaResolver =
        SchemaComponentFactory.getSchemaResolver();

    private final XmlSchemasView view;

    /**
     * Presenter's constructor accepting not-{@code null} reference to the
     * instance of {@link XmlSchemasView} with which this presenter is intended
     * to cooperate (MVP design pattern).
     *
     * @param view
     *            not-{@code null} reference to the {@link XmlSchemasView}
     *            instance.
     */
    public XmlSchemasPresenter(final XmlSchemasView view) {
        if (view == null) {
            throw new IllegalArgumentException("View must not be null.");
        }
        this.view = view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerSchema(final XmlSchemaDTO schema) {
        if (schema == null) {
            throw new IllegalArgumentException("DTO must not be null.");
        }
        if (schemaDTOs.add(schema)) {
            view.addToList(schema);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterSchema(final XmlSchemaDTO schema) {
        if (schema == null) {
            throw new IllegalArgumentException("DTO must not be null.");
        }
        if (schemaDTOs.remove(schema)) {
            view.removeFromList(schema);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoad(final Settings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Settings must not be null.");
        }
        final Set<XmlSchemaDTO> schemas = new LinkedHashSet<XmlSchemaDTO>(
                Arrays.asList(settings.getXmlSchemas()));
        for (XmlSchemaDTO schema : schemas) {
            registerSchema(schema);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(final Settings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Settings must not be null.");
        }
        settings.setXmlSchemas(schemaDTOs.toArray(new XmlSchemaDTO[]{}));
        schemaResolver.refresh();
    }

}
