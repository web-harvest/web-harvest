package org.webharvest.plugin.metadata;

/**
 * Core-owned marker types referenced by {@link org.webharvest.plugin.annotations.UiTool}
 * declarations. Downstream IDE/editor shells may map these markers to concrete
 * UI tooling without introducing IDE dependencies into Core.
 */
public final class CatalogUiToolMarkers {

    private CatalogUiToolMarkers() {
    }

    /**
     * Marker for an HTTP request/response inspector tool.
     */
    public static final class HttpInspector {
        private HttpInspector() {
        }
    }

    /**
     * Marker for a template/expression builder tool.
     */
    public static final class ExpressionBuilder {
        private ExpressionBuilder() {
        }
    }
}
