import type { PluginTemplate } from './VisualModelHelpers';

export interface PaletteTemplate extends PluginTemplate {
    semantics?: {
        roles?: string[];
        requiredParentTypes?: string[];
        allowsAnyChild?: boolean;
        acceptsAnyChild?: boolean;
    };
    tags?: string[];
    requiredParentTypes?: string[];
    allowedChildTypes?: string[];
}

export interface PaletteFilterOptions {
    searchTerm?: string;
    containersOnly?: boolean;
    roleFilter?: string | null;
}

export interface PaletteGroup {
    name: string;
    templates: PaletteTemplate[];
}

export interface PaletteViewModel {
    primaryGroups: PaletteGroup[];
    advancedGroups: PaletteGroup[];
}

const WORKFLOW_GROUP = 'Workflow Blocks';
const UTILITIES_GROUP = 'Utilities';

const rolesOf = (template: PaletteTemplate): string[] => {
    const semanticsRoles = template.semantics?.roles;
    if (semanticsRoles && Array.isArray(semanticsRoles)) {
        return semanticsRoles;
    }
    const legacyRole = (template as any).semantics?.role;
    return legacyRole ? [legacyRole] : [];
};

const requiredParentsOf = (template: PaletteTemplate): string[] => {
    if (template.requiredParentTypes && template.requiredParentTypes.length > 0) {
        return template.requiredParentTypes;
    }
    const semanticParents = template.semantics?.requiredParentTypes;
    if (semanticParents && semanticParents.length > 0) {
        return semanticParents;
    }
    return [];
};

const allowsChildren = (template: PaletteTemplate): boolean => {
    if (template.semantics?.allowsAnyChild || template.semantics?.acceptsAnyChild) {
        return true;
    }
    if (template.allowedChildTypes && template.allowedChildTypes.length > 0) {
        return true;
    }
    return rolesOf(template).includes('CONTEXT_CONTAINER');
};

const isDependentTemplate = (template: PaletteTemplate): boolean => {
    if (template.canAppearAtRoot === false) {
        return true;
    }
    const roles = rolesOf(template);
    if (roles.includes('DEPENDENT')) {
        return true;
    }
    return requiredParentsOf(template).length > 0;
};

const matchesSearch = (template: PaletteTemplate, searchTerm?: string): boolean => {
    if (!searchTerm) {
        return true;
    }
    const term = searchTerm.toLowerCase();
    if (template.type.toLowerCase().includes(term)) {
        return true;
    }
    if (template.label?.toLowerCase().includes(term)) {
        return true;
    }
    if (template.description?.toLowerCase().includes(term)) {
        return true;
    }
    if (template.tags && template.tags.some(tag => tag.toLowerCase().includes(term))) {
        return true;
    }
    return false;
};

const matchesRoleFilter = (template: PaletteTemplate, roleFilter?: string | null): boolean => {
    if (!roleFilter) {
        return true;
    }
    return rolesOf(template).includes(roleFilter);
};

export function filterPaletteTemplates(
    templates: PaletteTemplate[],
    options: PaletteFilterOptions
): PaletteTemplate[] {
    const {
        searchTerm,
        containersOnly = false,
        roleFilter = null
    } = options;

    return templates.filter(template => {
        if (!matchesSearch(template, searchTerm)) {
            return false;
        }

        if (!matchesRoleFilter(template, roleFilter)) {
            return false;
        }

        if (isDependentTemplate(template)) {
            return false;
        }

        if (containersOnly && !allowsChildren(template)) {
            return false;
        }

        return true;
    });
}

const resolveGroupName = (template: PaletteTemplate): string => {
    const roles = rolesOf(template);
    if (roles.includes('ROOT_CONTAINER') || roles.includes('CONTEXT_CONTAINER')) {
        return WORKFLOW_GROUP;
    }
    if (allowsChildren(template)) {
        return WORKFLOW_GROUP;
    }
    return UTILITIES_GROUP;
};

export function groupPaletteTemplates(templates: PaletteTemplate[]): PaletteGroup[] {
    const grouped = new Map<string, PaletteTemplate[]>();

    templates.forEach(template => {
        const groupName = resolveGroupName(template);
        const bucket = grouped.get(groupName);
        if (bucket) {
            bucket.push(template);
        } else {
            grouped.set(groupName, [template]);
        }
    });

    const order = [WORKFLOW_GROUP, UTILITIES_GROUP];
    const result: PaletteGroup[] = [];

    order.forEach(name => {
        if (grouped.has(name)) {
            result.push({
                name,
                templates: grouped
                    .get(name)!
                    .slice()
                    .sort((a, b) => a.label.localeCompare(b.label))
            });
        }
    });

    // Append any additional groups that are not part of the default ordering
    grouped.forEach((value, key) => {
        if (!order.includes(key)) {
            result.push({
                name: key,
                templates: value.slice().sort((a, b) => a.label.localeCompare(b.label))
            });
        }
    });

    return result;
}

export function buildPaletteViewModel(
    templates: PaletteTemplate[],
    options: PaletteFilterOptions
): PaletteViewModel {
    const primaryTemplates = filterPaletteTemplates(templates, options);

    return {
        primaryGroups: groupPaletteTemplates(primaryTemplates),
        advancedGroups: []
    };
}

