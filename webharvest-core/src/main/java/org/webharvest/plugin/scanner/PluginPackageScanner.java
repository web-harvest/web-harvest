/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.annotations.CorePlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Scanner for automatically discovering and registering plugins from specified packages.
 * 
 * <h3>Bug #40: Package Scanning for Plugins</h3>
 * <p>
 * This class implements the feature requested in Bug #40 to allow plugins from other
 * packages to be automatically scanned and registered. It uses reflection to find all
 * classes annotated with {@link CorePlugin} in the specified packages.
 * </p>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * CorePluginRegistry registry = CorePluginRegistry.getInstance();
 * PluginPackageScanner scanner = new PluginPackageScanner(registry);
 * 
 * // Scan custom packages
 * scanner.scanPackages("com.example.plugins", "com.mycompany.webharvest");
 * 
 * // Or via system property
 * // java -Dwebharvest.plugin.packages=com.example.plugins,com.other.plugins
 * scanner.scanSystemPropertyPackages();
 * }</pre>
 * 
 * <h3>System Property Configuration</h3>
 * <p>
 * Packages can be configured via system property:
 * {@code webharvest.plugin.packages} - comma-separated list of package names
 * </p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @see CorePlugin
 * @see CorePluginRegistry
 */
public class PluginPackageScanner {
    
    private static final Logger LOG = LoggerFactory.getLogger(PluginPackageScanner.class);
    
    /**
     * System property for configuring packages to scan.
     */
    public static final String PLUGIN_PACKAGES_PROPERTY = "webharvest.plugin.packages";
    
    private final CorePluginRegistry registry;
    private final List<String> packagesToScan;
    
    /**
     * Creates a new scanner with the specified registry.
     * 
     * @param registry the plugin registry to register discovered plugins
     */
    public PluginPackageScanner(CorePluginRegistry registry) {
        this.registry = registry;
        this.packagesToScan = new ArrayList<>();
    }
    
    /**
     * Creates a new scanner with the specified registry and packages to scan.
     * 
     * @param registry the plugin registry to register discovered plugins
     * @param packages the packages to scan for plugins
     */
    public PluginPackageScanner(CorePluginRegistry registry, String... packages) {
        this.registry = registry;
        this.packagesToScan = new ArrayList<>();
        for (String pkg : packages) {
            this.packagesToScan.add(pkg);
        }
    }
    
    /**
     * Scans the specified packages for plugins annotated with {@link CorePlugin}.
     * 
     * @param packages the packages to scan
     */
    public void scanPackages(String... packages) {
        for (String packageName : packages) {
            LOG.info("Scanning package for plugins: {}", packageName);
            scanPackage(packageName);
        }
    }
    
    /**
     * Scans packages specified in system property {@code webharvest.plugin.packages}.
     */
    public void scanSystemPropertyPackages() {
        String packagesProperty = System.getProperty(PLUGIN_PACKAGES_PROPERTY);
        if (packagesProperty != null && !packagesProperty.trim().isEmpty()) {
            String[] packages = packagesProperty.split(",");
            for (String pkg : packages) {
                String trimmed = pkg.trim();
                if (!trimmed.isEmpty()) {
                    scanPackages(trimmed);
                }
            }
        }
    }
    
    /**
     * Scans a single package for plugins.
     * 
     * @param packageName the package to scan
     */
    private void scanPackage(String packageName) {
        try {
            List<Class<?>> classes = getClassesInPackage(packageName);
            int registeredCount = 0;
            
            for (Class<?> clazz : classes) {
                if (isPluginClass(clazz)) {
                    registerPluginClass(clazz);
                    registeredCount++;
                }
            }
            
            LOG.info("Registered {} plugins from package: {}", registeredCount, packageName);
            
        } catch (Exception e) {
            LOG.warn("Failed to scan package: {} - {}", packageName, e.getMessage());
        }
    }
    
    /**
     * Checks if a class is a valid plugin class.
     * 
     * @param clazz the class to check
     * @return true if it's a plugin class, false otherwise
     */
    private boolean isPluginClass(Class<?> clazz) {
        // Must have @CorePlugin annotation
        if (!clazz.isAnnotationPresent(CorePlugin.class)) {
            return false;
        }
        
        // Must be a concrete class (not interface, not abstract)
        if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }
        
        // Must extend Plugin
        if (!Plugin.class.isAssignableFrom(clazz)) {
            LOG.warn("Class {} has @CorePlugin but doesn't extend Plugin", clazz.getName());
            return false;
        }
        
        return true;
    }
    
    /**
     * Registers a plugin class in the registry.
     * 
     * @param clazz the plugin class
     */
    @SuppressWarnings("unchecked")
    private void registerPluginClass(Class<?> clazz) {
        try {
            CorePlugin annotation = clazz.getAnnotation(CorePlugin.class);
            String elementName = annotation.elementName();
            
            // Cast to Class<? extends Plugin>
            Class<? extends Plugin> pluginClass = (Class<? extends Plugin>) clazz;
            
            // Register in CorePluginRegistry
            registry.registerMapping(null, pluginClass, elementName);
            
            LOG.debug("Registered plugin: {} -> {}", elementName, clazz.getName());
            
        } catch (Exception e) {
            LOG.error("Failed to register plugin class: {}", clazz.getName(), e);
        }
    }
    
    /**
     * Gets all classes in the specified package.
     * 
     * @param packageName the package name
     * @return list of classes
     * @throws IOException if there's an error reading the package
     * @throws ClassNotFoundException if a class can't be loaded
     */
    private List<Class<?>> getClassesInPackage(String packageName) 
            throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        Enumeration<URL> resources = classLoader.getResources(path);
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());
            
            if (directory.exists()) {
                classes.addAll(findClasses(directory, packageName));
            }
        }
        
        return classes;
    }
    
    /**
     * Recursively finds all classes in a directory.
     * 
     * @param directory the directory to search
     * @param packageName the package name
     * @return list of classes
     * @throws ClassNotFoundException if a class can't be loaded
     */
    private List<Class<?>> findClasses(File directory, String packageName) 
            throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        
        if (!directory.exists()) {
            return classes;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Recursive scan of subdirectories
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                // Load the class
                String className = packageName + '.' + 
                    file.getName().substring(0, file.getName().length() - 6);
                
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    LOG.warn("Could not load class: {}", className);
                } catch (NoClassDefFoundError e) {
                    // Ignore classes that can't be loaded (e.g., test classes in production)
                    LOG.trace("Skipping class with missing dependencies: {}", className);
                }
            }
        }
        
        return classes;
    }
}

