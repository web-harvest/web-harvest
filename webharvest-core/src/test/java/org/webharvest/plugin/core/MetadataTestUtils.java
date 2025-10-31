package org.webharvest.plugin.core;

import org.webharvest.plugin.annotations.CorePlugin;

final class MetadataTestUtils {

    private MetadataTestUtils() {
        // utility class
    }

    static String expectedCategory(Class<?> pluginClass) {
        CorePlugin annotation = pluginClass.getAnnotation(CorePlugin.class);
        return annotation != null ? annotation.category() : "Core";
    }
}

