/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.ftp;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.InputStream;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Behavioral tests for the FTP plugins against an in-process FTP server.
 *
 * <p>The server binds to a dynamically allocated loopback port and uses an
 * in-memory filesystem. No public FTP service or external test fixture is used.</p>
 */
public class FtpPluginBehaviorTest {

    private static final String USER = "webharvest";
    private static final String PASSWORD = "secret";
    private static final String HOST = "127.0.0.1";
    private static final String FILE_CONTENT = "download-payload";
    private static final String UPLOAD_CONTENT = "upload-payload";

    private FakeFtpServer server;
    private UnixFakeFileSystem fileSystem;
    private DynamicScopeContext context;

    @BeforeMethod
    public void startFtpServer() {
        server = new FakeFtpServer();
        server.setServerControlPort(0);
        fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data"));
        fileSystem.add(new FileEntry("/data/download.txt", FILE_CONTENT));
        fileSystem.add(new FileEntry("/data/delete.txt", "delete-me"));
        fileSystem.add(new DirectoryEntry("/empty"));
        server.setFileSystem(fileSystem);
        server.addUserAccount(new UserAccount(USER, PASSWORD, "/"));
        server.start();
        context = mock(DynamicScopeContext.class);
    }

    @AfterMethod
    public void stopFtpServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void containerGetDownloadsFileContents() throws Exception {
        Variable result = executeContainer("get", "/data/download.txt", PASSWORD, true, null);

        assertNotNull(result);
        assertEquals(result.toString(), FILE_CONTENT);
    }

    @Test
    public void containerPutUploadsBodyContents() throws Exception {
        executeContainer("put", "/data/upload.txt", PASSWORD, true, new NodeVariable(UPLOAD_CONTENT));

        assertEquals(readFile("/data/upload.txt"), UPLOAD_CONTENT);
    }

    @Test
    public void containerListReturnsRemoteNames() throws Exception {
        Variable result = executeContainer("list", "/data", PASSWORD, true, null);

        assertTrue(result instanceof ListVariable);
        assertTrue(((ListVariable) result).contains("download.txt"));
        assertTrue(((ListVariable) result).contains("delete.txt"));
    }

