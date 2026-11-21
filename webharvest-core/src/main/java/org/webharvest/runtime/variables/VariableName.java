/*
 Copyright (c) 2006-2025 the original author or authors.

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

package org.webharvest.runtime.variables;

import static java.text.MessageFormat.format;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.webharvest.exception.VariableException;

/**
 * Represents name of the {@link org.webharvest.runtime.variables.Variable}
 * that is set on {@link org.webharvest.runtime.DynamicScopeContext}.
 * The variable name must conform the general rules that apply
 * to variable names of programming languages.
 * 
 * <h2>Bug #6: Underscore Support (v2.2.0)</h2>
 * <p>Variable names can now contain underscores for better naming conventions:</p>
 * <ul>
 *   <li><strong>Function parameters:</strong> {@code p_parameter}, {@code p_username}</li>
 *   <li><strong>Descriptive names:</strong> {@code input_value}, {@code output_result}</li>
 *   <li><strong>Multiple underscores:</strong> {@code my_long_variable_name}</li>
 * </ul>
 * 
 * <p><strong>Naming Rules:</strong></p>
 * <ul>
 *   <li>Must start with a letter (a-z, A-Z)</li>
 *   <li>Can contain letters, digits (0-9), and underscores (_)</li>
 *   <li>Cannot start with underscore or digit</li>
 *   <li>Pattern: {@code ^[a-zA-Z][a-zA-Z0-9_]*?$}</li>
 * </ul>
 * 
 * <p><strong>Examples:</strong></p>
 * <pre>
 * ✅ Valid:   myVar, var1, p_parameter, input_value, my_var_123
 * ❌ Invalid: _var, 123var, my-var, my.var, my var
 * </pre>
 *
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 * @version %I%, %G%
 * @see org.webharvest.runtime.variables.Variable
 * @see org.webharvest.runtime.DynamicScopeContext
 */
public final class VariableName {

    private final String name;
    private static final Set<String> SYSTEM_VARIABLES = Set.of("_index", "_item");

    /**
     * Creates new {@link org.webharvest.runtime.variables.Variable} name.
     * 
     * <p>Bug #6 Fix: Now allows underscores in variable names for better
     * naming conventions (e.g., {@code p_parameter}, {@code input_value}).</p>
     *
     * @param name variable name (must match pattern: {@code ^[a-zA-Z][a-zA-Z0-9_]*?$})
     * @throws VariableException if name is null, empty, or doesn't match pattern
     */
    public VariableName(final String name) {
        if (StringUtils.isBlank(name)) {
            throw new VariableException(format("Invalid variable name ''{0}''",
                    name));
        }

        if (!name.matches("^[a-zA-Z][a-zA-Z0-9_]*?$")
                && !SYSTEM_VARIABLES.contains(name)) {
            throw new VariableException(format("Invalid variable name ''{0}''",
                    name));
        }

        this.name = name;
    }

    /**
     * Gets {@link org.webharvest.runtime.variables.Variable} value.
     *
     * @return variable name.
     */
    public String getValue() {
        return name;
    }

}
