package org.webharvest.plugin.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.utils.ClassPathScanner;
import org.webharvest.utils.ClassPathScannerImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * Dynamic scanner for discovering all plugin classes at runtime.
 * Replaces static PluginClassIndex with classpath-based discovery.
 * 
 * <p>Scans the Core and optional extension packages for classes annotated with
 * {@link CorePlugin}. Empty results are accepted for optional packages that are
 * absent from the runtime classpath. A failed package scan or an empty required
 * Core scan uses the complete compatibility fallback rather than caching partial
 * discovery results.</p>
 * 
 * <p>Successful discovery results are cached. A fallback result is returned for
 * the current call but is not cached, allowing a later call to retry scanning.</p>
 * 
 * @author WebHarvest Team
 * @since 2.3.0
 */
public final class PluginClassScanner {
    
    private static final Logger LOG = LoggerFactory.getLogger(PluginClassScanner.class);
    
    /**
     * Packages to scan for plugins (all modules)
     */
    private static final String[] PLUGIN_PACKAGES = {
        "org.webharvest.plugin.core",
        "org.webharvest.plugin.ftp",
        "org.webharvest.plugin.mail",
        "org.webharvest.plugin.db",
        "org.webharvest.plugin.zip",
        "org.webharvest.plugin.webbrowser"
    };
    
    /**
     * Fallback static list (for backward compatibility if scanning fails)
     */
    private static final List<String> FALLBACK_CLASSES = createFallbackList();
    
    private static volatile List<String> cachedClassNames = null;
    
    private PluginClassScanner() {
        // Utility class
    }
    
    /**
     * Gets all plugin class names by scanning the classpath.
     * Successful discovery results are cached; fallback results are retried on the next call.
     * 
     * @return list of fully qualified class names
     */
    public static List<String> allPluginClassNames() {
        return allPluginClassNames(PLUGIN_PACKAGES, PluginClassScanner::scanPackage, FALLBACK_CLASSES);
    }

    /**
     * Resolves plugin names with an injectable scanner and caches only successful discovery.
     *
     * @param packageNames configured packages to scan
     * @param packageScanner function that scans one package
     * @param fallback complete fallback list used after a scan failure
     * @return discovered names or the fallback list
     */
    static List<String> allPluginClassNames(
            String[] packageNames,
            Function<String, Set<Class<?>>> packageScanner,
            List<String> fallback) {
        if (cachedClassNames != null) {
            return cachedClassNames;
        }

        synchronized (PluginClassScanner.class) {
            if (cachedClassNames != null) {
                return cachedClassNames;
            }

            List<String> discovered = scanWithFallback(packageNames, packageScanner, fallback);
            if (discovered != fallback) {
                cachedClassNames = discovered;
            }
            LOG.info("Resolved {} plugin class names", discovered.size());
            return discovered;
        }
    }
    
    /**
     * Scans one package for classes annotated with {@link CorePlugin}.
     *
     * @param packageName package to scan
     * @return discovered plugin classes; empty when an optional package is absent
     */
    private static Set<Class<?>> scanPackage(String packageName) {
        ClassPathScanner scanner = new ClassPathScannerImpl(packageName);
        return scanner.getTypesAnnotatedWith(CorePlugin.class);
    }

    /**
     * Uses the complete fallback list if any configured package scan fails.
     * Empty results are valid for optional packages that are not on the classpath.
     * Fallback results are not cached by {@link #allPluginClassNames()}.
     *
     * @param packageNames configured plugin packages
     * @param packageScanner package scan function
     * @param fallback complete compatibility fallback list
     * @return discovered class names or the complete fallback list
     */

    static List<String> scanWithFallback(
            String[] packageNames,
            Function<String, Set<Class<?>>> packageScanner,
            List<String> fallback) {
        try {
            return scanPluginPackages(packageNames, packageScanner);
        } catch (RuntimeException e) {
            LOG.warn("Plugin package scanning failed; using complete fallback list", e);
            return fallback;
        }
    }

    private static List<String> scanPluginPackages(
            String[] packageNames,
            Function<String, Set<Class<?>>> packageScanner) {
        Set<String> classNames = new LinkedHashSet<>();

        for (String packageName : packageNames) {
            final Set<Class<?>> pluginClasses;
            try {
                pluginClasses = packageScanner.apply(packageName);
            } catch (RuntimeException e) {
                throw new IllegalStateException("Failed to scan plugin package " + packageName, e);
            }

            if (pluginClasses == null) {
                throw new IllegalStateException("Plugin package scanner returned null for " + packageName);
            }

            for (Class<?> clazz : pluginClasses) {
                if (isValidPluginClass(clazz)) {
                    classNames.add(clazz.getName());
                    LOG.debug("Discovered plugin: {} -> {}",
                            clazz.getAnnotation(CorePlugin.class).elementName(),
                            clazz.getName());
                }
            }

            LOG.debug("Scanned package {}: found {} plugin classes", packageName, pluginClasses.size());
        }

        if (classNames.stream().noneMatch(name -> name.startsWith("org.webharvest.plugin.core."))) {
            throw new IllegalStateException("Required Core plugin package scan produced no plugin classes");
        }

        List<String> result = new ArrayList<>(classNames);
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }
    
