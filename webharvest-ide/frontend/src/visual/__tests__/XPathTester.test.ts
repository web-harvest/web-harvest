/**
 * XPathTester - TDD Tests
 * Tests for interactive XPath editor with validation and result preview
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { XPathTester, XPathResult } from '../XPathTester';

describe('XPathTester', () => {
    let tester: XPathTester;
    let mockContainer: HTMLElement;

    const sampleXML = `
        <html>
            <head>
                <title>Example Page</title>
            </head>
            <body>
                <h1>Welcome</h1>
                <div class="content">
                    <p id="intro">This is a paragraph.</p>
                    <p class="highlight">Another paragraph.</p>
                </div>
            </body>
        </html>
    `;

    beforeEach(() => {
        tester = new XPathTester();
        mockContainer = document.createElement('div');
        mockContainer.id = 'test-xpath-tester';
        document.body.appendChild(mockContainer);
    });

    afterEach(() => {
        document.body.removeChild(mockContainer);
    });

    describe('render', () => {
        it('should render XPath input', () => {
            tester.render(mockContainer, '//title');

            const input = mockContainer.querySelector('.xpath-input') as HTMLTextAreaElement;
            expect(input).not.toBeNull();
            expect(input.value).toBe('//title');
        });

        it('should render sample XML textarea', () => {
            tester.render(mockContainer, '//title');

            const xmlInput = mockContainer.querySelector('.xml-input') as HTMLTextAreaElement;
            expect(xmlInput).not.toBeNull();
        });

        it('should render test button', () => {
            tester.render(mockContainer, '//title');

            const button = mockContainer.querySelector('.test-button');
            expect(button).not.toBeNull();
            expect(button?.textContent).toContain('Test');
        });

        it('should render results panel', () => {
            tester.render(mockContainer, '//title');

            const results = mockContainer.querySelector('.xpath-results');
            expect(results).not.toBeNull();
        });
    });

    describe('setXML', () => {
        it('should set sample XML for testing', () => {
            tester.render(mockContainer, '//title');
            tester.setXML(sampleXML);

            const xmlInput = mockContainer.querySelector('.xml-input') as HTMLTextAreaElement;
            expect(xmlInput.value).toBe(sampleXML);
        });
    });

    describe('validateXPath', () => {
        it('should validate correct XPath expression', () => {
            const result = tester.validateXPath('//title');
            expect(result.valid).toBe(true);
            expect(result.errors.length).toBe(0);
        });

        it('should detect syntax errors', () => {
            const result = tester.validateXPath('//title[');
            expect(result.valid).toBe(false);
            expect(result.errors.length).toBeGreaterThan(0);
        });

        it('should detect invalid axis', () => {
            const result = tester.validateXPath('//invalid::title');
            expect(result.valid).toBe(false);
        });

        it('should accept complex XPath', () => {
            const result = tester.validateXPath('//div[@class="content"]/p[1]');
            expect(result.valid).toBe(true);
        });

        it('should accept XPath with predicates', () => {
            const result = tester.validateXPath('//p[@id="intro"]');
            expect(result.valid).toBe(true);
        });
    });

    describe('executeXPath', () => {
        it('should execute XPath and return matching nodes', () => {
            tester.setXML(sampleXML);

            const result = tester.executeXPath('//title');

            expect(result.matches.length).toBe(1);
            expect(result.matches[0].textContent).toBe('Example Page');
        });

        it('should return multiple matches', () => {
            tester.setXML(sampleXML);

            const result = tester.executeXPath('//p');

            expect(result.matches.length).toBe(2);
        });

        it('should return empty array for no matches', () => {
            tester.setXML(sampleXML);

            const result = tester.executeXPath('//nonexistent');

            expect(result.matches.length).toBe(0);
        });

        it('should handle XPath with predicates', () => {
            tester.setXML(sampleXML);

            const result = tester.executeXPath('//p[@id="intro"]');

            expect(result.matches.length).toBe(1);
            expect(result.matches[0].textContent).toContain('This is a paragraph');
        });

        it('should handle attribute selection', () => {
            tester.setXML(sampleXML);

            const result = tester.executeXPath('//p/@id');

            expect(result.matches.length).toBe(1);
        });

        it('should return error for invalid XPath', () => {
            tester.setXML(sampleXML);

            const result = tester.executeXPath('//title[');

            expect(result.error).toBeDefined();
            expect(result.matches.length).toBe(0);
        });
    });

    describe('highlightResults', () => {
        it('should highlight matching nodes in XML', () => {
            tester.render(mockContainer, '//title');
            tester.setXML(sampleXML);

            const result = tester.executeXPath('//title');
            tester.highlightResults(result);

            const highlighted = mockContainer.querySelector('.xml-highlighted');
            expect(highlighted).not.toBeNull();
            expect(highlighted?.innerHTML).toContain('<mark');
        });

        it('should show match count', () => {
            tester.render(mockContainer, '//p');
            tester.setXML(sampleXML);

            const result = tester.executeXPath('//p');
            tester.highlightResults(result);

            const count = mockContainer.querySelector('.match-count');
            expect(count?.textContent).toContain('2 matches');
        });

        it('should show no matches message', () => {
            tester.render(mockContainer, '//nonexistent');
            tester.setXML(sampleXML);

            const result = tester.executeXPath('//nonexistent');
            tester.highlightResults(result);

            const message = mockContainer.querySelector('.no-matches');
            expect(message).not.toBeNull();
        });
    });

    describe('getMatchedElements', () => {
        it('should return matched elements with XPath', () => {
            tester.setXML(sampleXML);

            const elements = tester.getMatchedElements('//p');

            expect(elements.length).toBe(2);
            expect(elements[0].tagName.toLowerCase()).toBe('p');
        });

        it('should return element attributes', () => {
            tester.setXML(sampleXML);

            const elements = tester.getMatchedElements('//p[@id]');

            expect(elements.length).toBe(1);
            expect(elements[0].getAttribute('id')).toBe('intro');
        });
    });

    describe('formatXPathError', () => {
        it('should format XPath syntax error', () => {
            const error = new Error('Invalid XPath: //title[');
            const formatted = tester.formatXPathError(error);

            expect(formatted).toContain('Invalid XPath');
            expect(formatted).toContain('//title[');
        });

        it('should provide helpful suggestions', () => {
            const error = new Error('Unclosed bracket');
            const formatted = tester.formatXPathError(error);

            expect(formatted).toContain('bracket');
        });
    });

    describe('getXPathSuggestions', () => {
        it('should suggest common XPath patterns', () => {
            const suggestions = tester.getXPathSuggestions();

            expect(suggestions.length).toBeGreaterThan(0);
            expect(suggestions.some(s => s.includes('//'))).toBe(true);
        });

        it('should include examples with descriptions', () => {
            const suggestions = tester.getXPathSuggestions();

            const suggestion = suggestions[0];
            expect(suggestion).toHaveProperty('pattern');
            expect(suggestion).toHaveProperty('description');
        });
    });

    describe('onChange callback', () => {
        it('should call onChange when XPath changes', (done) => {
            tester.onChange((xpath) => {
                expect(xpath).toBe('//h1');
                done();
            });

            tester.render(mockContainer, '//title');

            const input = mockContainer.querySelector('.xpath-input') as HTMLTextAreaElement;
            input.value = '//h1';
            input.dispatchEvent(new Event('input'));
        });
    });

    describe('syntax highlighting', () => {
        it('should highlight XPath syntax', () => {
            const highlighted = tester.highlightXPathSyntax('//div[@class="content"]');

            expect(highlighted).toContain('<span');
            expect(highlighted).toContain('class');
        });

        it('should highlight axes', () => {
            const highlighted = tester.highlightXPathSyntax('//child::p');

            expect(highlighted).toContain('child');
        });

        it('should highlight predicates', () => {
            const highlighted = tester.highlightXPathSyntax('//p[1]');

            expect(highlighted).toContain('[1]');
        });
    });

    describe('XPath library', () => {
        it('should provide common XPath expressions', () => {
            const library = tester.getXPathLibrary();

            expect(library).toHaveProperty('basic');
            expect(library).toHaveProperty('attributes');
            expect(library).toHaveProperty('text');
            expect(library).toHaveProperty('advanced');
        });

        it('should categorize expressions', () => {
            const library = tester.getXPathLibrary();

            expect(Array.isArray(library.basic)).toBe(true);
            expect(library.basic.length).toBeGreaterThan(0);
        });
    });

    describe('real-time validation', () => {
        it('should show validation error on invalid input', (done) => {
            tester.render(mockContainer, '');

            tester.onValidationChange((isValid) => {
                expect(isValid).toBe(false);
                done();
            });

            const input = mockContainer.querySelector('.xpath-input') as HTMLTextAreaElement;
            input.value = '//title[';
            input.dispatchEvent(new Event('input'));
        });
    });
});


