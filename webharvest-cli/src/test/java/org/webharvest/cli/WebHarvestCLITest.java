/*  Copyright (c) 2006-2007, Vladimir Nikic
    All rights reserved.

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

    You can contact Vladimir Nikic by sending e-mail to
    nikic_vladimir@yahoo.com. Please include the word "Web-Harvest" in the
    subject line.
*/
package org.webharvest.cli;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for WebHarvestCLI.
 */
public class WebHarvestCLITest {

    @Test
    public void testGetArgValue() {
        String[] args = {"config=test.xml", "workdir=/tmp", "debug=yes"};
        Map<String, String> params = WebHarvestCLI.getArgValue(args);
        
        assertEquals("test.xml", params.get("config"));
        assertEquals("/tmp", params.get("workdir"));
        assertEquals("yes", params.get("debug"));
    }

    @Test
    public void testGetArgValueCaseInsensitive() {
        String[] args = {"CONFIG=test.xml", "WorkDir=/tmp", "DEBUG=yes"};
        Map<String, String> params = WebHarvestCLI.getArgValue(args);
        
        assertEquals("test.xml", params.get("config"));
        assertEquals("/tmp", params.get("workdir"));
        assertEquals("yes", params.get("debug"));
    }

    @Test
    public void testGetArgValueWithEmptyValues() {
        String[] args = {"config=test.xml", "workdir=", "debug=yes"};
        Map<String, String> params = WebHarvestCLI.getArgValue(args);
        
        assertEquals("test.xml", params.get("config"));
        assertEquals("", params.get("workdir"));
        assertEquals("yes", params.get("debug"));
    }

    @Test
    public void testGetArgValueWithoutEquals() {
        String[] args = {"-h", "config=test.xml"};
        Map<String, String> params = WebHarvestCLI.getArgValue(args);
        
        assertEquals("", params.get("-h"));
        assertEquals("test.xml", params.get("config"));
    }

    @Test
    public void testHelpOutput() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            WebHarvestCLI.main(new String[]{"-h"});
        } catch (Exception e) {
            // Expected to exit
        }
        
        String output = outContent.toString();
        assertTrue("Help should contain 'Web-Harvest Command Line Interface'", 
                   output.contains("Web-Harvest Command Line Interface"));
        assertTrue("Help should contain usage information", 
                   output.contains("Usage:"));
        assertTrue("Help should contain examples", 
                   output.contains("Examples:"));
        
        System.setOut(originalOut);
    }

    @Test
    public void testNoArgumentsShowsHelp() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            WebHarvestCLI.main(new String[]{});
        } catch (Exception e) {
            // Expected to exit
        }
        
        String output = outContent.toString();
        assertTrue("Should show help when no arguments provided", 
                   output.contains("Web-Harvest Command Line Interface"));
        
        System.setOut(originalOut);
    }

    @Test
    public void testMissingConfigShowsError() {
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent));
        
        try {
            WebHarvestCLI.main(new String[]{"workdir=/tmp"});
        } catch (Exception e) {
            // Expected to exit
        }
        
        String output = errContent.toString();
        assertTrue("Should show error for missing config", 
                   output.contains("You must specify configuration file path"));
        
        System.setErr(originalErr);
    }
}
































