package org.webharvest.plugin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares UI-specific metadata for a plugin attribute so downstream editors
 * (desktop IDE, VS Code extension, etc.) can render the appropriate control,
 * grouping, and helper text without relying on hard-coded heuristics.
 *
 * <p>The annotation is repeatable and can be attached to a plugin class once
 * per attribute. The {@code attribute} value must match the attribute name
 * defined in {@link org.webharvest.plugin.PluginConfigurationSchema}.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(UiFields.class)
public @interface UiField {

    /**
     * Name of the attribute this metadata applies to.
     */
    String attribute();

    /**
     * Preferred field kind to render in property editors.
     */
    UiFieldKind kind() default UiFieldKind.TEXT;

    /**
     * Optional grouping hint used to cluster related fields.
     */
    String group() default "";

    /**
     * Display order within the group (ascending).
     */
    int order() default 0;

    /**
     * Helper text shown below the editor.
     */
    String helperText() default "";

    /**
     * Placeholder hint rendered inside the editor.
     */
    String placeholder() default "";

    /**
     * Localization key for the field label (optional).
     */
    String localizationKey() default "";

    /**
     * Localization key for the helper text (optional).
     */
    String helperTextKey() default "";

    /**
     * Localization key for the placeholder (optional).
     */
    String placeholderKey() default "";

    /**
     * Indicates the field can open an expression builder.
     */
    boolean expressionCapable() default false;

    /**
     * Optional format descriptor (e.g., {@code "regex"}, {@code "json"}).
     */
    String format() default "";

    /**
     * Minimum string length validation hint (-1 = unset).
     */
    int minLength() default -1;

    /**
     * Maximum string length validation hint (-1 = unset).
     */
    int maxLength() default -1;

    /**
     * Whether the field is considered "advanced" and hidden by default.
     */
    boolean advanced() default false;
}


