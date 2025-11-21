import metadataFixture from '../../fixtures/metadata.json' assert { type: 'json' };
import sampleXml from '../../fixtures/sample.xml?raw';
import { normalizePlugin } from '@webharvest/metadata-client';
import { XMLParser } from '@webharvest/xml-roundtrip';

type VsCodeApi = {
    postMessage(message: unknown): void;
};

declare global {
    interface Window {
        acquireVsCodeApi?: () => VsCodeApi;
    }
}

const vscode = window.acquireVsCodeApi ? window.acquireVsCodeApi() : undefined;

function log(message: string): void {
    const el = document.createElement('div');
    el.textContent = message;
    el.className = 'harness-log';
    document.body.appendChild(el);
}

function renderStatus(): void {
    const container = document.createElement('section');
    container.className = 'harness-status';
    const heading = document.createElement('h2');
    heading.textContent = 'VS Code Harness Preview';
    container.appendChild(heading);
    document.body.appendChild(container);
}

renderStatus();

const normalizedPlugins = (metadataFixture.plugins || []).map((plugin: any) => normalizePlugin(plugin));
log(`Loaded ${normalizedPlugins.length} plugins via metadata-client.`);

const parser = new XMLParser();
try {
    const nodes = parser.parse(sampleXml);
    log(`XML round-trip ready (parsed ${nodes.length} root nodes).`);
} catch (error) {
    log(`Failed to parse XML fixture: ${(error as Error).message}`);
}

window.addEventListener('message', (event) => {
    log(`[webview] message: ${JSON.stringify(event.data)}`);
});

vscode?.postMessage({ command: 'STATUS', level: 'info', message: 'WebView ready' });

type VsCodeApi = {
    postMessage(message: unknown): void;
};

declare global {
    interface Window {
        acquireVsCodeApi?: () => VsCodeApi;
    }
}

import metadataFixture from '../../fixtures/metadata.json' assert { type: 'json' };
import sampleXml from '../../fixtures/sample.xml?raw';
import { normalizePlugin } from '@webharvest/metadata-client';
import { XMLParser } from '@webharvest/xml-roundtrip';

const vscode = window.acquireVsCodeApi ? window.acquireVsCodeApi() : undefined;

function log(message: string): void {
    const el = document.createElement('div');
    el.textContent = message;
    el.className = 'harness-log';
    document.body.appendChild(el);
}

function renderStatus(): void {
    const container = document.createElement('section');
    container.className = 'harness-status';
    const heading = document.createElement('h2');
    heading.textContent = 'VS Code Harness Preview';
    container.appendChild(heading);
    document.body.appendChild(container);
}

renderStatus();

const normalizedPlugins = (metadataFixture.plugins || []).map((plugin: any) => normalizePlugin(plugin));
log(`Loaded ${normalizedPlugins.length} plugins via metadata-client.`);

const parser = new XMLParser();
try {
    const nodes = parser.parse(sampleXml);
    log(`XML round-trip ready (parsed ${nodes.length} root nodes).`);
} catch (error) {
    log(`Failed to parse XML fixture: ${(error as Error).message}`);
}

window.addEventListener('message', (event) => {
    log(`[webview] message: ${JSON.stringify(event.data)}`);
});

vscode?.postMessage({ command: 'STATUS', level: 'info', message: 'WebView ready' });


