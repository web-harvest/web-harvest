/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISING OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.mail;

import java.io.IOException;

import javax.activation.DataSource;

import org.apache.commons.mail.ByteArrayDataSource;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mail plugin for sending emails.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
@CorePlugin(elementName = "mail")
public class MailPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailPlugin.class);

    private static final String SMTP_HOST_ATT_NAME = "smtp-host";
    private static final String SMTP_PORT_ATT_NAME = "smtp-port";
    private static final String FROM_ATT_NAME = "from";
    private static final String TO_ATT_NAME = "to";
    private static final String SUBJECT_ATT_NAME = "subject";
    private static final String TYPE_ATT_NAME = "type";

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getName() {
            return "Mail Plugin";
        }

        @Override
        public String getDescription() {
            return "Plugin for sending emails";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getClassName() {
            return MailPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "mail";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String[] getTags() {
            return new String[0];
        }

        @Override
        public String getCategory() {
            return "Communication";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getId() {
            return "MailPlugin";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new PluginConfigurationSchema() {
                @Override
                public AttributeDefinition[] getAttributeDefinitions() {
                    return new AttributeDefinition[0];
                }

                @Override
                public String[] getRequiredAttributes() {
                    return new String[] { SMTP_HOST_ATT_NAME, FROM_ATT_NAME, TO_ATT_NAME, SUBJECT_ATT_NAME };
                }

                @Override
                public String[] getOptionalAttributes() {
                    return new String[] { SMTP_PORT_ATT_NAME, TYPE_ATT_NAME };
                }

                @Override
                public String getVersion() {
                    return "2.1.0";
                }

                @Override
                public boolean allowsBody() {
                    return true;
                }

                @Override
                public String[] getRequiredSubElements() {
                    return new String[0];
                }

                @Override
                public String[] getAllowedSubElements() {
                    return new String[0];
                }

                @Override
                public AttributeDefinition getAttribute(String name) {
                    return null;
                }

                @Override
                public void validate(PluginConfiguration configuration) throws PluginValidationException {
                    if (configuration == null) {
                        throw new PluginValidationException("Configuration cannot be null");
                    }
                    
                    String[] required = getRequiredAttributes();
                    for (String attr : required) {
                        if (configuration.getProperty(attr) == null || configuration.getProperty(attr).trim().isEmpty()) {
                            throw new PluginValidationException("Required attribute '" + attr + "' is missing or empty");
                        }
                    }
                }
            };
        }
    };

    @Override
    public PluginMetadata getMetadata() {
        return METADATA;
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return METADATA.getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        doValidateConfiguration(configuration);
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        getConfigurationSchema().validate(configuration);
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        
        String smtpHost = config.getProperty(SMTP_HOST_ATT_NAME);
        int smtpPort = Integer.parseInt(evaluateTemplate(config.getProperty(SMTP_PORT_ATT_NAME, "25"), context));
        String from = config.getProperty(FROM_ATT_NAME);
        String to = config.getProperty(TO_ATT_NAME);
        String subject = config.getProperty(SUBJECT_ATT_NAME);
        String type = config.getProperty(TYPE_ATT_NAME, "text");
        
        // Evaluate template expressions
        smtpHost = evaluateTemplate(smtpHost, context);
        from = evaluateTemplate(from, context);
        to = evaluateTemplate(to, context);
        subject = evaluateTemplate(subject, context);
        
        System.out.println("Mail Plugin - SMTP: " + smtpHost + ":" + smtpPort + ", From: " + from + ", To: " + to + ", Subject: " + subject);
        
        try {
            Email email;
            if ("html".equalsIgnoreCase(type)) {
                email = new HtmlEmail();
            } else {
                email = new SimpleEmail();
            }
            
            email.setHostName(smtpHost);
            email.setSmtpPort(smtpPort);
            email.setFrom(from);
            email.addTo(to);
            email.setSubject(subject);
            
            // Get body content
            String bodyContent = getBodyContent(context);
            if ("html".equalsIgnoreCase(type)) {
                ((HtmlEmail) email).setHtmlMsg(bodyContent);
            } else {
                email.setMsg(bodyContent);
            }
            
            String messageId = email.send();
            System.out.println("Mail sent successfully with message ID: " + messageId);
            
            return new NodeVariable("Email sent successfully. Message ID: " + messageId);
            
        } catch (EmailException e) {
            LOGGER.error("Failed to send email", e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in mail plugin", e);
            throw new RuntimeException("Unexpected error in mail plugin: " + e.getMessage(), e);
        }
    }

    private String getBodyContent(DynamicScopeContext context) throws InterruptedException, PluginException {
        Variable bodyContent = executeBody(context);
        if (bodyContent == null || bodyContent instanceof EmptyVariable) {
            return "";
        }
        return bodyContent.toString();
    }

    public static DataSource createDataSourceOfVariable(Variable variable, String charset, String mimeType) throws IOException {
        if (variable != null) {
            byte[] bytes = variable.toBinary(charset);
            return new ByteArrayDataSource(bytes, mimeType);
        }
        return null;
    }
    
    private String evaluateTemplate(String template, DynamicScopeContext context) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        
        try {
            return org.webharvest.runtime.templaters.BaseTemplater.evaluateToString(template, context.getScriptingLanguage(), context);
        } catch (Exception e) {
            LOGGER.warn("Failed to evaluate template '{}': {}", template, e.getMessage());
            return template; // Return original if evaluation fails
        }
    }
}










