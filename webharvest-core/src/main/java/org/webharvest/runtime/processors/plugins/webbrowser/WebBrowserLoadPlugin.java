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

import org.webharvest.annotation.Definition;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.processors.plugins.Autoscanned;
import org.webharvest.runtime.processors.plugins.TargetNamespace;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

import static org.webharvest.WHConstants.XMLNS_CORE;
import static org.webharvest.WHConstants.XMLNS_CORE_10;

/**
 * Load page inside headless web browser.
 */
@Autoscanned
@TargetNamespace({ XMLNS_CORE, XMLNS_CORE_10 })
@Definition(value = "web-browser-load",
            validAttributes = {
                    "url", "page", "width", "height", "paperformat(A3;A4;A5;Legal;Letter;Tabloid)", "paperorientation(portrait;landscape)", "paperborder",
                    "javascriptenabled(true;false)", "loadimages(true;false)", "useragent", "username", "password", "zoomfactor"
            }
)
public class WebBrowserLoadPlugin extends WebHarvestPlugin {

    public Variable executePlugin(DynamicScopeContext context) throws InterruptedException {
        WebBrowserPlugin webBrowserPlugin = WebBrowserPlugin.findParentPlugin(this);
        if (webBrowserPlugin != null) {
            String url = evaluateAttribute("url", context);
            String pageName = evaluateAttribute("page", context);
            if (CommonUtil.isEmptyString(url)) {
                throw new WebBrowserlPluginException("Url must be non-empty!");
            }
            Variable content = executeBody(context);
            String urlContent = webBrowserPlugin.loadUrl(
                    url,
                    CommonUtil.nvl(pageName, ""),
                    evaluateAttribute("width", context),
                    evaluateAttribute("height", context),
                    evaluateAttribute("paperformat", context),
                    evaluateAttribute("paperorientation", context),
                    evaluateAttribute("paperborder", context),
                    evaluateAttribute("javascriptenabled", context),
                    evaluateAttribute("loadimages", context),
                    evaluateAttribute("useragent", context),
                    evaluateAttribute("username", context),
                    evaluateAttribute("password", context),
                    evaluateAttribute("zoomfactor", context),
                    content == null ? null : content.toString()
            );
            return new NodeVariable(urlContent);
        } else {
            throw new WebBrowserlPluginException("Plugin 'web-browser-load' must be inside 'web-browser' execution context");
        }
    }

}