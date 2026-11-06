export { XMLParser } from './XMLParser.js';
export { XMLGenerator } from './XMLGenerator.js';
export type { XMLGeneratorOptions } from './XMLGenerator.js';
export {
    sanitizeXmlProlog,
    trimLeadingNoise,
    stripVisualDesignerMetadata,
    prepareXmlForBackend
} from './xmlSanitizer.js';
export { resolveLegacyMapping } from './legacyMappings.js';
export type { LegacyNotice, LegacyRule } from './legacyMappings.js';
export type { VisualContentPart, VisualNode } from './visualNode.js';
