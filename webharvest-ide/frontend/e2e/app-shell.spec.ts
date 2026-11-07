import { expect, test } from '@playwright/test';

test('Vite serves the WebHarvest IDE application shell', async ({ page }) => {
    const response = await page.goto('/');

    expect(response, 'the application document should be served').not.toBeNull();
    expect(response!.ok()).toBeTruthy();
    await expect(page).toHaveTitle(/WebHarvest IDE/);
    await expect(page.locator('#app')).toBeAttached();
});
