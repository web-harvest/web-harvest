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

package org.webharvest.annotation;

import org.webharvest.definition.ElementInfo;
import org.webharvest.exception.PluginException;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.utils.CommonUtil;

/**
 * Temporary helper class responsible for instantiation of {@link ElementInfo}
 * from {@link Definition} annotation that is put on {@link WebHarvestPlugin}
 * that is about to be registered. Helper methods
 * {@link #getTagDesc(Definition)} and {@link #getAttributeDesc(Definition)}
 * have been moved from {@link WebHarvestPlugin} class almost without
 * modification.
 *
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 * @version %I%, %G%
 */
@Deprecated
// TODO Removed when finished with processors refactoring
public final class ElementInfoFactory {

    /**
     * Class constructor preventing instantiation.
     */
    private ElementInfoFactory() {
        // Do nothing constructor
    }

    /**
     * Creates new {@link ElementInfo} object from processor class that is about
     * to be registered.
     *
     * @param processorClass
     *            plugin/processor class to register.
     * @return
     */
    public static ElementInfo getElementInfo(
            final Class<? extends WebHarvestPlugin> processorClass) {
        if (processorClass == null) {
            throw new IllegalArgumentException("Processor is required");
        }

        // No need to check if annotation is set (thows NullPointerException
        // when missing)
        final Definition definition = processorClass
                .getAnnotation(Definition.class);

        final String name = definition.value();

        if (!CommonUtil.isValidXmlIdentifier(name)) {
            throw new PluginException("Plugin class \""
                    + processorClass.getName()
                    + "\" does not define valid name!");
        }

        return new ElementInfo(definition.value(),
                definition.definitionClass(), processorClass,
                getTagDesc(definition), getAttributeDesc(definition),
                definition.internal(), definition.dependantProcessors());
    }

    private static String getTagDesc(final Definition definition) {
        if (!definition.body()) {
            return "";
        }

        final String[] validSubprocessors = definition.validSubprocessors();
        if (validSubprocessors.length < 1) {
            return null;
        }

        final String requiredTags[] = definition.requiredSubprocessors();

        final StringBuilder result = new StringBuilder();
        for (String subProcessor : validSubprocessors) {
            if (result.length() != 0) {
                result.append(',');
            }
            if (CommonUtil
                    .existsInStringArray(requiredTags, subProcessor, true)) {
                result.append('!');
            }
            result.append(subProcessor);
        }
        return result.toString();
    }

    private static String getAttributeDesc(final Definition definition) {
        final String[] validAtts = definition.validAttributes();
        if (validAtts.length < 1) {
            return "id";
        }

        final String requiredAtts[] = definition.requiredAttributes();

        final StringBuilder result = new StringBuilder("id,");
        for (String attr : validAtts) {
            if (CommonUtil.existsInStringArray(requiredAtts, attr, true)) {
                result.append('!');
            }
            result.append(attr);
            result.append(",");
        }
        return result.toString();
    }

}
