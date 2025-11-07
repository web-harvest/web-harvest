/**
 * SemanticValidator - Validates plugin placement based on semantic metadata
 * Enforces constraints like "catch must be inside try", "ftp-put requires ftp parent", etc.
 */

import type { Plugin } from '../types';

interface ValidationResult {
    valid: boolean;
    message: string;
}

interface IPluginManager {
    findPlugin(id: string): Plugin | null;
    getPlugins(): Plugin[];
}

export class SemanticValidator {
    constructor(private pluginManager: IPluginManager) {
    }

    /**
     * Check if a plugin can appear at root level
     */
    canAppearAtRoot(pluginType: string): boolean {
        const plugin = this.pluginManager.findPlugin(pluginType);
        if (!plugin) return false;

        // Check semantic constraints
        if (plugin.semantics?.canAppearAtRoot === false) {
            return false;
        }

        // Fallback to schema constraints
        return plugin.schema?.canAppearAtRoot !== false;
    }

    /**
     * Check if a plugin is dependent (requires a parent)
     */
    isDependent(pluginType: string): boolean {
        const requiredParents = this.getRequiredParents(pluginType);
        return requiredParents.length > 0;
    }

    /**
     * Get required parent types for a plugin
     */
    getRequiredParents(pluginType: string): string[] {
        const plugin = this.pluginManager.findPlugin(pluginType);
        if (!plugin) return [];

        const parents = plugin.semantics?.requiredParentTypes || [];
        if (parents.length > 0) return parents;

        // Fallback to schema
        return plugin.schema?.requiredParents || [];
    }

    /**
     * Get allowed child types for a container
     */
    getAllowedChildren(parentType: string): string[] {
        const plugin = this.pluginManager.findPlugin(parentType);
        if (!plugin) return [];

        // Check semantic constraints
        const children = plugin.semantics?.allowedChildTypes || [];
        if (children.length > 0) return children;

        // Check if container accepts any child
        if (plugin.semantics?.acceptsAnyChild === true) {
            return [];
        }

        // Fallback to schema
        return plugin.schema?.allowedSubElements || [];
    }

