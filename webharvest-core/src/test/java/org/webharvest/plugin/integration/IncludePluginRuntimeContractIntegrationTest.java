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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;

import static org.testng.Assert.assertEquals;

/**
 * End-to-end Include contract through XML loading and the standard Harvester path.
 */
public class IncludePluginRuntimeContractIntegrationTest extends XmlBasedIntegrationTestBase {

    @Test(groups = {"integration", "core-plugins"})
    public void executesIncludedFileAndContinuesParentConfiguration() throws Exception {
        Path directory = Files.createTempDirectory(Paths.get("target"), "include-runtime-contract-");
        Path mainFile = directory.resolve("main.xml");
        Path includedFile = directory.resolve("included.xml");

        String includedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <def var=\"fromIncludedFile\" value=\"included-value\"/>"
            + "</config>";
        String mainXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <include path=\"included.xml\"/>"
            + "  <def var=\"afterInclude\" value=\"parent-continued\"/>"
            + "</config>";

        try {
            Files.write(includedFile, includedXml.getBytes(StandardCharsets.UTF_8));
            Files.write(mainFile, mainXml.getBytes(StandardCharsets.UTF_8));

            DynamicScopeContext context = executeXmlConfig(mainFile.toAbsolutePath().toString());

            assertEquals(context.getVar("fromIncludedFile").toString(), "included-value",
                "The included XML file must execute in the active runtime context");
            assertEquals(context.getVar("afterInclude").toString(), "parent-continued",
                "The parent XML configuration must continue after Include returns");
        } finally {
            Files.deleteIfExists(mainFile);
            Files.deleteIfExists(includedFile);
            Files.deleteIfExists(directory);
        }
    }
}
