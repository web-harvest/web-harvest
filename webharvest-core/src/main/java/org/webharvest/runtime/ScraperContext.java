/*  Copyright (c) 2006-2025 the original author or authors.
    All rights reserved.

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

    You can contact Vladimir Nikic by sending e-mail to
    nikic_vladimir@yahoo.com. Please include the word "Web-Harvest" in the
    subject line.
*/
package org.webharvest.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.definition.Config;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.scripting.ScriptEngineFactory;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.ExecutionOwner;
import org.webharvest.runtime.session.ExecutionTrackedSession;
import org.webharvest.runtime.resource.ExecutionResourceAccess;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ScriptingVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.variables.VariableName;
import org.webharvest.utils.Assert;
import org.webharvest.utils.CommonUtil;
import org.webharvest.utils.KeyValuePair;
import org.webharvest.utils.Stack;
import org.webharvest.utils.SystemUtilities;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

/**
 * Context of scraper execution. All the variables created during
 * scraper execution are stored in this context.
 */
public class ScraperContext implements DynamicScopeContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScraperContext.class);

    private static final class ScopeFrame {
        private final Set<String> createdVariables = new HashSet<>();
        private final Map<String, Integer> overrideDepths = new HashMap<>();
    }

    private Stack<ScopeFrame> scopeStack = new Stack<>();

    private Map<String, Stack<Variable>> centralReferenceTable = new HashMap<String, Stack<Variable>>();

    // map of function definitions - REMOVED in v2.2.x (FunctionDef deleted)
    // @Deprecated
    // private final Map<String, FunctionDef> functionDefs = new HashMap<String, FunctionDef>();

    @Deprecated
    private ScriptingLanguage scriptingLanguage;

    @Deprecated
    private String charset;

    @Inject
    private ScriptEngineFactory scriptEngineFactory;

    private Config config;
    
    // Bug #24: Event Sourcing for Inter-Processor Communication
    // These fields are set once during initialization and then immutable
    private EventBus eventBus;
    private ScraperSession session;
    private volatile ExecutionOwner executionOwner;
    private volatile ExecutionResourceAccess resourceAccess;

    public ScraperContext() {
        scopeStack.push(new ScopeFrame());
    }

    @PostConstruct
    public void initContext() {
        setLocalVar("sys", new ScriptingVariable(new SystemUtilities(this)));
        // Bug #41: httpClientManager removed - HttpComponents 4.x migration
        // ${http} variable no longer available
    }

    public void setLocalVar(String name, Object value) {
        setLocalVar(name, CommonUtil.createVariable(value));
    }

    @Override
    public void setLocalVar(String name, Variable variable) {
        // TODO rbala Currently used only for the sake of validation.
        new VariableName(name);
        Stack<Variable> variableValueStack = centralReferenceTable.get(name);
        final ScopeFrame scopeFrame = scopeStack.peek();

        if (variableValueStack == null) {
            variableValueStack = new Stack<Variable>();
            centralReferenceTable.put(name, variableValueStack);
            scopeFrame.createdVariables.add(name);
        } else if (scopeFrame.createdVariables.contains(name)) {
            // re-definition within same scope -> replace last value
            if (!variableValueStack.isEmpty()) {
                variableValueStack.pop();
            }
        } else {
            scopeFrame.overrideDepths.putIfAbsent(name, variableValueStack.size());
        }
        variableValueStack.push((Variable) ObjectUtils.defaultIfNull(variable, EmptyVariable.INSTANCE));
        // Trace logging removed - use debugger instead
    }

    @Override
    @SuppressWarnings({"ConstantConditions"})
    public Variable replaceExistingVar(String name, Variable variable) {
        // TODO rbala Currently used only for the sake of validation.
        new VariableName(name);
        final Stack<Variable> variableValueStack = centralReferenceTable.get(name);
        Assert.isFalse(variableValueStack == null || variableValueStack.isEmpty(), "Variable {0} does not exist", name);
        return variableValueStack.replaceTop((Variable) ObjectUtils.defaultIfNull(variable, EmptyVariable.INSTANCE));
    }

    @Override
    public Variable getVar(String name) {
        // TODO rbala Currently used only for the sake of validation.
        new VariableName(name);
        final Stack<Variable> stack = centralReferenceTable.get(name);
        Variable result = (stack == null) ? null : stack.peek();
        // Trace logging removed - use debugger instead
        return result;
    }

    @Override
    public boolean containsVar(String name) {
        return getVar(name) != null;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Iterator<KeyValuePair<Variable>> iterator() {
        return IteratorUtils.transformedIterator(centralReferenceTable.entrySet().iterator(), new Transformer() {
            @Override
            public Object transform(Object input) {
                final Map.Entry<String, Stack<Variable>> crtEntry = (Map.Entry<String, Stack<Variable>>) input;
                return new KeyValuePair<Variable>(crtEntry.getKey(), crtEntry.getValue().peek());
            }
        });
    }

    @Override
    public <R> R executeWithinNewContext(Callable<R> callable) throws InterruptedException {
        try {
            scopeStack.push(new ScopeFrame());
            return callable.call();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ScopeFrame frame = scopeStack.pop();
            for (String varName : frame.createdVariables) {
                removeVarFromCRT(varName);
            }
            for (Map.Entry<String, Integer> entry : frame.overrideDepths.entrySet()) {
                final Stack<Variable> stack = centralReferenceTable.get(entry.getKey());
                if (stack == null) {
                    continue;
                }
                final int targetDepth = entry.getValue();
                while (stack.size() > targetDepth) {
                    stack.pop();
                }
                if (stack.isEmpty()) {
                    centralReferenceTable.remove(entry.getKey());
                }
            }
        }
    }

    private Variable removeVarFromCRT(String varName) {
        // TODO rbala Currently used only for the sake of validation.
        new VariableName(varName);
        final Stack<Variable> stack = centralReferenceTable.get(varName);
        try {
            return stack.pop();
        } finally {
            if (stack.isEmpty()) {
                centralReferenceTable.remove(varName);
            }
        }
    }

    @Override
    // TODO Add unit test
    public void setLocalVar(final Map<String, Object> map) {
        // FIXME rbala code moved from Scraper object. Refactore ASAP
        if (map != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                setLocalVar(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public ScriptingLanguage getScriptingLanguage() {
        return scriptingLanguage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void setScriptingLanguage(final ScriptingLanguage language) {
        this.scriptingLanguage = language;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCharset() {
        return charset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCharset(final String charset) {
        this.charset = charset;
    }

    /**
     * {@inheritDoc}
     */
    /** Returns the script engine factory injected by this context's runtime. */
    @Override
    public ScriptEngineFactory getScriptEngineFactory() {
        return scriptEngineFactory;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    @Deprecated
    public void setConfig(final Config config) {
        this.config = config;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ScraperSession getSession() {
        return session;
    }

    /** Returns the immutable owner bound before this context is published. */
    @Override
    public ExecutionOwner getExecutionOwner() {
        return executionOwner;
    }

    /** Binds this context exactly once to its service-managed execution owner. */
    @Override
    public synchronized void setExecutionOwner(ExecutionOwner owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Execution owner cannot be null");
        }
        if (executionOwner != null) {
            throw new IllegalStateException("Execution owner is already bound to this context");
        }
        if (session != null && !session.getSessionId().equals(owner.getSessionId())) {
            throw new IllegalArgumentException("Execution owner session does not match context session");
        }
        if (session instanceof ExecutionTrackedSession
            && !owner.equals(((ExecutionTrackedSession) session).getExecutionOwner())) {
            throw new IllegalArgumentException("Execution owner does not match tracked session identity");
        }
        executionOwner = owner;
    }

    /** Returns the host-owned resource facade bound to this execution. */
    @Override
    public ExecutionResourceAccess getResourceAccess() {
        return resourceAccess;
    }

    /** Binds one resource facade whose immutable owner must match this context. */
    @Override
    public synchronized void setResourceAccess(ExecutionResourceAccess resourceAccess) {
        if (resourceAccess == null) {
            throw new IllegalArgumentException("Execution resource access cannot be null");
        }
        if (this.resourceAccess != null) {
            throw new IllegalStateException("Execution resource access is already bound to this context");
        }
        if (executionOwner == null || !executionOwner.equals(resourceAccess.getOwner())) {
            throw new IllegalArgumentException("Resource access owner does not match context execution owner");
        }
        this.resourceAccess = resourceAccess;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setSession(ScraperSession session) {
        if (executionOwner != null && session != null
            && !session.getSessionId().equals(executionOwner.getSessionId())) {
            throw new IllegalArgumentException("Context session does not match its execution owner");
        }
        if (executionOwner != null && session instanceof ExecutionTrackedSession
            && !executionOwner.equals(((ExecutionTrackedSession) session).getExecutionOwner())) {
            throw new IllegalArgumentException("Context session identity does not match its execution owner");
        }
        this.session = session;
    }

}