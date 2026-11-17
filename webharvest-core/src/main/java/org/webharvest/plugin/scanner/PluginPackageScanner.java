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
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scanner for explicitly configured packages. Scanning is opt-in and does not
 * participate in the default Core plugin registration.
 */
public class PluginPackageScanner {

    private static final Logger LOG = LoggerFactory.getLogger(PluginPackageScanner.class);

    /** System property for configuring packages to scan. */
    public static final String PLUGIN_PACKAGES_PROPERTY = "webharvest.plugin.packages";

    private final CorePluginRegistry registry;
    private final List<String> packagesToScan;

    /** Creates a scanner with the specified registry. */
    public PluginPackageScanner(CorePluginRegistry registry) {
        this.registry = registry;
        this.packagesToScan = new ArrayList<>();
    }

    /** Creates a scanner with the specified registry and packages to scan. */
    public PluginPackageScanner(CorePluginRegistry registry, String... packages) {
        this.registry = registry;
        this.packagesToScan = new ArrayList<>();
        for (String pkg : packages) {
            this.packagesToScan.add(pkg);
        }
    }

    /** Scans the explicitly supplied packages for annotated plugins. */
    public void scanPackages(String... packages) {
        for (String packageName : packages) {
            if (packageName == null || packageName.trim().isEmpty()) {
                continue;
            }
            LOG.info("Scanning package for plugins: {}", packageName);
            scanPackage(packageName.trim());
        }
    }

    /** Scans packages configured by the webharvest.plugin.packages system property. */
    public void scanSystemPropertyPackages() {
        String packagesProperty = System.getProperty(PLUGIN_PACKAGES_PROPERTY);
        if (packagesProperty != null && !packagesProperty.trim().isEmpty()) {
            for (String pkg : packagesProperty.split(",")) {
                String trimmed = pkg.trim();
                if (!trimmed.isEmpty()) {
                    scanPackages(trimmed);
                }
            }
        }
    }

    private void scanPackage(String packageName) {
        int registeredCount = 0;
        try {
            Map<String, ClassLoader> classes = getClassesInPackage(packageName);
            for (Map.Entry<String, ClassLoader> candidate : classes.entrySet()) {
                try {
                    Class<?> clazz = Class.forName(candidate.getKey(), false, candidate.getValue());
                    if (isPluginClass(clazz) && registerPluginClass(clazz)) {
                        registeredCount++;
                    }
                } catch (ClassNotFoundException | LinkageError | SecurityException e) {
                    LOG.warn("Skipping unloadable class {} while scanning {}: {}",
                            candidate.getKey(), packageName, e.toString());
                }
            }
            LOG.info("Registered {} plugins from package: {}", registeredCount, packageName);
        } catch (IOException e) {
            LOG.warn("Failed to enumerate package {}: {}", packageName, e.toString());
        }
    }

    private boolean isPluginClass(Class<?> clazz) {
        try {
            if (!clazz.isAnnotationPresent(CorePlugin.class)) {
                return false;
            }
            int modifiers = clazz.getModifiers();
            return !clazz.isInterface()
                    && !java.lang.reflect.Modifier.isAbstract(modifiers)
                    && Plugin.class.isAssignableFrom(clazz);
        } catch (LinkageError | SecurityException e) {
            LOG.warn("Cannot inspect potential plugin class {}: {}", clazz.getName(), e.toString());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean registerPluginClass(Class<?> clazz) {
        try {
            CorePlugin annotation = clazz.getAnnotation(CorePlugin.class);
            String elementName = annotation.elementName();
            Class<? extends Plugin> pluginClass = (Class<? extends Plugin>) clazz;
            registry.registerMapping(null, pluginClass, elementName);
            LOG.debug("Registered plugin: {} -> {}", elementName, clazz.getName());
            return true;
        } catch (RuntimeException | LinkageError e) {
            LOG.error("Failed to register plugin class " + clazz.getName(), e);
            return false;
        }
    }

    /**
     * Enumerates class names from both exploded directories and JAR package
     * entries. Each class name is associated with the loader that exposed it.
     */
    private Map<String, ClassLoader> getClassesInPackage(String packageName) throws IOException {
        String path = packageName.replace('.', '/');
        Map<String, ClassLoader> classes = new LinkedHashMap<>();
        for (ClassLoader loader : getCandidateClassLoaders()) {
            Enumeration<URL> resources = loader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    try {
                        findClasses(new File(resource.toURI()), packageName, loader, classes);
                    } catch (java.net.URISyntaxException e) {
                        LOG.warn("Invalid package resource URL {}: {}", resource, e.toString());
                    }
                } else if ("jar".equals(resource.getProtocol())) {
                    findClassesInJar(resource, path, loader, classes);
                } else {
                    LOG.debug("Ignoring unsupported package resource protocol {} for {}",
                            resource.getProtocol(), resource);
                }
            }
        }
        return classes;
    }

    private List<ClassLoader> getCandidateClassLoaders() {
        Set<ClassLoader> loaders = new LinkedHashSet<>();
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null) {
            loaders.add(contextLoader);
        }
        ClassLoader definingLoader = PluginPackageScanner.class.getClassLoader();
        if (definingLoader != null) {
            loaders.add(definingLoader);
        }
        if (loaders.isEmpty()) {
            loaders.add(ClassLoader.getSystemClassLoader());
        }
        return new ArrayList<>(loaders);
    }

    private void findClasses(File directory, String packageName, ClassLoader loader,
                             Map<String, ClassLoader> classes) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                findClasses(file, packageName + "." + file.getName(), loader, classes);
            } else if (file.getName().endsWith(".class")) {
                String simpleName = file.getName().substring(0, file.getName().length() - 6);
                addClassName(packageName + "." + simpleName, loader, classes);
            }
        }
    }

    private void findClassesInJar(URL resource, String packagePath, ClassLoader loader,
                                  Map<String, ClassLoader> classes) throws IOException {
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        connection.setUseCaches(false);
        try (JarFile jar = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            String prefix = packagePath.endsWith("/") ? packagePath : packagePath + "/";
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith(prefix) && name.endsWith(".class")) {
                    String className = name.substring(0, name.length() - 6).replace('/', '.');
                    addClassName(className, loader, classes);
                }
            }
        }
    }

    private void addClassName(String className, ClassLoader loader, Map<String, ClassLoader> classes) {
        if (!className.endsWith("module-info") && !className.endsWith("package-info")) {
            classes.putIfAbsent(className, loader);
        }
    }
}
