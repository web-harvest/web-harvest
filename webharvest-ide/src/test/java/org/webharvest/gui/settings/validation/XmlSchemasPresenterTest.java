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

package org.webharvest.gui.settings.validation;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.easymock.EasyMock.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.unitils.inject.annotation.InjectInto;
import org.unitils.inject.annotation.TestedObject;
import org.webharvest.definition.validation.SchemaResolver;
import org.webharvest.gui.Settings;

public class XmlSchemasPresenterTest extends UnitilsTestNG {

    private static final String SAMPLE_LOCATION = "/aaa.xsd";
    private static final String SAMPLE_OTHER_LOCATION = "/bbb.xsd";
    private static final String SAMPLE_ANOTHER_LOCATION = "/ccc.xsd";

    @RegularMock
    @InjectInto(property = "schemaResolver")
    private SchemaResolver mockSchemaResolver;

    @RegularMock
    private XmlSchemasView mockView;

    @TestedObject
    private XmlSchemasPresenter presenter;

    @BeforeMethod
    public void setUp() throws Exception {
        presenter = new XmlSchemasPresenter(mockView);

        final Set<XmlSchemaDTO> dtoSet = new HashSet<XmlSchemaDTO>(
                Arrays.asList(new XmlSchemaDTO(SAMPLE_LOCATION)));
        final Field field = presenter.getClass().getDeclaredField("schemaDTOs");
        field.setAccessible(true);
        field.set(presenter, dtoSet);
    }

    @AfterMethod
    public void tearDown() {
        presenter = null;
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testRegisterSchemaIfNullSchema() {
        presenter.registerSchema(null);
    }

    @Test
    public void testRegisterExistingSchema() {
        EasyMockUnitils.replay();

        presenter.registerSchema(new XmlSchemaDTO(SAMPLE_LOCATION));
    }

    @Test
    public void testRegisterSchema() {
        final XmlSchemaDTO dto = new XmlSchemaDTO(SAMPLE_OTHER_LOCATION);

        mockView.addToList(same(dto));
        expectLastCall();

        EasyMockUnitils.replay();

        presenter.registerSchema(dto);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testUnregisterSchemaIfNullSchema() {
        presenter.unregisterSchema(null);
    }

    @Test
    public void testUnregisterUnexistingSchema() {
        EasyMockUnitils.replay();

        presenter.unregisterSchema(new XmlSchemaDTO(SAMPLE_OTHER_LOCATION));
    }

    @Test
    public void testUnregisterSchema() {
        final XmlSchemaDTO dto = new XmlSchemaDTO(SAMPLE_LOCATION);

        mockView.removeFromList(same(dto));
        expectLastCall();

        EasyMockUnitils.replay();

        presenter.unregisterSchema(dto);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testOnLoadIfNullSettings() {
        presenter.onLoad(null);
    }

    @Test
    public void testOnLoad() {
        final XmlSchemaDTO dto1 = new XmlSchemaDTO(SAMPLE_OTHER_LOCATION);
        final XmlSchemaDTO dto2 = new XmlSchemaDTO(SAMPLE_ANOTHER_LOCATION);
        final Settings mockSettings = new MockSettings(dto1, dto2);

        mockView.addToList(same(dto1));
        expectLastCall();
        mockView.addToList(same(dto2));
        expectLastCall();

        EasyMockUnitils.replay();

        presenter.onLoad(mockSettings);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testOnUpdateIfNullSettings() {
        presenter.onUpdate(null);
    }

    @Test
    public void testOnUpdate() {
        final Settings mockSettings = new MockSettings();

        mockSchemaResolver.refresh();
        expectLastCall();

        EasyMockUnitils.replay();

        presenter.onUpdate(mockSettings);

        final XmlSchemaDTO[] schemas = mockSettings.getXmlSchemas();
        assertNotNull("Returned schema array is null.", schemas);
        assertEquals("Incorrect size of schema array", 1, schemas.length);
        assertEquals("Unexpected location of the schema.", SAMPLE_LOCATION,
                schemas[0].getLocation());
    }

    private class MockSettings extends Settings {

        private XmlSchemaDTO[] schemas;

        public MockSettings(XmlSchemaDTO...schemas) {
            this.schemas = schemas;
        }

        @Override
        public XmlSchemaDTO[] getXmlSchemas() {
            return schemas;
        }

        @Override
        public void setXmlSchemas(final XmlSchemaDTO[] xmlSchemas) {
            this.schemas = xmlSchemas;
        }

    }

}
