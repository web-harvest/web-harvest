/**
 * SemanticValidation - Tests for semantic validation of plugin placement
 * Ensures containers cannot be nested in themselves (e.g., try in try)
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { SemanticValidator } from '../SemanticValidator';
import type { Plugin } from '../../types';

class MockPluginManager {
    private plugins: Map<string, Plugin> = new Map();

    findPlugin(id: string): Plugin | null {
        return this.plugins.get(id) || null;
    }

    getPlugins(): Plugin[] {
        return Array.from(this.plugins.values());
    }

    addPlugin(plugin: Plugin): void {
        this.plugins.set(plugin.id, plugin);
    }
}

describe('SemanticValidator - Self-Nesting Prevention', () => {
    let validator: SemanticValidator;
    let pluginManager: MockPluginManager;

    beforeEach(() => {
        pluginManager = new MockPluginManager();
        validator = new SemanticValidator(pluginManager);

        // Add try plugin
        pluginManager.addPlugin({
            id: 'try',
            name: 'Try',
            type: 'try',
            category: 'Control',
            semantics: {
                role: 'CONTEXT_CONTAINER',
                canAppearAtRoot: true,
                allowsChildren: true,
                acceptsAnyChild: true,
                allowBody: true,
                allowTextContent: false,
                allowedChildTypes: [],
                requiredParentTypes: []
            }
        } as Plugin);

        // Add catch plugin
        pluginManager.addPlugin({
            id: 'catch',
            name: 'Catch',
            type: 'catch',
            category: 'Control',
            semantics: {
                role: 'DEPENDENT',
                canAppearAtRoot: false,
                allowsChildren: true,
                acceptsAnyChild: true,
                allowBody: true,
                allowTextContent: false,
                allowedChildTypes: [],
                requiredParentTypes: ['try']
            }
        } as Plugin);

        // Add if plugin
        pluginManager.addPlugin({
            id: 'if',
            name: 'If',
            type: 'if',
            category: 'Control',
            semantics: {
                role: 'CONTEXT_CONTAINER',
                canAppearAtRoot: true,
                allowsChildren: true,
                acceptsAnyChild: true,
                allowBody: true,
                allowTextContent: false,
                allowedChildTypes: [],
                requiredParentTypes: []
            }
        } as Plugin);

        // Add http plugin (can be nested)
        pluginManager.addPlugin({
            id: 'http',
            name: 'HTTP',
            type: 'http',
            category: 'Network',
            semantics: {
                role: 'ACTION',
                canAppearAtRoot: true,
                allowsChildren: true,
                acceptsAnyChild: false,
                allowBody: false,
                allowTextContent: false,
                allowedChildTypes: [],
                requiredParentTypes: []
            }
        } as Plugin);
    });

    describe('try block self-nesting', () => {
        it('should prevent try from being nested in try', () => {
            const result = validator.validateDrop('try', 'try');
            expect(result.valid).toBe(false);
            expect(result.message).toContain('cannot be nested');
        });

        it('should allow http inside try', () => {
            const result = validator.validateDrop('http', 'try');
            expect(result.valid).toBe(true);
        });

        it('should allow catch inside try', () => {
            const result = validator.validateDrop('catch', 'try');
            expect(result.valid).toBe(true);
        });
    });

    describe('catch block self-nesting', () => {
        it('should prevent catch from being nested in catch', () => {
            const result = validator.validateDrop('catch', 'catch');
            expect(result.valid).toBe(false);
            expect(result.message).toContain('cannot be nested');
        });
    });

    describe('if block self-nesting', () => {
        it('should prevent if from being nested in if', () => {
            const result = validator.validateDrop('if', 'if');
            expect(result.valid).toBe(false);
            expect(result.message).toContain('cannot be nested');
        });
    });

    describe('canBeChildOf', () => {
        it('should return false for try in try', () => {
            expect(validator.canBeChildOf('try', 'try')).toBe(false);
        });

        it('should return false for catch in catch', () => {
            expect(validator.canBeChildOf('catch', 'catch')).toBe(false);
        });

        it('should return false for if in if', () => {
            expect(validator.canBeChildOf('if', 'if')).toBe(false);
        });

        it('should return true for http in try', () => {
            expect(validator.canBeChildOf('http', 'try')).toBe(true);
        });

        it('should return true for catch in try', () => {
            expect(validator.canBeChildOf('catch', 'try')).toBe(true);
        });
    });
});


