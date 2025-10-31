/**
 * ExpressionBuilder - TDD Tests
 * Tests for visual expression builder with variable autocomplete
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ExpressionBuilder } from '../ExpressionBuilder';

describe('ExpressionBuilder', () => {
    let builder: ExpressionBuilder;
    let mockContainer: HTMLElement;

    beforeEach(() => {
        builder = new ExpressionBuilder();
        mockContainer = document.createElement('div');
        mockContainer.id = 'test-expression-builder';
        document.body.appendChild(mockContainer);
    });

    afterEach(() => {
        document.body.removeChild(mockContainer);
    });

    describe('render', () => {
        it('should render expression input', () => {
            builder.render(mockContainer, '${myVar}');

            const input = mockContainer.querySelector('.expression-input') as HTMLTextAreaElement;
            expect(input).not.toBeNull();
            expect(input.value).toBe('${myVar}');
        });

        it('should render variable suggestions panel', () => {
            builder.render(mockContainer, '');

            const suggestions = mockContainer.querySelector('.variable-suggestions');
            expect(suggestions).not.toBeNull();
        });

        it('should render function library', () => {
            builder.render(mockContainer, '');

            const functions = mockContainer.querySelector('.function-library');
            expect(functions).not.toBeNull();
        });

        it('should render preview panel', () => {
            builder.render(mockContainer, '');

            const preview = mockContainer.querySelector('.expression-preview');
            expect(preview).not.toBeNull();
        });
    });

    describe('setAvailableVariables', () => {
        it('should display available variables', () => {
            builder.render(mockContainer, '');
            builder.setAvailableVariables({
                httpResponse: '<html>...</html>',
                pageTitle: 'Example Page',
                loopIndex: '1'
            });

            const varItems = mockContainer.querySelectorAll('.variable-item');
            expect(varItems.length).toBe(3);
        });

        it('should show variable types', () => {
            builder.render(mockContainer, '');
            builder.setAvailableVariables({
                httpResponse: '<html>...</html>',
                pageTitle: 'Example Page'
            });

            const varList = mockContainer.querySelector('.variable-suggestions');
            expect(varList?.textContent).toContain('httpResponse');
            expect(varList?.textContent).toContain('pageTitle');
        });
    });

    describe('autocomplete', () => {
        it('should show autocomplete on ${', () => {
            builder.render(mockContainer, '');
            builder.setAvailableVariables({
                httpResponse: '<html>...</html>',
                pageTitle: 'Example Page'
            });

            const input = mockContainer.querySelector('.expression-input') as HTMLTextAreaElement;
            input.value = 'http://${';
            input.dispatchEvent(new Event('input'));

            const autocomplete = mockContainer.querySelector('.autocomplete-popup');
            expect(autocomplete).not.toBeNull();
        });

        it('should filter variables by prefix', () => {
            builder.render(mockContainer, '');
            builder.setAvailableVariables({
                httpResponse: '<html>...</html>',
                httpStatus: '200',
                pageTitle: 'Example Page'
            });

            const filtered = builder.filterVariables('http');
            expect(filtered.length).toBe(2);
            expect(filtered).toContain('httpResponse');
            expect(filtered).toContain('httpStatus');
        });

        it('should insert variable on click', (done) => {
            builder.render(mockContainer, '${');
            builder.setAvailableVariables({
                httpResponse: '<html>...</html>'
            });

            builder.onChange((expression) => {
                expect(expression).toContain('httpResponse');
                done();
            });

            // Simulate clicking a variable
            builder.insertVariable('httpResponse');
        });
    });

    describe('functions', () => {
        it('should list built-in functions', () => {
            builder.render(mockContainer, '');

            const functions = builder.getFunctions();
            expect(functions.length).toBeGreaterThan(0);
            expect(functions.some(f => f.name === 'concat')).toBe(true);
            expect(functions.some(f => f.name === 'replace')).toBe(true);
        });

        it('should insert function template on click', () => {
            builder.render(mockContainer, '');

            builder.insertFunction('concat', ['str1', 'str2']);

            const value = builder.getValue();
            expect(value).toContain('concat(');
        });

        it('should show function signature', () => {
            builder.render(mockContainer, '');

            const functions = builder.getFunctions();
            const concatFn = functions.find(f => f.name === 'concat');

            expect(concatFn).toBeDefined();
            expect(concatFn?.parameters).toEqual(['str1', 'str2']);
            expect(concatFn?.description).toBeTruthy();
        });
    });

    describe('preview', () => {
        it('should evaluate simple variable expression', () => {
            builder.render(mockContainer, '${pageTitle}');
            builder.setAvailableVariables({
                pageTitle: 'Example Page'
            });

            const preview = builder.evaluatePreview();
            expect(preview).toBe('Example Page');
        });

        it('should evaluate multiple variables', () => {
            builder.render(mockContainer, '${firstName} ${lastName}');
            builder.setAvailableVariables({
                firstName: 'John',
                lastName: 'Doe'
            });

            const preview = builder.evaluatePreview();
            expect(preview).toBe('John Doe');
        });

        it('should show error for undefined variables', () => {
            builder.render(mockContainer, '${unknownVar}');
            builder.setAvailableVariables({
                pageTitle: 'Example Page'
            });

            const preview = builder.evaluatePreview();
            expect(preview).toContain('undefined');
        });

        it('should update preview on input change', (done) => {
            builder.render(mockContainer, '${var1}');
            builder.setAvailableVariables({
                var1: 'Hello',
                var2: 'World'
            });

            // Change expression
            const input = mockContainer.querySelector('.expression-input') as HTMLTextAreaElement;
            input.value = '${var1} ${var2}';
            input.dispatchEvent(new Event('input'));

            setTimeout(() => {
                const previewElement = mockContainer.querySelector('.expression-preview');
                expect(previewElement?.textContent).toContain('Hello World');
                done();
            }, 100);
        });
    });

    describe('syntax highlighting', () => {
        it('should highlight variable syntax', () => {
            builder.render(mockContainer, '${myVar}');

            const highlighted = builder.getHighlightedExpression();
            expect(highlighted).toContain('<span');
            expect(highlighted).toContain('myVar');
        });

        it('should highlight multiple variables', () => {
            builder.render(mockContainer, '${var1} and ${var2}');

            const highlighted = builder.getHighlightedExpression();
            expect(highlighted).toContain('var1');
            expect(highlighted).toContain('var2');
        });
    });

    describe('getValue', () => {
        it('should return current expression', () => {
            builder.render(mockContainer, '${myVar}');

            const value = builder.getValue();
            expect(value).toBe('${myVar}');
        });

        it('should return updated expression after changes', () => {
            builder.render(mockContainer, '${var1}');

            const input = mockContainer.querySelector('.expression-input') as HTMLTextAreaElement;
            input.value = '${var2}';

            const value = builder.getValue();
            expect(value).toBe('${var2}');
        });
    });

    describe('validation', () => {
        it('should validate balanced braces', () => {
            builder.render(mockContainer, '${var1');

            const errors = builder.validate();
            expect(errors.length).toBeGreaterThan(0);
            expect(errors[0]).toContain('brace');
        });

        it('should pass validation for valid expressions', () => {
            builder.render(mockContainer, '${var1} text ${var2}');

            const errors = builder.validate();
            expect(errors.length).toBe(0);
        });

        it('should warn about undefined variables', () => {
            builder.render(mockContainer, '${unknownVar}');
            builder.setAvailableVariables({
                var1: 'value1'
            });

            const warnings = builder.getWarnings();
            expect(warnings.length).toBeGreaterThan(0);
            expect(warnings[0]).toContain('unknownVar');
        });
    });
});


