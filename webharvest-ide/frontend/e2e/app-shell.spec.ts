import { expect, test } from '@playwright/test';

test.beforeEach(async ({ page }) => {
    // The IDE's startup contract includes Monaco, but the browser-shell test
    // does not need to download or execute the third-party Monaco distribution.
    // Keep the public Monaco API used by MonacoEditor.initialize() deterministic.
    await page.addInitScript(() => {
        let value = '';
        const editor = {
            getValue: () => value,
            setValue: (nextValue: string) => {
                value = nextValue;
            },
            onDidChangeModelContent: () => undefined,
            layout: () => undefined,
            dispose: () => undefined,
            updateOptions: () => undefined,
        };

        Object.defineProperty(window, 'monaco', {
            configurable: true,
            value: {
                editor: {
                    create: () => editor,
                },
            },
        });

        // VisualSemanticModelManager opens an optional backend WebSocket during
        // startup. Keep this shell test fully offline and avoid Vite proxy retries.
        class OfflineWebSocket {
            static readonly CONNECTING = 0;
            static readonly OPEN = 1;
            static readonly CLOSING = 2;
            static readonly CLOSED = 3;

            readonly url: string;
            readyState = OfflineWebSocket.CONNECTING;
            onopen: ((event: Event) => void) | null = null;
            onmessage: ((event: MessageEvent) => void) | null = null;
            onerror: ((event: Event) => void) | null = null;
            onclose: ((event: CloseEvent) => void) | null = null;

            constructor(url: string | URL) {
                this.url = String(url);
                queueMicrotask(() => {
                    this.readyState = OfflineWebSocket.OPEN;
                    this.onopen?.(new Event('open'));
                });
            }

            send(_data: string): void {}

            close(): void {
                this.readyState = OfflineWebSocket.CLOSED;
                this.onclose?.(new CloseEvent('close', { code: 1000, wasClean: true }));
            }
        }

        Object.defineProperty(window, 'WebSocket', {
            configurable: true,
            value: OfflineWebSocket,
        });
    });

    // Backend plugin metadata is an explicit fixture, not a DOM substitute.
    await page.route('**/api/plugins/metadata', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                schemaVersion: 'e2e-fixture-1',
                count: 0,
                plugins: [],
            }),
        })
    );
    await page.route('**/static/version.json', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ version: 'e2e', buildDate: 'fixture' }),
        })
    );

    // Font Awesome, Google Fonts and the external Monaco loader are not part
    // of this shell/bootstrap contract.
    await page.route('https://**/*', route => route.abort());
});

test('Vite boots the canonical runtime shell and supports a real toolbar interaction', async ({ page, request }) => {
    const pageErrors: string[] = [];
    page.on('pageerror', error => pageErrors.push(error.message));

    const initialized = page.waitForEvent('console', message =>
        message.type() === 'log' &&
        message.text().includes('WebHarvest IDE initialized successfully')
    );

    const response = await page.goto('/');
    expect(response, 'the application document should be served').not.toBeNull();
    expect(response!.ok()).toBeTruthy();
    await expect(page).toHaveTitle(/WebHarvest IDE/);
    await expect(page.locator('script[src="/src/main.ts"]')).toHaveCount(1);
    await expect(page.locator('link[href*="dist/assets/main.css"]')).toHaveCount(0);

    // The production shell is authoritative and intentionally has no #app mount.
    await expect(page.locator('#canvas-area')).toBeAttached();
    await expect(page.locator('#plugin-palette')).toBeAttached();
    await expect(page.locator('#rightSidebar')).toBeAttached();

    // Ensure the runtime's existing /static/** URL contract is served in Vite dev.
    const stylesheet = await request.get('/static/css/enterprise.css');
    expect(stylesheet.ok()).toBeTruthy();
    expect(stylesheet.headers()['content-type']).toContain('text/css');

    // This log is emitted only after App.initialize() completes its startup path.
    await initialized;
    await expect(page.locator('#visual-canvas')).toBeVisible();
    await expect(page.locator('#canvas-area')).toBeVisible();

    const canvasBounds = await page.locator('#canvas-area').boundingBox();
    expect(canvasBounds, 'the canvas should have a browser layout box').not.toBeNull();
    expect(canvasBounds!.width).toBeGreaterThan(0);
    expect(canvasBounds!.height).toBeGreaterThan(0);

    // The runtime starts with the plugin palette visible. Exercise both
    // directions of the existing toolbar toggle and observe the user-visible state.
    const pluginSidebar = page.locator('#plugin-sidebar');
    await expect(pluginSidebar).toBeVisible();
    await page.locator('#togglePaletteBtn').click();
    await expect(pluginSidebar).toBeHidden();
    await page.locator('#togglePaletteBtn').click();
    await expect(pluginSidebar).toBeVisible();

    expect(pageErrors, 'the application should not throw uncaught browser errors').toEqual([]);
});

