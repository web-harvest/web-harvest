/**
 * SemanticValidator Tests - TDD for semantic validation logic
 */

import { SemanticValidator } from '../SemanticValidator';
import { PluginManager } from '../../core/PluginManager';
import type { Plugin, PluginSemanticsModel } from '../../types';

// Mock plugin data
const mockPlugins: Plugin[] = [
    {
        id: 'http',
        name: 'HTTP Request',
        description: 'HTTP request',
        category: 'Network',
        icon: 'fa-globe',
        schema: {
            attributes: [],
            canHaveBody: true,
            canHaveChildren: true,
            canAppearAtRoot: true,
            requiredParents: [],
            isContextSpecific: false
        },
        semantics: {
            roles: ['BODY_EVALUATOR', 'CONTEXT_CONTAINER'],
            role: 'BODY_EVALUATOR',
            canAppearAtRoot: true,
            allowsChildren: true,
            acceptsAnyChild: false,
            allowBody: true,
            allowTextContent: false,
            allowedChildTypes: ['http-header', 'http-param'],
            requiredParentTypes: []
        }
    },
    {
        id: 'http-header',
        name: 'HTTP Header',
        description: 'HTTP header',
        category: 'Network',
        icon: 'fa-tag',
        schema: {
            attributes: [],
            canHaveBody: true,
            canHaveChildren: false,
            canAppearAtRoot: false,
            requiredParents: ['http'],
            isContextSpecific: true
        },
        semantics: {
            roles: ['DEPENDENT'],
            role: 'DEPENDENT',
            canAppearAtRoot: false,
            allowsChildren: false,
            acceptsAnyChild: false,
            allowBody: true,
            allowTextContent: true,
            allowedChildTypes: [],
            requiredParentTypes: ['http']
        }
    },
    {
        id: 'http-param',
        name: 'HTTP Parameter',
        description: 'HTTP parameter',
        category: 'Network',
        icon: 'fa-list',
        schema: {
            attributes: [],
            canHaveBody: true,
            canHaveChildren: false,
            canAppearAtRoot: false,
            requiredParents: ['http'],
            isContextSpecific: true
        },
        semantics: {
            roles: ['DEPENDENT'],
            role: 'DEPENDENT',
            canAppearAtRoot: false,
            allowsChildren: false,
            acceptsAnyChild: false,
            allowBody: true,
            allowTextContent: true,
            allowedChildTypes: [],
            requiredParentTypes: ['http']
        }
    },
    {
        id: 'try',
        name: 'Try',
        description: 'Try block',
        category: 'Control',
        icon: 'fa-shield',
        schema: {
            attributes: [],
            canHaveBody: true,
            canHaveChildren: true,
            canAppearAtRoot: true,
            requiredParents: [],
            isContextSpecific: false
        },
        semantics: {
            roles: ['CONTEXT_CONTAINER'],
            role: 'CONTEXT_CONTAINER',
            canAppearAtRoot: true,
            allowsChildren: true,
            acceptsAnyChild: true,
            allowBody: true,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: []
        }
    },
    {
        id: 'catch',
        name: 'Catch',
        description: 'Catch block',
        category: 'Control',
        icon: 'fa-triangle-exclamation',
        schema: {
            attributes: [],
            canHaveBody: true,
            canHaveChildren: true,
            canAppearAtRoot: false,
            requiredParents: ['try'],
            isContextSpecific: true
        },
        semantics: {
            roles: ['DEPENDENT'],
            role: 'DEPENDENT',
            canAppearAtRoot: false,
            allowsChildren: true,
            acceptsAnyChild: true,
            allowBody: true,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: ['try']
        }
    },
    {
        id: 'call',
        name: 'Call',
        description: 'Call function',
        category: 'Control',
        icon: 'fa-phone',
        schema: {
            attributes: [],
            canHaveBody: true,
            canHaveChildren: true,
            canAppearAtRoot: true,
            requiredParents: [],
            isContextSpecific: false
        },
        semantics: {
            roles: ['CONTEXT_CONTAINER'],
            role: 'CONTEXT_CONTAINER',
            canAppearAtRoot: true,
            allowsChildren: true,
            acceptsAnyChild: false,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: ['call-param'],
            requiredParentTypes: []
        }
    },
    {
        id: 'call-param',
        name: 'Call Parameter',
        description: 'Call parameter',
        category: 'Control',
        icon: 'fa-sliders',
        schema: {
            attributes: [],
            canHaveBody: true,
            canHaveChildren: false,
            canAppearAtRoot: false,
            requiredParents: ['call'],
            isContextSpecific: true
        },
        semantics: {
            roles: ['DEPENDENT'],
            role: 'DEPENDENT',
            canAppearAtRoot: false,
            allowsChildren: false,
            acceptsAnyChild: false,
            allowBody: true,
            allowTextContent: true,
            allowedChildTypes: [],
            requiredParentTypes: ['call']
        }
    },
    {
        id: 'ftp',
        name: 'FTP',
        description: 'FTP connection',
        category: 'Network',
        icon: 'fa-server',
        schema: {
            attributes: [],
            canHaveBody: true,
            canHaveChildren: true,
            canAppearAtRoot: true,
            requiredParents: [],
            isContextSpecific: false
        },
        semantics: {
            roles: ['CONTEXT_CONTAINER'],
            role: 'CONTEXT_CONTAINER',
            canAppearAtRoot: true,
            allowsChildren: true,
            acceptsAnyChild: true,
            allowBody: true,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: []
        }
    },
    {
        id: 'ftp-put',
        name: 'FTP Put',
        description: 'FTP put command',
        category: 'Network',
        icon: 'fa-arrow-up',
        schema: {
            attributes: [],
            canHaveBody: true,
            canHaveChildren: false,
            canAppearAtRoot: false,
            requiredParents: ['ftp'],
            isContextSpecific: true
        },
        semantics: {
            roles: ['DEPENDENT'],
            role: 'DEPENDENT',
            canAppearAtRoot: false,
            allowsChildren: false,
            acceptsAnyChild: false,
            allowBody: true,
            allowTextContent: true,
            allowedChildTypes: [],
            requiredParentTypes: ['ftp']
        }
    }
];

