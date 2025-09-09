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

import java.util.Set;

/**
 * MVP View interface responsible for management of XML schemas. It is capable
 * of adding and removing schemas used by Web Harvest during configuration's
 * validation.
 *
 * @author mczapiewski
 * @since 2.1-SNAPSHOT
 * @version %I%, %G%
 */
public interface XmlSchemasView {

    /**
     * Adds given XML schema to displayed list of schemas.
     *
     * @param schema
     *            not-{@code null} XML schema DTO
     */
    void addToList(XmlSchemaDTO schema);

    /**
     * Removes XML schema from the displayed list.
     *
     * @param schema
     *            XML schema which is going to be removed from the list; must
     *            not be {@code null}
     */
    void removeFromList(XmlSchemaDTO schema);

    /**
     * Sets the view MVP's presenter reference.
     *
     * @param presenter
     *            reference to the view's presenter instance.
     */
    void setPresenter(Presenter presenter);

    /**
     * MVP presenter interface working with {@link XmlSchemasView}.
     */
    public interface Presenter {

        /**
         * Registers XML schema represented by the provided DTO. When this
         * method is completed, web harvest is fully capable of validation of
         * configuration using schema registered.
         *
         * @param schema
         *            DTO representing XML schema which is going to be
         *            registered; must not be {@code null}
         */
        void registerSchema(XmlSchemaDTO schema);

        /**
         * Unregisters XML schema represented by the provided DTO. When this
         * method is completed, web harvest can no longer validate configuration
         * using this XML schema.
         *
         * @param schema
         *            DTO representing XML schema which is going to be
         *            unregistered; must not be {@code null}
         */
        void unregisterSchema(XmlSchemaDTO schema);

    }
}
