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

import org.webharvest.definition.PluginConfigurationTree;

/**
 * Represents lazily loaded configuration object that is loaded from associated
 * {@link ConfigSource} on demand with call to {@link #reload()} method. For
 * already loaded configuration it is possible to get reference to a graph of
 * configuration elements with {@link #getElementDef} (deprecated) or new
 * {@link #getConfigurationTree()}.
 *
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 * @version %I%, %G%
 * @see ConfigSource
 * @see IElementDef
 * @see PluginConfigurationTree
 */
public interface Config {

    // TODO rbala Missing javadoc documentation
    enum Version {
        // TODO rbala Missing javadoc documentation
        v1,
        // TODO rbala Missing javadoc documentation
        v2
    }

    // TODO rbala Missing javadoc
    Version getVersion();

    /**
     * Gets reference to configuration resource {@link ConfigSource}.
     *
     * @return configuration resource that is meant to contain configuration.
     */
    ConfigSource getConfigSource();

    /**
     * Gets reference to configuration root element.
     *
     * @return for loaded configuration gets root element, throws
     *         {@link IllegalStateException} in case the configuration has not
     *         been loaded.
     * @deprecated Use {@link #getConfigurationTree()} instead. The old *Def architecture
     *             is being replaced with PluginConfigurationTree in v2.2.x.
     *             This method will be removed in v2.3.x.
     */
    @Deprecated
    // IElementDef getElementDef();

    /**
     * Gets reference to configuration tree (new architecture - v2.2.x).
     * This is the modern replacement for {@link #getElementDef()}.
     *
     * @return for loaded configuration gets root configuration tree, throws
     *         {@link IllegalStateException} in case the configuration has not
     *         been loaded.
     * @since 2.2.0
     */
    PluginConfigurationTree getConfigurationTree();

    /**
     * Loads configuration from resource defined by {@link #getConfigSource()}.
     * Once the configuration is successfully loaded it is possible to get
     * reference to root configuration element with {@link #getElementDef()}
     * (deprecated) or {@link #getConfigurationTree()} (new).
     */
    void reload();

}
