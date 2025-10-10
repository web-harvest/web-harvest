/*
 Copyright (c) 2006-2012 the original author or authors.

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

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.WHConstants;
import org.webharvest.definition.PluginConfigurationTree;
import org.webharvest.definition.StreamingXmlParser;

/**
 * Implementation of {@link ConfigSource} capable to work with XML
 * based configurations.
 *
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 * @version %I%, %G%
 * @see Config
 * @see ConfigSource
 * @see PluginConfigurationTree
 */
public final class XMLConfig implements Config {

    private static final Logger LOG = LoggerFactory.getLogger(XMLConfig.class);

    private final ConfigSource configSource;

    private final ConfigParser configParser;
    
    private final Injector injector;
    
    private PluginConfigurationTree configurationTree;

    /**
     * Constructor for both Guice and non-Guice usage.
     * 
     * @param configSource reference to configuration resource.
     * @param configParser reference to configuration parser.
     * @param injector Guice injector for lazy parser creation
     */
    @Inject
    public XMLConfig(@Assisted final ConfigSource configSource, ConfigParser configParser, Injector injector) {
        if (configSource == null) {
            throw new IllegalArgumentException("ConfigSource is required");
        }
        if (configParser == null) {
            throw new IllegalArgumentException("ConfigParser is required");
        }
        this.configSource = configSource;
        this.configParser = configParser;
        this.injector = injector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Version getVersion() {
        if (configurationTree == null) {
            throw new IllegalStateException("Not loaded configuration");
        }

        // Get namespace from root configuration
        String namespace = configurationTree.getConfiguration().getProperty("namespace");
        return WHConstants.XMLNS_CORE_10.equals(namespace) ? Version.v1 : Version.v2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigSource getConfigSource() {
        return configSource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginConfigurationTree getConfigurationTree() {
        if (configurationTree == null) {
            throw new IllegalStateException("No configuration loaded. Call reload() first.");
        }

        return configurationTree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reload() {
        if (configParser instanceof StreamingXmlParser) {
            this.configurationTree = ((StreamingXmlParser) configParser).parseToTree(configSource);
        } else {
            // Fallback: If custom ConfigParser implementation doesn't support tree generation
            // (should not happen in production - all parsers should be StreamingXmlParser now)
            LOG.warn("ConfigParser is not StreamingXmlParser: {}. Tree generation not available.", 
                    configParser.getClass().getName());
            this.configurationTree = null;
        }
    }

}
