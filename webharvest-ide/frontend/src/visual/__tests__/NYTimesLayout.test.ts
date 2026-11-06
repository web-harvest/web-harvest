import { describe, expect, it, beforeEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode, Plugin } from '../../types';
import type { PluginManager } from '../../core/PluginManager';

type PluginOverrides = Partial<Plugin> & { id: string };

const basePlugin = (overrides: PluginOverrides): Plugin => ({
    id: overrides.id,
    name: overrides.name ?? overrides.id,
    description: overrides.description ?? '',
    category: overrides.category ?? 'General',
    icon: overrides.icon ?? 'fa-puzzle-piece',
    schema: overrides.schema ?? {
        attributes: [],
        canHaveBody: false,
        canHaveChildren: false,
        canAppearAtRoot: true,
        requiredParents: [],
        isContextSpecific: false,
        allowedSubElements: []
    },
    semantics: overrides.semantics ?? {
        roles: ['LEAF'],
        canAppearAtRoot: true,
        allowsChildren: false,
        acceptsAnyChild: false,
        allowBody: false,
        allowTextContent: false,
        allowedChildTypes: [],
        requiredParentTypes: []
    },
    documentation: overrides.documentation ?? { summary: '', examples: [] },
    dependencies: overrides.dependencies ?? [],
    color: overrides.color,
    group: overrides.group,
    defaultWidth: overrides.defaultWidth,
    defaultHeight: overrides.defaultHeight,
    inputPorts: overrides.inputPorts,
    outputPorts: overrides.outputPorts,
    portType: overrides.portType,
    tooltip: overrides.tooltip,
    keyboardShortcuts: overrides.keyboardShortcuts ?? [],
    accessibility: overrides.accessibility,
    linkedServices: overrides.linkedServices ?? [],
    parentRelationship: overrides.parentRelationship,
    allowedChildPlugins: overrides.allowedChildPlugins ?? [],
    paletteGroup: overrides.paletteGroup,
    tags: overrides.tags,
    ui: overrides.ui
});

