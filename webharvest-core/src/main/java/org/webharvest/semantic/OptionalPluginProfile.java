package org.webharvest.semantic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.webharvest.WHConstants;

/**
 * Explicit list of optional extension plugins that may be absent from the classpath.
 *
 * <p>{@link PluginResolutionStatus#UNAVAILABLE_OPTIONAL} is emitted only when a
 * profile entry matches the canonical resolution key and {@link Class#forName(String)}
 * fails for that entry's class name. Arbitrary unknown elements stay
 * {@link PluginResolutionStatus#UNKNOWN_ELEMENT}.
 *
 * <p>Element names follow catalog metadata (e.g. {@code db}, not {@code database};
 * {@code web-browser} for WebBrowserPlugin).
 */
public final class OptionalPluginProfile {
    private final List<Entry> entries;

    public OptionalPluginProfile(List<Entry> entries) {
        Objects.requireNonNull(entries, "entries");
        this.entries = List.copyOf(entries);
    }

    /** Empty profile: never emits {@code UNAVAILABLE_OPTIONAL}. */
    public static OptionalPluginProfile empty() {
        return new OptionalPluginProfile(List.of());
    }

    /**
     * Known optional extension modules: ftp, mail, db, zip, web-browser.
     * Namespaces and local names match plugin metadata when present on the classpath.
     */
    public static OptionalPluginProfile knownExtensions() {
        List<Entry> list = new ArrayList<>();
        list.add(new Entry(WHConstants.XMLNS_CORE, "ftp", "org.webharvest.plugin.ftp.FtpPlugin"));
        list.add(new Entry(WHConstants.XMLNS_CORE, "mail", "org.webharvest.plugin.mail.MailPlugin"));
        list.add(new Entry(WHConstants.XMLNS_CORE, "db", "org.webharvest.plugin.db.DatabasePlugin"));
        list.add(new Entry("http://org.webharvest/schema/2.1/zip", "zip", "org.webharvest.plugin.zip.ZipPlugin"));
        list.add(new Entry(WHConstants.XMLNS_CORE, "web-browser",
                "org.webharvest.plugin.webbrowser.WebBrowserPlugin"));
        return new OptionalPluginProfile(list);
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public Optional<Entry> find(String canonicalNamespaceUri, String localName) {
        Objects.requireNonNull(canonicalNamespaceUri, "canonicalNamespaceUri");
        Objects.requireNonNull(localName, "localName");
        for (Entry entry : entries) {
            if (entry.namespaceUri.equals(canonicalNamespaceUri) && entry.localName.equals(localName)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * Reliable presence probe used only for profile entries.
     *
     * @return {@code true} when {@link Class#forName(String)} succeeds
     */
    public static boolean isClassPresent(String className) {
        Objects.requireNonNull(className, "className");
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }

    /** One optional extension plugin identity. */
    public static final class Entry {
        private final String namespaceUri;
        private final String localName;
        private final String className;

        public Entry(String namespaceUri, String localName, String className) {
            this.namespaceUri = Objects.requireNonNull(namespaceUri, "namespaceUri");
            this.localName = Objects.requireNonNull(localName, "localName");
            this.className = Objects.requireNonNull(className, "className");
            if (localName.isEmpty() || className.isEmpty()) {
                throw new IllegalArgumentException("localName and className must be non-empty");
            }
        }

        public String getNamespaceUri() {
            return namespaceUri;
        }

        public String getLocalName() {
            return localName;
        }

        public String getClassName() {
            return className;
        }

        public boolean isClassPresent() {
            return OptionalPluginProfile.isClassPresent(className);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Entry)) {
                return false;
            }
            Entry that = (Entry) other;
            return namespaceUri.equals(that.namespaceUri)
                    && localName.equals(that.localName)
                    && className.equals(that.className);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespaceUri, localName, className);
        }

        @Override
        public String toString() {
            return "{" + namespaceUri + "}" + localName + " -> " + className;
        }
    }
}
