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
