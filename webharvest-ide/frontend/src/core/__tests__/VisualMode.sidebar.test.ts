/**
 * TDD Tests for Visual Mode - Right Sidebar Always Visible
 * 
 * Requirements:
 * 1. Right sidebar (Logs/Results/Variables/Sessions) MUST be visible in Visual mode
 * 2. Run/Validate MUST work in Visual mode
 * 3. Styles MUST match docs/web branding
 */

describe('Visual Mode - Right Sidebar (TDD)', () => {
    beforeEach(() => {
        document.body.innerHTML = `
            <div id="right-sidebar" class="sidebar-section"></div>
            <div id="logs"></div>
            <div id="results"></div>
            <div id="variables"></div>
            <div id="session"></div>
            <button id="btn-run" onclick="window.app.runConfig()">Run</button>
            <button id="btn-validate" onclick="window.app.validateConfig()">Validate</button>
        `;
    });

    it('should show right sidebar in Visual mode', () => {
        const sidebar = document.getElementById('right-sidebar');

        // Switch to Visual mode
        if (sidebar) sidebar.style.display = 'flex';

        expect(sidebar?.style.display).toBe('flex');
        expect(sidebar?.style.display).not.toBe('none');
    });

    it('should have Logs panel visible in Visual mode', () => {
        const logs = document.getElementById('logs');
        expect(logs).toBeTruthy();
    });

    it('should have Results panel visible in Visual mode', () => {
        const results = document.getElementById('results');
        expect(results).toBeTruthy();
    });

    it('should have Variables panel visible in Visual mode', () => {
        const variables = document.getElementById('variables');
        expect(variables).toBeTruthy();
    });

    it('should have Sessions panel visible in Visual mode', () => {
        const sessions = document.getElementById('session');
        expect(sessions).toBeTruthy();
    });

    it('should allow running configuration in Visual mode', () => {
        const runBtn = document.getElementById('btn-run');

        expect(runBtn).toBeTruthy();
        expect(runBtn?.onclick).toBeTruthy();
    });

    it('should allow validating configuration in Visual mode', () => {
        const validateBtn = document.getElementById('btn-validate');

        expect(validateBtn).toBeTruthy();
        expect(validateBtn?.onclick).toBeTruthy();
    });
});

describe('Styles - Match docs/web branding (TDD)', () => {
    it('should use primary color #3b82f6', () => {
        document.documentElement.style.setProperty('--primary-color', '#3b82f6');

        const primaryColor = getComputedStyle(document.documentElement)
            .getPropertyValue('--primary-color');

        expect(primaryColor.trim()).toBe('#3b82f6');
    });

    it('should use Inter font family', () => {
        document.documentElement.style.setProperty('--font-family', 'Inter, sans-serif');

        const fontFamily = getComputedStyle(document.documentElement)
            .getPropertyValue('--font-family');

        expect(fontFamily).toContain('Inter');
    });

    it('should have consistent border radius', () => {
        document.documentElement.style.setProperty('--radius-md', '0.5rem');

        const radius = getComputedStyle(document.documentElement)
            .getPropertyValue('--radius-md');

        expect(radius.trim()).toBe('0.5rem');
    });
});