const plugins: Record<string, Plugin> = {
    http: basePlugin({
        id: 'http',
        name: 'HTTP',
        schema: {
            attributes: [
                { name: 'method', type: 'enum', required: true, allowedValues: ['GET', 'POST'], description: 'HTTP method' },
                { name: 'url', type: 'url', required: true, description: 'Request URL' }
            ],
            canHaveBody: false,
            canHaveChildren: true,
            canAppearAtRoot: true,
            requiredParents: [],
            isContextSpecific: false,
            allowedSubElements: ['http-param', 'http-header']
        },
        semantics: {
            roles: ['CONTEXT_CONTAINER'],
            canAppearAtRoot: true,
            allowsChildren: true,
            acceptsAnyChild: false,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: ['http-param', 'http-header'],
            requiredParentTypes: []
        },
        allowedChildPlugins: ['http-param', 'http-header']
    }),
    'http-param': basePlugin({
        id: 'http-param',
        name: 'HTTP Param',
        schema: {
            attributes: [
                { name: 'name', type: 'string', required: true, description: 'Parameter name' },
                { name: 'value', type: 'string', required: true, description: 'Parameter value' }
            ],
            canHaveBody: false,
            canHaveChildren: false,
            canAppearAtRoot: false,
            requiredParents: ['http'],
            isContextSpecific: true,
            allowedSubElements: []
        },
        semantics: {
            roles: ['DEPENDENT'],
            canAppearAtRoot: false,
            allowsChildren: false,
            acceptsAnyChild: false,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: ['http']
        }
    }),
    'http-header': basePlugin({
        id: 'http-header',
        name: 'HTTP Header',
        schema: {
            attributes: [
                { name: 'name', type: 'string', required: true, description: 'Header name' },
                { name: 'value', type: 'string', required: true, description: 'Header value' }
            ],
            canHaveBody: false,
            canHaveChildren: false,
            canAppearAtRoot: false,
            requiredParents: ['http'],
            isContextSpecific: true,
            allowedSubElements: []
        },
        semantics: {
            roles: ['DEPENDENT'],
            canAppearAtRoot: false,
            allowsChildren: false,
            acceptsAnyChild: false,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: ['http']
        }
    }),
    def: basePlugin({
        id: 'def',
        name: 'Define Var',
        schema: {
            attributes: [{ name: 'var', type: 'string', required: true }],
            canHaveBody: true,
            canHaveChildren: false,
            canAppearAtRoot: true,
            requiredParents: [],
            isContextSpecific: false,
            allowedSubElements: []
        },
        semantics: {
            roles: ['LEAF'],
            canAppearAtRoot: true,
            allowsChildren: false,
            acceptsAnyChild: false,
            allowBody: true,
            allowTextContent: true,
            allowedChildTypes: [],
            requiredParentTypes: []
        }
    }),
    file: basePlugin({
        id: 'file',
        name: 'File',
        schema: {
            attributes: [
                { name: 'action', type: 'enum', required: true, allowedValues: ['write'], description: 'File action' },
                { name: 'path', type: 'string', required: true, description: 'Output path' },
                { name: 'charset', type: 'string', required: false }
            ],
            canHaveBody: false,
            canHaveChildren: true,
            canAppearAtRoot: true,
            requiredParents: [],
            isContextSpecific: false,
            allowedSubElements: []
        },
        semantics: {
            roles: ['CONTEXT_CONTAINER'],
            canAppearAtRoot: true,
            allowsChildren: true,
            acceptsAnyChild: true,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: []
        }
    }),
    template: basePlugin({
        id: 'template',
        name: 'Template',
        schema: {
            attributes: [],
            canHaveBody: true,
            canHaveChildren: false,
            canAppearAtRoot: false,
            requiredParents: [],
            isContextSpecific: false,
            allowedSubElements: []
        },
        semantics: {
            roles: ['LEAF'],
            canAppearAtRoot: false,
            allowsChildren: false,
            acceptsAnyChild: false,
            allowBody: true,
            allowTextContent: true,
            allowedChildTypes: [],
            requiredParentTypes: []
        }
    }),
    loop: basePlugin({
        id: 'loop',
        name: 'Loop',
        schema: {
            attributes: [
                { name: 'item', type: 'string', required: true },
                { name: 'index', type: 'string', required: false }
            ],
            canHaveBody: true,
            canHaveChildren: true,
            canAppearAtRoot: false,
            requiredParents: [],
            isContextSpecific: false,
            allowedSubElements: []
        },
        semantics: {
            roles: ['CONTEXT_CONTAINER'],
            canAppearAtRoot: false,
            allowsChildren: true,
            acceptsAnyChild: true,
            allowBody: true,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: []
        }
    }),
    list: basePlugin({
        id: 'list',
        name: 'List',
        schema: {
            attributes: [],
            canHaveBody: false,
            canHaveChildren: true,
            canAppearAtRoot: false,
            requiredParents: [],
            isContextSpecific: false,
            allowedSubElements: []
        },
        semantics: {
            roles: ['CONTEXT_CONTAINER'],
            canAppearAtRoot: false,
            allowsChildren: true,
            acceptsAnyChild: true,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: []
        }
    }),
    xpath: basePlugin({
        id: 'xpath',
        name: 'XPath',
        schema: {
            attributes: [{ name: 'expression', type: 'xpath', required: true }],
            canHaveBody: false,
            canHaveChildren: true,
            canAppearAtRoot: false,
            requiredParents: [],
            isContextSpecific: false,
            allowedSubElements: []
        },
        semantics: {
            roles: ['BODY_EVALUATOR'],
            canAppearAtRoot: false,
            allowsChildren: true,
            acceptsAnyChild: true,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: []
        }
    }),
    'html-to-xml': basePlugin({
        id: 'html-to-xml',
        name: 'HTML to XML',
        schema: {
            attributes: [],
            canHaveBody: false,
            canHaveChildren: true,
            canAppearAtRoot: false,
            requiredParents: [],
            isContextSpecific: false,
            allowedSubElements: []
        },
        semantics: {
            roles: ['CONTEXT_CONTAINER'],
            canAppearAtRoot: false,
            allowsChildren: true,
            acceptsAnyChild: true,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: []
        }
    }),
    body: basePlugin({
        id: 'body',
        name: 'Body',
        schema: {
            attributes: [],
            canHaveBody: false,
            canHaveChildren: true,
            canAppearAtRoot: false,
            requiredParents: [],
            isContextSpecific: false,
            allowedSubElements: []
        },
        semantics: {
            roles: ['CONTEXT_CONTAINER'],
            canAppearAtRoot: false,
            allowsChildren: true,
            acceptsAnyChild: true,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: []
        }
    }),
    xquery: basePlugin({
        id: 'xquery',
        name: 'XQuery',
        schema: {
            attributes: [],
            canHaveBody: false,
            canHaveChildren: true,
            canAppearAtRoot: false,
            requiredParents: [],
            isContextSpecific: false,
            allowedSubElements: ['xq-param', 'xq-expression']
        },
        semantics: {
            roles: ['CONTEXT_CONTAINER'],
            canAppearAtRoot: false,
            allowsChildren: true,
            acceptsAnyChild: false,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: ['xq-param', 'xq-expression'],
            requiredParentTypes: []
        },
        allowedChildPlugins: ['xq-param', 'xq-expression']
    }),
    'xq-param': basePlugin({
        id: 'xq-param',
        name: 'XQuery Parameter',
        schema: {
            attributes: [
                { name: 'name', type: 'string', required: true, description: 'Parameter name' }
            ],
            canHaveBody: true,
            canHaveChildren: true,
            canAppearAtRoot: false,
            requiredParents: ['xquery'],
            isContextSpecific: true,
            allowedSubElements: []
        },
        semantics: {
            roles: ['DEPENDENT'],
            canAppearAtRoot: false,
            allowsChildren: true,
            acceptsAnyChild: true,
            allowBody: true,
            allowTextContent: true,
            allowedChildTypes: [],
            requiredParentTypes: ['xquery']
        }
    }),
    'xq-expression': basePlugin({
        id: 'xq-expression',
        name: 'XQuery Expression',
        schema: {
            attributes: [],
            canHaveBody: true,
            canHaveChildren: false,
            canAppearAtRoot: false,
            requiredParents: ['xquery'],
            isContextSpecific: true,
            allowedSubElements: []
        },
        semantics: {
            roles: ['DEPENDENT'],
            canAppearAtRoot: false,
            allowsChildren: false,
            acceptsAnyChild: false,
            allowBody: true,
            allowTextContent: true,
            allowedChildTypes: [],
            requiredParentTypes: ['xquery']
        }
    })
};

