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

import static org.webharvest.WHConstants.XMLNS_CORE;
import static org.webharvest.WHConstants.XMLNS_CORE_10;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;

import javax.activation.DataSource;

import org.apache.commons.mail.ByteArrayDataSource;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.webharvest.annotation.Definition;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.processors.plugins.Autoscanned;
import org.webharvest.runtime.processors.plugins.TargetNamespace;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

/**
 * Mail sending processor.
 */
@Autoscanned
@TargetNamespace({ XMLNS_CORE, XMLNS_CORE_10 })
@Definition("mail")
public class MailPlugin extends WebHarvestPlugin {

    public static DataSource createDataSourceOfVariable(Variable variable, String charset, String mimeType) throws IOException {
        if (variable != null) {
            byte[] bytes = variable.toBinary(charset);
            return new ByteArrayDataSource(bytes, mimeType);
        }
        return null;
    }

    private Email email = null;
    private int attachmentCounter = 0;

    public String getName() {
        return "mail";
    }

    public Variable executePlugin(DynamicScopeContext context) throws InterruptedException {
        email = null;

        boolean isHtml = "html".equalsIgnoreCase(evaluateAttribute("type", context));
        if (isHtml) {
            email = new HtmlEmail();
        } else {
            email = new SimpleEmail();
        }

        email.setHostName( evaluateAttribute("smtp-host", context) );
        email.setSmtpPort( evaluateAttributeAsInteger("smtp-port", 25, context) );

        try {
            email.setFrom( evaluateAttribute("from", context) );
        } catch (EmailException e) {
            throw new MailPluginException("Invalid \"from\" email address!", e);
        }

        for ( String replyTo:  CommonUtil.tokenize(evaluateAttribute("reply-to", context), ",") ) {
            try {
                email.addReplyTo(replyTo);
            } catch (EmailException e) {
                throw new MailPluginException("Invalid \"reply-to\" email address!", e);
            }
        }
        for ( String to:  CommonUtil.tokenize(evaluateAttribute("to", context), ",") ) {
            try {
                email.addTo(to);
            } catch (EmailException e) {
                throw new MailPluginException("Invalid \"to\" email address!", e);
            }
        }
        for ( String cc:  CommonUtil.tokenize(evaluateAttribute("cc", context), ",") ) {
            try {
                email.addCc(cc);
            } catch (EmailException e) {
                throw new MailPluginException("Invalid \"cc\" email address!", e);
            }
        }
        for ( String bcc:  CommonUtil.tokenize(evaluateAttribute("bcc", context), ",") ) {
            try {
                email.addBcc(bcc);
            } catch (EmailException e) {
                throw new MailPluginException("Invalid \"bcc\" email address!", e);
            }
        }

        email.setSubject( evaluateAttribute("subject", context) );

        String username = evaluateAttribute("username", context);
        String password = evaluateAttribute("password", context);
        if ( !CommonUtil.isEmptyString(username) ) {
            email.setAuthentication(username, password);
        }

        String security = evaluateAttribute("security", context);
        if ("tls".equals(security)) {
            email.setTLS(true);
        } else if ("ssl".equals(security)) {
            email.setSSL(true);
        }

        String charset = evaluateAttribute("charset", context);
        if (CommonUtil.isEmptyString(charset)) {
            charset = context.getCharset();
        }
        email.setCharset(charset);

        if (isHtml) {
            HtmlEmail htmlEmail = (HtmlEmail) email;
            String htmlContent = executeBody(context).toString();
            try {
                htmlEmail.setHtmlMsg(htmlContent);
            } catch (EmailException e) {
                throw new MailPluginException(e);
            }
        } else {
            try {
                email.setMsg(executeBody(context).toString());
            } catch (EmailException e) {
                throw new MailPluginException(e);
            }
        }

        try {
            email.send();
        } catch (EmailException e) {
            throw new MailPluginException(e);
        }

        email = null;

        return EmptyVariable.INSTANCE;
    }

    public String[] getValidAttributes() {
        return new String[] {
                "smtp-host",
                "smtp-port",
                "type",
                "from",
                "reply-to",
                "to",
                "cc",
                "bcc",
                "subject",
                "charset",
                "username",
                "password",
                "security"
        };
    }

    public String[] getRequiredAttributes() {
        return new String[] {"smtp-host", "from", "to"};
    }

    public String[] getValidSubprocessors() {
        return null;
    }

    public String[] getRequiredSubprocessors() {
        return null;
    }

    public Class[] getDependantProcessors() {
        return new Class[] {
            MailAttachPlugin.class,
        };
    }

    public Email getEmail() {
        return email;
    }

    public String[] getAttributeValueSuggestions(String attributeName) {
        if ("type".equalsIgnoreCase(attributeName)) {
            return new String[] {"html", "text"};
        } else if ("charset".equalsIgnoreCase(attributeName)) {
            Set<String> charsetKeys = Charset.availableCharsets().keySet();
            return new ArrayList<String>(charsetKeys).toArray(new String[charsetKeys.size()]);
        } else if ("security".equalsIgnoreCase(attributeName)) {
            return new String[] {"ssl", "tls", "none"};
        }
        return null;
    }

    protected String getNextAttachmentName() {
        attachmentCounter++;
        return "Attachment " + attachmentCounter;
    }

}