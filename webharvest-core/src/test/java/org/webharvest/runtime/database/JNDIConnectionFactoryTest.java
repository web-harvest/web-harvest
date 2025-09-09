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

package org.webharvest.runtime.database;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.fail;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.webharvest.exception.DatabaseException;

public class JNDIConnectionFactoryTest extends UnitilsTestNG {

    private JNDIConnectionFactory factory;

    private Context mockContext;

    private DataSource mockDataSource;

    private Connection mockConnection;

    @BeforeMethod
    public void setUp() {
        this.mockDataSource = EasyMockUnitils.createMock(DataSource.class);
        this.mockConnection = EasyMockUnitils.createMock(Connection.class);
        this.mockContext = EasyMockUnitils.createRegularMock(Context.class);

        this.factory = new JNDIConnectionFactory() {
            protected Context createContext()
                    throws javax.naming.NamingException {
                return mockContext;
            };
        };
    }

    @AfterMethod
    public void tearDown() {
        this.mockDataSource = null;
        this.mockConnection = null;
        this.mockContext = null;
        this.factory = null;
    }

    @Test
    public void getConnectionForExistingHook() throws Exception {
        expect(mockContext.lookup(eq("java:comp/env/hook")))
            .andReturn(mockDataSource);
        expect(mockDataSource.getConnection()).andReturn(mockConnection);

        EasyMockUnitils.replay();
        final Connection connection = factory.getConnection("hook");
        assertSame("Unexpected connection returned",
                mockConnection, connection);
    }

    @Test
    public void getConnectionWhenMissingHook() throws Exception {
        expect(mockContext.lookup(eq("java:comp/env/notexisting"))).andThrow(
                new NamingException("TEST"));

        EasyMockUnitils.replay();
        try {
            factory.getConnection("notexisting");
            fail("DatabaseException expected");
        } catch (DatabaseException e) {
            // ok, expected
        }
    }

    @Test
    public void getConnectionWhenDatasourceFails() throws Exception {
        expect(mockContext.lookup(eq("java:comp/env/jdbc/hook")))
            .andReturn(mockDataSource);
        expect(mockDataSource.getConnection()).andThrow(
                new SQLException("TEST"));

        EasyMockUnitils.replay();
        try {
            factory.getConnection("jdbc/hook");
            fail("DatabaseException expected");
        } catch (DatabaseException e) {
            // ok, expected
        }
    }

    @Test
    public void getConnectionForFullyQualifiedHookName() throws Exception {
        expect(mockContext.lookup(eq("java:env/db/book"))).andReturn(
                mockDataSource);
        expect(mockDataSource.getConnection()).andReturn(mockConnection);

        EasyMockUnitils.replay();
        final Connection connection = factory.getConnection("java:env/db/book");
        assertSame("Unexpected connection returned",
                mockConnection, connection);
    }

    @Test(expectedExceptions = DatabaseException.class)
    public void doNotCreateFactoryWhenContextFails() {
        new JNDIConnectionFactory() {
            protected Context createContext()
                    throws javax.naming.NamingException {
                throw new NamingException("TEST");
            };
        };
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void createConnectionForParametersUnsupported() throws Exception {
        factory.getConnection("com.my.Driver", "jdbc:my://naughty:3306/url",
                null, null);
    }
}
