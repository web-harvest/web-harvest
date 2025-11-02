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

describe('App - open visual configuration', () => {
    let app: App;
    let visualDesigner: any;
    let logsSpy: jest.SpyInstance;
    let updateToolbarSpy: jest.SpyInstance;
    let loadNodesSpy: jest.SpyInstance;
    let setFilenameSpy: jest.SpyInstance;
    let markCleanSpy: jest.SpyInstance;
    let parseSpy: jest.SpyInstance;
    let createElementSpy: jest.SpyInstance;
    let fileReaderSpy: jest.SpyInstance;

    let changeHandler: ((event: Event & { target: HTMLInputElement }) => void) | null;
    let mockInput: any;
    let fileReaderMock: any;

    beforeEach(async () => {
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

        changeHandler = null;
        const realCreateElement = document.createElement.bind(document);
        createElementSpy = jest.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
            if (tagName.toLowerCase() === 'input') {
                const input = realCreateElement(tagName) as HTMLInputElement;
                const originalAddEventListener = input.addEventListener.bind(input);
                input.addEventListener = jest.fn((event: string, handler: any, options?: any) => {
                    if (event === 'change') {
                        changeHandler = handler;
                    }
                    return originalAddEventListener(event, handler, options as any);
                });
                input.click = jest.fn();
                mockInput = input;
                return input;
            }
            return realCreateElement(tagName);
        });

        fileReaderMock = {
            onload: null as ((event: ProgressEvent<FileReader>) => void) | null,
            onerror: null as ((event: ProgressEvent<FileReader>) => void) | null,
            readAsText: jest.fn(function (this: typeof fileReaderMock, _file: File) {
                // no-op, will trigger manually in tests
            }),
        };

        fileReaderSpy = jest.spyOn(window as any, 'FileReader').mockImplementation(() => fileReaderMock);

        app = new App();
        await flushPromises();
        visualDesigner = (app as any).visualDesigner;

        logsSpy = jest.spyOn((app as any).logsPanel, 'addLog').mockImplementation(() => { });
        updateToolbarSpy = jest.spyOn(app, 'updateUnifiedToolbar').mockImplementation(() => { });
        loadNodesSpy = jest.spyOn(visualDesigner, 'loadNodesWithPrompt').mockResolvedValue(true);
        setFilenameSpy = jest.spyOn(visualDesigner, 'setCurrentFilename').mockImplementation(() => { });
        markCleanSpy = jest.spyOn(visualDesigner, 'markClean').mockImplementation(() => { });

        parseSpy = jest.spyOn((app as any).xmlParser, 'parse').mockReturnValue([
            {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
            },
        ]);
    });

    afterEach(() => {
        createElementSpy.mockRestore();
        fileReaderSpy.mockRestore();
        if (logsSpy) logsSpy.mockRestore();
        if (updateToolbarSpy) updateToolbarSpy.mockRestore();
        if (loadNodesSpy) loadNodesSpy.mockRestore();
        if (setFilenameSpy) setFilenameSpy.mockRestore();
        if (markCleanSpy) markCleanSpy.mockRestore();
        if (parseSpy) parseSpy.mockRestore();
        jest.clearAllMocks();
    });

    it('loads XML file into visual designer', async () => {
        const fileContent = '<config xmlns="http://org.webharvest/schema/2.1/core"></config>';
        const sampleFile = new File([fileContent], 'workflow.xml', { type: 'text/xml' });

        (fileReaderMock.readAsText as jest.Mock).mockImplementation(function () {
            if (fileReaderMock.onload) {
                fileReaderMock.onload({ target: { result: fileContent } } as any);
            }
        });

        await app.openFileInVisual();

        expect((mockInput as HTMLInputElement).click).toHaveBeenCalled();
        expect(mockInput.accept).toContain('.xml');

        expect(changeHandler).toBeTruthy();
        if (!changeHandler) {
            throw new Error('Change handler was not registered');
        }

        const event = {
            target: { files: [sampleFile] },
        } as unknown as Event & { target: HTMLInputElement };

        await changeHandler(event);
        await flushPromises();

        expect(parseSpy).toHaveBeenCalledWith(fileContent);
        expect(loadNodesSpy).toHaveBeenCalled();
        expect(setFilenameSpy).toHaveBeenCalledWith('workflow.xml');
        expect(markCleanSpy).toHaveBeenCalled();
        expect(updateToolbarSpy).toHaveBeenCalled();
        expect(logsSpy).toHaveBeenCalledWith('success', expect.stringContaining('workflow.xml'));
    });
});


