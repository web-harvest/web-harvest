package org.webharvest.definition;

/**
 * Builder for creating AnnotatedPluginsPostProcessor instances.
 * Implements the Builder pattern for flexible configuration.
 * 
 * @author Web-Harvest Team
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




