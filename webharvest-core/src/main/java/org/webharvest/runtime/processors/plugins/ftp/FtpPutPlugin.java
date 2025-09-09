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

package org.webharvest.runtime.processors.plugins.ftp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.net.ftp.FTPClient;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

/**
 * Ftp Put plugin - can be used only inside ftp plugin for storing file to remote directory.
 */
public class FtpPutPlugin extends WebHarvestPlugin {

    public String getName() {
        return "ftp-put";
    }

    public Variable executePlugin(DynamicScopeContext context) throws InterruptedException {
        FtpPlugin ftpPlugin = (FtpPlugin) getParentProcessor();
        if (ftpPlugin != null) {
            FTPClient ftpClient = ftpPlugin.getFtpClient();

            String path = CommonUtil.nvl( evaluateAttribute("path", context), "" );
            String charset = CommonUtil.nvl( evaluateAttribute("charset", context), "" );
            if (CommonUtil.isEmptyString(charset)) {
                charset = context.getCharset();
            }

            setProperty("Path", path);
            setProperty("Charset", charset);

            Variable body = executeBody(context);

            try {
                ByteArrayInputStream stream = new ByteArrayInputStream(body.toBinary(charset));
                boolean succ = ftpClient.storeFile(path, stream);
                stream.close();
                if (!succ) {
                    throw new FtpPluginException("Cannot store file \"" + path + "\" to FTP server!");
                }
            } catch (IOException e) {
                throw new FtpPluginException(e);
            }
        } else {
            throw new FtpPluginException("Cannot use ftp put plugin out of ftp plugin context!");
        }

        return EmptyVariable.INSTANCE;
    }

    public String[] getValidAttributes() {
        return new String[] {"path", "charset"};
    }

    public String[] getRequiredAttributes() {
        return new String[] {"path"};
    }

    public String[] getAttributeValueSuggestions(String attributeName) {
        if ("charset".equalsIgnoreCase(attributeName)) {
            Set<String> charsetKeys = Charset.availableCharsets().keySet();
            return new ArrayList<String>(charsetKeys).toArray(new String[charsetKeys.size()]);
        }
        return null;
    }

}