    /**
     * Validates that a class is a valid plugin class.
     * 
     * @param clazz the class to validate
     * @return true if valid plugin class
     */
    private static boolean isValidPluginClass(Class<?> clazz) {
        // Must have @CorePlugin annotation (already filtered by scanner)
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
     * Creates fallback static list for backward compatibility.
     * This is used if dynamic scanning fails.
     * 
     * @return fallback list of plugin class names
     */
    private static List<String> createFallbackList() {
        List<String> names = new ArrayList<>();
        
        // Core plugin classes
        Collections.addAll(names,
                "org.webharvest.plugin.core.LoopPlugin",
                "org.webharvest.plugin.core.WhilePlugin",
                "org.webharvest.plugin.core.IfPlugin",
                "org.webharvest.plugin.core.ElsePlugin",
                "org.webharvest.plugin.core.CasePlugin",
                "org.webharvest.plugin.core.TryPlugin",
                "org.webharvest.plugin.core.CatchPlugin",
                "org.webharvest.plugin.core.ScriptPlugin",
                "org.webharvest.plugin.core.TemplatePlugin",
                "org.webharvest.plugin.core.XsltPlugin",
                "org.webharvest.plugin.core.StylesheetPlugin",
                "org.webharvest.plugin.core.HtmlToXmlPlugin",
                "org.webharvest.plugin.core.JsonToXmlPlugin",
                "org.webharvest.plugin.core.XmlToJsonPlugin",
                "org.webharvest.plugin.core.XPathPlugin",
                "org.webharvest.plugin.core.XQueryPlugin",
                "org.webharvest.plugin.core.XQExpressionPlugin",
                "org.webharvest.plugin.core.XQParamPlugin",
                "org.webharvest.plugin.core.TextPlugin",
                "org.webharvest.plugin.core.ConstantPlugin",
                "org.webharvest.plugin.core.EmptyPlugin",
                "org.webharvest.plugin.core.ListPlugin",
                "org.webharvest.plugin.core.DefPlugin",
                "org.webharvest.plugin.core.GetVarPlugin",
                "org.webharvest.plugin.core.ValueOfPlugin",
                "org.webharvest.plugin.core.TokenizePlugin",
                "org.webharvest.plugin.core.FunctionPlugin",
                "org.webharvest.plugin.core.CallPlugin",
                "org.webharvest.plugin.core.CallParamPlugin",
                "org.webharvest.plugin.core.ReturnPlugin",
                "org.webharvest.plugin.core.HttpPlugin",
                "org.webharvest.plugin.core.HttpHeaderPlugin",
                "org.webharvest.plugin.core.HttpParamPlugin",
                "org.webharvest.plugin.core.FilePlugin",
                "org.webharvest.plugin.core.IncludePlugin",
                "org.webharvest.plugin.core.RegexpPlugin",
                "org.webharvest.plugin.core.RegexpPatternPlugin",
                "org.webharvest.plugin.core.RegexpSourcePlugin",
                "org.webharvest.plugin.core.RegexpResultPlugin",
                "org.webharvest.plugin.core.SleepPlugin",
                "org.webharvest.plugin.core.ExitPlugin",
                "org.webharvest.plugin.core.LogPlugin",
                "org.webharvest.plugin.core.XMLPlugin");

        // Optional extension modules (loaded reflectively if present)
        Collections.addAll(names,
                "org.webharvest.plugin.ftp.FtpPlugin",
                "org.webharvest.plugin.ftp.FtpGetPlugin",
                "org.webharvest.plugin.ftp.FtpPutPlugin",
                "org.webharvest.plugin.ftp.FtpListPlugin",
                "org.webharvest.plugin.mail.MailPlugin",
                "org.webharvest.plugin.db.DatabasePlugin",
                "org.webharvest.plugin.db.ConnectionPlugin",
                "org.webharvest.plugin.zip.ZipPlugin",
                "org.webharvest.plugin.zip.ZipEntryPlugin",
                "org.webharvest.plugin.webbrowser.WebBrowserPlugin");

        return Collections.unmodifiableList(names);
    }
    
    /**
     * Clears the cache, forcing a new scan on next access.
     * Useful for testing or when plugins are dynamically loaded.
     */
    public static void clearCache() {
        synchronized (PluginClassScanner.class) {
            cachedClassNames = null;
        }
    }
}

