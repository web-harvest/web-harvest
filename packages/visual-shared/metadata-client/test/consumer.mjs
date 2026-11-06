import { pathToFileURL } from 'node:url';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const pkgRoot = path.resolve(__dirname, '..');
const entry = pathToFileURL(path.join(pkgRoot, 'dist', 'index.js')).href;

const { normalizePlugin } = await import(entry);

const plugin = normalizePlugin({
  id: 'var',
  name: 'Variable',
  schema: { attributes: [{ name: 'name', type: 'string', required: true }], canHaveBody: true },
});

if (!plugin || plugin.id !== 'var') {
  console.error('metadata-client consumer: unexpected normalizePlugin result', plugin);
  process.exit(1);
}

console.log('metadata-client consumer: ok');
