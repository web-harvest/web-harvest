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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.webharvest.definition.ConfigLocationVisitor.VisitableLocation;

/**
 * Implementation of {@link ConfigSource} that uses plain old string as source
 * of configuration/
 *
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 * @version %I%, %G%
 * @see ConfigSource
 */
public final class BufferConfigSource extends AbstractConfigSource {

    private final String content;

    private final Location location;

    /**
     * Class constructor expecting plain old string configuration. For
     * instance it can represent configuration in the editor's buffer for new
     * file (that is not yet saved to file system). In such a case location is
     * unknown.
     *
     * @param content
     *            just an XML configuration.
     */
    public BufferConfigSource(final String content) {
        this(content, UNDEFINED_LOCATION);
    }

    /**
     * Class constructor expecting both the XML configuration as well as
     * configuration location (eg. in situation when the original
     *  {@link ConfigSource} object has been modified and such a change
     *  should be reflected in new immutable state.
     *
     * @param content
     *            just an XML configuration.
     * @param location
     *            location pointing from where this configuration is stored (eg.
     *            remote web server or file system path).
     */
    // FIXME rbala I believe there could be a better solution in terms of
    // creating dynamic copy of other ConfigSource types but with modified
    // content.
    public BufferConfigSource(final String content, final Location location) {
        if (content == null) {
            throw new IllegalArgumentException("Configuration content is"
                    + " required");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location is requried");
        }
        this.content = content;
        this.location = location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reader getReader() throws IOException {
        return new StringReader(content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getLocation() {
        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void visit(final ConfigLocationVisitor visitor)
            throws IOException {
        // FIXME rbala Ugly but effective solution to overcome the problem with lost type
        if (location instanceof VisitableLocation) {
            ((VisitableLocation) location).accept(visitor);
        }
    }

}
