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

package org.webharvest.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.webharvest.definition.IElementDef;
import org.webharvest.definition.WebHarvestPluginDef;

/**
 * Annotation on {@link org.webharvest.runtime.processors.Processor} classes
 * specifying the {@link org.webharvest.definition.IElementDef} this particular
 * processor is described by.
 *
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 * @version %I%, %G%
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Inherited
@Documented
public @interface Definition {

    /**
     * Defines name of the processor. Should be valid identifier. Processor's
     * tag will use this name. For example, if this name is "myprocessor" in
     * config file it will be used as
     * &lt;myprocessor...&gt;...&lt;/myprocessor&gt;
     *
     * @return Name of the processor
     */
    String value();

    /**
     * The definition type that the processor is mapped to.
     */
    Class<? extends IElementDef> definitionClass() default WebHarvestPluginDef.class;

    /**
     * This method should return all possible attribute names for the plugin processor.
     *
     * @return Array of attribute names
     */
    String[] validAttributes() default {};

    /**
     * This method should return all mandatory attribute names for the plugin processor.
     *
     * @return Array of attribute names
     */
    String[] requiredAttributes() default {};

    /**
     * Defines dependant subprocessors that are used inside this plugin and that will
     * automatically be registered with this plugin.
     *
     * @return Array of subprocessor classes
     */
    Class[] dependantProcessors() default {};

    boolean body() default true;

    /**
     * This method should return all names of all allowed processors inside the body of
     * this processor plugin. If null is returned, then all subprocessors are allowed.
     *
     * @return Array of allowed subprocessor names (case insensitive)
     */
    String[] validSubprocessors() default {};

    /**
     * This method should return all mandatory subprocessor names, or in other words all
     * mandatory subtags that must be present in the body of this processor plugin.
     *
     * @return Array of mandatory subprocessor names
     */
    String[] requiredSubprocessors() default {};

    boolean internal() default false;

}
