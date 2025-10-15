/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Unified settings for WebHarvest CLI and GUI/IDE.
 * 
 * <h3>Bug #39: Synchronize CLI Arguments and GUI Settings (v2.2.1)</h3>
 * <p>
 * This class provides a unified configuration system that can be used by both
 * CLI and GUI/IDE, eliminating the need to configure settings separately.
 * </p>
 * 
 * <h3>Settings File Locations (in order of priority):</h3>
 * <ol>
 *   <li>System property: {@code -Dwebharvest.settings=path/to/settings.properties}</li>
 *   <li>User home: {@code ~/.webharvest/webharvest.properties}</li>
 *   <li>Current directory: {@code ./webharvest.properties}</li>
 * </ol>
 * 
 * <h3>Usage Example (CLI):</h3>
 * <pre>{@code
 * // Load default settings (from file if exists)
 * WebHarvestSettings settings = WebHarvestSettings.loadDefault();
 * 
 * // Override with CLI arguments
 * settings.setWorkDir("/custom/workdir");
 * settings.setDebug(true);
 * 
 * // Use settings
 * String workDir = settings.getWorkDir();
 * }</pre>
 * 
 * <h3>Settings File Format:</h3>
 * <pre>
 * # ~/.webharvest/webharvest.properties
 * workdir=/projects/scraping
 * debug=true
 * loglevel=DEBUG
 * proxyhost=proxy.company.com
 * proxyport=8080
 * proxyuser=username
 * proxypassword=password
 * </pre>
 * 
 * @author Robert Bala
 * @since 2.2.1 (Bug #39)
 */
public class WebHarvestSettings {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebHarvestSettings.class);
    
    /**
     * System property for custom settings file location.
     */
    public static final String SETTINGS_FILE_PROPERTY = "webharvest.settings";
    
    /**
     * Default settings file name.
     */
    public static final String DEFAULT_SETTINGS_FILE = "webharvest.properties";
    
    private final Properties properties;
    
    /**
     * Creates empty settings with defaults.
     */
    public WebHarvestSettings() {
        this.properties = new Properties();
        setDefaults();
    }
    
    /**
     * Creates settings from properties.
     *
     * @param properties the properties to use
     */
    public WebHarvestSettings(Properties properties) {
        this.properties = new Properties();
        this.properties.putAll(properties);
        setDefaults();
    }
    
    /**
     * Loads settings from default location.
     * 
     * @return settings instance
     */
    public static WebHarvestSettings loadDefault() {
        // 1. Check system property
        String customPath = System.getProperty(SETTINGS_FILE_PROPERTY);
        if (customPath != null) {
            File customFile = new File(customPath);
            if (customFile.exists()) {
                try {
                    return fromFile(customFile);
                } catch (IOException e) {
                    LOG.warn("Failed to load settings from: {}", customPath, e);
                }
            }
        }
        
        // 2. Check user home
        String userHome = System.getProperty("user.home");
        File userHomeFile = new File(userHome, ".webharvest/" + DEFAULT_SETTINGS_FILE);
        if (userHomeFile.exists()) {
            try {
                return fromFile(userHomeFile);
            } catch (IOException e) {
                LOG.warn("Failed to load settings from: {}", userHomeFile, e);
            }
        }
        
        // 3. Check current directory
        File currentDirFile = new File(DEFAULT_SETTINGS_FILE);
        if (currentDirFile.exists()) {
            try {
                return fromFile(currentDirFile);
            } catch (IOException e) {
                LOG.warn("Failed to load settings from: {}", currentDirFile, e);
            }
        }
        
        // 4. Return default settings
        LOG.info("No settings file found, using defaults");
        return new WebHarvestSettings();
    }
    
    /**
     * Loads settings from specific file.
     * 
     * @param file the settings file
     * @return settings instance
     * @throws IOException if file cannot be read
     */
    public static WebHarvestSettings fromFile(File file) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        }
        LOG.info("Loaded settings from: {}", file.getAbsolutePath());
        return new WebHarvestSettings(props);
    }
    
    /**
     * Saves settings to file.
     * 
     * @param file the target file
     * @throws IOException if file cannot be written
     */
    public void save(File file) throws IOException {
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.store(fos, "WebHarvest Settings");
        }
        LOG.info("Saved settings to: {}", file.getAbsolutePath());
    }
    
    private void setDefaults() {
        // Set default values if not present
        if (!properties.containsKey("workdir")) {
            properties.setProperty("workdir", ".");
        }
        if (!properties.containsKey("debug")) {
            properties.setProperty("debug", "false");
        }
    }
    
    // Getters
    
    public String getWorkDir() {
        return properties.getProperty("workdir");
    }
    
    public Boolean getDebug() {
        String value = properties.getProperty("debug");
        return value != null ? Boolean.parseBoolean(value) : null;
    }
    
    public String getLogLevel() {
        return properties.getProperty("loglevel");
    }
    
    public String getLogPropsFile() {
        return properties.getProperty("logpropsfile");
    }
    
    public String getProxyHost() {
        return properties.getProperty("proxyhost");
    }
    
    public Integer getProxyPort() {
        String value = properties.getProperty("proxyport");
        return value != null ? Integer.parseInt(value) : null;
    }
    
    public String getProxyUser() {
        return properties.getProperty("proxyuser");
    }
    
    public String getProxyPassword() {
        return properties.getProperty("proxypassword");
    }
    
    public String getProxyNtHost() {
        return properties.getProperty("proxynthost");
    }
    
    public String getProxyNtDomain() {
        return properties.getProperty("proxyntdomain");
    }
    
    public String getDbDrivers() {
        return properties.getProperty("dbdrivers");
    }
    
    public String getPlugins() {
        return properties.getProperty("plugins");
    }
    
    // Setters
    
    public void setWorkDir(String workDir) {
        properties.setProperty("workdir", workDir);
    }
    
    public void setDebug(Boolean debug) {
        properties.setProperty("debug", String.valueOf(debug));
    }
    
    public void setLogLevel(String logLevel) {
        if (logLevel != null) {
            properties.setProperty("loglevel", logLevel);
        }
    }
    
    public void setLogPropsFile(String logPropsFile) {
        if (logPropsFile != null) {
            properties.setProperty("logpropsfile", logPropsFile);
        }
    }
    
    public void setProxyHost(String proxyHost) {
        if (proxyHost != null) {
            properties.setProperty("proxyhost", proxyHost);
        }
    }
    
    public void setProxyPort(Integer proxyPort) {
        if (proxyPort != null) {
            properties.setProperty("proxyport", String.valueOf(proxyPort));
        }
    }
    
    public void setProxyUser(String proxyUser) {
        if (proxyUser != null) {
            properties.setProperty("proxyuser", proxyUser);
        }
    }
    
    public void setProxyPassword(String proxyPassword) {
        if (proxyPassword != null) {
            properties.setProperty("proxypassword", proxyPassword);
        }
    }
    
    public void setProxyNtHost(String proxyNtHost) {
        if (proxyNtHost != null) {
            properties.setProperty("proxynthost", proxyNtHost);
        }
    }
    
    public void setProxyNtDomain(String proxyNtDomain) {
        if (proxyNtDomain != null) {
            properties.setProperty("proxyntdomain", proxyNtDomain);
        }
    }
    
    public void setDbDrivers(String dbDrivers) {
        if (dbDrivers != null) {
            properties.setProperty("dbdrivers", dbDrivers);
        }
    }
    
    public void setPlugins(String plugins) {
        if (plugins != null) {
            properties.setProperty("plugins", plugins);
        }
    }
    
    /**
     * Gets all properties.
     * 
     * @return the properties
     */
    public Properties getProperties() {
        return (Properties) properties.clone();
    }
}

