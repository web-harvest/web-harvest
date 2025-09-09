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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.annotations.InjectDependency;

/**
 * Default implementation of PluginDependencyInjector using reflection.
 * 
 * <p>
 * This implementation uses reflection to inject dependencies into plugin fields
 * annotated with @InjectDependency. It supports both singleton and prototype
 * dependency scopes.
 * </p>
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public class DefaultPluginDependencyInjector implements PluginDependencyInjector {
    
    private final Map<Class<?>, DependencyProvider<?>> providers = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> singletons = new ConcurrentHashMap<>();
    
    @Override
    public void injectDependencies(Plugin plugin) throws PluginDependencyInjectionException {
        if (plugin == null) {
            throw new PluginDependencyInjectionException("Plugin cannot be null");
        }
        
        Class<?> pluginClass = plugin.getClass();
        Field[] fields = pluginClass.getDeclaredFields();
        
        for (Field field : fields) {
            if (field.isAnnotationPresent(InjectDependency.class)) {
                injectField(plugin, field);
            }
        }
    }
    
    @Override
    public <T> void registerProvider(Class<T> dependencyType, DependencyProvider<T> provider) {
        if (dependencyType == null) {
            throw new IllegalArgumentException("Dependency type cannot be null");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        providers.put(dependencyType, provider);
    }
    
    @Override
    public <T> void registerSingleton(Class<T> dependencyType, T instance) {
        if (dependencyType == null) {
            throw new IllegalArgumentException("Dependency type cannot be null");
        }
        if (instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }
        singletons.put(dependencyType, instance);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getDependency(Class<T> dependencyType) throws PluginDependencyInjectionException {
        if (dependencyType == null) {
            throw new IllegalArgumentException("Dependency type cannot be null");
        }
        
        // Check singletons first
        Object singleton = singletons.get(dependencyType);
        if (singleton != null) {
            return (T) singleton;
        }
        
        // Check providers
        DependencyProvider<?> provider = providers.get(dependencyType);
        if (provider != null) {
            try {
                return (T) provider.provide();
            } catch (Exception e) {
                throw new PluginDependencyInjectionException(
                    "Failed to create dependency instance for " + dependencyType.getName(), e);
            }
        }
        
        throw new PluginDependencyInjectionException(
            "No provider or singleton registered for dependency type: " + dependencyType.getName());
    }
    
    private void injectField(Plugin plugin, Field field) throws PluginDependencyInjectionException {
        Class<?> fieldType = field.getType();
        Object dependency = getDependency(fieldType);
        
        try {
            field.setAccessible(true);
            field.set(plugin, dependency);
        } catch (IllegalAccessException e) {
            throw new PluginDependencyInjectionException(
                "Failed to inject dependency into field " + field.getName() + 
                " of plugin " + plugin.getClass().getName(), e);
        }
    }
}
