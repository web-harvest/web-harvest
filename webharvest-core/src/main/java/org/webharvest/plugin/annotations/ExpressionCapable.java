package org.webharvest.plugin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an attribute as supporting expression builder tooling even if its
 * {@link UiField} annotation is not present. Used to inform property editors
 * that the value accepts XPath or script expressions.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(ExpressionCapabilities.class)
public @interface ExpressionCapable {

    /**
     * Name of the attribute that can open an expression builder.
     */
    String attribute();

    /**
     * Optional description of the expression context (e.g., XPath, Groovy).
     */
    String context() default "";
}