    /**
     * Determine if a plugin allows nested child plugins
     */
    allowsChildren(pluginType: string): boolean {
        const plugin = this.pluginManager.findPlugin(pluginType);
        if (!plugin) {
            return false;
        }

        if (plugin.semantics) {
            if (plugin.semantics.allowedChildTypes && plugin.semantics.allowedChildTypes.length > 0) {
                return true;
            }
            if (plugin.semantics.acceptsAnyChild) {
                return true;
            }
            if (plugin.semantics.allowsChildren) {
                return true;
            }
        }

        if (plugin.schema) {
            if (plugin.schema.canHaveChildren) {
                return true;
            }
            if (plugin.schema.allowedSubElements && plugin.schema.allowedSubElements.length > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get semantic roles for a plugin (primary role first)
     */
    getRoles(pluginType: string): string[] {
        const plugin = this.pluginManager.findPlugin(pluginType);
        if (!plugin || !plugin.semantics) {
            return [];
        }

        if (Array.isArray(plugin.semantics.roles) && plugin.semantics.roles.length > 0) {
            return plugin.semantics.roles;
        }

        if ((plugin.semantics as any).role) {
            return [(plugin.semantics as any).role];
        }

        return [];
    }

    hasRole(pluginType: string, role: string): boolean {
        return this.getRoles(pluginType).includes(role);
    }

    /**
     * Check if a plugin allows body content
     */
    allowsBody(pluginType: string): boolean {
        const plugin = this.pluginManager.findPlugin(pluginType);
        if (!plugin) return false;
        
        // Check semantic constraints first
        if (plugin.semantics?.allowBody !== undefined) {
            return plugin.semantics.allowBody;
        }
        
        // Fallback to schema
        return plugin.schema?.canHaveBody === true;
    }

    /**
     * Check if a plugin allows text content
     */
    allowsTextContent(pluginType: string): boolean {
        const plugin = this.pluginManager.findPlugin(pluginType);
        if (!plugin) return false;
        
        // Check semantic constraints first
        if (plugin.semantics?.allowTextContent !== undefined) {
            return plugin.semantics.allowTextContent;
        }
        
        // Fallback to schema
        return plugin.schema?.canHaveBody === true;
    }

    /**
     * Check if a child can be placed in a parent
     */
    canBeChildOf(childType: string, parentType: string | null): boolean {
        // Root placement check
        if (parentType === null) {
            return this.canAppearAtRoot(childType);
        }

        // Prevent self-nesting for certain container types
        // These containers should not accept themselves as children
        const nonNestableContainers = ['try', 'catch', 'if', 'else'];
        if (childType === parentType && nonNestableContainers.includes(childType)) {
            return false;
        }

        const childPlugin = this.pluginManager.findPlugin(childType);
        const parentPlugin = this.pluginManager.findPlugin(parentType);

        if (!childPlugin || !parentPlugin) {
            return true; // Allow unknown plugins for backwards compatibility
        }

        // Check if parent accepts any child
        if (parentPlugin.semantics?.acceptsAnyChild === true) {
            // Still need to check if parent is allowed for this child
            const requiredParents = this.getRequiredParents(childType);
            return requiredParents.length === 0 || requiredParents.includes(parentType);
        }

        // Check specific allowed children
        const allowedChildren = this.getAllowedChildren(parentType);
        if (allowedChildren.length > 0) {
            return allowedChildren.includes(childType);
        }

        // If parent has no restrictions, allow if child doesn't require a specific parent
        const requiredParents = this.getRequiredParents(childType);
        return requiredParents.length === 0 || requiredParents.includes(parentType);
    }

    /**
     * Validate a drop operation
     */
    validateDrop(childType: string, parentType: string | null): ValidationResult {
        const childPlugin = this.pluginManager.findPlugin(childType);
        if (!childPlugin) {
            return {
                valid: true,
                message: 'Unknown plugin, allowing drop'
            };
        }

        const dependentPlacementHints: Record<string, string> = {
            'http-header': 'HTTP headers are configured from the HTTP node. Use the HTTP toolbar to add a header.',
            'http-param': 'HTTP parameters are configured from the HTTP node. Use the HTTP toolbar to add a parameter.',
            'else': 'Else branches are created from the if node. Use the if toolbar to add an else block.',
            'catch': 'Catch blocks are created from the try node. Use the try toolbar to add a catch block.'
        };

        if (parentType === null) {
            if (!this.canAppearAtRoot(childType)) {
                const requiredParents = this.getRequiredParents(childType);
                const parentList = requiredParents.join(', ');

                const dependentMessage = dependentPlacementHints[childType];
                if (dependentMessage) {
                    return {
                        valid: false,
                        message: dependentMessage
                    };
                }

                return {
                    valid: false,
                    message: `"${childType}" requires a parent element. Parent must be: ${parentList}`
                };
            }
            return {
                valid: true,
                message: `"${childType}" can be placed at root level`
            };
        }

        // Prevent self-nesting for certain container types
        const nonNestableContainers = ['try', 'catch', 'if', 'else'];
        if (childType === parentType && nonNestableContainers.includes(childType)) {
            const childName = childPlugin.name || childType;
            return {
                valid: false,
                message: `"${childName}" cannot be nested inside another "${childName}" block`
            };
        }

        // Check child-parent compatibility
        if (!this.canBeChildOf(childType, parentType)) {
            const parentPlugin = this.pluginManager.findPlugin(parentType);
            const parentName = parentPlugin?.name || parentType;

            const requiredParents = this.getRequiredParents(childType);
            if (requiredParents.length > 0) {
                const parentList = requiredParents.join(', ');
                return {
                    valid: false,
                    message: `"${childType}" requires parent: ${parentList}, but found: ${parentName}`
                };
            }

            const allowedChildren = this.getAllowedChildren(parentType);
            if (allowedChildren.length > 0) {
                const childList = allowedChildren.join(', ');
                const dependentMessage = dependentPlacementHints[childType];
                if (dependentMessage) {
                    return {
                        valid: false,
                        message: dependentMessage
                    };
                }
                return {
                    valid: false,
                    message: `"${childType}" is not allowed inside ${parentName}. Allowed children: ${childList}`
                };
            }
        }

        return {
            valid: true,
            message: `"${childType}" can be placed inside "${parentType}"`
        };
    }
}

