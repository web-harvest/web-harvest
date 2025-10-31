/**
 * SchemaEditor - TDD Tests
 * Tests for schema-driven property editor generation
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { SchemaEditor, EditorField } from '../SchemaEditor';
import type { Plugin } from '../../types';

describe('SchemaEditor', () => {
    let editor: SchemaEditor;
    let mockContainer: HTMLElement;

    beforeEach(() => {
        editor = new SchemaEditor();
        mockContainer = document.createElement('div');
        mockContainer.id = 'test-editor-container';
        document.body.appendChild(mockContainer);
    });

    afterEach(() => {
        document.body.removeChild(mockContainer);
    });

    describe('generateEditor', () => {
        it('should generate text input for string type', () => {
            const plugin: Plugin = {
                id: 'http',
                name: 'HTTP Request',
                description: 'HTTP plugin',
                category: 'Network',
                icon: 'fa-globe',
                schema: {
                    attributes: [
                        {
                            name: 'url',
                            type: 'string',
                            required: true,
                            description: 'Target URL'
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: false,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            const attributes = { url: 'http://example.com' };
            editor.generateEditor(mockContainer, plugin, attributes);

            const input = mockContainer.querySelector('input[name="url"]') as HTMLInputElement;
            expect(input).not.toBeNull();
            expect(input.type).toBe('text');
            expect(input.value).toBe('http://example.com');
            expect(input.required).toBe(true);
        });

        it('should generate URL input with validation for url type', () => {
            const plugin: Plugin = {
                id: 'http',
                name: 'HTTP Request',
                description: 'HTTP plugin',
                category: 'Network',
                icon: 'fa-globe',
                schema: {
                    attributes: [
                        {
                            name: 'url',
                            type: 'url',
                            required: true
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: false,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            editor.generateEditor(mockContainer, plugin, {});

            const input = mockContainer.querySelector('input[name="url"]') as HTMLInputElement;
            expect(input.type).toBe('url');
        });

        it('should generate number input with min/max for number type', () => {
            const plugin: Plugin = {
                id: 'loop',
                name: 'Loop',
                description: 'Loop plugin',
                category: 'Control',
                icon: 'fa-repeat',
                schema: {
                    attributes: [
                        {
                            name: 'maxloops',
                            type: 'number',
                            required: false,
                            defaultValue: '10'
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: true,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            editor.generateEditor(mockContainer, plugin, { maxloops: '10' });

            const input = mockContainer.querySelector('input[name="maxloops"]') as HTMLInputElement;
            expect(input.type).toBe('number');
            expect(input.value).toBe('10');
        });

        it('should generate checkbox for boolean type', () => {
            const plugin: Plugin = {
                id: 'http',
                name: 'HTTP Request',
                description: 'HTTP plugin',
                category: 'Network',
                icon: 'fa-globe',
                schema: {
                    attributes: [
                        {
                            name: 'followRedirects',
                            type: 'boolean',
                            required: false,
                            defaultValue: 'true'
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: false,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            editor.generateEditor(mockContainer, plugin, { followRedirects: 'true' });

            const input = mockContainer.querySelector('input[name="followRedirects"]') as HTMLInputElement;
            expect(input.type).toBe('checkbox');
            expect(input.checked).toBe(true);
        });

        it('should generate select dropdown for enum type', () => {
            const plugin: Plugin = {
                id: 'http',
                name: 'HTTP Request',
                description: 'HTTP plugin',
                category: 'Network',
                icon: 'fa-globe',
                schema: {
                    attributes: [
                        {
                            name: 'method',
                            type: 'enum',
                            required: true,
                            allowedValues: ['GET', 'POST', 'PUT', 'DELETE'],
                            defaultValue: 'GET'
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: false,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            editor.generateEditor(mockContainer, plugin, { method: 'POST' });

            const select = mockContainer.querySelector('select[name="method"]') as HTMLSelectElement;
            expect(select).not.toBeNull();
            expect(select.value).toBe('POST');
            expect(select.options.length).toBe(4);
        });

        it('should generate textarea for xpath type', () => {
            const plugin: Plugin = {
                id: 'xpath',
                name: 'XPath',
                description: 'XPath plugin',
                category: 'Extraction',
                icon: 'fa-code-branch',
                schema: {
                    attributes: [
                        {
                            name: 'expression',
                            type: 'xpath',
                            required: true
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: false,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            editor.generateEditor(mockContainer, plugin, { expression: '//title' });

            const textarea = mockContainer.querySelector('textarea[name="expression"]') as HTMLTextAreaElement;
            expect(textarea).not.toBeNull();
            expect(textarea.value).toBe('//title');
        });
    });

    describe('validation', () => {
        it('should validate required fields', () => {
            const plugin: Plugin = {
                id: 'http',
                name: 'HTTP Request',
                description: 'HTTP plugin',
                category: 'Network',
                icon: 'fa-globe',
                schema: {
                    attributes: [
                        {
                            name: 'url',
                            type: 'string',
                            required: true
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: false,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            editor.generateEditor(mockContainer, plugin, {});

            const errors = editor.validate();
            expect(errors.length).toBeGreaterThan(0);
            expect(errors[0].field).toBe('url');
            expect(errors[0].message).toContain('required');
        });

        it('should validate URL format', () => {
            const plugin: Plugin = {
                id: 'http',
                name: 'HTTP Request',
                description: 'HTTP plugin',
                category: 'Network',
                icon: 'fa-globe',
                schema: {
                    attributes: [
                        {
                            name: 'url',
                            type: 'url',
                            required: true
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: false,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            editor.generateEditor(mockContainer, plugin, { url: 'invalid-url' });

            const errors = editor.validate();
            expect(errors.length).toBeGreaterThan(0);
            expect(errors[0].field).toBe('url');
            expect(errors[0].message).toContain('valid URL');
        });

        it('should validate number range', () => {
            const plugin: Plugin = {
                id: 'loop',
                name: 'Loop',
                description: 'Loop plugin',
                category: 'Control',
                icon: 'fa-repeat',
                schema: {
                    attributes: [
                        {
                            name: 'maxloops',
                            type: 'number',
                            required: true
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: true,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            editor.generateEditor(mockContainer, plugin, { maxloops: 'abc' });

            const errors = editor.validate();
            expect(errors.length).toBeGreaterThan(0);
            expect(errors[0].field).toBe('maxloops');
        });

        it('should pass validation for valid inputs', () => {
            const plugin: Plugin = {
                id: 'http',
                name: 'HTTP Request',
                description: 'HTTP plugin',
                category: 'Network',
                icon: 'fa-globe',
                schema: {
                    attributes: [
                        {
                            name: 'url',
                            type: 'url',
                            required: true
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: false,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            editor.generateEditor(mockContainer, plugin, { url: 'http://example.com' });

            const errors = editor.validate();
            expect(errors.length).toBe(0);
        });
    });

    describe('getValue', () => {
        it('should return current attribute values', () => {
            const plugin: Plugin = {
                id: 'http',
                name: 'HTTP Request',
                description: 'HTTP plugin',
                category: 'Network',
                icon: 'fa-globe',
                schema: {
                    attributes: [
                        {
                            name: 'url',
                            type: 'string',
                            required: true
                        },
                        {
                            name: 'method',
                            type: 'enum',
                            required: false,
                            allowedValues: ['GET', 'POST'],
                            defaultValue: 'GET'
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: false,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            editor.generateEditor(mockContainer, plugin, { url: 'http://example.com', method: 'POST' });

            const values = editor.getValue();
            expect(values.url).toBe('http://example.com');
            expect(values.method).toBe('POST');
        });

        it('should return boolean values correctly', () => {
            const plugin: Plugin = {
                id: 'http',
                name: 'HTTP Request',
                description: 'HTTP plugin',
                category: 'Network',
                icon: 'fa-globe',
                schema: {
                    attributes: [
                        {
                            name: 'followRedirects',
                            type: 'boolean',
                            required: false,
                            defaultValue: 'true'
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: false,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            editor.generateEditor(mockContainer, plugin, { followRedirects: 'true' });

            const values = editor.getValue();
            expect(values.followRedirects).toBe('true');
        });
    });

    describe('onChange callback', () => {
        it('should call onChange when field value changes', (done) => {
            const plugin: Plugin = {
                id: 'http',
                name: 'HTTP Request',
                description: 'HTTP plugin',
                category: 'Network',
                icon: 'fa-globe',
                schema: {
                    attributes: [
                        {
                            name: 'url',
                            type: 'string',
                            required: true
                        }
                    ],
                    canHaveBody: false,
                    canHaveChildren: false,
                    canAppearAtRoot: true,
                    requiredParents: [],
                    isContextSpecific: false
                }
            };

            editor.onChange((attributes) => {
                expect(attributes.url).toBe('http://newurl.com');
                done();
            });

            editor.generateEditor(mockContainer, plugin, { url: 'http://example.com' });

            const input = mockContainer.querySelector('input[name="url"]') as HTMLInputElement;
            input.value = 'http://newurl.com';
            input.dispatchEvent(new Event('input', { bubbles: true }));
        });
    });
});


