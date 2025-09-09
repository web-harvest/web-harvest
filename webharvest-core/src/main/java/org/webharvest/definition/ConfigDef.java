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

package org.webharvest.definition;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.webharvest.runtime.processors.Processor;
import org.webharvest.runtime.scripting.ScriptingLanguage;

/**
 * Web Harvest's definition of the {@code <config>} element.
 *
 * @author mczapiewski
 * @since 2.1-SNAPSHOT
 * @version %I%, %G%
 */
public final class ConfigDef extends WebHarvestPluginDef {

    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final ScriptingLanguage DEFAULT_SCRIPTING_LANGUAGE =
        ScriptingLanguage.BEANSHELL;

    private String charset;
    private ScriptingLanguage scriptingLanguage;

    /**
     * Default class constructor which obtains from given {@link XmlNode}
     * attributes such as charset and scripting language. If any of these
     * attributes is empty or has not been specified the defaults will be used.
     *
     * @param xmlNode
     *            reference to {@link XmlNode}
     * @param pluginClass
     *            class of the processor/plugin
     */
    public ConfigDef(final XmlNode xmlNode,
            final Class<? extends Processor> pluginClass) {
        super(xmlNode, pluginClass);

        this.charset = StringUtils.defaultIfEmpty(
                xmlNode.getAttribute("charset"), DEFAULT_CHARSET);
        this.scriptingLanguage = (ScriptingLanguage) ObjectUtils
                .defaultIfNull(ScriptingLanguage.recognize(xmlNode
                        .getAttribute("scriptlang")),
                        DEFAULT_SCRIPTING_LANGUAGE);
    }

    /**
     * Returns default charset for current configuration.
     *
     * @return default charset for current configuration
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Returns default {@link ScriptingLanguage} for current configuration.
     *
     * @return default {@link ScriptingLanguage} for current configuration
     */
    public ScriptingLanguage getScriptingLanguage() {
        return scriptingLanguage;
    }

}
