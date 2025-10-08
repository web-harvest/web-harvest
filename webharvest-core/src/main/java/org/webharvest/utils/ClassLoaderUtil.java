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

package org.webharvest.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.exception.PluginException;

/**
 * Class loading utility - used for loading JDBC driver classes and plugin
 * classes.
 */
public final class ClassLoaderUtil {

    private static final Logger LOG =
        LoggerFactory.getLogger(ClassLoaderUtil.class);

    /**
     * class loader that includes all JAR libraries in the working
     * folder of the application.
     */
    private static URLClassLoader rootClassLoader = null;

    /**
     * Prevents against instantiation of utility class.
     */
    private ClassLoaderUtil() {
    }

    /**
     * Lists all JARs in the working folder (folder of WebHarvest executable)
     */
    private static void defineRootLoader() {
        List<URL> urls = new ArrayList<URL>();
        String rootDirPath = new File("").getAbsolutePath();

        try {
            urls.add(new File("").toURI().toURL());
        } catch (MalformedURLException e) {
            LOG.error("Cannot define root loader", e);
        }

        // add all JAR files from the root folder to the class path
        File[] entries = new File(rootDirPath).listFiles();
        if (entries != null) {
            for (int f = 0; f < entries.length; f++) {
                File entry = entries[f];
                if (entry != null && !entry.isDirectory() && isJar(entry)) {
                    try {
                        String jarAbsolutePath = entry.getAbsolutePath();
                        urls.add(new URL("jar:file://" + jarAbsolutePath.replace('\\', '/') + "!/"));
                    } catch (MalformedURLException e) {
                        LOG.error("Cannot define root loader", e);
                    }
                }
            }
        }

        URL[] urlsArray = new URL[urls.size()];
        for (int i = 0; i < urls.size(); i++) {
            urlsArray[i] = (URL) urls.get(i);
        }

        rootClassLoader = new URLClassLoader(urlsArray,
                ClassLoaderUtil.class.getClassLoader());
    }

    private static boolean isJar(final File file) {
        return file.getName().toLowerCase().endsWith(".jar");
    }

    public static Class< ? > getPluginClass(String fullClassName)
            throws PluginException {
        if (fullClassName == null) {
            throw new PluginException("Plugin class name cannot be null");
        }
        if (rootClassLoader == null) {
            defineRootLoader();
        }
        try {
            // FIXME: http://stackoverflow.com/questions/11273303/java-classloader-dilemma
            return Class.forName(fullClassName, true, rootClassLoader);
        } catch (ClassNotFoundException e) {
            throw new PluginException("Error finding plugin class \""
                    + fullClassName + "\": " + e.getMessage(), e);
        } catch (NoClassDefFoundError e) {
            throw new PluginException("Error finding plugin class \""
                    + fullClassName + "\": " + e.getMessage(), e);
        }
    }
}
