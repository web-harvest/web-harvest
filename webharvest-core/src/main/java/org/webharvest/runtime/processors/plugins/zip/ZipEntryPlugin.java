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

package org.webharvest.runtime.processors.plugins.zip;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

/**
 * Zip entry plugin - can be used only inside zip plugin.
 */
public class ZipEntryPlugin extends WebHarvestPlugin {

    public String getName() {
        return "zip-entry";
    }

    public Variable executePlugin(DynamicScopeContext context) throws InterruptedException {
        ZipPlugin zipPlugin = (ZipPlugin) getParentProcessor();
        if (zipPlugin != null) {
            String name = evaluateAttribute("name", context);
            if (CommonUtil.isEmptyString(name)) {
                throw new ZipPluginException("Name of zip entry cannot be empty!");
            }
            String charset = evaluateAttribute("charset", context);
            if (CommonUtil.isEmptyString(charset)) {
                charset = context.getCharset();
            }

            ZipOutputStream zipOutStream = zipPlugin.getZipOutStream();
            Variable bodyResult = executeBody(context);
            try {
                zipOutStream.putNextEntry(new ZipEntry(name));
                zipOutStream.write(bodyResult.toBinary(charset));
                zipOutStream.closeEntry();
            } catch (IOException e) {
                throw new ZipPluginException(e);
            }
        } else {
            throw new ZipPluginException("Cannot use zip entry plugin out of zip plugin context!");
        }
        return EmptyVariable.INSTANCE;
    }

    public String[] getValidAttributes() {
        return new String[] {"name", "charset"};
    }

    public String[] getRequiredAttributes() {
        return new String[] {"name"};
    }

    public String[] getAttributeValueSuggestions(String attributeName) {
        if ("charset".equalsIgnoreCase(attributeName)) {
            Set<String> charsetKeys = Charset.availableCharsets().keySet();
            return new ArrayList<String>(charsetKeys).toArray(new String[charsetKeys.size()]);
        }
        return null;
    }

}