const pluginManager = {
    getSchemaVersion: () => null,
    findPlugin: (id: string) => plugins[id] ?? null,
    findPluginById: (id: string) => plugins[id] ?? null,
    getPlugins: () => Object.values(plugins),
    getAllPlugins: () => Object.values(plugins),
    getSemantics: (id: string) => plugins[id]?.semantics ?? null,
    searchPlugins: () => Object.values(plugins),
    getRootLevelPlugins: () => [],
    getDependentPlugins: () => [],
    getMasterPlugins: () => []
} as unknown as PluginManager;

const createHttpParam = (id: string, name: string, value: string): VisualNode => ({
    id,
    type: 'http-param',
    attributes: { name, value },
    body: null,
    children: []
});

const createNYTimesNodes = (): VisualNode[] => [
    {
        id: 'ny-http',
        type: 'http',
        attributes: {
            method: 'POST',
            url: 'http://www.nytimes.com/auth/login'
        },
        body: null,
        children: [
            createHttpParam('ny-http-param-continue', 'is_continue', 'true'),
            createHttpParam('ny-http-param-uri', 'URI', 'http://'),
            createHttpParam('ny-http-param-user', 'USERID', 'web-harvest'),
            createHttpParam('ny-http-param-password', 'PASSWORD', 'web-harvest')
        ]
    },
    {
        id: 'ny-def',
        type: 'def',
        attributes: { var: 'startUrl' },
        body: 'http://www.nytimes.com/pages/todayspaper/index.html',
        children: []
    },
    {
        id: 'ny-file',
        type: 'file',
        attributes: {
            action: 'write',
            path: 'nytimes/nytimes${sys.date()}.xml',
            charset: 'UTF-8'
        },
        body: null,
        children: [
            {
                id: 'ny-template-start',
                type: 'template',
                attributes: {},
                body: '<newyork_times date="${sys.datetime("dd.MM.yyyy")}">',
                children: []
            },
            {
                id: 'ny-loop',
                type: 'loop',
                attributes: { item: 'articleUrl', index: 'i' },
                body: null,
                children: [
                    {
                        id: 'ny-list',
                        type: 'list',
                        attributes: {},
                        body: null,
                        children: [
                            {
                                id: 'ny-xpath-1',
                                type: 'xpath',
                                attributes: { expression: "//div[@class='story clearfix']/h5/a[1]/@href" },
                                body: null,
                                children: [
                                    {
                                        id: 'ny-html-1',
                                        type: 'html-to-xml',
                                        attributes: {},
                                        body: null,
                                        children: [
                                            {
                                                id: 'ny-http-front',
                                                type: 'http',
                                                attributes: { url: '${startUrl}' },
                                                body: null,
                                                children: []
                                            }
                                        ]
                                    }
                                ]
                            },
                            {
                                id: 'ny-xpath-2',
                                type: 'xpath',
                                attributes: { expression: "//div[@class='story' or @class='story headline']/a[1]/@href" },
                                body: null,
                                children: []
                            }
                        ]
                    },
                    {
                        id: 'ny-body',
                        type: 'body',
                        attributes: {},
                        body: null,
                        children: [
                            {
                                id: 'ny-xquery',
                                type: 'xquery',
                                attributes: {},
                                body: null,
                                children: [
                                    {
                                        id: 'ny-xq-param',
                                        type: 'xq-param',
                                        attributes: { name: 'doc' },
                                        body: null,
                                        children: [
                                            {
                                                id: 'ny-html-article',
                                                type: 'html-to-xml',
                                                attributes: {},
                                                body: null,
                                                children: [
                                                    {
                                                        id: 'ny-http-article',
                                                        type: 'http',
                                                        attributes: { url: '${sys.fullUrl(startUrl, articleUrl)}?&pagewanted=print' },
                                                        body: null,
                                                        children: []
                                                    }
                                                ]
                                            }
                                        ]
                                    },
                                    {
                                        id: 'ny-xq-expression',
                                        type: 'xq-expression',
                                        attributes: {},
                                        body: '<article>...</article>',
                                        children: []
                                    }
                                ]
                            }
                        ]
                    }
                ]
            },
            {
                id: 'ny-template-end',
                type: 'template',
                attributes: {},
                body: '</newyork_times>',
                children: []
            }
        ]
    }
];

