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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Abstract SQL {@link Driver} mock. Facilitates testing classes using
 * {@link java.sql.DriverManager} static methods (not possible to mock).
 */
public abstract class AbstractMockedDriver implements Driver {

    /**
     * Returns SQL URL accepted by driver.
     */
    protected abstract String getURLAccepted();

    /**
     * Handles connection logic. This method is invoked only, if driver
     * agreed to accept URL proposed by the driver manager.
     */
    protected abstract Connection doConnect(String url, Properties info)
            throws SQLException;

    /**
     * {@inheritDoc}
     */
    @Override
    public final Connection connect(final String url,
            final Properties info) throws SQLException {
        if (getURLAccepted().equals(url)) {
            return doConnect(url, info);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean acceptsURL(final String url) throws SQLException {
        return getURLAccepted().equals(url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url,
            final Properties info) throws SQLException {
        throw new UnsupportedOperationException("TEST MOCK");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMajorVersion() {
        throw new UnsupportedOperationException("TEST MOCK");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinorVersion() {
        throw new UnsupportedOperationException("TEST MOCK");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean jdbcCompliant() {
        throw new UnsupportedOperationException("TEST MOCK");
    }

    // added in JDK7 Driver interface, we can't add @Override because
    // it would break build in JDK6 ;/
    public Logger getParentLogger() {
        throw new UnsupportedOperationException("TEST MOCK");
    }

}
