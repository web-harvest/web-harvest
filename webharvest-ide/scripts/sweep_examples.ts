#!/usr/bin/env tsx
import { promises as fs } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { JSDOM } from 'jsdom';
import { XMLParser } from '../frontend/src/visual/XMLParser';
import { XMLGenerator } from '../frontend/src/visual/XMLGenerator';
import { trimLeadingNoise } from '../frontend/src/utils/xmlSanitizer';

const { window } = new JSDOM('<!doctype html><html></html>');
(globalThis as any).DOMParser = window.DOMParser;
(globalThis as any).Node = window.Node;
(globalThis as any).Element = window.Element;
(globalThis as any).Document = window.Document;

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const EXAMPLES_DIR = path.resolve(__dirname, '../src/main/resources/webapp/examples');

interface SweepResult {
    file: string;
    mismatched: boolean;
    warnings: string[];
    difference: string | null;
}

function parseXmlDocument(xml: string): Document {
    const document = new DOMParser().parseFromString(xml, 'text/xml');
    const parseError = document.querySelector('parsererror');
    if (parseError) {
        throw new Error('XML Parse Error: ' + parseError.textContent);
    }
    return document;
}

/**
 * Compare XML structure rather than serialized formatting:
 * comments and declarations are non-semantic; attribute order and indentation
 * are irrelevant. Actual text and CDATA content (including significant
 * whitespace) remain part of the comparison.
 */
function canonicalizeElement(element: Element): string {
    const attributes = Array.from(element.attributes)
        .filter(attribute => !/^data-(width|height)$/.test(attribute.name))
        .sort((left, right) => {
            const leftName = (left.namespaceURI || '') + ':' + (left.localName || left.name);
            const rightName = (right.namespaceURI || '') + ':' + (right.localName || right.name);
            return leftName.localeCompare(rightName);
        })
        .map(attribute => [
            attribute.namespaceURI || '',
            attribute.localName || attribute.name,
            attribute.value
        ]);

    const childNodes = Array.from(element.childNodes);
    const hasElementChildren = childNodes.some(node => node.nodeType === Node.ELEMENT_NODE);
    const hasMeaningfulText = childNodes.some(node =>
        (node.nodeType === Node.TEXT_NODE || node.nodeType === Node.CDATA_SECTION_NODE) &&
        (node.nodeValue || '').trim().length > 0
    );

    const content: string[] = [];
    let pendingText = '';
    const flushText = (): void => {
        if (pendingText.length > 0) {
            const indentationOnly = hasElementChildren &&
                !hasMeaningfulText &&
                pendingText.trim().length === 0;
            if (!indentationOnly) {
                content.push('#' + JSON.stringify(pendingText));
            }
            pendingText = '';
        }
    };

    childNodes.forEach(node => {
        if (node.nodeType === Node.TEXT_NODE || node.nodeType === Node.CDATA_SECTION_NODE) {
            pendingText += node.nodeValue || '';
            return;
        }
        if (node.nodeType === Node.COMMENT_NODE) {
            // Ignore comments and coalesce adjacent text around them.
            return;
        }

        flushText();
        if (node.nodeType === Node.ELEMENT_NODE) {
            content.push(canonicalizeElement(node as Element));
        }
    });
    flushText();

    const identity = [
        element.namespaceURI || '',
        element.localName || element.tagName,
        attributes
    ];
    return 'E' + JSON.stringify(identity) + '[' + content.join('') + ']';
}

function canonicalizeXml(xml: string): string {
    const document = parseXmlDocument(xml);
    if (!document.documentElement) {
        throw new Error('XML document has no root element');
    }
    return canonicalizeElement(document.documentElement);
}

function describeFirstDifference(original: string, rebuilt: string): string {
    let index = 0;
    while (
        index < original.length &&
        index < rebuilt.length &&
        original[index] === rebuilt[index]
    ) {
        index++;
    }
    const start = Math.max(0, index - 100);
    return 'First structural difference at offset ' + index +
        ': original=' + JSON.stringify(original.slice(start, index + 180)) +
        '; rebuilt=' + JSON.stringify(rebuilt.slice(start, index + 180));
}

function escapeXmlAttribute(value: string): string {
    return value
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&apos;');
}

async function sweepExample(file: string): Promise<SweepResult> {
    const absolutePath = path.join(EXAMPLES_DIR, file);
    const original = await fs.readFile(absolutePath, 'utf-8');
    const sanitized = trimLeadingNoise(original);

    const parser = new XMLParser();
    const nodes = parser.parse(sanitized);

    const generator = new XMLGenerator();
    const regeneratedBody = generator.generate(nodes, {
        indentLevel: 1,
        includeLayoutMetadata: true,
        metadataComment: 'Generated by WebHarvest sweeper'
    });
    // XMLParser models config children, not the config wrapper. Preserve the
    // source wrapper and its namespace/charset declarations for comparison.
    const originalDocument = parseXmlDocument(sanitized);
    const originalRoot = originalDocument.documentElement;
    const rootAttributes = Array.from(originalRoot.attributes)
        .map(attribute => ' ' + attribute.name + '="' + escapeXmlAttribute(attribute.value) + '"')
        .join('');
    const rebuilt = '<?xml version="1.0" encoding="UTF-8"?>\\n' +
        '<' + originalRoot.tagName + rootAttributes + '>\\n' +
        regeneratedBody + '\\n</' + originalRoot.tagName + '>';

    const normalizedOriginal = canonicalizeXml(trimLeadingNoise(original));
    const normalizedRebuilt = canonicalizeXml(trimLeadingNoise(rebuilt));

    const mismatched = normalizedOriginal !== normalizedRebuilt;
    const warnings = parser.getLegacyNotices().map((notice) =>
        `Converted <${notice.legacyTag}> to <${notice.replacement}>${notice.message ? ` – ${notice.message}` : ''}`
    );

    return {
        file,
        mismatched,
        warnings,
        difference: mismatched
            ? describeFirstDifference(normalizedOriginal, normalizedRebuilt)
            : null
    };
}

async function sweepExamples(): Promise<void> {
    const files = (await fs.readdir(EXAMPLES_DIR)).filter((filename) => filename.endsWith('.xml'));
    const results = await Promise.all(files.map((file) => sweepExample(file)));

    let mismatches = 0;
    results.forEach((result) => {
        if (result.warnings.length > 0) {
            console.warn(`⚠️  ${result.file}`);
            result.warnings.forEach((warning) => console.warn(`   • ${warning}`));
        }
        if (result.mismatched) {
            mismatches += 1;
            console.error(`❌ Semantic round-trip mismatch detected for ${result.file}`);
            if (result.difference) {
                console.error('   ' + result.difference);
            }
        } else {
            console.log(`✅ ${result.file}`);
        }
    });

    if (mismatches > 0) {
        console.error(`\n${mismatches} file(s) failed the round-trip check.`);
        process.exitCode = 1;
    } else {
        console.log('\nAll examples passed round-trip validation.');
    }
}

void sweepExamples();


