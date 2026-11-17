/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior written permission.
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
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.webharvest.plugin.integration;

import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Runtime contracts for the Empty plugin.
 */
public class EmptyPluginRuntimeContractIntegrationTest extends XmlBasedIntegrationTestBase {

    @Test(groups = {"integration", "core-plugins"})
    public void executesNestedPluginsForSideEffectsAndReturnsEmptyWithoutStoppingParentExecution()
            throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <def var=\"emptyResult\">"
            + "    <empty>"
            + "      <def var=\"firstInsideEmpty\" value=\"first-ran\"/>"
            + "      <def var=\"secondInsideEmpty\" value=\"second-ran\"/>"
            + "    </empty>"
            + "  </def>"
            + "  <def var=\"afterEmpty\" value=\"continued\"/>"
            + "</config>";

        DynamicScopeContext context = executeXmlString(xml);

        assertEquals(context.getVar("firstInsideEmpty").toString(), "first-ran",
            "Empty must execute its first child for side effects");
        assertEquals(context.getVar("secondInsideEmpty").toString(), "second-ran",
            "Empty must execute all child processors for side effects");
        assertNotNull(context.getVar("emptyResult"),
            "The enclosing Def must receive Empty's result");
        assertTrue(context.getVar("emptyResult") instanceof EmptyVariable,
            "Empty must discard the aggregate body result and return EmptyVariable");
        assertEquals(context.getVar("afterEmpty").toString(), "continued",
            "Parent execution must continue after Empty completes");
    }
}
