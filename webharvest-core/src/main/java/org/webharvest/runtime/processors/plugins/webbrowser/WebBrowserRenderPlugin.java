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

package org.webharvest.runtime.processors.plugins.webbrowser;

import com.google.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.webharvest.annotation.Definition;
import org.webharvest.ioc.WorkingDir;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.processors.plugins.Autoscanned;
import org.webharvest.runtime.processors.plugins.TargetNamespace;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

import java.io.File;
import java.io.IOException;

import static org.webharvest.WHConstants.XMLNS_CORE;
import static org.webharvest.WHConstants.XMLNS_CORE_10;

/**
 * Evaluates javascript on the page inside headless web browser.
 */
@Autoscanned
@TargetNamespace({ XMLNS_CORE, XMLNS_CORE_10 })
@Definition(value = "web-browser-render", validAttributes = {"type(jpeg;gif;png;pdf)", "page"})
public class WebBrowserRenderPlugin extends WebHarvestPlugin {

    private static final String TEMP_PDF_FILE_NAME = "__temp__.pdf";

    @Inject
    @WorkingDir
    private String workingDir;

    public Variable executePlugin(DynamicScopeContext context) throws InterruptedException {
        WebBrowserPlugin webBrowserPlugin = WebBrowserPlugin.findParentPlugin(this);
        if (webBrowserPlugin != null) {
            String pageName = CommonUtil.nvl(evaluateAttribute("page", context), "");
            String type = evaluateAttribute("type", context);
            if ( CommonUtil.isEmptyString(type) || (!"png".equalsIgnoreCase(type) && !"gif".equalsIgnoreCase(type) && !"jpeg".equalsIgnoreCase(type) && !"pdf".equalsIgnoreCase(type)) ) {
                type = "JPEG";
            }

            if ("pdf".equalsIgnoreCase(type)) {
                String path = CommonUtil.getAbsoluteFilename(workingDir, TEMP_PDF_FILE_NAME);
                String urlContent = webBrowserPlugin.renderToPdf(path, pageName);
                if (CommonUtil.isEmptyString(urlContent)) {
                    File file = new File(path);
                    try {
                        byte[] data = CommonUtil.readBytesFromFile(file);
                        return new NodeVariable(data);
                    } catch (IOException e) {
                        throw new WebBrowserlPluginException(e);
                    } finally {
                        try{
                            file.delete();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    return new NodeVariable(urlContent);
                }
            } else {
                String urlContent = webBrowserPlugin.renderToImage(type.toUpperCase(), pageName);
                return new NodeVariable(urlContent != null ? Base64.decodeBase64(urlContent) : "");
            }
        } else {
            throw new WebBrowserlPluginException("Plugin 'web-browser-render' must be inside 'web-browser' execution context");
        }
    }

}