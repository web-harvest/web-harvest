import { App } from '../App';

const mockMonaco = {
    editor: {
        create: jest.fn(() => ({
            getValue: jest.fn(() => '<config></config>'),
            setValue: jest.fn(),
            onDidChangeModelContent: jest.fn(),
            getModel: jest.fn(() => ({
                onDidChangeContent: jest.fn(),
            })),
            updateOptions: jest.fn(),
        })),
        defineTheme: jest.fn(),
        setTheme: jest.fn(),
    },
};

(global as any).monaco = mockMonaco;

const flushPromises = async (): Promise<void> => {
    await Promise.resolve();
    await Promise.resolve();
};

describe('App - Visual save events', () => {
    let app: App;
    let visualDesigner: any;
    let createObjectURLSpy: jest.Mock;
    let revokeObjectURLSpy: jest.Mock;
    let createElementSpy: jest.SpyInstance;
    let clickSpy: jest.Mock;
    let anchors: HTMLAnchorElement[];
    let hadCreateObjectURL: boolean;
    let hadRevokeObjectURL: boolean;
    let originalCreateObjectURL: ((obj: Blob | MediaSource) => string) | undefined;
    let originalRevokeObjectURL: ((url: string) => void) | undefined;
    const realCreateElement = document.createElement.bind(document);

    beforeEach(async () => {
        anchors = [];
        clickSpy = jest.fn();

        (global as any).fetch = jest.fn((url: string) => {
            if (url.includes('/api/plugins/metadata')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ plugins: [], version: 'test' }),
                } as Response);
            }

            if (url.includes('/api/plugins')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ plugins: [], count: 0 }),
                } as Response);
            }

            return Promise.resolve({
                ok: true,
                json: () => Promise.resolve({}),
            } as Response);
        }) as jest.Mock;

        document.body.innerHTML = `
            <div id="editor"></div>
            <div id="logs"></div>
            <div id="results"></div>
            <div id="variables"></div>
            <div id="session"></div>
            <div id="canvas-area"></div>
            <div id="propertyPanel"></div>
            <div id="plugin-palette"></div>
            <div id="tabs"></div>
            <div id="tabs-container"></div>
            <span id="logsBadge">0</span>
            <span id="varsBadge">0</span>
            <span id="current-filename"></span>
            <span id="dirty-indicator"></span>
        `;

        app = new App();
        await flushPromises();
        visualDesigner = (app as any).visualDesigner;

        hadCreateObjectURL = typeof URL.createObjectURL === 'function';
        hadRevokeObjectURL = typeof URL.revokeObjectURL === 'function';
        originalCreateObjectURL = hadCreateObjectURL ? URL.createObjectURL : undefined;
        originalRevokeObjectURL = hadRevokeObjectURL ? URL.revokeObjectURL : undefined;

        createObjectURLSpy = jest.fn().mockReturnValue('blob://test');
        revokeObjectURLSpy = jest.fn();

        (URL as unknown as { createObjectURL: (obj: Blob | MediaSource) => string }).createObjectURL = createObjectURLSpy;
        (URL as unknown as { revokeObjectURL: (url: string) => void }).revokeObjectURL = revokeObjectURLSpy;
        createElementSpy = jest.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
            if (tagName === 'a') {
                const anchor = realCreateElement(tagName) as HTMLAnchorElement;
                anchor.click = clickSpy;
                anchors.push(anchor);
                return anchor;
            }
            return realCreateElement(tagName);
        });
    });

    afterEach(() => {
        if (createElementSpy) {
            createElementSpy.mockRestore();
        }

        if (hadCreateObjectURL && originalCreateObjectURL) {
            (URL as unknown as { createObjectURL: (obj: Blob | MediaSource) => string }).createObjectURL = originalCreateObjectURL;
        } else {
            delete (URL as unknown as { createObjectURL?: (obj: Blob | MediaSource) => string }).createObjectURL;
        }

        if (hadRevokeObjectURL && originalRevokeObjectURL) {
            (URL as unknown as { revokeObjectURL: (url: string) => void }).revokeObjectURL = originalRevokeObjectURL;
        } else {
            delete (URL as unknown as { revokeObjectURL?: (url: string) => void }).revokeObjectURL;
        }

        jest.clearAllMocks();
    });

    it('downloads XML when saveRequested emits with filename', async () => {
        const xml = '<?xml version="1.0" encoding="UTF-8"?>\n<config></config>';
        const markCleanSpy = jest.spyOn(visualDesigner, 'markClean');
        markCleanSpy.mockClear();

        (visualDesigner as any).emit('saveRequested', {
            filename: 'workflow.xml',
            xml,
            nodes: [],
            connections: [],
        });

        await flushPromises();

        expect(createObjectURLSpy).toHaveBeenCalledTimes(1);
        expect(clickSpy).toHaveBeenCalledTimes(1);
        expect(anchors[0].download).toBe('workflow.xml');
        expect(markCleanSpy).toHaveBeenCalled();
    });

    it('prompts for filename and delegates to saveConfigurationAs when missing name', async () => {
        const xml = '<?xml version="1.0" encoding="UTF-8"?>\n<config></config>';
        const saveAsSpy = jest.spyOn(visualDesigner, 'saveConfigurationAs');
        const markCleanSpy = jest.spyOn(visualDesigner, 'markClean');
        markCleanSpy.mockClear();
        saveAsSpy.mockClear();

        const promptSpy = jest.spyOn(window, 'prompt').mockReturnValue('generated.xml');

        (visualDesigner as any).emit('saveRequested', {
            filename: '',
            xml,
            nodes: [],
            connections: [],
        });

        await flushPromises();

        expect(promptSpy).toHaveBeenCalled();
        expect(saveAsSpy).toHaveBeenCalledWith('generated.xml');
        expect(createObjectURLSpy).toHaveBeenCalled();
        expect(clickSpy).toHaveBeenCalled();
        expect(anchors[anchors.length - 1].download).toBe('generated.xml');
        expect(markCleanSpy).toHaveBeenCalled();

        promptSpy.mockRestore();
    });

    it('saveFile delegates to VisualDesigner.requestSave in visual mode', async () => {
        const requestSaveMock = jest.fn().mockResolvedValue(true);
        (visualDesigner as any).requestSave = requestSaveMock;

        (app as any).currentMode = 'visual';

        await app.saveFile();
        await flushPromises();

        expect(requestSaveMock).toHaveBeenCalled();
    });
});

