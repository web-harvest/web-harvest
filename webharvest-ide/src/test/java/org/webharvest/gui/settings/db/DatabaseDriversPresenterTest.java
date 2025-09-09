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

package org.webharvest.gui.settings.db;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;

import java.io.File;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.unitils.inject.annotation.InjectInto;
import org.unitils.inject.annotation.TestedObject;
import org.webharvest.runtime.database.DriverManager;

public class DatabaseDriversPresenterTest extends UnitilsTestNG {

    private static final String LOCATION = "/tmp/driver.jar";

    @RegularMock
    @InjectInto(property = "driverManager")
    private DriverManager mockDriverManager;

    @RegularMock
    private DatabaseDriversView mockView;

    private DatabaseDriverDTO dto;

    @TestedObject
    private DatabaseDriversPresenter presenter;

    @BeforeMethod
    public void setUp() {
        this.dto = new DatabaseDriverDTO(LOCATION);
        this.presenter = new DatabaseDriversPresenter(mockView);
    }

    @AfterMethod
    public void tearDown() {
        this.dto = null;
        this.presenter = null;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullViewNotAllowed() {
        new DatabaseDriversPresenter(null);
    }

    @Test
    public void testRegisterDriver() {
        mockView.addToList(same(dto));
        expectLastCall();
        mockDriverManager.addDriverResource(eq(new File(LOCATION).toURI()));
        expectLastCall();

        EasyMockUnitils.replay();

        presenter.registerDriver(dto);
    }

    @Test
    public void testUnregisterDriver() {
        mockView.removeFromList(same(dto));
        expectLastCall();
        mockDriverManager.removeDriverResource(eq(new File(LOCATION).toURI()));
        expectLastCall();

        EasyMockUnitils.replay();

        presenter.unregisterDriver(dto);
    }
}
