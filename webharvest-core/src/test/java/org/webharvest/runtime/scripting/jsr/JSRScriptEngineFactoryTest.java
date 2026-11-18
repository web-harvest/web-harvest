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

package org.webharvest.runtime.scripting.jsr;

import static org.testng.Assert.assertNotNull;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.scripting.ScriptEngine;
import org.webharvest.runtime.scripting.ScriptSource;
import org.webharvest.runtime.scripting.ScriptingLanguage;

// We can't do more as testing if engines for all scripting languages declared
// in web harvest documentation as supported are accessible...
public class JSRScriptEngineFactoryTest {

    private JSRScriptEngineFactory factory;

    @BeforeMethod
    public void setUp() {
        this.factory = new JSRScriptEngineFactory();
    }

    @AfterMethod
    public void tearDown() {
        this.factory = null;
    }

    @Test
    public void getJSRBeanshellEngine() {
        final ScriptEngine engine = factory.getEngine(
                new ScriptSource("a = 2", ScriptingLanguage.BEANSHELL));
        assertNotNull(engine, "Null script engine");
    }

    @Test
    public void getGroovyEngine() {
        final ScriptEngine engine = factory.getEngine(new ScriptSource(
                "def name = 'mashup'", ScriptingLanguage.GROOVY));
        assertNotNull(engine, "Null script engine");
    }

    @Test
    public void getJavascriptEngine() {
        // JavaScript is a declared runtime capability. Missing its JSR-223
        // provider must fail the build rather than silently skip this test.
        final ScriptEngine engine = factory.getEngine(new ScriptSource(
                "var x = 123", ScriptingLanguage.JAVASCRIPT));
        assertNotNull(engine, "JavaScript JSR-223 engine provider must be available");
    }
}
