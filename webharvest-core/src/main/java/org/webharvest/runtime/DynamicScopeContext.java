/*
 Copyright (c) 2006-2025 the original author or authors.
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

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.common.eventbus.EventBus;
import org.webharvest.definition.Config;
// REMOVED in v2.2.x - these classes no longer exist
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.KeyValuePair;

/**
 * Created by IntelliJ IDEA.
 * User: awajda
 * Date: Sep 20, 2010
 * Time: 9:32:52 PM
 */
public interface DynamicScopeContext extends Iterable<KeyValuePair<Variable>> {

    Variable getVar(String name);

    void setLocalVar(String name, Variable value);

    void setLocalVar(String name, Object value);

    void setLocalVar(Map<String, Object> map);

    <R> R executeWithinNewContext(Callable<R> callable) throws InterruptedException;

    Variable replaceExistingVar(String name, Variable variable);

    boolean containsVar(String name);

    // REMOVED in v2.2.x - FunctionDef no longer exists
    // @Deprecated
    // FunctionDef getFunctionDef(String name);
    // @Deprecated
    // void addFunctionDef(FunctionDef funcDef);

    @Deprecated
    ScriptingLanguage getScriptingLanguage();

    @Deprecated
    void setScriptingLanguage(final ScriptingLanguage language);

    /**
     * Returns default configuration's charset.
     *
     * @return default configuration's charset.
     */
    String getCharset();

    /**
     * Sets default configuration's charset.
     *
     * @param charset
     *            new default configuration's charset
     */
    void setCharset(String charset);

    Config getConfig();

    @Deprecated
    void setConfig(Config config);
    
    /**
     * Returns the EventBus for publishing plugin execution events.
     * 
     * <p>This method is part of Bug #24: Event Sourcing for Inter-Processor Communication.
     * It provides plugins access to the EventBus for publishing execution events.</p>
     * 
     * <h4>Thread Safety:</h4>
     * <p>The EventBus reference is immutable (set once during context initialization).
     * EventBus.post() is thread-safe and can be called from multiple threads.</p>
     * 
     * @return the EventBus, or null if not set (backwards compatibility)
     * @since 2.2.0
     * @see com.google.common.eventbus.EventBus
     */
    EventBus getEventBus();
    
    /**
     * Returns the ScraperSession associated with this execution context.
     * 
     * <p>This method is part of Bug #24: Event Sourcing for Inter-Processor Communication.
     * It provides plugins access to session metadata for event publishing.</p>
     * 
     * <h4>Thread Safety:</h4>
     * <p>The ScraperSession reference is immutable (set once during context initialization).
     * ScraperSession methods are thread-safe (volatile fields, synchronized transitions).</p>
     * 
     * @return the ScraperSession, or null if not set (backwards compatibility)
     * @since 2.2.0
     * @see org.webharvest.runtime.session.ScraperSession
     */
    ScraperSession getSession();
    
    /**
     * Sets the EventBus for this context.
     * 
     * <p>This method should be called once during context initialization,
     * before any plugins are executed.</p>
     * 
     * <h4>Thread Safety:</h4>
     * <p>This method is NOT thread-safe. It must be called during initialization
     * before the context is shared across threads.</p>
     * 
     * @param eventBus the EventBus to use for publishing events
     * @since 2.2.0
     */
    void setEventBus(EventBus eventBus);
    
    /**
     * Sets the ScraperSession for this context.
     * 
     * <p>This method should be called once during context initialization,
     * before any plugins are executed.</p>
     * 
     * <h4>Thread Safety:</h4>
     * <p>This method is NOT thread-safe. It must be called during initialization
     * before the context is shared across threads.</p>
     * 
     * @param session the ScraperSession to associate with this context
     * @since 2.2.0
     */
    void setSession(ScraperSession session);

}
