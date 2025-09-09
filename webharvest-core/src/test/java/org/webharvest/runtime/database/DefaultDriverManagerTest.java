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

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultDriverManagerTest {

    private static final String JAR_DRIVER_LOCATION =
        "src/test/resources/MockDBDriver.jar";
    private static final String JAR_DRIVER_CLASS = "MockJarDriver";
    private static final String JAR_DRIVER_URL_ACCEPTED =
        "jdbc:dummydb:jarhost";

    private final DefaultDriverManager manager = DefaultDriverManager.INSTANCE;

    @BeforeMethod
    public void setUp() {
        manager.addDriverResource(new File(JAR_DRIVER_LOCATION).toURI());

    }

    @AfterMethod
    public void tearDown() {
        manager.removeDriverResource(new File(JAR_DRIVER_LOCATION).toURI());
    }

    /**
     * Tests if driver available on classpath ({@link MockClasspathDriver})
     * can be successfully registered by manager.
     */
    @Test
    public void registerDriverFromClassPath() throws Exception {
        manager.registerDriver(MockClasspathDriver.class.getName());
        assertNotNull("Driver has not been registered",
                DriverManager.getDriver(MockClasspathDriver.URL_ACCEPTED));
    }

    /**
     * Tests if in case of registration of driver not available on classpath
     * nor in any of previously added resources,
     * {@link ClassNotFoundException} is thrown.
     */
    @Test(expectedExceptions = ClassNotFoundException.class)
    public void doNotRegisterNotExistingDriver() throws Exception {
        manager.registerDriver("not.existing.Driver123");
    }

    /**
     * Tests if driver accessible in the one of previously added resources
     * (but not on the classpath!) can be successfully registered.
     */
    @Test
    public void registerDriverFromJAR() throws Exception {
        manager.registerDriver(JAR_DRIVER_CLASS);
        assertNotNull("Driver has not been registered",
                DriverManager.getDriver(JAR_DRIVER_URL_ACCEPTED));
    }

    /**
     * Tests if resources are properly removed - if resource has been removed,
     * {@link ClassNotFoundException} should be thrown in case of instantiation
     * of driver located in this resource.
     */
    @Test
    public void unregisteredResourceNoLongerAccessible() {
          manager.removeDriverResource(new File(JAR_DRIVER_LOCATION).toURI());
          try {
              manager.registerDriver(JAR_DRIVER_CLASS);
              fail("ClassNotFoundException expected");
          } catch (ClassNotFoundException e) {
              // ok, it's expected
          }
    }

    /**
     * Mock {@link Driver} implementation. It is available on test's classpath.
     */
    static class MockClasspathDriver extends AbstractMockedDriver {

        public static String URL_ACCEPTED = "jdbc:dummydb:myhost";

        @Override
        protected String getURLAccepted() {
            return URL_ACCEPTED;
        }

        @Override
        protected Connection doConnect(final String url, final Properties info)
                throws SQLException {
            throw new UnsupportedOperationException("TEST MOCK");
        }
    }
}
