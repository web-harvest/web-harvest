package org.webharvest.plugin.metadata;

import java.util.Collections;
import java.util.List;

/**
 * Documentation block associated with a plugin.
 */
public final class PluginDocumentation {

    private final String summary;
    private final String url;
    private final List<String> examples;

    public PluginDocumentation(String summary, String url, List<String> examples) {
        this.summary = summary;
        this.url = url;
        this.examples = examples == null ? Collections.emptyList() : List.copyOf(examples);
    }

    public String getSummary() {
        return summary;
    }

    public String getUrl() {
        return url;
    }

    public List<String> getExamples() {
        return examples;
    }
}

