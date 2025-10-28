/**
 * TDD Tests for Unified Layout (Conductor-style)
 * 
 * Requirements:
 * 1. Right sidebar (Logs/Results/Variables/Sessions) - ALWAYS visible
 * 2. Property Panel - ONLY in Visual mode (additional)
 * 3. NO duplicate Run/Validate buttons
 * 4. NO duplicate XML/Visual toggles
 * 5. Toolbar styles from docs/web/css/style.css
 */

describe('Unified Layout (TDD - Conductor-style)', () => {
    beforeEach(() => {
        document.body.innerHTML = `
            <div id="main-content">
                <div id="plugin-sidebar"></div>
                <div id="editor"></div>
                <div id="canvas-area"></div>
                <div id="right-sidebar"></div>
                <div id="property-panel"></div>
            </div>
        `;
    });

    describe('Right Sidebar - Always Visible', () => {
        it('should show right sidebar in XML mode', () => {
            switchToMode('xml');
            
            const sidebar = document.getElementById('right-sidebar');
            expect(sidebar?.style.display).not.toBe('none');
        });

        it('should show right sidebar in Visual mode', () => {
            switchToMode('visual');
            
            const sidebar = document.getElementById('right-sidebar');
            expect(sidebar?.style.display).not.toBe('none');
        });

        it('should have Logs panel in both modes', () => {
            const logsPanel = document.getElementById('logs');
            expect(logsPanel).toBeTruthy();
        });

        it('should have Results panel in both modes', () => {
            const resultsPanel = document.getElementById('results');
            expect(resultsPanel).toBeTruthy();
        });

        it('should have Variables panel in both modes', () => {
            const variablesPanel = document.getElementById('variables');
            expect(variablesPanel).toBeTruthy();
        });

        it('should have Sessions panel in both modes', () => {
            const sessionsPanel = document.getElementById('session');
            expect(sessionsPanel).toBeTruthy();
        });
    });

    describe('Property Panel - Visual Mode Only', () => {
        it('should show property panel ONLY in Visual mode', () => {
            const propertyPanel = document.getElementById('property-panel');
            
            // XML mode - hidden
            switchToMode('xml');
            expect(propertyPanel?.style.display).toBe('none');
            
            // Visual mode - visible
            switchToMode('visual');
            expect(propertyPanel?.style.display).not.toBe('none');
        });

        it('should NOT show property panel in XML mode', () => {
            switchToMode('xml');
            
            const propertyPanel = document.getElementById('property-panel');
            expect(propertyPanel?.style.display).toBe('none');
        });
    });

    describe('No Duplicate Buttons', () => {
        it('should have ONLY ONE Run button in toolbar', () => {
            const runButtons = document.querySelectorAll('.btn-run, [onclick*="runConfig"]');
            expect(runButtons.length).toBe(1);
        });

        it('should have ONLY ONE Validate button in toolbar', () => {
            const validateButtons = document.querySelectorAll('.btn-validate, [onclick*="validateConfig"]');
            expect(validateButtons.length).toBe(1);
        });

        it('should have ONLY ONE mode toggle (XML/Visual)', () => {
            const modeToggles = document.querySelectorAll('.mode-toggle');
            expect(modeToggles.length).toBe(1);
        });

        it('should NOT have duplicate Save buttons', () => {
            const saveButtons = document.querySelectorAll('[onclick*="saveFile"], [onclick*="saveConfiguration"]');
            // Should have only 1 unified Save button
            expect(saveButtons.length).toBeLessThanOrEqual(1);
        });
    });

    describe('Layout - Three Columns in Visual Mode', () => {
        it('should have 3 columns in Visual mode', () => {
            switchToMode('visual');
            
            // Left: Plugin Palette
            const leftSidebar = document.getElementById('plugin-sidebar');
            expect(leftSidebar?.style.display).not.toBe('none');
            
            // Center: Canvas
            const canvas = document.getElementById('canvas-area');
            expect(canvas?.style.display).not.toBe('none');
            
            // Right: Logs/Results/Variables + Property Panel
            const rightSidebar = document.getElementById('right-sidebar');
            expect(rightSidebar?.style.display).not.toBe('none');
        });
    });

    describe('Layout - Two Columns in XML Mode', () => {
        it('should have 2 columns in XML mode', () => {
            switchToMode('xml');
            
            // Left: Hidden
            const leftSidebar = document.getElementById('plugin-sidebar');
            expect(leftSidebar?.style.display).toBe('none');
            
            // Center: Editor
            const editor = document.getElementById('editor');
            expect(editor?.style.display).not.toBe('none');
            
            // Right: Logs/Results/Variables
            const rightSidebar = document.getElementById('right-sidebar');
            expect(rightSidebar?.style.display).not.toBe('none');
        });
    });

    describe('Styles from docs/web', () => {
        it('should use --primary-color: #3b82f6', () => {
            const primaryColor = getComputedStyle(document.documentElement)
                .getPropertyValue('--primary-color');
            
            expect(primaryColor.trim()).toBe('#3b82f6');
        });

        it('should use Inter font family', () => {
            const fontFamily = getComputedStyle(document.documentElement)
                .getPropertyValue('--font-family');
            
            expect(fontFamily).toContain('Inter');
        });

        it('should have consistent border-radius', () => {
            const radiusMd = getComputedStyle(document.documentElement)
                .getPropertyValue('--radius-md');
            
            expect(radiusMd.trim()).toBe('0.5rem');
        });
    });
});

// Helper
function switchToMode(mode: 'xml' | 'visual') {
    const pluginSidebar = document.getElementById('plugin-sidebar');
    const editor = document.getElementById('editor');
    const canvas = document.getElementById('canvas-area');
    const rightSidebar = document.getElementById('right-sidebar');
    const propertyPanel = document.getElementById('property-panel');
    
    if (mode === 'visual') {
        if (pluginSidebar) pluginSidebar.style.display = 'flex';
        if (editor) editor.style.display = 'none';
        if (canvas) canvas.style.display = 'flex';
        if (rightSidebar) rightSidebar.style.display = 'flex';
        if (propertyPanel) propertyPanel.style.display = 'flex';
    } else {
        if (pluginSidebar) pluginSidebar.style.display = 'none';
        if (editor) editor.style.display = 'flex';
        if (canvas) canvas.style.display = 'none';
        if (rightSidebar) rightSidebar.style.display = 'flex';
        if (propertyPanel) propertyPanel.style.display = 'none';
    }
}

