package org.webharvest.definition;

import org.testng.annotations.Test;
import org.webharvest.exception.ParserException;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class DefaultConfigFactoryTest {

    private static class StubStreamingParser extends StreamingXmlParser {
        private final PluginConfigurationTree tree;
        private ConfigSource lastSource;

        private StubStreamingParser(PluginConfigurationTree tree) {
            this.tree = tree;
        }

        @Override
        public PluginConfigurationTree parseToTree(final ConfigSource source) throws ParserException {
            this.lastSource = source;
            return tree;
        }

        ConfigSource getLastSource() {
            return lastSource;
        }
    }

    private static class StringConfigSource implements ConfigSource {
        private final String xml;

        StringConfigSource(String xml) {
            this.xml = xml;
        }

        @Override
        public Reader getReader() {
            return new StringReader(xml);
        }

        @Override
        public Location getLocation() {
            return new Location() { };
        }

        @Override
        public ConfigSource include(Location location) throws IOException {
            throw new UnsupportedOperationException("include not supported in tests");
        }
    }

    @Test
    public void shouldProduceXmlConfigUsingProvidedParser() {
        PluginConfiguration rootConfiguration = new DefaultPluginConfiguration(
                "config", "http://org.webharvest/schema/2.1/core", "config");
        PluginConfigurationTree tree = new PluginConfigurationTree(rootConfiguration, "config", Object.class);

        StubStreamingParser parser = new StubStreamingParser(tree);
        DefaultConfigFactory factory = new DefaultConfigFactory(() -> parser);

        ConfigSource source = new StringConfigSource("<config/>");
        Config config = factory.create(source);

        assertTrue(config instanceof XMLConfig, "Factory should create XMLConfig instances");
        assertSame(source, config.getConfigSource(), "Config should reference provided source");

        config.reload();
        assertSame(tree, config.getConfigurationTree(), "Reload should use provided parser");
        assertSame(source, parser.getLastSource(), "Parser should receive the same source instance");
    }
}
