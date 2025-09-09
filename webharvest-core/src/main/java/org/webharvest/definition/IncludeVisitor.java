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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.webharvest.definition.FileConfigSource.FileLocation;
import org.webharvest.definition.URLConfigSource.URLLocation;
import org.webharvest.utils.CommonUtil;

// TODO Missing unit test
// TODO Missing documentation
public final class IncludeVisitor implements ConfigLocationVisitor {

    private final String path;

    private ConfigSource configSource;

    // TODO Missing unit test
    // TODO Missing documentation
    public IncludeVisitor(final String path) {
        // FIXME Moved directly form IncludeProcessor
        this.path = CommonUtil.adaptFilename(path);
    }

    // TODO Missing documentation
    // TODO Missing unit test
    public ConfigSource getConfigSource() {
        return configSource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // TODO Missing unit test
    public void visit(final URLLocation location) throws IOException {
        // FIXME Moved directly form IncludeProcessor
        final String newURL = CommonUtil.fullUrl(location.toString(), path);

        this.configSource = new URLConfigSource(new URL(newURL));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // TODO Missing unit test
    public void visit(final FileLocation location) throws IOException {
        // FIXME Moved directly form IncludeProcessor
         final String oldPath = CommonUtil.adaptFilename(location.toString());
         int index = oldPath.lastIndexOf('/');
         String newPath;
         if (index > 0) {
             final String workingPath = oldPath.substring(0, index);
             newPath = CommonUtil.getAbsoluteFilename(workingPath, path);
         } else {
             newPath = path;
         }

        this.configSource = new FileConfigSource(new File(newPath));
    }

}
