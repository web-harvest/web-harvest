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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

/**
 * Ftp List plugin - can be used only inside ftp plugin for listing file in working remote directory.
 */
public class FtpListPlugin extends WebHarvestPlugin {

    public String getName() {
        return "ftp-list";
    }

    public Variable executePlugin(DynamicScopeContext context) {
        FtpPlugin ftpPlugin = (FtpPlugin) getParentProcessor();
        if (ftpPlugin != null) {
            FTPClient ftpClient = ftpPlugin.getFtpClient();

            String path = CommonUtil.nvl( evaluateAttribute("path", context), "" );
            boolean listFiles = evaluateAttributeAsBoolean("listfiles", true, context);
            boolean listDirs = evaluateAttributeAsBoolean("listdirs", true, context);
            boolean listLinks = evaluateAttributeAsBoolean("listlinks", true, context);
            String listFilter = CommonUtil.nvl( evaluateAttribute("listfilter", context), "" );

            Pattern pattern = null;
            if ( !CommonUtil.isEmptyString(listFilter) ) {
                StringBuilder buffer = new StringBuilder();
                for (int i = 0; i < listFilter.length(); i++) {
                    char ch = listFilter.charAt(i);
                    switch (ch) {
                        case '.' : buffer.append("\\."); break;
                        case '*' : buffer.append(".*"); break;
                        case '?' : buffer.append("."); break;
                        default : buffer.append(ch); break;
                    }
                }
                try {
                    pattern = Pattern.compile( buffer.toString() );
                } catch (PatternSyntaxException e) {
                    pattern = Pattern.compile("");
                }
            }

            setProperty("Path", path);
            setProperty("List Files", listFiles);
            setProperty("List Directories", listDirs);
            setProperty("List Symbolic Links", listLinks);
            setProperty("List Filter", listFilter);

            try {
                FTPFile[] files = ftpClient.listFiles(path);
                if (files != null) {
                    List<String> filenameList = new ArrayList<String>();
                    for (FTPFile ftpFile: files) {
                        if ( (listFiles && ftpFile.isFile()) || (listDirs && ftpFile.isDirectory()) || (listLinks && ftpFile.isSymbolicLink()) ) {
                            String name = ftpFile.getName();
                            if ( pattern == null || pattern.matcher(name).matches() ) {
                                filenameList.add(name);
                            }
                        }
                    }

                    return new ListVariable(filenameList);
                }
            } catch (IOException e) {
                throw new FtpPluginException(e);
            }
        } else {
            throw new FtpPluginException("Cannot use ftp list plugin out of ftp plugin context!");
        }

        return EmptyVariable.INSTANCE;
    }

    public String[] getValidAttributes() {
        return new String[] {"path", "listfiles", "listdirs", "listlinks", "listfilter"};
    }

    public String[] getRequiredAttributes() {
        return new String[] {};
    }

    public boolean hasBody() {
        return false;
    }

    public String[] getAttributeValueSuggestions(String attributeName) {
        if ( "listfiles".equalsIgnoreCase(attributeName) ||
             "listdirs".equalsIgnoreCase(attributeName) ||
             "listlinks".equalsIgnoreCase(attributeName) ) {
            return new String[] {"true", "false"};
        }
        return null;
    }

}