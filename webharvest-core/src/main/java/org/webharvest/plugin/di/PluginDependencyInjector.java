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

package org.webharvest.plugin.di;

import org.webharvest.plugin.Plugin;

/**
 * Interface for dependency injection in plugins.
 * Provides methods to inject dependencies into plugin instances.
 * 
 * <p>
 * This interface allows for easy mocking and testing of plugins by providing
 * a way to inject mock dependencies during testing.
 * </p>
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public interface PluginDependencyInjector {
    
    /**
     * Injects dependencies into the given plugin instance.
     * 
     * @param plugin the plugin instance to inject dependencies into
     * @throws PluginDependencyInjectionException if dependency injection fails
     */
    void injectDependencies(Plugin plugin) throws PluginDependencyInjectionException;
    
    /**
     * Registers a dependency provider for a specific type.
     * 
     * @param <T> the type of dependency
     * @param dependencyType the class of the dependency
     * @param provider the provider that creates instances of the dependency
     */
    <T> void registerProvider(Class<T> dependencyType, DependencyProvider<T> provider);
    
    /**
     * Registers a singleton instance for a specific type.
     * 
     * @param <T> the type of dependency
     * @param dependencyType the class of the dependency
     * @param instance the singleton instance
     */
    <T> void registerSingleton(Class<T> dependencyType, T instance);
    
    /**
     * Gets a dependency instance of the specified type.
     * 
     * @param <T> the type of dependency
     * @param dependencyType the class of the dependency
     * @return the dependency instance
     * @throws PluginDependencyInjectionException if the dependency cannot be resolved
     */
    <T> T getDependency(Class<T> dependencyType) throws PluginDependencyInjectionException;
}


