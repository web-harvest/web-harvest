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

package org.webharvest.ioc;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.testng.annotations.Test;

import com.google.inject.TypeLiteral;

/**
 * Test for TypeMatchers utility class.
 * 
 * @since 2.2.0
 */
public class TypeMatchersTest {

    // Test annotation
    @Retention(RetentionPolicy.RUNTIME)
    @interface TestAnnotation {
    }

    // Test classes for inheritance testing
    static class BaseClass {
    }

    static class SubClass extends BaseClass {
    }

    static class UnrelatedClass {
    }

    @TestAnnotation
    static class AnnotatedClass {
    }

    static class NonAnnotatedClass {
    }

    @Test
    public void testSubclassesOfWithSameClass() {
        var matcher = TypeMatchers.subclassesOf(BaseClass.class);
        var typeLiteral = TypeLiteral.get(BaseClass.class);
        
        assertTrue(matcher.matches(typeLiteral), "Same class should match");
    }

    @Test
    public void testSubclassesOfWithSubclass() {
        var matcher = TypeMatchers.subclassesOf(BaseClass.class);
        var typeLiteral = TypeLiteral.get(SubClass.class);
        
        assertTrue(matcher.matches(typeLiteral), "Subclass should match");
    }

    @Test
    public void testSubclassesOfWithUnrelatedClass() {
        var matcher = TypeMatchers.subclassesOf(BaseClass.class);
        var typeLiteral = TypeLiteral.get(UnrelatedClass.class);
        
        assertFalse(matcher.matches(typeLiteral), "Unrelated class should not match");
    }

    @Test
    public void testAnnotatedWithWithAnnotatedClass() {
        var matcher = TypeMatchers.annotatedWith(TestAnnotation.class);
        var typeLiteral = TypeLiteral.get(AnnotatedClass.class);
        
        assertTrue(matcher.matches(typeLiteral), "Annotated class should match");
    }

    @Test
    public void testAnnotatedWithWithNonAnnotatedClass() {
        var matcher = TypeMatchers.annotatedWith(TestAnnotation.class);
        var typeLiteral = TypeLiteral.get(NonAnnotatedClass.class);
        
        assertFalse(matcher.matches(typeLiteral), "Non-annotated class should not match");
    }

    @Test
    public void testAnnotatedWithWithNullAnnotation() {
        var matcher = TypeMatchers.annotatedWith(TestAnnotation.class);
        var typeLiteral = TypeLiteral.get(Object.class);
        
        assertFalse(matcher.matches(typeLiteral), "Object class should not match TestAnnotation");
    }
}





