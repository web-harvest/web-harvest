/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.Plugin;

// Core plugins
import org.webharvest.plugin.core.*;

/**
 * Manually registers core plugins.
 * v2.2.x ULTRA-LEAN architecture: Replaces CorePluginsPostProcessor.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class CorePluginsRegistrar {
    
    private static final Logger LOG = LoggerFactory.getLogger(CorePluginsRegistrar.class);
    private static boolean initialized = false;
    
    /**
     * Registers all core plugins manually.
     */
    public static synchronized void registerCorePlugins() {
        if (initialized) {
            LOG.debug("Core plugins already registered");
            return;
        }
        
        LOG.info("Registering core plugins...");
        
        CorePluginRegistry registry = CorePluginRegistry.getInstance();
        
        // Manual registration of all core plugins
        // v2.2.x: registerMapping no longer requires Processor class
        registry.registerMapping(null, CallPlugin.class, "call");
        registry.registerMapping(null, CallParamPlugin.class, "call-param");
        registry.registerMapping(null, CasePlugin.class, "case");
        registry.registerMapping(null, CatchPlugin.class, "catch");
        registry.registerMapping(null, ConstantPlugin.class, "constant");
        registry.registerMapping(null, DefPlugin.class, "def");
        registry.registerMapping(null, ElsePlugin.class, "else");
        registry.registerMapping(null, EmptyPlugin.class, "empty");
        registry.registerMapping(null, ExitPlugin.class, "exit");
        registry.registerMapping(null, FilePlugin.class, "file");
        registry.registerMapping(null, FunctionPlugin.class, "function");
        registry.registerMapping(null, GetVarPlugin.class, "get");
        registry.registerMapping(null, HtmlToXmlPlugin.class, "html-to-xml");
        registry.registerMapping(null, HttpPlugin.class, "http");
        registry.registerMapping(null, HttpHeaderPlugin.class, "http-header");
        registry.registerMapping(null, HttpParamPlugin.class, "http-param");
        registry.registerMapping(null, IfPlugin.class, "if");
        registry.registerMapping(null, IncludePlugin.class, "include");
        registry.registerMapping(null, JsonToXmlPlugin.class, "json-to-xml");
        registry.registerMapping(null, LoopPlugin.class, "loop");
        registry.registerMapping(null, RegexpPlugin.class, "regexp");
        registry.registerMapping(null, RegexpPatternPlugin.class, "regexp-pattern");
        registry.registerMapping(null, RegexpResultPlugin.class, "regexp-result");
        registry.registerMapping(null, RegexpSourcePlugin.class, "regexp-source");
        registry.registerMapping(null, ReturnPlugin.class, "return");
        registry.registerMapping(null, ScriptPlugin.class, "script");
        registry.registerMapping(null, ListPlugin.class, "list");
        registry.registerMapping(null, LogPlugin.class, "log");
        registry.registerMapping(null, ValueOfPlugin.class, "value-of");
        registry.registerMapping(null, XMLPlugin.class, "xml");
        // Legacy XML compatibility: keep <set-var> available alongside <def>
        registry.registerMapping(null, SetVarPlugin.class, "set-var");
        registry.registerMapping(null, SleepPlugin.class, "sleep");
        registry.registerMapping(null, StylesheetPlugin.class, "stylesheet");
        registry.registerMapping(null, TemplatePlugin.class, "template");
        registry.registerMapping(null, TextPlugin.class, "text");
        registry.registerMapping(null, TokenizePlugin.class, "tokenize");
        registry.registerMapping(null, TryPlugin.class, "try");
        // VarPlugin removed - use DefPlugin instead
        registry.registerMapping(null, WhilePlugin.class, "while");
        registry.registerMapping(null, XmlToJsonPlugin.class, "xml-to-json");
        registry.registerMapping(null, XPathPlugin.class, "xpath");
        registry.registerMapping(null, XQueryPlugin.class, "xquery");
        registry.registerMapping(null, XQExpressionPlugin.class, "xq-expression");
        registry.registerMapping(null, XQParamPlugin.class, "xq-param");
        registry.registerMapping(null, XsltPlugin.class, "xslt");
        
        LOG.info("Registered 44 core plugins (including legacy set-var compatibility alias)");
        initialized = true;
    }
    
    /**
     * Resets the initialized flag (for testing).
     */
    public static synchronized void reset() {
        initialized = false;
        CorePluginRegistry.getInstance().clear();
        LOG.info("Core plugins registry reset");
    }
}

