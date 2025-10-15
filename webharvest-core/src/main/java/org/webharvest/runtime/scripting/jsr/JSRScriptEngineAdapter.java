/*
 Copyright (c) 2006-2012 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
*/

package org.webharvest.runtime.scripting.jsr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.exception.ScriptException;

import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.scripting.ScriptEngine;
import org.webharvest.runtime.scripting.ScriptSource;
import org.webharvest.runtime.variables.ScriptingVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.KeyValuePair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Adapter design pattern implementation. Adapts provided
 * {@link javax.script.ScriptEngine} representing JSR-223 script engine to the
 * {@link org.webharvest.runtime.scripting.ScriptEngine} interface. This adapter
 * is universal for all scripting language implementations supporting JSR-223
 * specification.
 * <p/>
 * It is important to bear in mind that this implementation is not thread-safe,
 * that is, {@link javax.script.ScriptEngine} being adapted can not be shared
 * between multiple threads. Bindings from scraper's {@link DynamicScopeContext}
 * are copied directly to the {@link ScriptEngine}. This is performance
 * trade-off, since currently creation of brand new
 * {@link javax.script.ScriptContext} instances each time script is evaluated is
 * quite expensive.
 * 
 * <p><b>v2.2.1 CRITICAL FIX (October 2025):</b> Fixed 18-year-old bug in {@link #copyVariables(DynamicScopeContext)}.
 * Previously, {@link Variable} objects were passed directly to BeanShell for non-{@link ScriptingVariable} types,
 * causing "Operator '+' inappropriate for objects" errors. Now always calls {@link Variable#getWrappedObject()}
 * and performs automatic String→Number conversion for arithmetic operations.</p>
 * 
 * <p><b>Bug #7 FIX (October 2025):</b> Fixed function parameter scope pollution where variables stayed in
 * ScriptEngine "forever" across multiple evaluations. Previously, {@link #copyVariables(DynamicScopeContext)}
 * only ADDED variables via {@code put()} but never CLEARED old ones, causing second function call to see
 * parameters from first call. Now calls {@link #clearJSRScriptContextAttributes()} before copying to ensure
 * clean scope for each evaluation. Reported on SourceForge as incorrect scope behavior vs v2.0.</p>
 *
 * @see ScriptEngine
 *
 * @author Piotr Dyraga
 * @since 2.1.0-SNAPSHOT
 * @version 2.2.1
 */
public final class JSRScriptEngineAdapter implements ScriptEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSRScriptEngineAdapter.class);
    
    /**
     * Bug #7: System property to enable strict scope checking.
     * When enabled, throws exception if scope pollution is detected.
     * Default: false (lenient mode - warnings only)
     */
    public static final String STRICT_SCOPE_PROPERTY = "webharvest.script.scope.strict";

    private javax.script.ScriptEngine adaptee;

    /**
     * {@link JSRScriptEngineAdapter} constructor accepting reference to the
     * not-{@code null} {@link javax.script.ScriptEngine} delegate.
     *
     * @param adaptee
     *            adaptee reference; mandatory, must not be {@code null}
     */
    public JSRScriptEngineAdapter(final javax.script.ScriptEngine adaptee) {
        if (adaptee == null) {
            throw new IllegalArgumentException(
                    "Adaptee engine must not be null");
        }
        this.adaptee = adaptee;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object evaluate(final DynamicScopeContext context,
            final ScriptSource script) {
        try {
            copyVariables(context);
            return adaptee.eval(script.getSourceCode());
        } catch (javax.script.ScriptException e) {
            throw new ScriptException(e);
        }
    }

    private void copyVariables(final DynamicScopeContext context) {
        // Bug #7 FIX (SourceForge Patch #7): Clear bindings BEFORE copying
        // Prevents scope pollution where function parameters stay "forever"
        
        // Bug #7 DETECTION: Check for scope pollution BEFORE clearing
        detectScopePollution(context);
        
        // Bug #7 FIX: Clear all bindings
        clearJSRScriptContextAttributes();
        
        for (KeyValuePair<Variable> pair : context) {
            final Variable value = pair.getValue();
            LOGGER.trace("copyVariables: Copying variable '{}' = {}", pair.getKey(), value);

            // v2.2.x FIX: ALL Variable types have getWrappedObject() method!
            // Use it directly instead of instanceof check.
            // This fixes: "Operator '+' inappropriate for objects" in BeanShell
            // NOTE: The old code only called getWrappedObject() for ScriptingVariable,
            //       passing raw Variable objects to BeanShell for other types.
            //       This caused type errors in arithmetic/comparison operations.
            Object scriptValue = value.getWrappedObject();
            
            // v2.2.x FIX: Try to convert String to Number for BeanShell arithmetic
            // NodeVariable often wraps Strings (e.g. "5") but BeanShell needs Numbers
            // for arithmetic operations to work correctly.
            if (scriptValue instanceof String) {
                String str = ((String) scriptValue).trim();
                if (!str.isEmpty()) {
                    try {
                        // Try Integer first (no decimal point)
                        if (!str.contains(".") && !str.contains("e") && !str.contains("E")) {
                            scriptValue = Integer.parseInt(str);
                        } else {
                            // Try Double for floating point numbers
                            scriptValue = Double.parseDouble(str);
                        }
                    } catch (NumberFormatException e) {
                        // Keep as String - not a number
                    }
                }
            }
            
            adaptee.put(pair.getKey(), scriptValue);
        }
    }
    
    /**
     * Bug #7 DETECTION: Detects scope pollution before evaluation.
     * 
     * <p>
     * Checks if ScriptEngine bindings contain variables that are NOT in current context.
     * This indicates scope pollution from previous evaluations.
     * </p>
     * 
     * <h3>Behavior:</h3>
     * <ul>
     *   <li><strong>Strict mode</strong> (webharvest.script.scope.strict=true): Throws ScriptException</li>
     *   <li><strong>Lenient mode</strong> (default): Logs warning only</li>
     * </ul>
     * 
     * @param context Current DynamicScopeContext with expected variables
     * @throws ScriptException if strict mode and pollution detected
     */
    private void detectScopePollution(final DynamicScopeContext context) {
        final javax.script.ScriptContext scriptContext = adaptee.getContext();
        
        if (scriptContext == null) {
            return;
        }
        
        // Collect current context variable names
        java.util.Set<String> contextVars = new java.util.HashSet<>();
        for (KeyValuePair<Variable> pair : context) {
            contextVars.add(pair.getKey());
        }
        
        // Check ENGINE_SCOPE bindings for unexpected variables
        final javax.script.Bindings bindings = scriptContext.getBindings(javax.script.ScriptContext.ENGINE_SCOPE);
        
        if (bindings != null && !bindings.isEmpty()) {
            java.util.List<String> pollutedVars = new java.util.ArrayList<>();
            
            for (String key : bindings.keySet()) {
                // Skip internal BeanShell variables
                if (key.startsWith("bsh") || key.startsWith("org_beanshell")) {
                    continue;
                }
                
                // Check if variable is in current context
                if (!contextVars.contains(key)) {
                    pollutedVars.add(key);
                }
            }
            
            if (!pollutedVars.isEmpty()) {
                String message = String.format(
                    "Bug #7: Scope pollution detected! Variables from previous evaluation: %s. " +
                    "These variables are NOT in current context but remain in ScriptEngine. " +
                    "Set -Dwebharvest.script.scope.strict=false to disable this check.",
                    pollutedVars);
                
                boolean strictMode = Boolean.getBoolean(STRICT_SCOPE_PROPERTY);
                
                if (strictMode) {
                    LOGGER.error(message);
                    throw new ScriptException(new IllegalStateException(message));
                } else {
                    LOGGER.warn(message + " (lenient mode - continuing)");
                }
            }
        }
    }
    
    /**
     * Bug #7 FIX: Clears all bindings from JSR-223 ScriptContext.
     * 
     * <p>
     * This method removes ALL variables from the script engine's context across all scopes.
     * This is essential to prevent scope pollution where variables from previous evaluations
     * remain visible in subsequent evaluations.
     * </p>
     * 
     * <h3>Problem (Bug #7 - SourceForge):</h3>
     * <p>
     * Without this cleanup, variables/parameters stay in ScriptEngine "forever":
     * <pre>
     * // First call
     * context1.setVar("param1", "ValueA");
     * engine.evaluate(context1, script);  // param1 = "ValueA"
     * 
     * // Second call (param1 OMITTED)
     * context2 = new Context();  // NO param1!
     * engine.evaluate(context2, script);  // param1 STILL = "ValueA" ❌
     * </pre>
     * </p>
     * 
     * <h3>Solution:</h3>
     * <p>
     * Clear all bindings before {@link #copyVariables(DynamicScopeContext)}:
     * <pre>
     * clearJSRScriptContextAttributes();  // Remove ALL old variables
     * copyVariables(context);              // Add ONLY current context variables
     * </pre>
     * Now second call correctly has param1 = undefined/null as expected in v2.0.
     * </p>
     * 
     * <h3>Implementation Details:</h3>
     * <ul>
     *   <li>Iterates through ALL scopes in ScriptContext</li>
     *   <li>Gets Bindings for each scope</li>
     *   <li>Uses iterator.remove() to safely clear all entries</li>
     *   <li>Null-safe for testing environments</li>
     * </ul>
     * 
     * @since 2.2.1
     * @see #copyVariables(DynamicScopeContext)
     */
    /**
     * Bug #7 FIX (SourceForge Patch #7): Clears JSR-223 ScriptContext bindings.
     * 
     * Original patch by user - uses Iterator.remove() to clear all bindings.
     * This prevents scope pollution where variables from previous evaluations
     * remain visible in subsequent calls.
     */
    private void clearJSRScriptContextAttributes() {
        final javax.script.ScriptContext scriptContext = adaptee.getContext();
        
        if (scriptContext != null) {
            final List<Integer> scopes = scriptContext.getScopes();
            
            for (final Integer scope : scopes) {
                final javax.script.Bindings bindings = scriptContext.getBindings(scope);
                
                if (bindings != null) {
                    // Original patch: use Iterator.remove() for safe clearing
                    final Iterator<Entry<String, Object>> iterator = bindings.entrySet().iterator();
                    
                    while (iterator.hasNext()) {
                        iterator.next();
                        iterator.remove();
                    }
                }
            }
        }
    }
}
