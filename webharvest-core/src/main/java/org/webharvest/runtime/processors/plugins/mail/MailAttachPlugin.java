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

package org.webharvest.runtime.processors.plugins.mail;

import java.io.IOException;

import javax.activation.DataSource;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.webharvest.WHConstants;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.Processor;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

/**
 * Mail attachment plugin - can be used only inside mail plugin.
 */
public class MailAttachPlugin extends WebHarvestPlugin {

    public String getName() {
        return "mail-attach";
    }

    public Variable executePlugin(DynamicScopeContext context) throws InterruptedException {
        Processor processor = getParentProcessor();
        if (processor != null) {
            MailPlugin mailPlugin = (MailPlugin) processor;
            Email email = mailPlugin.getEmail();
            if (email instanceof HtmlEmail) {
                String attachmentName = evaluateAttribute("name", context);
                if (CommonUtil.isEmptyString(attachmentName)) {
                    attachmentName = mailPlugin.getNextAttachmentName();
                }
                String mimeType = evaluateAttribute("mimetype", context);
                boolean isInline = evaluateAttributeAsBoolean("inline", false, context);
                HtmlEmail htmlEmail = (HtmlEmail) email;
                Variable bodyVar = executeBody(context);
                try {
                    if (CommonUtil.isEmptyString(mimeType)) {
                        mimeType = isInline ? "image/jpeg" : "application/octet-stream";
                    }
                    DataSource dataSource = MailPlugin.createDataSourceOfVariable(bodyVar, context.getCharset(), mimeType);
                    String cid = htmlEmail.embed(dataSource, attachmentName);
                    return isInline ? new NodeVariable("cid:" + cid) : EmptyVariable.INSTANCE;
                } catch (IOException e) {
                    throw new MailPluginException(e);
                } catch (EmailException e) {
                    throw new MailPluginException(e);
                }
            } else {
                throw new MailPluginException("Cannot use mail attach plugin if mail type is not html!");
            }
        } else {
            throw new MailPluginException("Cannot use mail attach plugin out of mail plugin context!");
        }
    }

    public String[] getValidAttributes() {
        return new String[]{"name", "mimetype", "inline"};
    }

    public String[] getAttributeValueSuggestions(String attributeName) {
        if ("mimetype".equalsIgnoreCase(attributeName)) {
            return WHConstants.MIME_TYPES;
        } else if ("inline".equalsIgnoreCase(attributeName)) {
            return new String[]{"true", "false"};
        }
        return null;
    }


}