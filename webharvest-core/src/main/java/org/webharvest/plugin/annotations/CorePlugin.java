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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark core plugins.
 * Core plugins are the original processors that have been migrated to the plugin architecture.
 * They are part of the core WebHarvest functionality and are always available.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CorePlugin {
    
    /**
     * The namespace URI for this core plugin.
     * 
     * @return the namespace URI
     */
    String namespace() default "http://org.webharvest/schema/2.1/core";
    
    /**
     * The element name for this core plugin.
     * 
     * @return the element name
     */
    String elementName();
    
    /**
     * The description of this core plugin.
     * 
     * @return the description
     */
    String description() default "";
    
    /**
     * The version of this core plugin.
     * 
     * @return the version
     */
    String version() default "2.1.0";
    
    /**
     * The category for Visual Designer palette.
     * 
     * <p>Standard categories:</p>
     * <ul>
     *   <li><strong>Network</strong> - HTTP, FTP, Mail</li>
     *   <li><strong>Extraction</strong> - XPath, XQuery, Regexp, HTML-to-XML, JSON-to-XML</li>
     *   <li><strong>Variables</strong> - Def, Get-Var</li>
     *   <li><strong>Control</strong> - Loop, While, If, Try, Case</li>
     *   <li><strong>Processing</strong> - Script, Template, XSLT</li>
     *   <li><strong>Storage</strong> - File</li>
     *   <li><strong>Utilities</strong> - Log, Sleep, Return, Exit</li>
     * </ul>
     * 
     * @return the category
     */
    String category() default "Core";
    
    /**
     * Indicates whether this plugin is a system component (not a user-facing plugin).
     * 
     * <p>System components are internal framework components that are used automatically
     * by the framework and should not appear in visual editors or be included in
     * plugin metadata tests. Examples include:</p>
     * <ul>
     *   <li><strong>config</strong> - Root container element, not a regular plugin</li>
     *   <li><strong>body-processor</strong> - Internal framework component for executing child processors</li>
     * </ul>
     * 
     * <p>System components are excluded from:</p>
     * <ul>
     *   <li>Visual editor plugin palettes</li>
     *   <li>Plugin metadata completeness tests</li>
     *   <li>User-facing documentation</li>
     * </ul>
     * 
     * @return true if this is a system component, false otherwise (default: false)
     */
    boolean systemComponent() default false;
}


