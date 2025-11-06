package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
// REMOVED in v2.2.x - old architecture classes no longer exist
// import org.webharvest.definition.IElementDef;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class FilePluginTest {

    @Mock
    private PluginConfiguration configuration;

    @Mock
    private DynamicScopeContext context;

    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private IElementDef elementDef;

    private FilePlugin plugin;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        plugin = new FilePlugin();
        
        // v2.2.x: elementDef removed - no longer needed
        // when(elementDef.hasOperations()).thenReturn(false);
        // when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
    }

    @Test
    public void testGetMetadata() {
        assertNotNull(plugin.getMetadata());
        assertEquals("file", plugin.getMetadata().getId());
        assertEquals("File", plugin.getMetadata().getName());
        assertEquals(plugin.getMetadata().getVersion(), "2.3.0", plugin.getMetadata().getVersion());
        assertTrue(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testGetConfigurationSchema() {
        assertNotNull(plugin.getConfigurationSchema());
        assertTrue(plugin.getConfigurationSchema().allowsBody());
        assertEquals(9, plugin.getConfigurationSchema().getAttributeDefinitions().length);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNull() throws PluginValidationException {
        plugin.getConfigurationSchema().validate(null);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithEmptyPath() throws PluginValidationException {
        when(configuration.getProperty("path")).thenReturn("");
        plugin.getConfigurationSchema().validate(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullPath() throws PluginValidationException {
        when(configuration.getProperty("path")).thenReturn(null);
        plugin.getConfigurationSchema().validate(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithInvalidAction() throws PluginValidationException {
        when(configuration.getProperty("path")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("action", "read")).thenReturn("invalid");
        plugin.getConfigurationSchema().validate(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithInvalidType() throws PluginValidationException {
        when(configuration.getProperty("path")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("type", "text")).thenReturn("invalid");
        plugin.getConfigurationSchema().validate(configuration);
    }

    @Test
    public void testValidateConfigurationWithValidRead() throws PluginValidationException {
        when(configuration.getProperty("path")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        plugin.getConfigurationSchema().validate(configuration);
    }

    @Test
    public void testValidateConfigurationWithValidWrite() throws PluginValidationException {
        when(configuration.getProperty("path")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("action", "read")).thenReturn("write");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        plugin.getConfigurationSchema().validate(configuration);
    }

    @Test
    public void testValidateConfigurationWithValidAppend() throws PluginValidationException {
        when(configuration.getProperty("path")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("action", "read")).thenReturn("append");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        plugin.getConfigurationSchema().validate(configuration);
    }

    @Test
    public void testValidateConfigurationWithValidList() throws PluginValidationException {
        when(configuration.getProperty("path")).thenReturn("/tmp");
        when(configuration.getProperty("action", "read")).thenReturn("list");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        plugin.getConfigurationSchema().validate(configuration);
    }

    @Test
    public void testValidateConfigurationWithValidBinary() throws PluginValidationException {
        when(configuration.getProperty("path")).thenReturn("/tmp/test.bin");
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("type", "text")).thenReturn("binary");
        plugin.getConfigurationSchema().validate(configuration);
    }

    @Test
    public void testExecuteWithEmptyPath() throws Exception {
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("path")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("path", "")).thenReturn("");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testExecuteWithUnknownAction() throws Exception {
        when(configuration.getProperty("action", "read")).thenReturn("unknown");
        when(configuration.getProperty("path")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("path", "")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithReadActionNonExistentFile() throws Exception {
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("path")).thenReturn("/non/existent/file.txt");
        when(configuration.getProperty("path", "")).thenReturn("/non/existent/file.txt");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        when(configuration.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithReadActionBinaryFile() throws Exception {
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("path")).thenReturn("/tmp/test.bin");
        when(configuration.getProperty("path", "")).thenReturn("/tmp/test.bin");
        when(configuration.getProperty("type", "text")).thenReturn("binary");
        when(configuration.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithWriteAction() throws Exception {
        when(configuration.getProperty("action", "read")).thenReturn("write");
        when(configuration.getProperty("path")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("path", "")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        when(configuration.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithAppendAction() throws Exception {
        when(configuration.getProperty("action", "read")).thenReturn("append");
        when(configuration.getProperty("path")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("path", "")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        when(configuration.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithListActionNonExistentDirectory() throws Exception {
        when(configuration.getProperty("action", "read")).thenReturn("list");
        when(configuration.getProperty("path")).thenReturn("/non/existent/dir");
        when(configuration.getProperty("path", "")).thenReturn("/non/existent/dir");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        when(configuration.getProperty("listfilter", "")).thenReturn("");
        when(configuration.getProperty("listfiles", "true")).thenReturn("true");
        when(configuration.getProperty("listdirs", "true")).thenReturn("true");
        when(configuration.getProperty("listrecursive", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithListActionFile() throws Exception {
        when(configuration.getProperty("action", "read")).thenReturn("list");
        when(configuration.getProperty("path")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("path", "")).thenReturn("target/test-output/test.txt");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        when(configuration.getProperty("listfilter", "")).thenReturn("");
        when(configuration.getProperty("listfiles", "true")).thenReturn("true");
        when(configuration.getProperty("listdirs", "true")).thenReturn("true");
        when(configuration.getProperty("listrecursive", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithListActionRecursive() throws Exception {
        when(configuration.getProperty("action", "read")).thenReturn("list");
        when(configuration.getProperty("path")).thenReturn("/tmp");
        when(configuration.getProperty("path", "")).thenReturn("/tmp");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        when(configuration.getProperty("listfilter", "")).thenReturn("");
        when(configuration.getProperty("listfiles", "true")).thenReturn("true");
        when(configuration.getProperty("listdirs", "true")).thenReturn("true");
        when(configuration.getProperty("listrecursive", "false")).thenReturn("true");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithListActionFilter() throws Exception {
        when(configuration.getProperty("action", "read")).thenReturn("list");
        when(configuration.getProperty("path")).thenReturn("/tmp");
        when(configuration.getProperty("path", "")).thenReturn("/tmp");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        when(configuration.getProperty("listfilter", "")).thenReturn("*.txt");
        when(configuration.getProperty("listfiles", "true")).thenReturn("true");
        when(configuration.getProperty("listdirs", "true")).thenReturn("false");
        when(configuration.getProperty("listrecursive", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testAppendBinaryWithNull() {
        NodeVariable result = plugin.appendBinary(null);
        assertNotNull(result);
        assertEquals("", result.toString());
    }

    @Test
    public void testAppendBinaryWithEmptyVariable() {
        NodeVariable result = plugin.appendBinary(EmptyVariable.INSTANCE);
        assertNotNull(result);
    }

    @Test
    public void testAppendBinaryWithNodeVariable() {
        NodeVariable var = new NodeVariable("test");
        NodeVariable result = plugin.appendBinary(var);
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithAbsolutePath() throws PluginException, InterruptedException {
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("path")).thenReturn("/absolute/path/test.txt");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithRelativePath() throws PluginException, InterruptedException {
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("path")).thenReturn("relative/path/test.txt");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithSpecialCharactersInPath() throws PluginException, InterruptedException {
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("path")).thenReturn("test-file_2024.txt");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithUnicodeInPath() throws PluginException, InterruptedException {
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("path")).thenReturn("测试文件.txt");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithCharsetUTF16() throws PluginException, InterruptedException {
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("path")).thenReturn("test.txt");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        when(configuration.getProperty("charset", "UTF-8")).thenReturn("UTF-16");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithCharsetISO() throws PluginException, InterruptedException {
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("path")).thenReturn("test.txt");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        when(configuration.getProperty("charset", "UTF-8")).thenReturn("ISO-8859-1");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWriteBinaryWithLargeData() throws PluginException, InterruptedException {
        when(configuration.getProperty("action", "read")).thenReturn("write");
        when(configuration.getProperty("path")).thenReturn("test.bin");
        when(configuration.getProperty("type", "text")).thenReturn("binary");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteListWithRecursive() throws PluginException, InterruptedException {
        when(configuration.getProperty("action", "read")).thenReturn("list");
        when(configuration.getProperty("path")).thenReturn("test-dir");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        when(configuration.getProperty("listfilter", "")).thenReturn("*");
        when(configuration.getProperty("listfiles", "true")).thenReturn("true");
        when(configuration.getProperty("listdirs", "true")).thenReturn("true");
        when(configuration.getProperty("listrecursive", "false")).thenReturn("true");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteMultipleReadOperations() throws PluginException, InterruptedException {
        when(configuration.getProperty("action", "read")).thenReturn("read");
        when(configuration.getProperty("path")).thenReturn("test.txt");
        when(configuration.getProperty("type", "text")).thenReturn("text");
        
        plugin.initialize(configuration);
        
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
    }
}
