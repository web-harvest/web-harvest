package org.webharvest.plugin.ftp;

import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Integration tests for ftp-get text decoding.
 */
public class FtpGetPluginCharsetTest {

    private FakeFtpServer fakeFtpServer;
    private boolean serverStarted;

    @BeforeMethod
    public void setUp() {
        fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.setServerControlPort(0);

        UnixFakeFileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/"));

        FileEntry latin1File = new FileEntry("/latin1.txt");
        latin1File.setContents(new byte[] {0x63, 0x61, 0x66, (byte) 0xE9});
        fileSystem.add(latin1File);

        FileEntry utf8File = new FileEntry("/utf8.txt");
        utf8File.setContents("café".getBytes(StandardCharsets.UTF_8));
        fileSystem.add(utf8File);

        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.addUserAccount(new UserAccount("test-user", "test-pass", "/"));
        fakeFtpServer.start();
        serverStarted = true;
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (serverStarted) {
            fakeFtpServer.stop();
            serverStarted = false;
        }
    }

    @Test
    public void decodesDownloadedTextUsingConfiguredExecutionCharset() throws Exception {
        Variable result = execute("/latin1.txt", "ISO-8859-1");

        assertEquals(result.toString(), "café");
    }

    @Test
    public void fallsBackToUtf8WhenExecutionCharsetIsMissing() throws Exception {
        Variable result = execute("/utf8.txt", null);

        assertEquals(result.toString(), "café");
    }

    private Variable execute(String path, String charset) throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("host", "127.0.0.1");
        properties.put("port", Integer.toString(fakeFtpServer.getServerControlPort()));
        properties.put("user", "test-user");
        properties.put("password", "test-pass");
        properties.put("path", path);

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "ftp-get",
                "http://org.webharvest/schema/2.1/core",
                "ftp-get",
                properties);

        FtpGetPlugin plugin = new FtpGetPlugin();
        plugin.initialize(configuration);

        ScraperContext context = new ScraperContext();
        context.setCharset(charset);
        return plugin.execute(context);
    }
}
