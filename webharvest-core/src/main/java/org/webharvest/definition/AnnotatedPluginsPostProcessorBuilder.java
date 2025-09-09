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

package org.webharvest.definition;

/**
 * Builder for creating AnnotatedPluginsPostProcessor instances.
 * Implements the Builder pattern for flexible configuration.
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public final class AnnotatedPluginsPostProcessorBuilder {
    
    private String packageName;
    private PluginRegistrationStrategy registrationStrategy;
    
    private AnnotatedPluginsPostProcessorBuilder() {
        // Private constructor
    }
    
    /**
     * Creates a new builder instance.
     * 
     * @return new builder instance
     */
    public static AnnotatedPluginsPostProcessorBuilder create() {
        return new AnnotatedPluginsPostProcessorBuilder();
    }
    
    /**
     * Sets the package name to scan for plugins.
     * 
     * @param packageName the package name to scan
     * @return this builder instance
     */
    public AnnotatedPluginsPostProcessorBuilder withPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }
    
    /**
     * Sets the registration strategy to use.
     * 
     * @param registrationStrategy the strategy to use
     * @return this builder instance
     */
    public AnnotatedPluginsPostProcessorBuilder withRegistrationStrategy(PluginRegistrationStrategy registrationStrategy) {
        this.registrationStrategy = registrationStrategy;
        return this;
    }
    
    /**
     * Builds the AnnotatedPluginsPostProcessor instance.
     * 
     * @return configured AnnotatedPluginsPostProcessor
     * @throws IllegalArgumentException if required parameters are missing
     */
    public AnnotatedPluginsPostProcessor build() {
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Package name is required");
        }
        
        if (registrationStrategy == null) {
            registrationStrategy = new DefaultPluginRegistrationStrategy();
        }
        
        return new AnnotatedPluginsPostProcessor(packageName, registrationStrategy);
    }
}




