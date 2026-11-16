/*
 * Copyright (c) 2006-2026 Web-Harvest contributors.
 */
package org.webharvest.plugin.core;

import org.webharvest.plugin.DefaultPluginBodyExecutor;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Resolves the supported Loop input forms before iteration begins.
 *
 * <p>Source normalization is deliberately separate from iteration execution:
 * explicit attributes, child source plugins and inline text all become an
 * ordered list of Variables while the body remains a separate set of children.</p>
 */
final class LoopInputSourceResolver {

    private static final String VARIABLE_START = "${";
    private static final String VARIABLE_END = "}";

    private LoopInputSourceResolver() {
    }

    static ResolvedInput resolve(final PluginConfiguration configuration,
                                 final DynamicScopeContext context) throws InterruptedException {
        final String itemsAttribute = configuration.getProperty("items");
        final List<PluginConfiguration> children = configuration.getChildren();

        if (itemsAttribute != null) {
            final boolean hasBodyWrapper = containsBodyWrapper(children);
            final List<PluginConfiguration> bodyChildren = unwrapBodyChildren(children);
            return new ResolvedInput(true,
                resolveItemsAttribute(itemsAttribute, configuration.getProperty("delimiter", ","), context),
                bodyChildren, hasBodyWrapper);
        }

        if (!children.isEmpty() && isSourceElement(children.get(0))) {
            final PluginConfiguration sourceChild = children.get(0);
            final List<PluginConfiguration> bodyChildren =
                unwrapBodyChildren(children.subList(1, children.size()));
            final Variable sourceResult = executeSourceChild(configuration, sourceChild, context);
            return new ResolvedInput(true, flatten(sourceResult), bodyChildren, true);
        }

        final String textContent = configuration.getProperty("_textContent", "");
        if (textContent != null && !textContent.isBlank()) {
            final boolean hasBodyWrapper = containsBodyWrapper(children);
            final List<PluginConfiguration> bodyChildren = unwrapBodyChildren(children);
            final String delimiter = configuration.getProperty("delimiter", "\n");
            return new ResolvedInput(true, resolveInlineText(textContent, delimiter, context),
                bodyChildren, hasBodyWrapper);
        }

        final boolean hasBodyWrapper = containsBodyWrapper(children);
        final List<PluginConfiguration> bodyChildren = unwrapBodyChildren(children);
        return new ResolvedInput(false, Collections.<Variable>emptyList(),
            bodyChildren, hasBodyWrapper);
    }

    private static List<Variable> resolveItemsAttribute(final String items,
                                                        final String delimiter,
                                                        final DynamicScopeContext context) {
        final String trimmed = items == null ? "" : items.trim();
        if (isExactVariableReference(trimmed)) {
            final String variableName = trimmed.substring(VARIABLE_START.length(),
                trimmed.length() - VARIABLE_END.length()).trim();
            final Variable variable = context.getVar(variableName);
            if (variable instanceof ListVariable || variable instanceof EmptyVariable) {
                return flatten(variable);
            }
            if (variable != null) {
                return splitText(variable.toString(), delimiter);
            }
        }
        return splitText(items == null ? "" : items, delimiter);
    }

    private static List<Variable> resolveInlineText(final String textContent,
                                                    final String delimiter,
                                                    final DynamicScopeContext context) {
        final String trimmed = textContent.trim();
        if (isExactVariableReference(trimmed)) {
            final String variableName = trimmed.substring(VARIABLE_START.length(),
                trimmed.length() - VARIABLE_END.length()).trim();
            final Variable variable = context.getVar(variableName);
            if (variable instanceof ListVariable || variable instanceof EmptyVariable) {
                return flatten(variable);
            }
            if (variable != null) {
                return splitText(variable.toString(), delimiter);
            }
        }
        return splitText(textContent, delimiter);
    }

    private static boolean isExactVariableReference(final String value) {
        return value.startsWith(VARIABLE_START)
            && value.endsWith(VARIABLE_END)
            && value.indexOf(VARIABLE_START, VARIABLE_START.length()) < 0;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    // Each token must have its own Variable instance; sharing one would alias item values.
    private static List<Variable> splitText(final String value, final String delimiter) {
        final List<Variable> items = new ArrayList<>();
        if (value == null) {
            return items;
        }
        if (delimiter == null || delimiter.isEmpty()) {
            items.add(new NodeVariable(value.trim()));
            return items;
        }

        final String[] tokens = value.split(Pattern.quote(delimiter), -1);
        for (final String token : tokens) {
            items.add(new NodeVariable(token.trim()));
        }
        return items;
    }

    private static Variable executeSourceChild(final PluginConfiguration parent,
                                               final PluginConfiguration sourceChild,
                                               final DynamicScopeContext context)
            throws InterruptedException {
        final DefaultPluginConfiguration sourceContainer = new DefaultPluginConfiguration(
            "loop-source", parent.getNamespace(), "loop-source", new Properties());

        sourceContainer.setChildren(Collections.singletonList(sourceChild));

        try {
            return context.executeWithinNewContext(
                () -> new DefaultPluginBodyExecutor().executeBody(sourceContainer, context));
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to evaluate Loop source element: "
                + sourceChild.getElementName(), e);
        }
    }

    private static List<Variable> flatten(final Variable variable) {
        final List<Variable> items = new ArrayList<>();
        if (variable instanceof ListVariable) {
            final Collection<Variable> values = ((ListVariable) variable).getList();
            for (final Variable value : values) {
                if (value != null) {
                    // Preserve each direct member's type and nested collection boundary.
                    items.add(value);
                }
            }
        } else if (variable != null && !(variable instanceof EmptyVariable)) {
            items.add(variable);
        }
        return items;
    }

    private static boolean isSourceElement(final PluginConfiguration child) {
        final String elementName = child.getElementName();
        return "list".equals(elementName) || "xpath".equals(elementName);
    }

    private static boolean containsBodyWrapper(final List<PluginConfiguration> children) {
        for (final PluginConfiguration child : children) {
            if ("body".equals(child.getElementName())) {
                return true;
            }
        }
        return false;
    }

    private static List<PluginConfiguration> unwrapBodyChildren(
            final List<PluginConfiguration> children) {
        final List<PluginConfiguration> bodyChildren = new ArrayList<>();
        for (final PluginConfiguration child : children) {
            if ("body".equals(child.getElementName())) {
                bodyChildren.addAll(child.getChildren());
            } else {
                bodyChildren.add(child);
            }
        }
        return bodyChildren;
    }

    static final class ResolvedInput {
        private final boolean hasSource;
        private final List<Variable> items;
        private final List<PluginConfiguration> bodyChildren;
        private final boolean requiresCustomBodyExecution;

        private ResolvedInput(final boolean hasSource, final List<Variable> items,
                              final List<PluginConfiguration> bodyChildren,
                              final boolean requiresCustomBodyExecution) {
            this.hasSource = hasSource;
            this.items = items;
            this.bodyChildren = bodyChildren;
            this.requiresCustomBodyExecution = requiresCustomBodyExecution;
        }

        boolean hasSource() {
            return hasSource;
        }

        List<Variable> getItems() {
            return items;
        }

        List<PluginConfiguration> getBodyChildren() {
            return bodyChildren;
        }

        boolean requiresCustomBodyExecution() {
            return requiresCustomBodyExecution;
        }
    }
}
