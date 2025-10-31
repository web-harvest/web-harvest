package org.webharvest.definition;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Default implementation of {@link ConfigFactory} that avoids Guice assisted
 * injection in favour of plain object construction. This eliminates runtime
 * warnings produced by Guice's legacy assisted inject support on Java 11+.
 */
@Singleton
public final class DefaultConfigFactory implements ConfigFactory {

    private final Provider<ConfigParser> configParserProvider;

    @Inject
    public DefaultConfigFactory(final Provider<ConfigParser> configParserProvider) {
        this.configParserProvider = configParserProvider;
    }

    @Override
    public Config create(final ConfigSource source) {
        ConfigParser parser = configParserProvider.get();
        return new XMLConfig(source, parser);
    }
}
