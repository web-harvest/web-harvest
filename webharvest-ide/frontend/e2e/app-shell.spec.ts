import { expect, test } from '@playwright/test';

test.beforeEach(async ({ page }) => {
    // The first browser harness is deliberately backend-independent. The app
    // requests plugin metadata during startup; provide an empty, valid catalog.
    await page.route('**/api/plugins/metadata', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ schemaVersion: 'e2e-fixture-1', plugins: [] }),
        })
    );
    await page.route('**/static/version.json', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ version: 'e2e', buildDate: 'fixture' }),
        })
    );

    // Font Awesome and Google Fonts are presentation dependencies, not part
    // of the application-shell smoke contract.
    await page.route('https://**/*', route => route.abort());
});

test('Vite serves the WebHarvest IDE application shell', async ({ page }) => {
    const response = await page.goto('/');

    expect(response, 'the application document should be served').not.toBeNull();
    expect(response!.ok()).toBeTruthy();
    await expect(page).toHaveTitle(/WebHarvest IDE/);
    await expect(page.locator('#app')).toBeAttached();
});