describe('VisualDesigner NYTimes layout', () => {
    beforeEach(() => {
        document.body.innerHTML = '<div id="canvas-area" style="position:relative; width:1600px; height:900px;"></div>';
    });

    it('positions root nodes horizontally and renders HTTP detail summaries', () => {
        const designer = new VisualDesigner('canvas-area');
        designer.setPluginManager(pluginManager);
        designer.loadNodes(createNYTimesNodes());

        const httpNode = document.querySelector('[data-node-id="ny-http"]') as HTMLElement;
        const defNode = document.querySelector('[data-node-id="ny-def"]') as HTMLElement;
        const fileNode = document.querySelector('[data-node-id="ny-file"]') as HTMLElement;

        expect(httpNode).not.toBeNull();
        expect(defNode).not.toBeNull();
        expect(fileNode).not.toBeNull();

        const httpLeft = parseInt(httpNode.style.left, 10);
        const defLeft = parseInt(defNode.style.left, 10);
        const fileLeft = parseInt(fileNode.style.left, 10);

        expect(defLeft).toBeGreaterThan(httpLeft);
        expect(fileLeft).toBeGreaterThan(defLeft);

        (designer as unknown as { enterContainer: (id: string) => void }).enterContainer('ny-http');

        const detailCards = Array.from(document.querySelectorAll('.focus-summary__detail-card'));
        expect(detailCards.length).toBeGreaterThan(0);
        expect(detailCards[0].textContent?.toLowerCase()).toContain('param');
    });
});

