package org.webharvest.settings;

import static org.testng.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Branch contracts for {@link WebHarvestSettings} defaults, nullable setters,
 * property cloning, and {@code loadDefault()} resolution order.
 */
public class WebHarvestSettingsBranchContractTest {

    private static final String PREV_SETTINGS_PATH = System.getProperty(WebHarvestSettings.SETTINGS_FILE_PROPERTY);

    private File tempSettingsFile;

    @BeforeMethod
    public void setUp() throws IOException {
        tempSettingsFile = Files.createTempFile("wh-settings-contract", ".properties").toFile();
        tempSettingsFile.deleteOnExit();
    }

    @AfterMethod
    public void restoreSystemProperty() {
        if (PREV_SETTINGS_PATH == null) {
            System.clearProperty(WebHarvestSettings.SETTINGS_FILE_PROPERTY);
        } else {
            System.setProperty(WebHarvestSettings.SETTINGS_FILE_PROPERTY, PREV_SETTINGS_PATH);
        }
    }

    @Test
    public void nullableSettersDoNotIntroduceProxyOrLogKeys() {
        WebHarvestSettings settings = new WebHarvestSettings();
        assertNull(settings.getProxyHost());
        assertNull(settings.getProxyPort());
        assertNull(settings.getLogLevel());

        settings.setProxyHost(null);
        settings.setProxyPort(null);
        settings.setProxyUser(null);
        settings.setProxyPassword(null);
        settings.setLogLevel(null);
        settings.setLogPropsFile(null);
        settings.setDbDrivers(null);
        settings.setPlugins(null);

        assertNull(settings.getProxyHost());
        assertNull(settings.getLogLevel());
        assertFalse(settings.getProperties().containsKey("proxyhost"));
        assertFalse(settings.getProperties().containsKey("loglevel"));
    }

    @Test
    public void nonNullProxySettersRoundTripThroughGetters() {
        WebHarvestSettings settings = new WebHarvestSettings();
        settings.setProxyHost("proxy.local");
        settings.setProxyPort(3128);
        settings.setProxyUser("user");
        settings.setProxyPassword("secret");
        settings.setProxyNtHost("nt-host");
        settings.setProxyNtDomain("CORP");

        assertEquals(settings.getProxyHost(), "proxy.local");
        assertEquals(settings.getProxyPort(), Integer.valueOf(3128));
        assertEquals(settings.getProxyUser(), "user");
        assertEquals(settings.getProxyPassword(), "secret");
        assertEquals(settings.getProxyNtHost(), "nt-host");
        assertEquals(settings.getProxyNtDomain(), "CORP");
    }

    @Test
    public void getPropertiesReturnsIndependentClone() {
        WebHarvestSettings settings = new WebHarvestSettings();
        settings.setWorkDir("/tmp/work");
        Properties snapshot = settings.getProperties();
        snapshot.setProperty("workdir", "/mutated");

        assertEquals(settings.getWorkDir(), "/tmp/work");
        assertEquals(snapshot.getProperty("workdir"), "/mutated");
    }

    @Test
    public void constructorPreservesExplicitValuesWhileFillingMissingDefaults() {
        Properties props = new Properties();
        props.setProperty("workdir", "/explicit");
        props.setProperty("debug", "true");

        WebHarvestSettings settings = new WebHarvestSettings(props);
        assertEquals(settings.getWorkDir(), "/explicit");
        assertEquals(settings.getDebug(), Boolean.TRUE);
    }

    @Test
    public void loadDefaultPrefersSystemPropertyFileOverImplicitDefaults() throws Exception {
        try (FileWriter writer = new FileWriter(tempSettingsFile)) {
            Properties props = new Properties();
            props.setProperty("workdir", "/from-system-property");
            props.setProperty("debug", "false");
            props.store(writer, "contract");
        }

        System.setProperty(WebHarvestSettings.SETTINGS_FILE_PROPERTY, tempSettingsFile.getAbsolutePath());
        WebHarvestSettings loaded = WebHarvestSettings.loadDefault();

        assertEquals(loaded.getWorkDir(), "/from-system-property");
        assertEquals(loaded.getDebug(), Boolean.FALSE);
    }

    @Test
    public void loadDefaultIgnoresMissingCustomPathAndReturnsDefaults() {
        System.setProperty(WebHarvestSettings.SETTINGS_FILE_PROPERTY,
                new File(tempSettingsFile.getParentFile(), "definitely-missing.properties").getAbsolutePath());

        WebHarvestSettings loaded = WebHarvestSettings.loadDefault();
        assertEquals(loaded.getWorkDir(), ".");
        assertEquals(loaded.getDebug(), Boolean.FALSE);
    }

    @Test
    public void saveAndReloadPreservesMutableSettings() throws Exception {
        WebHarvestSettings settings = new WebHarvestSettings();
        settings.setWorkDir("/persisted");
        settings.setDebug(true);
        settings.setPlugins("ext.jar");

        File target = Files.createTempFile("wh-save-contract", ".properties").toFile();
        target.deleteOnExit();
        settings.save(target);

        WebHarvestSettings reloaded = WebHarvestSettings.fromFile(target);
        assertEquals(reloaded.getWorkDir(), "/persisted");
        assertEquals(reloaded.getDebug(), Boolean.TRUE);
        assertEquals(reloaded.getPlugins(), "ext.jar");
    }
}
