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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dynamic scanner for discovering all plugin classes at runtime.
 * Replaces static PluginClassIndex with classpath-based discovery.
 * 
 * <p>Scans all plugin packages (core, ftp, mail, database, zip, webbrowser)
 * to discover classes annotated with {@link CorePlugin}.</p>
 * 
 * <p>Results are cached for performance. Scanning happens once at first access.</p>
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
    private static final AtomicBoolean scanInProgress = new AtomicBoolean(false);
    
    private PluginClassScanner() {
        // Utility class
    }
    
    /**
     * Gets all plugin class names by scanning the classpath.
     * Results are cached after first scan.
     * 
     * @return list of fully qualified class names
     */
    public static List<String> allPluginClassNames() {
        if (cachedClassNames != null) {
            return cachedClassNames;
        }
        
        synchronized (PluginClassScanner.class) {
            if (cachedClassNames != null) {
                return cachedClassNames;
            }
            
            if (scanInProgress.compareAndSet(false, true)) {
                try {
                    cachedClassNames = scanPluginClasses();
                    LOG.info("Discovered {} plugin classes via dynamic scanning", cachedClassNames.size());
                } catch (Exception e) {
                    LOG.warn("Dynamic plugin scanning failed, using fallback list: {}", e.getMessage());
                    cachedClassNames = FALLBACK_CLASSES;
                } finally {
                    scanInProgress.set(false);
                }
            } else {
                // Another thread is scanning, wait and use fallback temporarily
                cachedClassNames = FALLBACK_CLASSES;
            }
        }
        
        return cachedClassNames;
    }
    
    /**
     * Scans all plugin packages for classes annotated with @CorePlugin.
     * 
     * @return list of discovered plugin class names
     */
    private static List<String> scanPluginClasses() {
        Set<String> classNames = new LinkedHashSet<>();
        
        for (String packageName : PLUGIN_PACKAGES) {
            try {
                ClassPathScanner scanner = new ClassPathScannerImpl(packageName);
                Set<Class<?>> pluginClasses = scanner.getTypesAnnotatedWith(CorePlugin.class);
                
                for (Class<?> clazz : pluginClasses) {
                    if (isValidPluginClass(clazz)) {
                        classNames.add(clazz.getName());
                        LOG.debug("Discovered plugin: {} -> {}", 
                            clazz.getAnnotation(CorePlugin.class).elementName(), 
                            clazz.getName());
                    }
                }
                
                LOG.debug("Scanned package {}: found {} plugin classes", packageName, pluginClasses.size());
                
            } catch (Exception e) {
                LOG.warn("Failed to scan package {}: {}", packageName, e.getMessage());
            }
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
                "org.webharvest.plugin.core.BodyProcessorPlugin",
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
            scanInProgress.set(false);
        }
    }
}

