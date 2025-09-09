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

package org.webharvest.definition.validation;

import static org.easymock.EasyMock.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.io.ByteArrayInputStream;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.webharvest.TransformationException;
import org.webharvest.Transformer;

public class SchemaResourcesPostProcessorTest extends UnitilsTestNG {

    private static final String RESOURCE1 = "resource1";
    private static final String RESOURCE2 = "resource2";
    private static final String INCORRECT_RESOURCE = "incorrect";

    @RegularMock
    private Transformer<String, SchemaSource> mockTransformer;
    @RegularMock
    private SchemaResolver mockResolver;
    @RegularMock
    private Logger mockLogger;

    private SchemaResourcesPostProcessor<String> postProcessor;

    @BeforeMethod
    public void setUp() throws Exception {
        final Field field =
            SchemaResourcesPostProcessor.class.getDeclaredField("LOG");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, mockLogger);

        postProcessor = new SchemaResourcesPostProcessor<String>(
                mockTransformer, RESOURCE1, RESOURCE2);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        postProcessor = null;
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testConstructorIfNullTransformer() {
        new SchemaResourcesPostProcessor<String>(null, RESOURCE1, RESOURCE2);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testConstructorIfNullResourceArray() {
        new SchemaResourcesPostProcessor<String>(mockTransformer, null);
    }

    @Test
    public void testProcessIfFirstResourceIsIncorrect() throws Exception {
        final SchemaSource source =
            new SchemaSource(new ByteArrayInputStream(RESOURCE2.getBytes()), RESOURCE2);
        postProcessor = new SchemaResourcesPostProcessor<String>(
                mockTransformer, INCORRECT_RESOURCE, RESOURCE2);

        expect(mockTransformer.transform(same(INCORRECT_RESOURCE))).andThrow(
                new TransformationException(new RuntimeException()));
        mockLogger.error("Transformation of {} resource failed.",
                INCORRECT_RESOURCE);
        expectLastCall();
        expect(mockTransformer.transform(same(RESOURCE2))).andReturn(source);
        mockResolver.registerSchemaSource(same(source));
        expectLastCall();

        EasyMockUnitils.replay();

        postProcessor.postProcess(mockResolver);
    }

    @Test
    public void testProcess() throws Exception {
        final SchemaSource source1 =
            new SchemaSource(new ByteArrayInputStream(RESOURCE1.getBytes()), RESOURCE1);
        final SchemaSource source2 =
            new SchemaSource(new ByteArrayInputStream(RESOURCE2.getBytes()), RESOURCE2);

        expect(mockTransformer.transform(same(RESOURCE1))).andReturn(source1);
        mockResolver.registerSchemaSource(same(source1));
        expectLastCall();
        expect(mockTransformer.transform(same(RESOURCE2))).andReturn(source2);
        mockResolver.registerSchemaSource(same(source2));
        expectLastCall();

        EasyMockUnitils.replay();

        postProcessor.postProcess(mockResolver);
    }
}
