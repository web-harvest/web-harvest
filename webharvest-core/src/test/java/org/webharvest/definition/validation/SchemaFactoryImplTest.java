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

import static org.testng.AssertJUnit.assertFalse;

import javax.xml.validation.Schema;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.mock.Mock;

public class SchemaFactoryImplTest extends UnitilsTestNG {

    private Mock<SchemaResolverPostProcessor> postProcessor;

    private SchemaFactoryImpl schemaFactory;

    @BeforeMethod
    public void setUp() {
        schemaFactory = SchemaFactoryImpl.INSTANCE;
    }

    @AfterMethod
    public void tearDown() {
        schemaFactory = null;
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testAddNullPostProcessor() {
        schemaFactory.addPostProcessor(null);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testRegisterNullSchemaSource() {
        schemaFactory.registerSchemaSource(null);
    }

    @Test
    public void postProcessorExecutedOnRefresh() {
        schemaFactory.addPostProcessor(postProcessor.getMock());

        schemaFactory.refresh();

        postProcessor.assertInvoked().postProcess(schemaFactory);
    }

    @Test
    public void createsNewSchemaOnRefresh() {
        final Schema previous = schemaFactory.getSchema();
        schemaFactory.refresh();
        final Schema current = schemaFactory.getSchema();

        assertFalse("New instance of schema expected", previous == current);
    }

}
