package org.webharvest.runtime;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.definition.PluginConfigurationTree;
import org.webharvest.plugin.DefaultPluginBodyExecutor;
import org.webharvest.plugin.PluginBodyExecutor;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Executes the root {@code <config>} element without relying on the legacy ConfigPlugin.
 * Responsible for applying runtime settings (charset, scripting language) and
 * delegating execution of child plugins using {@link PluginBodyExecutor}.
 */
class RootConfigurationExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootConfigurationExecutor.class);

    private static final Set<String> ALLOWED_SCRIPT_LANGUAGES = Set.of("beanshell", "javascript", "groovy");

    private final PluginBodyExecutor bodyExecutor;

    RootConfigurationExecutor() {
        this(new DefaultPluginBodyExecutor());
    }

    RootConfigurationExecutor(PluginBodyExecutor bodyExecutor) {
        this.bodyExecutor = bodyExecutor;
    }

    void execute(PluginConfigurationTree rootTree, DynamicScopeContext context) throws InterruptedException {
        if (rootTree == null) {
            LOGGER.warn("Root configuration tree is null; nothing to execute");
            return;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Root tree has {} direct children", rootTree.getChildCount());
            for (PluginConfigurationTree child : rootTree.getChildren()) {
                LOGGER.debug("Root child element: {}", child.getPluginElementName());
            }
        }

        PluginConfiguration configuration = rootTree.getConfiguration();
        if (configuration == null) {
            LOGGER.warn("Root configuration is null; nothing to execute");
            return;
        }

        applyRuntimeSettings(configuration, context);
        ensureChildConfigurationConsistency(rootTree, configuration);
        LOGGER.debug("Root config children after sync: {}", configuration.getChildren().size());

        Variable result = bodyExecutor.executeBody(configuration, context);
        context.setLocalVar("configResult", result != null ? result : EmptyVariable.INSTANCE);
    }

    private void applyRuntimeSettings(PluginConfiguration configuration, DynamicScopeContext context) {
        String charsetName = configuration.getProperty("charset", "UTF-8");
        charsetName = charsetName.trim();

        if (!charsetName.isEmpty()) {
            charsetName = validateCharset(charsetName);
        } else {
            charsetName = "UTF-8";
        }

        context.setCharset(charsetName);
        LOGGER.debug("Root configuration charset set to {}", charsetName);

        String scriptLang = configuration.getProperty("scriptlang", "beanshell");
        scriptLang = scriptLang.trim().toLowerCase(Locale.ROOT);

        if (!ALLOWED_SCRIPT_LANGUAGES.contains(scriptLang)) {
            LOGGER.warn("Invalid scriptlang '{}' provided. Falling back to 'beanshell'", scriptLang);
            scriptLang = "beanshell";
        }

        ScriptingLanguage scriptingLanguage = ScriptingLanguage.recognize(scriptLang);
        if (scriptingLanguage == null) {
            scriptingLanguage = ScriptingLanguage.BEANSHELL;
        }

        context.setScriptingLanguage(scriptingLanguage);
        LOGGER.debug("Root configuration script language set to {}", scriptingLanguage);
    }

    private String validateCharset(String charsetName) {
        try {
            Charset.forName(charsetName);
            return charsetName;
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Unsupported charset '{}' provided. Falling back to UTF-8", charsetName);
            return "UTF-8";
        }
    }

    private void ensureChildConfigurationConsistency(PluginConfigurationTree rootTree, PluginConfiguration configuration) {
        if (!rootTree.hasChildren()) {
            LOGGER.debug("Root tree reports no children");
            return;
        }

        List<PluginConfiguration> childConfigs = configuration.getChildren();
        if (childConfigs != null && !childConfigs.isEmpty()) {
            LOGGER.debug("Configuration already has {} children", childConfigs.size());
            return;
        }

        if (configuration instanceof org.webharvest.plugin.DefaultPluginConfiguration) {
            List<PluginConfiguration> syncedChildren = new ArrayList<>();
            for (PluginConfigurationTree childTree : rootTree.getChildren()) {
                syncedChildren.add(childTree.getConfiguration());
            }
            ((org.webharvest.plugin.DefaultPluginConfiguration) configuration).setChildren(syncedChildren);
            LOGGER.debug("Synchronized {} children into configuration", syncedChildren.size());
        }
    }
}

