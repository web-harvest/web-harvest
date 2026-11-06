import { JSDOM } from 'jsdom';
import { XMLParser, XMLGenerator, prepareXmlForBackend } from '@webharvest/xml-roundtrip';

const { window } = new JSDOM('<!doctype html><html></html>');
globalThis.DOMParser = window.DOMParser;
globalThis.Node = window.Node;

const parser = new XMLParser();
const nodes = parser.parse('<config><var name="x">1</var></config>');
if (!Array.isArray(nodes) || nodes.length !== 1 || nodes[0].type !== 'var') {
  console.error('xml-roundtrip consumer: unexpected parse result', nodes);
  process.exit(1);
}

const generator = new XMLGenerator();
const xml = generator.generate(nodes);
const prepared = prepareXmlForBackend(`<?xml version="1.0"?>\n${xml}`);
if (!prepared.includes('<var')) {
  console.error('xml-roundtrip consumer: unexpected generate/sanitize result', prepared);
  process.exit(1);
}

console.log('xml-roundtrip consumer: ok');
