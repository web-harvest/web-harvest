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
import java.time.Duration;

import javax.activation.DataSource;

import org.apache.commons.mail.ByteArrayDataSource;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Email sending processor - SMTP email support with HTML, attachments, and authentication.
 * 
 * <p>
 * The {@code <mail>} processor sends emails via SMTP servers. Supports plain text and HTML
 * emails with authentication for Gmail, Office365, and custom SMTP servers.
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Example 1: Simple text email with friendly sender name (Bug #4)</h3>
 * <pre>{@code
 * <mail smtp-host="smtp.gmail.com"
 *       smtp-port="587"
 *       username="user@gmail.com"
 *       password="app-password"
 *       from="scraper@example.com"
 *       from-name="WebHarvest Scraper"
 *       to="admin@example.com"
 *       subject="Scraping completed">
 *     
 *     Scraping job completed successfully.
 *     Products extracted: 150
 *     Time: ${_date}
 * </mail>
 * <!-- Recipient sees: "WebHarvest Scraper <scraper@example.com>" -->
 * }</pre>
 * 
 * <h3>Example 2: HTML email with scraping results</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="products">
 *   <xpath expression="//product">
 *     <html-to-xml>
 *       <http url="https://example.com/products"/>
 *     </html-to-xml>
 *   </xpath>
 * </def>
 * 
 * <mail smtp-host="smtp.gmail.com"
 *       smtp-port="587"
 *       username="user@gmail.com"
 *       password="app-password"
 *       from="scraper@example.com"
 *       to="admin@example.com"
 *       subject="Daily Product Report"
 *       type="html">
 *     
 *     <template>
 *       <![CDATA[
 *       <html>
 *         <body>
 *           <h2>Daily Scraping Report</h2>
 *           <p><strong>Products found:</strong> ${productCount}</p>
 *           <p><strong>Timestamp:</strong> ${_date}</p>
 *         </body>
 *       </html>
 *       ]]>
 *     </template>
 * </mail>
 * </config>
 * }</pre>
 * 
 * <h3>Example 3: Error notification email</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <try>
 *   <http url="https://api.example.com/data"/>
 * </try>
 * <catch>
 *   <mail smtp-host="smtp.example.com"
 *         from="scraper@example.com"
 *         to="admin@example.com"
 *         subject="[ERROR] Scraping Failed">
 *       
 *       Scraping job failed with error:
 *       ${_error}
 *       
 *       Time: ${_date}
 *   </mail>
 *   <exit/>
 * </catch>
 * </config>
 * }</pre>
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><strong>smtp-host:</strong> SMTP server hostname (required)</li>
 *   <li><strong>smtp-port:</strong> SMTP port (default: 25, use 587 for TLS)</li>
 *   <li><strong>username:</strong> SMTP authentication username</li>
 *   <li><strong>password:</strong> SMTP authentication password</li>
 *   <li><strong>from:</strong> Sender email address (required)</li>
 *   <li><strong>from-name:</strong> Friendly sender name (Bug #4, v2.2.0) - displayed as "Name &lt;email@example.com&gt;"</li>
 *   <li><strong>to:</strong> Recipient email address (required)</li>
 *   <li><strong>subject:</strong> Email subject (required)</li>
 *   <li><strong>type:</strong> Email type: "text" or "html" (default: text)</li>
 * </ul>
 * 
 * <h2>SMTP Servers</h2>
 * <ul>
 *   <li><strong>Gmail:</strong> smtp.gmail.com:587 (requires App Password)</li>
 *   <li><strong>Office365:</strong> smtp.office365.com:587</li>
 *   <li><strong>Custom:</strong> Any SMTP server with authentication</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Gmail security:</strong> Use App Passwords, not account password</li>
 *   <li><strong>TLS port:</strong> Use 587 for TLS, 465 for SSL, 25 for plain</li>
 *   <li><strong>HTML emails:</strong> Set type="html" for HTML content</li>
 *   <li><strong>Template support:</strong> All attributes support variable interpolation</li>
 * </ul>
 * 
 * <h2>Related Processors</h2>
 * <ul>
 *   <li>{@code <http>} - HTTP requests</li>
 *   <li>{@code <file>} - File I/O for attachments</li>
 *   <li>{@code <template>} - HTML email generation</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
 */
@CorePlugin(elementName = "mail")
public class MailPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailPlugin.class);

    private static final String SMTP_HOST_ATT_NAME = "smtp-host";
    private static final String SMTP_PORT_ATT_NAME = "smtp-port";
    private static final String FROM_ATT_NAME = "from";
    private static final String FROM_NAME_ATT_NAME = "from-name";  // Bug #4: Friendly sender name
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
                    return new String[] { SMTP_PORT_ATT_NAME, TYPE_ATT_NAME, FROM_NAME_ATT_NAME };  // Bug #4
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
        String fromName = config.getProperty(FROM_NAME_ATT_NAME);  // Bug #4: Friendly sender name
        String to = config.getProperty(TO_ATT_NAME);
        String subject = config.getProperty(SUBJECT_ATT_NAME);
        String type = config.getProperty(TYPE_ATT_NAME, "text");
        
        // Evaluate template expressions
        smtpHost = evaluateTemplate(smtpHost, context);
        from = evaluateTemplate(from, context);
        fromName = evaluateTemplate(fromName, context);  // Bug #4: Expand template in from-name
        to = evaluateTemplate(to, context);
        subject = evaluateTemplate(subject, context);
        
        LOGGER.debug("Mail Plugin - SMTP: " + smtpHost + ":" + smtpPort + ", From: " + from + ", To: " + to + ", Subject: " + subject);

        boolean sendStarted = false;
        boolean sendConfirmed = false;
        try {
            Email email;
            if ("html".equalsIgnoreCase(type)) {
                email = new HtmlEmail();
            } else {
                email = new SimpleEmail();
            }
            
            email.setHostName(smtpHost);
            email.setSmtpPort(smtpPort);
            
            // Bug #4: Set friendly sender name if provided
            if (fromName != null && !fromName.trim().isEmpty()) {
                email.setFrom(from, fromName);
                LOGGER.debug("Mail from: {} <{}>", fromName, from);
            } else {
                email.setFrom(from);
                LOGGER.debug("Mail from: {}", from);
            }
            
            email.addTo(to);
            email.setSubject(subject);
            
            // Get body content
            String bodyContent = getBodyContent(context);
            setBodyContent(email, bodyContent, "html".equalsIgnoreCase(type));
            
            // Commons Email defaults to 60-second socket timeouts. Keep SMTP
            // connection and I/O waits finite and expose the underlying socket to
            // the execution owner through the session's SocketFactory instance.
            email.setSocketConnectionTimeout(Duration.ofMillis(SmtpSocketResource.CONNECT_TIMEOUT_MILLIS));
            email.setSocketTimeout(Duration.ofMillis(SmtpSocketResource.READ_TIMEOUT_MILLIS));
            javax.mail.Session mailSession = email.getMailSession();
            TrackingSmtpSocketFactory socketFactory = new TrackingSmtpSocketFactory();
            mailSession.getProperties().put("mail.smtp.socketFactory", socketFactory);
            // Do not let JavaMail silently fall back to an untracked raw socket.
            mailSession.getProperties().setProperty("mail.smtp.socketFactory.fallback", "false");
            mailSession.getProperties().setProperty(
                "mail.smtp.writetimeout", Integer.toString(SmtpSocketResource.WRITE_TIMEOUT_MILLIS));

            try (SmtpSocketResource smtpResource = SmtpSocketResource.open(context, socketFactory)) {
                sendStarted = true;
                String messageId = email.send();
                sendConfirmed = true;
                LOGGER.debug("Mail sent successfully with message ID: " + messageId);
                return new NodeVariable("Email sent successfully. Message ID: " + messageId);
            }
            
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw interrupted;
        } catch (EmailException e) {
            LOGGER.error("Failed to send email", e);
            MailPluginException.DeliveryDisposition disposition = sendStarted
                ? MailPluginException.DeliveryDisposition.UNKNOWN
                : MailPluginException.DeliveryDisposition.NOT_STARTED;
            MailPluginException failure = new MailPluginException(
                "SMTP delivery disposition: " + disposition, disposition, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), failure);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in mail plugin", e);
            MailPluginException.DeliveryDisposition disposition = sendConfirmed
                ? MailPluginException.DeliveryDisposition.CONFIRMED
                : sendStarted ? MailPluginException.DeliveryDisposition.UNKNOWN
                    : MailPluginException.DeliveryDisposition.NOT_STARTED;
            MailPluginException failure = new MailPluginException(
                "SMTP delivery disposition: " + disposition, disposition, e);
            throw new RuntimeException("Unexpected error in mail plugin: " + e.getMessage(), failure);
        }
    }

    static void setBodyContent(Email email, String bodyContent, boolean html) throws EmailException {
        // Commons Email 1.6.0 rejects empty strings in setMsg/setHtmlMsg.
        // Preserve WebHarvest's previous behavior: an omitted body is a valid empty message.
        if (bodyContent == null || bodyContent.isEmpty()) {
            return;
        }
        if (html) {
            ((HtmlEmail) email).setHtmlMsg(bodyContent);
        } else {
            email.setMsg(bodyContent);
        }
    }

    private String getBodyContent(DynamicScopeContext context) throws InterruptedException, PluginException {
        Variable bodyContent = executeBody(context);
        if (bodyContent == null || bodyContent instanceof EmptyVariable) {
            return "";
        }
        return bodyContent.toString(context.getCharset());
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










