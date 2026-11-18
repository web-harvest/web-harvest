package org.webharvest.plugin.scanner;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.core.HttpPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/** Regression tests for package resources exposed by non-directory classpaths. */
public class PluginPackageScannerClasspathTest {

    private static final String PACKAGE_NAME = "org.webharvest.plugin.core";
    private static final String PACKAGE_PATH = "org/webharvest/plugin/core";
    private static final String PLUGIN_CLASS_NAME = HttpPlugin.class.getName();
    private static final String PLUGIN_CLASS_RESOURCE =
            "/" + PACKAGE_PATH + "/HttpPlugin.class";

    private final CorePluginRegistry registry = CorePluginRegistry.getInstance();
    private ClassLoader originalContextLoader;

    @AfterMethod
    public void restoreState() {
        if (originalContextLoader != null) {
            Thread.currentThread().setContextClassLoader(originalContextLoader);
            originalContextLoader = null;
        }
        registry.clear();
    }

    @Test
    public void scansPackageWhenThreadContextClassLoaderIsNull() {
        originalContextLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(null);
        registry.clear();

        new PluginPackageScanner(registry).scanPackages(PACKAGE_NAME);

        assertTrue(registry.isPluginRegistered("http"),
                "The scanner should fall back to its defining classloader");
        assertSame(registry.getPluginClass("http"), HttpPlugin.class);
    }

    @Test
    public void discoversPluginClassFromJarPackageResource() throws Exception {
        originalContextLoader = Thread.currentThread().getContextClassLoader();
        registry.clear();
        Path jarPath = Files.createTempFile("webharvest-plugin-scan", ".jar");
        try {
            writePluginClassJar(jarPath);
            URL jarUrl = jarPath.toUri().toURL();
            URL packageResource = new URL("jar:" + jarUrl.toExternalForm() + "!/" + PACKAGE_PATH + "/");
            try (URLClassLoader jarLoader = new URLClassLoader(
                    new URL[] {jarUrl}, PluginPackageScanner.class.getClassLoader()) {
                @Override
                public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    if (!PLUGIN_CLASS_NAME.equals(name)) {
                        return super.loadClass(name, resolve);
                    }
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> loaded = findLoadedClass(name);
                        if (loaded == null) {
                            loaded = findClass(name);
                        }
                        if (resolve) {
                            resolveClass(loaded);
                        }
                        return loaded;
                    }
                }

                @Override
                public Enumeration<URL> getResources(String name) throws IOException {
                    if (PACKAGE_PATH.equals(name)) {
                        // Repeat the same root to verify class-name deduplication.
                        return Collections.enumeration(Arrays.asList(packageResource, packageResource));
                    }
                    return super.getResources(name);
                }
            }) {
                Thread.currentThread().setContextClassLoader(jarLoader);
                new PluginPackageScanner(registry).scanPackages(PACKAGE_NAME);

                assertTrue(registry.isPluginRegistered("http"),
                        "The scanner should enumerate .class entries inside a JAR");
                Class<? extends Plugin> discovered = registry.getPluginClass("http");
                assertEquals(discovered.getName(), PLUGIN_CLASS_NAME);
                assertSame(discovered.getClassLoader(), jarLoader,
                        "The plugin implementation must be loaded from the JAR that exposed it");
                assertTrue(Plugin.class.isAssignableFrom(discovered),
                        "The JAR-loaded implementation must remain compatible with the parent Plugin API");
            }
        } finally {
            Files.deleteIfExists(jarPath);
        }
    }

    private void writePluginClassJar(Path jarPath) throws IOException {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jarPath));
             InputStream classBytes = HttpPlugin.class.getResourceAsStream(PLUGIN_CLASS_RESOURCE)) {
            if (classBytes == null) {
                throw new IOException("Missing test fixture class resource " + PLUGIN_CLASS_RESOURCE);
            }
            output.putNextEntry(new JarEntry(PACKAGE_PATH + "/"));
            output.closeEntry();
            output.putNextEntry(new JarEntry(PACKAGE_PATH + "/HttpPlugin.class"));
            byte[] buffer = new byte[4096];
            int read;
            while ((read = classBytes.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.closeEntry();
        }
    }
}
