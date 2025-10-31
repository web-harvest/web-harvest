/**
 * RegexValidator - TDD Tests
 * Tests for interactive regex editor with pattern library
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { RegexValidator, RegexTestResult, RegexPattern } from '../RegexValidator';

describe('RegexValidator', () => {
    let validator: RegexValidator;
    let mockContainer: HTMLElement;

    beforeEach(() => {
        validator = new RegexValidator();
        mockContainer = document.createElement('div');
        mockContainer.id = 'test-regex-validator';
        document.body.appendChild(mockContainer);
    });

    afterEach(() => {
        document.body.removeChild(mockContainer);
    });

    describe('render', () => {
        it('should render regex input', () => {
            validator.render(mockContainer, '\\d+');

            const input = mockContainer.querySelector('.regex-input') as HTMLInputElement;
            expect(input).not.toBeNull();
            expect(input.value).toBe('\\d+');
        });

        it('should render test text input', () => {
            validator.render(mockContainer, '');

            const textInput = mockContainer.querySelector('.test-text-input') as HTMLTextAreaElement;
            expect(textInput).not.toBeNull();
        });

        it('should render flags checkboxes', () => {
            validator.render(mockContainer, '');

            const globalFlag = mockContainer.querySelector('input[name="flag-g"]') as HTMLInputElement;
            const caseFlag = mockContainer.querySelector('input[name="flag-i"]') as HTMLInputElement;
            const multilineFlag = mockContainer.querySelector('input[name="flag-m"]') as HTMLInputElement;

            expect(globalFlag).not.toBeNull();
            expect(caseFlag).not.toBeNull();
            expect(multilineFlag).not.toBeNull();
        });

        it('should render results panel', () => {
            validator.render(mockContainer, '');

            const results = mockContainer.querySelector('.regex-results');
            expect(results).not.toBeNull();
        });

        it('should render pattern library', () => {
            validator.render(mockContainer, '');

            const library = mockContainer.querySelector('.pattern-library');
            expect(library).not.toBeNull();
        });
    });

    describe('validateRegex', () => {
        it('should validate correct regex', () => {
            const result = validator.validateRegex('\\d+');
            expect(result.valid).toBe(true);
            expect(result.error).toBeUndefined();
        });

        it('should detect invalid regex', () => {
            const result = validator.validateRegex('[a-z');
            expect(result.valid).toBe(false);
            expect(result.error).toBeDefined();
        });

        it('should accept empty regex', () => {
            const result = validator.validateRegex('');
            expect(result.valid).toBe(true);
        });

        it('should validate complex patterns', () => {
            const result = validator.validateRegex('^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$');
            expect(result.valid).toBe(true);
        });
    });

    describe('testRegex', () => {
        it('should find matches in text', () => {
            const result = validator.testRegex('\\d+', 'abc 123 def 456', 'g');

            expect(result.matches.length).toBe(2);
            expect(result.matches[0].text).toBe('123');
            expect(result.matches[1].text).toBe('456');
        });

        it('should return match positions', () => {
            const result = validator.testRegex('\\d+', 'abc 123 def', 'g');

            expect(result.matches[0].index).toBe(4);
            expect(result.matches[0].length).toBe(3);
        });

        it('should handle no matches', () => {
            const result = validator.testRegex('\\d+', 'abc def', 'g');

            expect(result.matches.length).toBe(0);
            expect(result.totalMatches).toBe(0);
        });

        it('should find only first match without global flag', () => {
            const result = validator.testRegex('\\d+', 'abc 123 def 456', '');

            expect(result.matches.length).toBe(1);
            expect(result.matches[0].text).toBe('123');
        });

        it('should handle case-insensitive flag', () => {
            const result = validator.testRegex('[a-z]+', 'ABC def GHI', 'i');

            expect(result.matches.length).toBeGreaterThan(0);
        });

        it('should extract capture groups', () => {
            const result = validator.testRegex('(\\w+)@(\\w+\\.\\w+)', 'user@example.com', 'g');

            expect(result.matches.length).toBe(1);
            expect(result.matches[0].groups).toBeDefined();
            expect(result.matches[0].groups?.length).toBe(2);
        });
    });

    describe('highlightMatches', () => {
        it('should highlight matches in text', () => {
            validator.render(mockContainer, '\\d+');
            validator.setTestText('abc 123 def');

            const result = validator.testRegex('\\d+', 'abc 123 def', 'g');
            validator.highlightMatches(result);

            const highlighted = mockContainer.querySelector('.highlighted-text');
            expect(highlighted).not.toBeNull();
            expect(highlighted?.innerHTML).toContain('<mark');
        });

        it('should show match count', () => {
            validator.render(mockContainer, '\\d+');
            validator.setTestText('123 456 789');

            const result = validator.testRegex('\\d+', '123 456 789', 'g');
            validator.highlightMatches(result);

            const count = mockContainer.querySelector('.match-count');
            expect(count?.textContent).toContain('3 matches');
        });

        it('should show capture groups', () => {
            validator.render(mockContainer, '(\\d+)-(\\d+)');
            validator.setTestText('123-456');

            const result = validator.testRegex('(\\d+)-(\\d+)', '123-456', 'g');
            validator.highlightMatches(result);

            const groups = mockContainer.querySelector('.capture-groups');
            expect(groups).not.toBeNull();
        });
    });

    describe('getPatternLibrary', () => {
        it('should provide common patterns', () => {
            const library = validator.getPatternLibrary();

            expect(library).toHaveProperty('email');
            expect(library).toHaveProperty('url');
            expect(library).toHaveProperty('phone');
            expect(library).toHaveProperty('date');
        });

        it('should have working email pattern', () => {
            const library = validator.getPatternLibrary();
            const result = validator.testRegex(library.email.pattern, 'user@example.com', 'g');

            expect(result.matches.length).toBe(1);
        });

        it('should have working URL pattern', () => {
            const library = validator.getPatternLibrary();
            const result = validator.testRegex(library.url.pattern, 'https://example.com', 'g');

            expect(result.matches.length).toBe(1);
        });

        it('should have pattern descriptions', () => {
            const library = validator.getPatternLibrary();

            expect(library.email.description).toBeTruthy();
            expect(library.url.description).toBeTruthy();
        });
    });

    describe('loadPattern', () => {
        it('should load pattern from library', () => {
            validator.render(mockContainer, '');

            const library = validator.getPatternLibrary();
            validator.loadPattern(library.email);

            const input = mockContainer.querySelector('.regex-input') as HTMLInputElement;
            expect(input.value).toBe(library.email.pattern);
        });
    });

    describe('explainRegex', () => {
        it('should explain simple pattern', () => {
            const explanation = validator.explainRegex('\\d+');

            expect(explanation.length).toBeGreaterThan(0);
            expect(explanation.some(e => e.includes('digit'))).toBe(true);
        });

        it('should explain character classes', () => {
            const explanation = validator.explainRegex('[a-z]+');

            expect(explanation.some(e => e.includes('lowercase'))).toBe(true);
        });

        it('should explain quantifiers', () => {
            const explanation = validator.explainRegex('\\d+');

            expect(explanation.some(e => e.includes('one or more'))).toBe(true);
        });

        it('should explain anchors', () => {
            const explanation = validator.explainRegex('^\\d+$');

            expect(explanation.some(e => e.includes('start'))).toBe(true);
            expect(explanation.some(e => e.includes('end'))).toBe(true);
        });
    });

    describe('getValue', () => {
        it('should return current regex pattern', () => {
            validator.render(mockContainer, '\\d+');

            const value = validator.getValue();
            expect(value).toBe('\\d+');
        });

        it('should return flags', () => {
            validator.render(mockContainer, '\\d+');

            const flags = validator.getFlags();
            expect(flags).toHaveProperty('global');
            expect(flags).toHaveProperty('caseInsensitive');
            expect(flags).toHaveProperty('multiline');
        });
    });

    describe('setTestText', () => {
        it('should set sample text for testing', () => {
            validator.render(mockContainer, '');
            validator.setTestText('Sample text for testing');

            const textInput = mockContainer.querySelector('.test-text-input') as HTMLTextAreaElement;
            expect(textInput.value).toBe('Sample text for testing');
        });
    });

    describe('onChange callback', () => {
        it('should call onChange when pattern changes', (done) => {
            validator.onChange((pattern, flags) => {
                expect(pattern).toBe('\\w+');
                done();
            });

            validator.render(mockContainer, '');

            const input = mockContainer.querySelector('.regex-input') as HTMLInputElement;
            input.value = '\\w+';
            input.dispatchEvent(new Event('input'));
        });
    });
});