    @Test
    public void containerActionDispatchIsIndependentOfDefaultLocale() throws Exception {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            Variable result = executeContainer("LIST", "/data", PASSWORD, true, null);

            assertTrue(result instanceof ListVariable);
            assertTrue(((ListVariable) result).contains("download.txt"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void containerSupportsDirectoryAndDeleteActions() throws Exception {
        executeContainer("mkdir", "/created", PASSWORD, true, null);
        assertNotNull(fileSystem.getEntry("/created"));

        executeContainer("rmdir", "/created", PASSWORD, true, null);
        assertNull(fileSystem.getEntry("/created"));

        executeContainer("del", "/data/delete.txt", PASSWORD, true, null);
        assertNull(fileSystem.getEntry("/data/delete.txt"));
    }

    @Test
    public void containerListReturnsEmptyListForEmptyDirectory() throws Exception {
        Variable result = executeContainer("list", "/empty", PASSWORD, true, null);

        assertTrue(result instanceof ListVariable);
        assertTrue(((ListVariable) result).isEmpty());
    }

    @Test
    public void containerPutRejectsNullBody() throws Exception {
        FtpPlugin plugin = new BodyFtpPlugin(null);
        plugin.initialize(configuration(containerProperties("put", "/data/null-body.txt", PASSWORD, true), "ftp"));

        RuntimeException failure = executeExpectingFailureAfterInitialization(plugin);

        assertTrue(causeMessages(failure).contains("Body content is required"));
    }

    @Test
    public void containerFallsBackWhenTemplateEvaluationFails() throws Exception {
        when(context.getScriptingLanguage()).thenThrow(new IllegalStateException("scripting engine unavailable"));
        Variable result = executeContainer("get", "/data/download.txt", PASSWORD, true, null);

        assertEquals(result.toString(), FILE_CONTENT);
    }

    @Test
    public void containerCanUseActiveMode() throws Exception {
        Variable result = executeContainer("get", "/data/download.txt", PASSWORD, false, null);

        assertEquals(result.toString(), FILE_CONTENT);
    }

    @Test
    public void containerRejectsMissingRemotePath() throws Exception {
        String[] actions = { "get", "put", "mkdir", "rmdir", "del" };
        for (String action : actions) {
            RuntimeException failure = executeContainerExpectingFailure(action, "", PASSWORD);
            assertTrue(causeMessages(failure).contains("Remote path is required"), action);
        }
    }

    @Test
    public void containerRejectsMissingRemoteFile() throws Exception {
        RuntimeException failure = executeContainerExpectingFailure("get", "/data/missing.txt", PASSWORD);

        assertTrue(causeMessages(failure).contains("Cannot retrieve file"));
    }

    @Test
    public void containerRejectsFailedServerOperations() throws Exception {
        assertTrue(causeMessages(executeContainerExpectingFailure("mkdir", "/data", PASSWORD))
                .contains("Cannot create directory"));
        assertTrue(causeMessages(executeContainerExpectingFailure("rmdir", "/data", PASSWORD))
                .contains("Cannot remove directory"));
        assertTrue(causeMessages(executeContainerExpectingFailure("del", "/data/missing.txt", PASSWORD))
                .contains("Cannot delete file"));

        FtpPlugin plugin = new BodyFtpPlugin(new NodeVariable(UPLOAD_CONTENT));
        plugin.initialize(configuration(containerProperties("put", "/missing/upload.txt", PASSWORD, true), "ftp"));
        RuntimeException failure = executeExpectingFailureAfterInitialization(plugin);
        assertTrue(causeMessages(failure).contains("Cannot store file"));
    }

    @Test
    public void containerPropagatesInterruptedBodyExecution() throws Exception {
        InterruptedBodyFtpPlugin plugin = new InterruptedBodyFtpPlugin();
        plugin.initialize(configuration(containerProperties("put", "/data/interrupted.txt", PASSWORD, true), "ftp"));
        try {
            plugin.execute(context);
            fail("Expected body interruption to propagate through the FTP plugin");
        } catch (InterruptedException expected) {
            assertTrue(Thread.currentThread().isInterrupted(),
                    "The FTP plugin must restore the interrupt flag before propagating interruption");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void containerRejectsInvalidAction() throws Exception {
        RuntimeException failure = executeContainerExpectingFailure("unknown", "/data/download.txt", PASSWORD);

        assertTrue(causeMessages(failure).contains("Unknown FTP action"));
    }

    @Test
    public void containerRejectsInvalidCredentials() throws Exception {
        RuntimeException failure = executeContainerExpectingFailure("list", "/data", "wrong-password");

        assertTrue(causeMessages(failure).contains("FTP login failed"));
    }

    @Test
    public void containerReportsConnectionFailure() throws Exception {
        Map<String, String> properties = containerProperties("get", "/data/download.txt", PASSWORD, true);
        properties.put("port", Integer.toString(stopServerAndGetClosedPort()));
        FtpPlugin plugin = new FtpPlugin();
        RuntimeException failure = executeExpectingFailure(plugin, configuration(properties, "ftp"));

        assertTrue(causeMessages(failure).contains("FTP operation failed"));
    }

    @Test
    public void containerRejectsUploadWithoutBody() throws Exception {
        RuntimeException failure = executeContainerExpectingFailure("put", "/data/empty.txt", PASSWORD);

        assertTrue(causeMessages(failure).contains("Body content is required"));
    }

    @Test
    public void standaloneGetDownloadsFileContents() throws Exception {
        FtpGetPlugin plugin = new FtpGetPlugin();
        plugin.initialize(configuration(standaloneProperties("/data/download.txt", PASSWORD), "ftp-get"));

        Variable result = plugin.execute(context);

        assertEquals(result.toString(), FILE_CONTENT);
    }

    @Test
    public void standaloneGetRejectsMissingFile() throws Exception {
        FtpGetPlugin plugin = new FtpGetPlugin();
        plugin.initialize(configuration(standaloneProperties("/data/missing.txt", PASSWORD), "ftp-get"));

        RuntimeException failure = executeExpectingFailureAfterInitialization(plugin);

        assertTrue(causeMessages(failure).contains("Failed to download file"));
    }

    @Test
    public void standaloneGetRejectsInvalidCredentials() throws Exception {
        FtpGetPlugin plugin = new FtpGetPlugin();
        plugin.initialize(configuration(standaloneProperties("/data/download.txt", "wrong-password"), "ftp-get"));

        RuntimeException failure = executeExpectingFailureAfterInitialization(plugin);

        assertTrue(causeMessages(failure).contains("FTP login failed"));
    }

    @Test
    public void standaloneListReturnsDirectoryEntries() throws Exception {
        FtpListPlugin plugin = new FtpListPlugin();
        plugin.initialize(configuration(standaloneProperties("/data", PASSWORD), "ftp-list"));

        Variable result = plugin.execute(context);

        assertTrue(result.toString().contains("<name>download.txt</name>"));
        assertTrue(result.toString().contains("<name>delete.txt</name>"));
    }

    @Test
    public void standaloneGetWithMissingPathFailsDeterministically() throws Exception {
        FtpGetPlugin plugin = new FtpGetPlugin();
        plugin.initialize(configuration(standaloneProperties("", PASSWORD), "ftp-get"));

        RuntimeException failure = executeExpectingFailureAfterInitialization(plugin);

        assertTrue(causeMessages(failure).contains("Failed to download file"));
    }

    @Test
    public void standaloneGetReportsConnectionFailure() throws Exception {
        Map<String, String> properties = standaloneProperties("/data/download.txt", PASSWORD);
        properties.put("port", Integer.toString(stopServerAndGetClosedPort()));
        FtpGetPlugin plugin = new FtpGetPlugin();
        plugin.initialize(configuration(properties, "ftp-get"));

        RuntimeException failure = executeExpectingFailureAfterInitialization(plugin);

        assertTrue(hasCause(failure, ConnectException.class), causeMessages(failure));
    }

    @Test
    public void standaloneListDefaultsToRootWhenBodyIsEmpty() throws Exception {
        FtpListPlugin plugin = new FtpListPlugin();
        plugin.initialize(configuration(standaloneProperties("/", PASSWORD), "ftp-list"));

        Variable result = plugin.execute(context);

        assertTrue(result.toString().contains("<name>data</name>"));
        assertTrue(result.toString().contains("<name>empty</name>"));
    }

    @Test
    public void standaloneListUsesBodyPathWhenDefaultIsRoot() throws Exception {
        BodyFtpListPlugin plugin = new BodyFtpListPlugin("/data");
        plugin.initialize(configuration(standaloneProperties("/", PASSWORD), "ftp-list"));

        Variable result = plugin.execute(context);

        assertTrue(result.toString().contains("<name>download.txt</name>"));
    }

    @Test
    public void standaloneListRejectsInvalidCredentials() throws Exception {
        FtpListPlugin plugin = new FtpListPlugin();
        plugin.initialize(configuration(standaloneProperties("/data", "wrong-password"), "ftp-list"));

        RuntimeException failure = executeExpectingFailureAfterInitialization(plugin);

        assertTrue(causeMessages(failure).contains("FTP login failed"));
    }

    @Test
    public void standaloneListRejectsMissingDirectory() throws Exception {
        FtpListPlugin plugin = new FtpListPlugin();
        plugin.initialize(configuration(standaloneProperties("/missing", PASSWORD), "ftp-list"));

        RuntimeException failure = executeExpectingFailureAfterInitialization(plugin);

        assertTrue(causeMessages(failure).contains("Failed to change to directory"));
    }

    @Test
    public void standaloneListReportsConnectionFailure() throws Exception {
        Map<String, String> properties = standaloneProperties("/data", PASSWORD);
        properties.put("port", Integer.toString(stopServerAndGetClosedPort()));
        FtpListPlugin plugin = new FtpListPlugin();
        plugin.initialize(configuration(properties, "ftp-list"));

        RuntimeException failure = executeExpectingFailureAfterInitialization(plugin);

        assertTrue(hasCause(failure, ConnectException.class), causeMessages(failure));
    }

    @Test
    public void standalonePutUploadsBodyContents() throws Exception {
        BodyFtpPutPlugin plugin = new BodyFtpPutPlugin(UPLOAD_CONTENT);
        plugin.initialize(configuration(standaloneProperties("/data/standalone-upload.txt", PASSWORD), "ftp-put"));

        Variable result = plugin.execute(context);

        assertTrue(result.toString().contains("File uploaded successfully"));
        assertEquals(readFile("/data/standalone-upload.txt"), UPLOAD_CONTENT);
    }

    @Test
    public void standalonePutPropagatesInterruptedBodyExecution() throws Exception {
        InterruptedBodyFtpPutPlugin plugin = new InterruptedBodyFtpPutPlugin();
        plugin.initialize(configuration(standaloneProperties("/data/interrupted-upload.txt", PASSWORD), "ftp-put"));
        try {
            plugin.execute(context);
            fail("Expected body interruption to propagate through standalone FTP upload");
        } catch (InterruptedException expected) {
            assertTrue(Thread.currentThread().isInterrupted(),
                    "The standalone FTP upload must restore the interrupt flag");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void standalonePutUploadsEmptyBody() throws Exception {
        BodyFtpPutPlugin plugin = new BodyFtpPutPlugin("");
        plugin.initialize(configuration(standaloneProperties("/data/empty-upload.txt", PASSWORD), "ftp-put"));

        Variable result = plugin.execute(context);

        assertTrue(result.toString().contains("File uploaded successfully"));
        assertEquals(readFile("/data/empty-upload.txt"), "");
    }

    @Test
    public void standalonePutUsesBodyForMissingPath() throws Exception {
        SequenceBodyFtpPutPlugin plugin = new SequenceBodyFtpPutPlugin(
                "/data/body-path-upload.txt", "body-path-content");
        plugin.initialize(configuration(standaloneProperties("", PASSWORD), "ftp-put"));

        plugin.execute(context);

        assertEquals(readFile("/data/body-path-upload.txt"), "body-path-content");
    }

    @Test
    public void standalonePutReportsConnectionFailure() throws Exception {
        Map<String, String> properties = standaloneProperties("/data/connection-failure.txt", PASSWORD);
        properties.put("port", Integer.toString(stopServerAndGetClosedPort()));
        BodyFtpPutPlugin plugin = new BodyFtpPutPlugin(UPLOAD_CONTENT);
        plugin.initialize(configuration(properties, "ftp-put"));

        RuntimeException failure = executeExpectingFailureAfterInitialization(plugin);

        assertTrue(hasCause(failure, ConnectException.class), causeMessages(failure));
    }

    @Test
    public void standalonePutRejectsInvalidCredentials() throws Exception {
        BodyFtpPutPlugin plugin = new BodyFtpPutPlugin(UPLOAD_CONTENT);
        plugin.initialize(configuration(standaloneProperties("/data/standalone-upload.txt", "wrong-password"), "ftp-put"));

        RuntimeException failure = executeExpectingFailureAfterInitialization(plugin);

        assertTrue(causeMessages(failure).contains("FTP login failed"));
    }

    @Test
    public void standalonePutRejectsMissingDestinationDirectory() throws Exception {
        BodyFtpPutPlugin plugin = new BodyFtpPutPlugin(UPLOAD_CONTENT);
        plugin.initialize(configuration(standaloneProperties("/missing/upload.txt", PASSWORD), "ftp-put"));

        RuntimeException failure = executeExpectingFailureAfterInitialization(plugin);

        assertTrue(causeMessages(failure).contains("Failed to upload file"));
    }

    private Variable executeContainer(String action, String remotePath, String password,
                                      boolean passive, Variable body) throws Exception {
        FtpPlugin plugin = body == null ? new FtpPlugin() : new BodyFtpPlugin(body);
        plugin.initialize(configuration(containerProperties(action, remotePath, password, passive), "ftp"));
        return plugin.execute(context);
    }

    private RuntimeException executeContainerExpectingFailure(String action, String remotePath,
                                                               String password) throws Exception {
        FtpPlugin plugin = new FtpPlugin();
        plugin.initialize(configuration(containerProperties(action, remotePath, password, true), "ftp"));
        return executeExpectingFailureAfterInitialization(plugin);
    }

    private RuntimeException executeExpectingFailure(AbstractPlugin plugin,
                                                      PluginConfiguration configuration) throws Exception {
        plugin.initialize(configuration);
        return executeExpectingFailureAfterInitialization(plugin);
    }

    private RuntimeException executeExpectingFailureAfterInitialization(AbstractPlugin plugin)
            throws InterruptedException {
        try {
            plugin.execute(context);
        } catch (RuntimeException expected) {
            return expected;
        }
        fail("Expected FTP plugin execution to fail");
        return null;
    }

    private PluginConfiguration configuration(Map<String, String> properties, String elementName) {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty(anyString())).thenAnswer(invocation ->
                properties.get(invocation.getArgument(0)));
        when(configuration.getProperty(anyString(), anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            String defaultValue = invocation.getArgument(1);
            return properties.containsKey(name) ? properties.get(name) : defaultValue;
        });
        when(configuration.getBooleanProperty(anyString(), anyBoolean())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            boolean defaultValue = invocation.getArgument(1);
            return properties.containsKey(name) ? Boolean.parseBoolean(properties.get(name)) : defaultValue;
        });
        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(Collections.<PluginConfiguration>emptyList());
        when(configuration.getElementName()).thenReturn(elementName);
        return configuration;
    }

    private Map<String, String> containerProperties(String action, String remotePath,
                                                    String password, boolean passive) {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("host", HOST);
        properties.put("port", Integer.toString(server.getServerControlPort()));
        properties.put("username", USER);
        properties.put("password", password);
        properties.put("action", action);
        properties.put("remotePath", remotePath);
        properties.put("passive", Boolean.toString(passive));
        return properties;
    }

    private Map<String, String> standaloneProperties(String path, String password) {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("host", HOST);
        properties.put("port", Integer.toString(server.getServerControlPort()));
        properties.put("user", USER);
        properties.put("password", password);
        properties.put("path", path);
        return properties;
    }

    private int stopServerAndGetClosedPort() {
        int port = server.getServerControlPort();
        server.stop();
        server = null;
        return port;
    }

    private String readFile(String path) throws Exception {
        FileEntry entry = (FileEntry) fileSystem.getEntry(path);
        assertNotNull(entry);
        try (InputStream input = entry.createInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private boolean hasCause(Throwable failure, Class<? extends Throwable> expectedType) {
        Throwable current = failure;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String causeMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        Throwable current = failure;
        while (current != null) {
            messages.append(current.getMessage()).append('\n');
            current = current.getCause();
        }
        return messages.toString();
    }

    private static final class BodyFtpPlugin extends FtpPlugin {
        private final Variable body;

        private BodyFtpPlugin(Variable body) {
            this.body = body;
        }

        @Override
        protected Variable executeBody(DynamicScopeContext context) {
            return body;
        }
    }

    private static final class InterruptedBodyFtpPlugin extends FtpPlugin {
        @Override
        protected Variable executeBody(DynamicScopeContext context) throws InterruptedException {
            throw new InterruptedException("simulated body cancellation");
        }
    }

    private static final class BodyFtpListPlugin extends FtpListPlugin {
        private final Variable body;

        private BodyFtpListPlugin(String path) {
            this.body = new NodeVariable(path);
        }

        @Override
        protected Variable executeBody(DynamicScopeContext context) {
            return body;
        }
    }

    private static final class SequenceBodyFtpPutPlugin extends FtpPutPlugin {
        private final Variable[] bodies;
        private int nextBody;

        private SequenceBodyFtpPutPlugin(String path, String content) {
            this.bodies = new Variable[] { new NodeVariable(path), new NodeVariable(content) };
        }

        @Override
        protected Variable executeBody(DynamicScopeContext context) {
            return bodies[nextBody++];
        }
    }

    private static final class InterruptedBodyFtpPutPlugin extends FtpPutPlugin {
        @Override
        protected Variable executeBody(DynamicScopeContext context) throws InterruptedException {
            throw new InterruptedException("simulated upload body cancellation");
        }
    }

    private static final class BodyFtpPutPlugin extends FtpPutPlugin {
        private final Variable body;

        private BodyFtpPutPlugin(String body) {
            this.body = new NodeVariable(body);
        }

        @Override
        protected Variable executeBody(DynamicScopeContext context) {
            return body;
        }
    }
}
