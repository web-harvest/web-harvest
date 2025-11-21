/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define master/detail relationship constraints for plugins.
 * 
 * <p>This annotation allows plugin classes to declare constraints on detail plugins
 * that can be children of a master plugin. It defines cardinality constraints
 * (minOccurs, maxOccurs) and UI hints for the visual designer.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * @CorePlugin(elementName = "if", category = "Control")
 * @MasterDetailConstraint(
 *     detailType = "else",
 *     maxOccurs = 1,
 *     minOccurs = 0,
 *     sectionTitle = "Else Branch",
 *     addLabel = "Add Else"
 * )
 * public class IfPlugin extends AbstractPlugin {
 *     // ...
 * }
 * }</pre>
 * 
 * <p>This annotation is repeatable, allowing a master plugin to define multiple
 * detail constraints (e.g., if a plugin can have both "else" and "catch" details).</p>
 * 
 * @author Robert Bala
 * @since 2.3.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(MasterDetailConstraints.class)
public @interface MasterDetailConstraint {
    
    /**
     * The element name of the detail plugin type.
     * 
     * <p>This must match the element name of the detail plugin class
     * (e.g., "else", "catch", "http-header").</p>
     * 
     * @return the detail plugin element name
     */
    String detailType();
    
    /**
     * Maximum number of occurrences of this detail plugin.
     * 
     * <p>Use -1 to indicate unlimited occurrences (default).
     * Use 0 to indicate this detail is not allowed.
     * Use 1 or higher to set a specific maximum.</p>
     * 
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code maxOccurs = 1} - exactly one or zero (if/else, try/catch)</li>
     *   <li>{@code maxOccurs = -1} - unlimited (http/http-header)</li>
     *   <li>{@code maxOccurs = 0} - not allowed</li>
     * </ul>
     * 
     * @return maximum occurrences (-1 for unlimited, default: -1)
     */
    int maxOccurs() default -1;
    
    /**
     * Minimum number of occurrences of this detail plugin.
     * 
     * <p>Use 0 to indicate the detail is optional (default).
     * Use 1 or higher to make the detail required.</p>
     * 
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code minOccurs = 0} - optional (if/else, try/catch)</li>
     *   <li>{@code minOccurs = 1} - required</li>
     * </ul>
     * 
     * @return minimum occurrences (default: 0)
     */
    int minOccurs() default 0;
    
    /**
     * Title for the detail section in the UI.
     * 
     * <p>If not specified, the UI will generate a title from the detail plugin name.
     * This allows customization of the section title in the property panel.</p>
     * 
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code sectionTitle = "Else Branch"}</li>
     *   <li>{@code sectionTitle = "Catch Block"}</li>
     *   <li>{@code sectionTitle = "HTTP Headers"}</li>
     * </ul>
     * 
     * @return section title (optional, empty string if not specified)
     */
    String sectionTitle() default "";
    
    /**
     * Label for the "Add" button in the UI.
     * 
     * <p>If not specified, the UI will generate a label from the detail plugin name.
     * This allows customization of the button text in the property panel.</p>
     * 
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code addLabel = "Add Else"}</li>
     *   <li>{@code addLabel = "Add Catch"}</li>
     *   <li>{@code addLabel = "Add Header"}</li>
     * </ul>
     * 
     * @return add button label (optional, empty string if not specified)
     */
    String addLabel() default "";

    /**
     * Label shown when no details exist (empty state).
     */
    String emptyLabel() default "";

    /**
     * Attribute used as the primary summary label for each detail entry.
     */
    String primaryAttribute() default "";

    /**
     * Attribute used as the secondary summary label for each detail entry.
     */
    String secondaryAttribute() default "";

    /**
     * Whether the detail editor should expose body-editor controls.
     */
    boolean usesBody() default false;
}


