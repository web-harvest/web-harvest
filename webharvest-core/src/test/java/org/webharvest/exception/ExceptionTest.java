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
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.exception;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/**
 * Unit tests for exception classes.
 * 
 * @author Robert Bala
 * @since 2.2.0-SNAPSHOT
 */
public class ExceptionTest {
    
    @Test
    public void testBaseExceptionWithMessage() {
        String message = "Test exception message";
        BaseException exception = new BaseException(message);
        
        assertEquals(exception.getMessage(), message);
        assertNull(exception.getCause());
    }
    
    @Test
    public void testBaseExceptionWithMessageAndCause() {
        String message = "Test exception message";
        Throwable cause = new RuntimeException("Root cause");
        BaseException exception = new BaseException(message, cause);
        
        assertEquals(exception.getMessage(), message);
        assertEquals(exception.getCause(), cause);
    }
    
    @Test
    public void testConfigurationException() {
        String message = "Configuration error";
        ConfigurationException exception = new ConfigurationException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testDatabaseException() {
        String message = "Database error";
        DatabaseException exception = new DatabaseException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testEnvironmentException() {
        Exception cause = new RuntimeException("Environment error");
        EnvironmentException exception = new EnvironmentException(cause);
        
        assertEquals(exception.getCause(), cause);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testFileException() {
        String message = "File error";
        FileException exception = new FileException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testFunctionException() {
        String message = "Function error";
        FunctionException exception = new FunctionException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testHttpException() {
        String message = "HTTP error";
        HttpException exception = new HttpException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testParserException() {
        String message = "Parser error";
        ParserException exception = new ParserException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testPluginException() {
        String message = "Plugin error";
        PluginException exception = new PluginException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testScraperXPathException() {
        String message = "XPath error";
        ScraperXPathException exception = new ScraperXPathException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testScraperXQueryException() {
        String message = "XQuery error";
        ScraperXQueryException exception = new ScraperXQueryException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testScriptEngineException() {
        Throwable cause = new RuntimeException("Script engine error");
        ScriptEngineException exception = new ScriptEngineException(cause);
        
        assertEquals(exception.getCause(), cause);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testScriptException() {
        Throwable cause = new RuntimeException("Script error");
        ScriptException exception = new ScriptException(cause);
        
        assertEquals(exception.getCause(), cause);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testTemplateException() {
        String message = "Template error";
        TemplateException exception = new TemplateException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testTemplaterException() {
        String message = "Templater error";
        TemplaterException exception = new TemplaterException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testUserException() {
        String message = "User error";
        UserException exception = new UserException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testVariableException() {
        String message = "Variable error";
        VariableException exception = new VariableException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    @Test
    public void testXsltException() {
        String message = "XSLT error";
        XsltException exception = new XsltException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof BaseException);
    }
    
    // v2.2.x: ErrMsg removed (only used in tests)
    // @Test
    // public void testErrMsg() {
    //     // Test that ErrMsg class is accessible
    //     assertNotNull(ErrMsg.class);
    //     
    //     // Test some common error message methods
    //     String msg1 = ErrMsg.missingAttribute("test", "attr");
    //     assertNotNull(msg1);
    //     assertTrue(msg1.contains("test") || msg1.contains("attr"));
    //     
    //     String msg2 = ErrMsg.invalidAttribute("test", "attr");
    //     assertNotNull(msg2);
    //     assertTrue(msg2.contains("test") || msg2.contains("attr"));
    // }
}
