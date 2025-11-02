package org.webharvest.plugin.metadata;

import java.util.Collections;
import java.util.List;

/**
 * UI-specific hints for palette grouping, icons, and property editor helpers.
 */
public final class PluginUIHints {

    private final String paletteGroup;
    private final String icon;
    private final List<String> tags;

    public PluginUIHints(String paletteGroup, String icon, List<String> tags) {
        this.paletteGroup = paletteGroup;
        this.icon = icon;
        this.tags = tags == null ? Collections.emptyList() : List.copyOf(tags);
    }

    public String getPaletteGroup() {
        return paletteGroup;
    }

    public String getIcon() {
        return icon;
    }

    public List<String> getTags() {
        return tags;
    }
}

