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

import org.webharvest.definition.FileConfigSource;
import org.webharvest.definition.SAXConfigParser;
import org.webharvest.definition.XMLConfig;
import org.webharvest.definition.validation.SchemaComponentFactory;
import org.webharvest.definition.validation.SchemaResolver;

import java.io.File;

public class TestRunnerWithSchemaRefresh {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java TestRunnerWithSchemaRefresh <config-file>");
            System.exit(1);
        }
        
        String configFile = args[0];
        System.out.println("Running configuration: " + configFile);
        
        try {
            // Force schema refresh before parsing
            System.out.println("Refreshing schema resolver...");
            SchemaResolver schemaResolver = SchemaComponentFactory.getSchemaResolver();
            schemaResolver.refresh();
            
            // Create configuration
            FileConfigSource configSource = new FileConfigSource(new File(configFile));
            SAXConfigParser parser = new SAXConfigParser();
            XMLConfig config = new XMLConfig(configSource, parser);
            
            // Load configuration
            config.reload();
            
            System.out.println("Configuration loaded successfully!");
            System.out.println("Version: " + config.getVersion());
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
