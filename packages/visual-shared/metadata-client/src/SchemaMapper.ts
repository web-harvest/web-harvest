/**
 * SchemaMapper - Utility for mapping schema extensions to unified format
 * 
 * Ensures consistent schema format by always using extensions.schema.attributeDefinitions
 * as the source of truth, with fallback to legacy formats.
 */

import type { PluginSchema, AttributeDefinition } from './types.js';

/**
 * Map schema from extensions.schema to unified PluginSchema format
 * Always prefers extensions.schema.attributeDefinitions over legacy attributes
 */
export function mapSchemaFromExtensions(
    extensions: any,
    legacySchema?: any
): PluginSchema {
    // First, try to get from extensions.schema
    const schemaExt = extensions?.schema;
    if (schemaExt && schemaExt.attributeDefinitions) {
        return {
            attributes: mapAttributeDefinitions(schemaExt.attributeDefinitions),
            attributeDefinitions: schemaExt.attributeDefinitions,
            canHaveBody: schemaExt.allowsBody ?? true,
            canHaveChildren: Array.isArray(schemaExt.allowedSubElements) 
                ? schemaExt.allowedSubElements.length > 0 
                : true,
            canAppearAtRoot: schemaExt.canAppearAtRoot ?? true,
            requiredParents: Array.isArray(schemaExt.requiredParents) 
                ? schemaExt.requiredParents 
                : [],
            isContextSpecific: Array.isArray(schemaExt.requiredParents) 
                ? schemaExt.requiredParents.length > 0 
                : false,
            allowedSubElements: schemaExt.allowedSubElements,
            requiredSubElements: schemaExt.requiredSubElements,
            allowsBody: schemaExt.allowsBody
        };
    }
    
    // Fallback to legacy schema format
    if (legacySchema) {
        const attributeList: any[] = legacySchema.attributes || legacySchema.attributeDefinitions || [];
        return {
            attributes: mapAttributeDefinitions(attributeList),
            attributeDefinitions: attributeList,
            canHaveBody: legacySchema.canHaveBody ?? legacySchema.allowsBody ?? true,
            canHaveChildren: legacySchema.canHaveChildren ?? 
                (Array.isArray(legacySchema.allowedSubElements) ? legacySchema.allowedSubElements.length > 0 : true),
            canAppearAtRoot: legacySchema.canAppearAtRoot ?? true,
            requiredParents: legacySchema.requiredParents ?? [],
            isContextSpecific: Array.isArray(legacySchema.requiredParents) 
                ? legacySchema.requiredParents.length > 0 
                : false,
            allowedSubElements: legacySchema.allowedSubElements,
            requiredSubElements: legacySchema.requiredSubElements,
            allowsBody: legacySchema.allowsBody
        };
    }
    
    // Default empty schema
    return {
        attributes: [],
        attributeDefinitions: [],
        canHaveBody: true,
        canHaveChildren: true,
        canAppearAtRoot: true,
        requiredParents: [],
        isContextSpecific: false
    };
}

/**
 * Map attribute definitions to unified format
 */
function mapAttributeDefinitions(attrDefs: any[]): AttributeDefinition[] {
    if (!Array.isArray(attrDefs)) {
        return [];
    }
    
    return attrDefs.map(attr => ({
        name: attr.name,
        type: attr.type || 'string',
        required: Boolean(attr.required),
        defaultValue: attr.defaultValue,
        allowedValues: attr.allowedValues,
        description: attr.description,
        minValue: attr.minValue,
        maxValue: attr.maxValue,
        pattern: attr.pattern,
        ui: attr.ui
    }));
}

/**
 * Get attribute definitions from plugin data (prioritizes extensions.schema)
 */
export function getAttributeDefinitions(plugin: any): AttributeDefinition[] {
    // First try extensions.schema.attributeDefinitions
    if (plugin.extensions?.schema?.attributeDefinitions) {
        return mapAttributeDefinitions(plugin.extensions.schema.attributeDefinitions);
    }
    
    // Then try schema.attributeDefinitions
    if (plugin.schema?.attributeDefinitions) {
        return mapAttributeDefinitions(plugin.schema.attributeDefinitions);
    }
    
    // Then try schema.attributes
    if (plugin.schema?.attributes) {
        return mapAttributeDefinitions(plugin.schema.attributes);
    }
    
    // Finally try root-level attributes
    if (plugin.attributes) {
        return mapAttributeDefinitions(plugin.attributes);
    }
    
    return [];
}





