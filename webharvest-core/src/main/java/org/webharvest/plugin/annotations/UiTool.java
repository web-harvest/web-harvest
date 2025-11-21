package org.webharvest.plugin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an auxiliary UI tool (e.g., HTTP inspector, expression builder) that
 * should be surfaced by metadata-driven editors when the annotated plugin is
 * selected.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(UiTools.class)
public @interface UiTool {

    /**
     * Implementation class of the tool. The client may lazy-load it by name.
     */
    Class<?> value();

    /**
     * Unique identifier (defaults to the simple class name).
     */
    String id() default "";

    /**
     * Human-friendly label (defaults to synthesized name).
     */
    String label() default "";

    /**
     * Short description displayed in tooltips or menus.
     */
    String description() default "";

    /**
     * Optional icon resource (relative path or token).
     */
    String icon() default "";

    /**
     * Preferred placement (e.g., "panel", "modal", "inline").
     */
    String placement() default "panel";
}


