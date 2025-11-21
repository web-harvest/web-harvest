import metadataFixture from '../../fixtures/metadata.json' assert { type: 'json' };
import { normalizePlugin } from '@webharvest/metadata-client';

const plugins = (metadataFixture.plugins || []).map((plugin: any) => normalizePlugin(plugin));

console.info(`[vscode-harness] Normalized ${plugins.length} plugins via shared metadata-client.`);
console.info('[vscode-harness] TODO: wire @vscode/test-web to launch the WebView bundle.');


