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

package org.webharvest.utils;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ClassPathScannerImplTest {

    private ClassPathScannerImpl scanner;

    @BeforeMethod
    public void setUp() {
        this.scanner = new ClassPathScannerImpl(
                this.getClass().getPackage().getName());
    }

    @AfterMethod
    public void tearDown() {
        this.scanner = null;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void doesNotAllowNullPackage() {
        new ClassPathScannerImpl(null);
    }

    @Test
    public void scanAnnotatedTypes() {
        final Set<Class< ? >> results = scanner
                .getTypesAnnotatedWith(MyAnnotation.class);
        assertNotNull("Null set of results", results);
        assertEquals("One element expected", 1, results.size());
        assertEquals("Unexpected element", AnnotatedClass.class, results
                .iterator().next());
    }

    @Test
    public void scanAnnotatedTypesAtUnknownPackage() {
        final Set<Class< ? >> results = new ClassPathScannerImpl(
                "not.existing.package")
                .getTypesAnnotatedWith(MyAnnotation.class);

        assertNotNull("Null set of results", results);
        assertTrue("Empty set expected", results.isEmpty());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface MyAnnotation {
    }

    @MyAnnotation
    static class AnnotatedClass {
    }

    static class NotAnnotatedClass {
    }
}
