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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Runtime contracts for the Log plugin.
 */
public class LogPluginRuntimeContractIntegrationTest extends XmlBasedIntegrationTestBase {

    @Test(groups = {"integration", "core-plugins"})
    public void writesXmlBodyToConfiguredStreamsAndReturnsTheMessage() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <def var=\"stdoutMessage\"><log dest=\"out\">stdout-runtime-contract</log></def>"
            + "  <def var=\"stderrMessage\"><log dest=\"err\">stderr-runtime-contract</log></def>"
            + "</config>";

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        try (PrintStream capturedOut = new PrintStream(stdout, true, "UTF-8");
             PrintStream capturedErr = new PrintStream(stderr, true, "UTF-8")) {
            System.setOut(capturedOut);
            System.setErr(capturedErr);

            DynamicScopeContext context = executeXmlString(xml);

            capturedOut.flush();
            capturedErr.flush();
            assertTrue(stdout.toString("UTF-8").contains("stdout-runtime-contract"),
                "The XML log with dest=out must reach stdout");
            assertTrue(stderr.toString("UTF-8").contains("stderr-runtime-contract"),
                "The XML log with dest=err must reach stderr");
            assertEquals(context.getVar("stdoutMessage").toString(), "stdout-runtime-contract",
                "The stdout log must return its original message");
            assertEquals(context.getVar("stderrMessage").toString(), "stderr-runtime-contract",
                "The stderr log must return its original message");
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
}
