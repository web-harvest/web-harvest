package org.webharvest.runtime.database;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.exception.DatabaseException;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertSame;

/** Deterministic tests for JNDI name normalization and DataSource lookup. */
public class JNDIConnectionFactoryTest {

    private static Context context;
    private Context mockedContext;
    private JNDIConnectionFactory factory;

    @BeforeMethod
    public void setUp() {
        mockedContext = mock(Context.class);
        context = mockedContext;
        factory = new TestJNDIConnectionFactory();
    }

    @Test
    public void prefixesRelativeNamesAndReturnsDatasourceConnection() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(mockedContext.lookup("java:comp/env/reporting")).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);

        assertSame(factory.getConnection("reporting"), connection);
        verify(mockedContext).lookup("java:comp/env/reporting");
    }

    @Test
    public void preservesContainerAndOtherQualifiedNames() throws Exception {
        DataSource containerSource = mock(DataSource.class);
        DataSource qualifiedSource = mock(DataSource.class);
        when(mockedContext.lookup("java:comp/env/reporting")).thenReturn(containerSource);
        when(mockedContext.lookup("jdbc:reporting")).thenReturn(qualifiedSource);

        factory.getConnection("java:comp/env/reporting");
        factory.getConnection("jdbc:reporting");

        verify(mockedContext).lookup("java:comp/env/reporting");
        verify(mockedContext).lookup("jdbc:reporting");
    }

    @Test(expectedExceptions = DatabaseException.class)
    public void wrapsNamingLookupFailure() throws Exception {
        when(mockedContext.lookup("java:comp/env/missing"))
                .thenThrow(new NamingException("not bound"));
        factory.getConnection("missing");
    }

    @Test(expectedExceptions = DatabaseException.class)
    public void wrapsDatasourceConnectionFailure() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(mockedContext.lookup("java:comp/env/broken")).thenReturn(dataSource);
        when(dataSource.getConnection()).thenThrow(new SQLException("offline"));
        factory.getConnection("broken");
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void rejectsDriverBasedConnections() {
        factory.getConnection("driver", "url", "user", "password");
    }

    private static final class TestJNDIConnectionFactory extends JNDIConnectionFactory {
        @Override
        protected Context createContext() {
            return context;
        }
    }
}