// Mock PluginManager
class MockPluginManager {
    private plugins: Plugin[];

    constructor(plugins: Plugin[]) {
        this.plugins = plugins;
    }

    findPlugin(id: string): Plugin | null {
        return this.plugins.find(p => p.id === id) || null;
    }

    getPlugins(): Plugin[] {
        return [...this.plugins];
    }
}

describe('SemanticValidator', () => {
    let validator: SemanticValidator;
    let mockPluginManager: MockPluginManager;

    beforeEach(() => {
        mockPluginManager = new MockPluginManager(mockPlugins);
        validator = new SemanticValidator(mockPluginManager as any);
    });

    describe('canAppearAtRoot', () => {
        it('should return true for plugins that can appear at root', () => {
            expect(validator.canAppearAtRoot('http')).toBe(true);
            expect(validator.canAppearAtRoot('try')).toBe(true);
            expect(validator.canAppearAtRoot('ftp')).toBe(true);
        });

        it('should return false for dependent plugins', () => {
            expect(validator.canAppearAtRoot('catch')).toBe(false);
            expect(validator.canAppearAtRoot('call-param')).toBe(false);
            expect(validator.canAppearAtRoot('ftp-put')).toBe(false);
        });

        it('should return false for unknown plugins', () => {
            expect(validator.canAppearAtRoot('unknown')).toBe(false);
        });
    });

    describe('isDependent', () => {
        it('should return true for dependent plugins', () => {
            expect(validator.isDependent('catch')).toBe(true);
            expect(validator.isDependent('call-param')).toBe(true);
            expect(validator.isDependent('ftp-put')).toBe(true);
        });

        it('should return false for independent plugins', () => {
            expect(validator.isDependent('http')).toBe(false);
            expect(validator.isDependent('try')).toBe(false);
            expect(validator.isDependent('call')).toBe(false);
        });
    });

    describe('getRequiredParents', () => {
        it('should return required parent types for dependent plugins', () => {
            expect(validator.getRequiredParents('catch')).toEqual(['try']);
            expect(validator.getRequiredParents('call-param')).toEqual(['call']);
            expect(validator.getRequiredParents('ftp-put')).toEqual(['ftp']);
        });

        it('should return empty array for independent plugins', () => {
            expect(validator.getRequiredParents('http')).toEqual([]);
            expect(validator.getRequiredParents('try')).toEqual([]);
            expect(validator.getRequiredParents('call')).toEqual([]);
        });
    });

    describe('getAllowedChildren', () => {
        it('should return allowed child types for containers', () => {
            expect(validator.getAllowedChildren('call')).toEqual(['call-param']);
            expect(validator.getAllowedChildren('http')).toEqual(['http-header', 'http-param']);
        });

        it('should return empty array for containers that accept any child', () => {
            expect(validator.getAllowedChildren('try')).toEqual([]);
        });

        it('should return empty array for non-container plugins', () => {
            expect(validator.getAllowedChildren('catch')).toEqual([]);
            expect(validator.getAllowedChildren('ftp-put')).toEqual([]);
        });
    });

    describe('allowsChildren', () => {
        it('should return true for plugins that expose child slots', () => {
            expect(validator.allowsChildren('call')).toBe(true);
            expect(validator.allowsChildren('http')).toBe(true);
            expect(validator.allowsChildren('try')).toBe(true);
        });

        it('should return false for leaf or dependent plugins', () => {
            expect(validator.allowsChildren('call-param')).toBe(false);
            expect(validator.allowsChildren('http-header')).toBe(false);
            expect(validator.allowsChildren('ftp-put')).toBe(false);
        });
    });

    describe('canBeChildOf', () => {
        it('should return true when child is allowed in parent', () => {
            expect(validator.canBeChildOf('catch', 'try')).toBe(true);
            expect(validator.canBeChildOf('call-param', 'call')).toBe(true);
            expect(validator.canBeChildOf('ftp-put', 'ftp')).toBe(true);
            expect(validator.canBeChildOf('http-header', 'http')).toBe(true);
            expect(validator.canBeChildOf('http-param', 'http')).toBe(true);
            expect(validator.canBeChildOf('http', 'try')).toBe(true); // acceptsAnyChild
        });

        it('should return false when child is not allowed in parent', () => {
            expect(validator.canBeChildOf('call-param', 'try')).toBe(false);
            expect(validator.canBeChildOf('catch', 'http')).toBe(false);
            expect(validator.canBeChildOf('catch', 'ftp')).toBe(false);
        });

        it('should return false for root placement when parent is null', () => {
            expect(validator.canBeChildOf('http', null)).toBe(true);
            expect(validator.canBeChildOf('catch', null)).toBe(false);
            expect(validator.canBeChildOf('call-param', null)).toBe(false);
        });
    });

    describe('validateDrop', () => {
        it('should validate successful drops at root level', () => {
            const result = validator.validateDrop('http', null);
            expect(result.valid).toBe(true);
            expect(result.message).toBeDefined();
        });

        it('should validate successful drops in valid parent', () => {
            const result = validator.validateDrop('catch', 'try');
            expect(result.valid).toBe(true);
        });

        it('should reject dependent plugins dropped at root', () => {
            const result = validator.validateDrop('catch', null);
            expect(result.valid).toBe(false);
            expect(result.message).toContain('requires a parent element');
        });

        it('should reject dependent plugins dropped in wrong parent', () => {
            const result = validator.validateDrop('catch', 'http');
            expect(result.valid).toBe(false);
            expect(result.message).toContain('requires parent');
        });

        it('should allow http detail plugins inside http container', () => {
            const headerResult = validator.validateDrop('http-header', 'http');
            const paramResult = validator.validateDrop('http-param', 'http');
            expect(headerResult.valid).toBe(true);
            expect(paramResult.valid).toBe(true);
        });

        it('should reject plugins dropped in parent that does not accept them', () => {
            const result = validator.validateDrop('catch', 'call');
            expect(result.valid).toBe(false);
            expect(result.message).toContain('requires parent: try, but found:');
        });

        it('should handle unknown child plugins gracefully', () => {
            const result = validator.validateDrop('unknown', null);
            expect(result.valid).toBe(true); // Allow unknown plugins by default
        });

        it('should handle unknown parent plugins gracefully', () => {
            const result = validator.validateDrop('http', 'unknown');
            expect(result.valid).toBe(true); // Allow if child can appear anywhere
        });
    });
});

