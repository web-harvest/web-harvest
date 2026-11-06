import { normalizePlugin } from '@webharvest/metadata-client';

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
