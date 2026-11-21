package org.webharvest.server.api;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies plugin help links resolve only to documentation pages preserved in GitHub.
 */
public class PluginMetadataHelpUrlTest {

    @Test
    public void resolvesCorePluginDocumentation() {
        Assert.assertEquals(
                PluginMetadataServlet.getHelpUrl("http"),
                "https://github.com/web-harvest/web-harvest/blob/main/docs/web/documentation/2.2.0/http.html");
    }

    @Test
    public void resolvesOptionalWebBrowserDocumentation() {
        Assert.assertEquals(
                PluginMetadataServlet.getHelpUrl("webbrowser"),
                "https://github.com/web-harvest/web-harvest/blob/main/docs/web/documentation/2.2.0/external/web-browser.html");
    }

    @Test
    public void resolvesOptionalFtpDocumentation() {
        Assert.assertEquals(
                PluginMetadataServlet.getHelpUrl("ftp-get"),
                "https://github.com/web-harvest/web-harvest/blob/main/docs/web/documentation/2.2.0/external/ftp-get.html");
    }

    @Test
    public void unknownPluginUsesLocalDocumentationIndex() {
        Assert.assertEquals(
                PluginMetadataServlet.getHelpUrl("not-a-real-plugin"),
                "https://github.com/web-harvest/web-harvest/blob/main/docs/web/documentation/2.2.0/index.html");
    }

    @Test
    public void legacySourceForgeUrlIsReplaced() {
        Assert.assertEquals(
                PluginMetadataServlet.resolveHelpUrl(
                        "webbrowser",
                        "https://web-harvest.sourceforge.io/documentation/2.2.0/webbrowser.html"),
                PluginMetadataServlet.getHelpUrl("webbrowser"));
    }

    @Test
    public void nonLegacyConfiguredUrlIsPreserved() {
        String configuredUrl = "https://example.org/plugin-help";
        Assert.assertEquals(
                PluginMetadataServlet.resolveHelpUrl("http", configuredUrl),
                configuredUrl);
    }

    @Test
    public void missingElementNameUsesDocumentationIndex() {
        Assert.assertEquals(
                PluginMetadataServlet.getHelpUrl(null),
                "https://github.com/web-harvest/web-harvest/blob/main/docs/web/documentation/2.2.0/index.html");
    }
}
