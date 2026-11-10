package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Validation and execute contracts for request-parameter child plugins
 * ({@link HttpParamPlugin}) without using an {@code *Http*} test filename
 * (Surefire excludes those patterns).
 */
public class RequestParamPluginBranchContractTest {

    @Mock
    private PluginConfiguration configuration;

    @Mock
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void validateRejectsMissingName() throws Exception {
        when(configuration.getProperty("name")).thenReturn(null);
        new HttpParamPlugin().validateConfiguration(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void validateRejectsInvalidNameCharacters() throws Exception {
        when(configuration.getProperty("name")).thenReturn("bad name!");
        new HttpParamPlugin().validateConfiguration(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void validateRejectsInvalidContentType() throws Exception {
        when(configuration.getProperty("name")).thenReturn("q");
        when(configuration.getProperty("contenttype")).thenReturn("not-a-mime");
        new HttpParamPlugin().validateConfiguration(configuration);
    }

    @Test
    public void validateAcceptsNameAndOptionalMimeType() throws Exception {
        when(configuration.getProperty("name")).thenReturn("q-1");
        when(configuration.getProperty("contenttype")).thenReturn("text/plain");
        new HttpParamPlugin().validateConfiguration(configuration);
    }

    @Test
    public void executeReturnsEmptyForConfiguredParameter() throws Exception {
        when(configuration.getProperty("name")).thenReturn("q");
        when(configuration.getProperty("name", "")).thenReturn("q");
        when(configuration.getProperty("isfile", "false")).thenReturn("false");
        when(configuration.getProperty("filename", "")).thenReturn("");
        when(configuration.getProperty("contenttype", "")).thenReturn("");
        when(configuration.getProperty("contenttype")).thenReturn(null);

        HttpParamPlugin plugin = new HttpParamPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result instanceof EmptyVariable || result.isEmpty());
    }
}