test('D3 renders a semantic connection path and matching arrow for a two-node workflow', async ({ page }) => {
    const initialized = page.waitForEvent('console', message =>
        message.type() === 'log' &&
        message.text().includes('WebHarvest IDE initialized successfully')
    );

    await page.goto('/');
    await initialized;

    // Use the application's existing public fixture-loading API. The node shape
    // is the shared VisualNode contract; no DOM or semantic-model internals are mocked.
    await page.evaluate(() => {
        const designer = (window as any).app?.visualDesigner;
        if (!designer) {
            throw new Error('The initialized application did not expose VisualDesigner');
        }

        designer.loadNodes([
            {
                id: 'e2e-log',
                type: 'log',
                attributes: { level: 'info' },
                body: null,
                children: [],
                position: { x: 100, y: 100 },
            },
            {
                id: 'e2e-sleep',
                type: 'sleep',
                attributes: { milliseconds: '10' },
                body: null,
                children: [],
                position: { x: 100, y: 360 },
            },
        ]);
    });

    await expect(page.locator('.visual-node[data-node-id="e2e-log"]')).toBeVisible();
    await expect(page.locator('.visual-node[data-node-id="e2e-sleep"]')).toBeVisible();

    const connectionPath = page.locator('svg path.connection-path').first();
    const connectionArrow = page.locator('svg polygon.connection-arrow').first();

    // Anchor registration and semantic-model notification are asynchronous;
    // wait for their observable browser output instead of using fixed sleeps.
    await expect(connectionPath).toBeAttached({ timeout: 15_000 });
    await expect(connectionArrow).toBeAttached({ timeout: 15_000 });
    await expect(connectionPath).toBeVisible();
    await expect(connectionArrow).toBeVisible();

    const pathData = await connectionPath.getAttribute('d');
    const pathConnectionId = await connectionPath.getAttribute('data-connection-id');
    const arrowConnectionId = await connectionArrow.getAttribute('data-connection-id');
    const arrowPoints = await connectionArrow.getAttribute('points');

    expect(pathData).toMatch(/^M/);
    expect(pathConnectionId).toBeTruthy();
    expect(arrowConnectionId).toBe(pathConnectionId);
    expect(arrowPoints).toBeTruthy();
});

test('selecting a canvas node opens its Properties panel', async ({ page }) => {
    const pageErrors: string[] = [];
    page.on('pageerror', error => pageErrors.push(error.message));

    const initialized = page.waitForEvent('console', message =>
        message.type() === 'log' &&
        message.text().includes('WebHarvest IDE initialized successfully')
    );

    await page.goto('/');
    await initialized;

    await page.evaluate(() => {
        const designer = (window as any).app?.visualDesigner;
        if (!designer) {
            throw new Error('The initialized application did not expose VisualDesigner');
        }

        designer.loadNodes([
            {
                id: 'e2e-log',
                type: 'log',
                attributes: { level: 'info' },
                body: null,
                children: [],
                position: { x: 100, y: 100 },
            },
            {
                id: 'e2e-sleep',
                type: 'sleep',
                attributes: { milliseconds: '10' },
                body: null,
                children: [],
                position: { x: 100, y: 360 },
            },
        ]);
    });

    const selectedNode = page.locator('.visual-node[data-node-id="e2e-log"]');
    await expect(selectedNode).toBeVisible();
    await selectedNode.locator('.visual-node__header-title').click();

    // Selection is a real DOM interaction; App handles the emitted nodeSelected event.
    await expect(selectedNode).toHaveClass(/node-selected/);
    await expect(page.locator('#rightSidebar')).toBeVisible();
    await expect(page.locator('#rightSidebar .right-sidebar__section--properties')).toBeVisible();

    const propertyForm = page.locator('#propertyPanel .property-panel__form');
    await expect(propertyForm).toBeVisible();
    await expect(propertyForm).toContainText('Type:');
    await expect(propertyForm).toContainText('log');

    expect(pageErrors, 'node selection and property rendering should not throw browser errors').toEqual([]);
});

