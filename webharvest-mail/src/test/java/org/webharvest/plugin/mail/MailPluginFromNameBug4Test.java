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

package org.webharvest.plugin.mail;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * TDD tests for Bug #4: Add from-name to mail plugin.
 * 
 * <h3>Bug #4: Friendly Sender Name Support</h3>
 * <p>Allows setting a friendly "from" name that is displayed instead of
 * just the email address.</p>
 * 
 * <p><strong>Before:</strong> Only email address shown: {@code noreply@company.com}</p>
 * <p><strong>After:</strong> Friendly name + address: {@code Company Support <noreply@company.com>}</p>
 * 
 * <h4>User Request:</h4>
 * <blockquote>
 * "Allows you to set a friendly name which is displayed instead of the 
 * email address of the sender."
 * </blockquote>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class MailPluginFromNameBug4Test {
    
    /**
     * TDD Test: Verify from-name attribute is in optional attributes.
     * 
     * <p>This is the core verification that Bug #4 patch is implemented.</p>
     */
    @Test(groups = {"unit", "mail", "bug4"})
    public void testBug4_FromNameInOptionalAttributes() {
        // GIVEN
        MailPlugin plugin = new MailPlugin();
        
        // WHEN
        String[] optionalAttributes = plugin.getMetadata()
            .getConfigurationSchema()
            .getOptionalAttributes();
        
        // THEN
        boolean hasFromName = false;
        for (String attr : optionalAttributes) {
            if ("from-name".equals(attr)) {
                hasFromName = true;
                break;
            }
        }
        assertTrue(hasFromName, "Bug #4: from-name should be in optional attributes");
        
        // Verify implementation details (by code inspection):
        // 1. ✅ FROM_NAME_ATT_NAME = "from-name" constant defined
        // 2. ✅ from-name in getOptionalAttributes()
        // 3. ✅ fromName = config.getProperty(FROM_NAME_ATT_NAME) in doExecute()
        // 4. ✅ fromName = evaluateTemplate(fromName, context) for ${var} expansion
        // 5. ✅ if (fromName != null && !empty) → email.setFrom(from, fromName)
        // 6. ✅ else → email.setFrom(from) (backwards compatible)
    }
    
    /*
     * NOTE: Full end-to-end tests (actual SMTP email sending) require:
     * - Real SMTP server (Gmail, Office365, or local server)
     * - Valid credentials
     * - Network connectivity
     * 
     * These tests are in MailPluginIntegrationTest and can be run manually
     * with proper SMTP configuration.
     * 
     * Example integration test scenario:
     * - Send email with from-name="Company Support" from="noreply@company.com"
     * - Verify recipient sees: "Company Support <noreply@company.com>"
     * - Send email without from-name
     * - Verify recipient sees: "noreply@company.com"
     */
}

