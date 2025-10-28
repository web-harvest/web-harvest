/**
 * Plugin Metadata Types with Hierarchy Support
 */

export interface AttributeDefinition {
    name: string;
    type: string;
    required: boolean;
    description: string;
    defaultValue?: any;
    allowedValues?: string[];
    options?: string[];
    min?: number;
    max?: number;
    pattern?: string;
}

export interface PluginSchema {
    attributes: AttributeDefinition[];
    allowsBody: boolean;
    allowedSubElements: string[];
    requiredSubElements: string[];
    requiredAttributes?: string[];
    optionalAttributes?: string[];
}

export interface PluginMetadata {
    elementName: string;
    name: string;
    description: string;
    category: string;
    icon: string;
    helpUrl: string;
    schema: PluginSchema;

    // Hierarchy support
    isContainer: boolean; // Can this plugin contain children?
    allowedChildren: string[]; // Which children are allowed
    requiredChildren: string[]; // Which children are required
    mustBeChildOf: string[]; // Must this plugin be child of specific parent?
}

/**
 * Extended PluginMetadata with computed hierarchy properties
 */
export function enrichPluginMetadata(plugin: PluginMetadata): PluginMetadata {
    const { schema } = plugin;

    // Determine if this is a container plugin
    const isContainer = schema.allowedSubElements && schema.allowedSubElements.length > 0;

    // Allowed children (from schema)
    const allowedChildren = schema.allowedSubElements || [];

    // Required children (from schema)
    const requiredChildren = schema.requiredSubElements || [];

    // Determine if this must be child of specific parent
    const mustBeChildOf: string[] = [];

    // Special cases based on element name
    if (plugin.elementName === 'http-header') {
        mustBeChildOf.push('http');
    } else if (plugin.elementName === 'http-param') {
        mustBeChildOf.push('http');
    } else if (plugin.elementName === 'db-param') {
        mustBeChildOf.push('database');
    } else if (plugin.elementName === 'call-param') {
        mustBeChildOf.push('call');
    }

    return {
        ...plugin,
        isContainer,
        allowedChildren,
        requiredChildren,
        mustBeChildOf
    };
}

/**
 * Check if a plugin can be dropped into a container
 */
export function canDropIntoContainer(
    childPlugin: PluginMetadata,
    containerPlugin: PluginMetadata
): boolean {
    // If container doesn't allow children, reject
    if (!containerPlugin.isContainer) {
        return false;
    }

    // If child must be child of specific parent
    if (childPlugin.mustBeChildOf.length > 0) {
        return childPlugin.mustBeChildOf.includes(containerPlugin.elementName);
    }

    // If container specifies allowed children
    if (containerPlugin.allowedChildren.length > 0) {
        return containerPlugin.allowedChildren.includes(childPlugin.elementName);
    }

    // Default: allow if container accepts any children
    return true;
}

/**
 * Get container plugins (can have children)
 */
export function getContainerPlugins(plugins: PluginMetadata[]): PluginMetadata[] {
    return plugins.filter(p => p.isContainer);
}

/**
 * Get leaf plugins (cannot have children)
 */
export function getLeafPlugins(plugins: PluginMetadata[]): PluginMetadata[] {
    return plugins.filter(p => !p.isContainer);
}

/**
 * Get plugins that must be children of specific parents
 */
export function getSpecialChildPlugins(plugins: PluginMetadata[]): PluginMetadata[] {
    return plugins.filter(p => p.mustBeChildOf && p.mustBeChildOf.length > 0);
}


