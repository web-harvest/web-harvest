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

package org.webharvest.runtime.processors.plugins.db;

import static org.webharvest.WHConstants.XMLNS_CORE;
import static org.webharvest.WHConstants.XMLNS_CORE_10;

import java.sql.Connection;
import java.sql.SQLException;

import org.webharvest.annotation.Definition;
import org.webharvest.exception.DatabaseException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.database.ConnectionFactory;
import org.webharvest.runtime.processors.plugins.Autoscanned;
import org.webharvest.runtime.processors.plugins.TargetNamespace;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Support for database operations.
 */
@Autoscanned
@TargetNamespace({ XMLNS_CORE, XMLNS_CORE_10 })
@Definition(value = "database", validAttributes = {
        "connection", "jdbcclass", "username", "password", "autocommit" })
public final class DatabasePlugin extends AbstractDatabasePlugin {

    @Inject
    @Named("standalone")
    private ConnectionFactory connectionFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection obtainConnection(final DynamicScopeContext context) {
        final String jdbc = evaluateAttribute("jdbcclass", context);
        final String connection = evaluateAttribute("connection", context);
        final String username = evaluateAttribute("username", context);
        final String password = evaluateAttribute("password", context);
        final boolean isAutoCommit = evaluateAttributeAsBoolean("autocommit",
                true, context);

        final Connection conn = connectionFactory.getConnection(jdbc,
                connection, username, password);
        try {
            conn.setAutoCommit(isAutoCommit);
        } catch (SQLException cause) {
            throw new DatabaseException(
                    "Cannot set connection autocommit mode", cause);
        }
        return conn;
    }

    public String[] getValidAttributes() {
        return new String[] {"jdbcclass", "connection", "username",
                "password", "autocommit" };
    }

    public String[] getRequiredAttributes() {
        return new String[] {"jdbcclass", "connection"};
    }

    public String[] getAttributeValueSuggestions(String attributeName) {
        if ("output".equalsIgnoreCase(attributeName)) {
            return new String[] {"text", "xml"};
        } else if ("autocommit".equalsIgnoreCase(attributeName)) {
            return new String[] {"true", "false"};
        }
        return null;
    }
}