test('dragging a canvas node beyond the movement threshold repositions it', async ({ page }) => {
    const initialized = page.waitForEvent('console', message =>
        message.type() === 'log' &&
        message.text().includes('WebHarvest IDE initialized successfully')
    );

    await page.goto('/');
    await initialized;

    await page.evaluate(() => {
        const designer = (window as any).app?.visualDesigner;
        if (!designer) {
            throw new Error('The initialized application did not expose VisualDesigner');
        }

        designer.loadNodes([
            {
                id: 'e2e-log',
                type: 'log',
                attributes: { level: 'info' },
                body: null,
                children: [],
                position: { x: 100, y: 100 },
            },
            {
                id: 'e2e-sleep',
                type: 'sleep',
                attributes: { milliseconds: '10' },
                body: null,
                children: [],
                position: { x: 100, y: 360 },
            },
        ]);
    });

    const node = page.locator('.visual-node[data-node-id="e2e-log"]');
    await expect(node).toBeVisible();

    const before = await node.boundingBox();
    expect(before, 'the node should have a measurable browser position').not.toBeNull();

    const startX = before!.x + before!.width / 2;
    const startY = before!.y + before!.height / 2;
    await page.mouse.move(startX, startY);
    await page.mouse.down();
    await page.mouse.move(startX + 50, startY + 40, { steps: 5 });
    await page.mouse.up();

    await expect.poll(async () => {
        const after = await node.boundingBox();
        if (!after) return 0;
        return Math.hypot(after.x - before!.x, after.y - before!.y);
    }).toBeGreaterThan(20);
});


test('PropertyPanel Cancel restores the original value and Apply persists edits to the visual model', async ({ page }) => {
    const pageErrors: string[] = [];
    page.on('pageerror', error => pageErrors.push(error.message));

    const initialized = page.waitForEvent('console', message =>
        message.type() === 'log' &&
        message.text().includes('WebHarvest IDE initialized successfully')
    );

    await page.goto('/');
    await initialized;

    await page.evaluate(() => {
        const designer = (window as any).app?.visualDesigner;
        if (!designer) {
            throw new Error('The initialized application did not expose VisualDesigner');
        }

        designer.loadNodes([
            {
                id: 'e2e-log',
                type: 'log',
                attributes: { level: 'info' },
                body: null,
                children: [],
                position: { x: 100, y: 100 },
            },
            {
                id: 'e2e-sleep',
                type: 'sleep',
                attributes: { milliseconds: '10' },
                body: null,
                children: [],
                position: { x: 100, y: 360 },
            },
        ]);
    });

    const logNode = page.locator('.visual-node[data-node-id="e2e-log"]');
    await expect(logNode).toBeVisible();
    await logNode.locator('.visual-node__header-title').click();

    const propertyForm = page.locator('#propertyPanel .property-panel__form');
    await expect(propertyForm).toBeVisible();
    await expect(propertyForm).toContainText('Type:');
    await expect(propertyForm).toContainText('log');

    const levelInput = propertyForm.locator('input.detail-input').first();
    await expect(levelInput).toHaveValue('info');

    const applyButton = page.locator('#propertyPanel .property-panel-shell__button--primary');
    const cancelButton = page.locator('#propertyPanel .property-panel-shell__button--ghost');

    // Cancel must discard the working copy and leave the actual VisualDesigner node untouched.
    await levelInput.fill('debug');
    await levelInput.press('Tab');
    await expect(applyButton).toBeEnabled();
    await cancelButton.click();

    await expect(propertyForm.locator('input.detail-input').first()).toHaveValue('info');
    const afterCancel = await page.evaluate(() =>
        (window as any).app.visualDesigner.findNode('e2e-log')?.attributes?.level
    );
    expect(afterCancel).toBe('info');
    await expect(applyButton).toBeDisabled();

    // Apply must flow through App.onChange → VisualDesigner.updateNode.
    const restoredLevelInput = propertyForm.locator('input.detail-input').first();
    await restoredLevelInput.fill('warn');
    await restoredLevelInput.press('Tab');
    await expect(applyButton).toBeEnabled();
    await applyButton.click();

    await expect.poll(() => page.evaluate(() =>
        (window as any).app.visualDesigner.findNode('e2e-log')?.attributes?.level
    )).toBe('warn');
    await expect(applyButton).toBeDisabled();

    // App's documented sync path updates both the active tab and Monaco value.
    await expect.poll(() => page.evaluate(() =>
        (window as any).app._instance.getEditor().getValue()
    )).toContain('<log level="warn"/>');
    const activeTabXml = await page.evaluate(() =>
        (window as any).app._instance.getTabManager().getActiveTab()?.content
    );
    expect(activeTabXml).toContain('<log level="warn"/>');

    expect(pageErrors, 'PropertyPanel edit lifecycle should not throw browser errors').toEqual([]);
});
