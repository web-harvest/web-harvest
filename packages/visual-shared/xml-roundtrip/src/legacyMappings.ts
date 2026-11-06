export interface LegacyNotice {
    legacyTag: string;
    replacement: string;
    message?: string;
}

export interface LegacyRule {
    replacement: string;
    note?: string;
    transform?: (context: LegacyTransformContext) => void;
}

interface LegacyTransformContext {
    attributes: Record<string, string>;
    element: Element;
}

const LEGACY_RULES: Record<string, LegacyRule> = {
    body: {
        replacement: 'fragment',
        note: 'Legacy <body> container converted to fragment to retain child nodes.'
    },
    'search_results': {
        replacement: 'template',
        note: 'Converted legacy search_results to template element.'
    },
    'search-results': {
        replacement: 'template',
        note: 'Converted legacy search-results to template element.'
    },
    'search_result': {
        replacement: 'template-entry',
        note: 'Converted legacy search_result to template-entry for detail rows.'
    },
    'scriptlet': {
        replacement: 'script',
        note: 'Converted scriptlet to script element.',
        transform: ({ attributes }) => {
            if (!attributes.language) {
                attributes.language = 'javascript';
            }
        }
    }
};

export function resolveLegacyMapping(tagName: string): LegacyRule | null {
    return LEGACY_RULES[tagName.toLowerCase()] || null;
}

