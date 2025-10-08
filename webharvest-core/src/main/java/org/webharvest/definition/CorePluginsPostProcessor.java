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

package org.webharvest.definition;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.exception.PluginException;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.utils.ClassPathScanner;
import org.webharvest.utils.ClassPathScannerImpl;

/**
 * {@link ResolverPostProcessor} implementation capable of scanning
 * Java class path looking for {@link CorePlugin} annotated plugins.
 * Found {@link CorePlugin} plugins are registered in the post processed 
 * {@link ConfigurableResolver}.
 *
 * @author Robert Bala
 * @since 2.1.0
 * @version %I%, %G%
 */
public final class CorePluginsPostProcessor implements ResolverPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            CorePluginsPostProcessor.class);

    private final ClassPathScanner scanner;
    private final CorePluginRegistrationStrategy registrationStrategy;

    /**
     * Constructs {@link CorePluginsPostProcessor} accepting name of java
     * package which is going to be scanned for the core plugins.
     * Provided package and all its subpackages are scanned for plugins
     * annotated with {@link CorePlugin}.
     *
     * @param packageName
     *            name of package to be scanned
     */
    public CorePluginsPostProcessor(final String packageName) {
        this(packageName, new CorePluginRegistrationStrategy());
        System.out.println("CorePluginsPostProcessor: Created for package: " + packageName);
    }

    /**
     * Constructs {@link CorePluginsPostProcessor} with custom registration strategy.
     *
     * @param packageName
     *            name of package to be scanned
     * @param registrationStrategy
     *            strategy to use for plugin registration
     */
    public CorePluginsPostProcessor(final String packageName, 
                                   final CorePluginRegistrationStrategy registrationStrategy) {
        this.scanner = new ClassPathScannerImpl(packageName);
        this.registrationStrategy = registrationStrategy;
    }

    /**
     * Post process provided {@link ConfigurableResolver} by registering all
     * {@link CorePlugin} annotated plugins available on classpath under
     * package (and its subpackages) declared in the post processor's
     * constructor.
     *
     * @param resolver
     *            post processed {@link ConfigurableResolver} instance
     */
    @Override
    public void postProcess(final ConfigurableResolver resolver) {
        LOGGER.info("CorePluginsPostProcessor: Starting to scan for core plugins in package: {}", scanner.getClass().getSimpleName());
        System.out.println("CorePluginsPostProcessor: Starting to scan for core plugins in package: " + scanner.getClass().getSimpleName());
        Set<Class<? extends Plugin>> plugins = scanPlugins();
        LOGGER.info("CorePluginsPostProcessor: Found {} core plugins", plugins.size());
        System.out.println("CorePluginsPostProcessor: Found " + plugins.size() + " core plugins");
        
        for (Class<? extends Plugin> plugin : plugins) {
            try {
                LOGGER.info("CorePluginsPostProcessor: Registering plugin {}", plugin.getName());
                registrationStrategy.registerPlugin(plugin, resolver);
            } catch (PluginException e) {
                if (e.getMessage().contains("already registered")) {
                    LOGGER.info("Core plugin {} was already registered, skipping", plugin.getSimpleName());
                } else {
                    LOGGER.error("Failed to register core plugin {}: {}",
                               plugin.getCanonicalName(), e.getMessage());
                }
            }
        }
        
        LOGGER.info("CorePluginsPostProcessor: Finished registering core plugins");
    }

    /**
     * Scans class path looking for plugins annotated with {@link CorePlugin}.
     * Candidate plugins must be a subtype of {@link Plugin}.
     *
     * @return core plugins eligible for registration
     */
    @SuppressWarnings("unchecked")
    private Set<Class<? extends Plugin>> scanPlugins() {
        final Set<Class<?>> candidates = scanner
                .getTypesAnnotatedWith(CorePlugin.class);
        return filterCandidates(candidates);
    }

    /**
     * Filters provided {@link Set} of candidates according to the following
     * criteria:
     * <ul>
     * <li>type must be a subtype of {@link Plugin}</li>
     * </ul>
     * Only candidates matching criteria are returned.
     *
     * @param candidates
     *            {@link Set} of candidates to be filtered
     * @return {@link Set} of candidates matching filtering criteria
     */
    @SuppressWarnings("unchecked")
    private Set<Class<? extends Plugin>> filterCandidates(
            final Set<Class<?>> candidates) {
        final Set<Class<? extends Plugin>> filteredCandidates = 
            new java.util.HashSet<>();

        for (final Class<?> candidate : candidates) {
            if (Plugin.class.isAssignableFrom(candidate)) {
                filteredCandidates.add((Class<? extends Plugin>) candidate);
                System.out.println("Found core plugin candidate: " + candidate.getName());
            } else {
                LOGGER.warn("Class {} annotated with @CorePlugin but does not extend Plugin", 
                          candidate.getName());
            }
        }

        return filteredCandidates;
    }
}
