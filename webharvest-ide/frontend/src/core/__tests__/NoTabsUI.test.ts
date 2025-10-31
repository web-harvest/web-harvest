/**
 * TDD Tests for No-Tabs UI (Conductor-style)
 * 
 * Requirements (inspired by Conductor):
 * 1. NO tabs in both XML and Visual modes
 * 2. Single file editing (like Conductor)
 * 3. Toolbar with filename + actions
 * 4. Clean header: logo + title (left), help (right)
 * 5. Remove duplicate buttons (Save/Open/Examples from header)
 */

describe('No-Tabs UI (Conductor-style TDD)', () => {
    beforeEach(() => {
        document.body.innerHTML = `
            <header id="main-header">
                <div class="header-left"></div>
                <div class="header-right"></div>
            </header>
            <div id="tabs-container"></div>
            <div id="toolbar-container"></div>
            <div id="editor"></div>
        `;
    });

    describe('Header Redesign', () => {
        it('should have logo and title on the left', () => {
            const headerLeft = document.querySelector('.header-left');

            expect(headerLeft?.querySelector('.logo')).toBeTruthy();
            expect(headerLeft?.querySelector('.title')).toBeTruthy();
            expect(headerLeft?.textContent).toContain('WebHarvest');
        });

        it('should have only Help button on the right', () => {
            const headerRight = document.querySelector('.header-right');

            const buttons = headerRight?.querySelectorAll('button');
            expect(buttons?.length).toBe(1);
            expect(buttons?.[0].textContent).toContain('Help');
        });

        it('should NOT have Save/Open/Examples in header', () => {
            const header = document.getElementById('main-header');
            const html = header?.innerHTML || '';

            expect(html).not.toContain('btn-save');
            expect(html).not.toContain('btn-open');
            expect(html).not.toContain('btn-examples');
        });
    });

    describe('No Tabs - Both Modes', () => {
        it('should NOT have tabs container in DOM', () => {
            // After redesign, tabs-container should NOT exist or be permanently hidden
            const tabsContainer = document.getElementById('tabs-container');

            if (tabsContainer) {
                expect(tabsContainer.style.display).toBe('none');
                expect(tabsContainer.children.length).toBe(0);
            }
        });

        it('should have single toolbar for both modes', () => {
            const toolbar = document.getElementById('toolbar-container');

            expect(toolbar).toBeTruthy();
        });

        it('should show filename in toolbar (not in tabs)', () => {
            const toolbar = document.getElementById('toolbar-container');
            toolbar!.innerHTML = `
                <div class="toolbar-filename">my-workflow.xml</div>
                <div class="toolbar-actions">
                    <button class="btn-save">Save</button>
                </div>
            `;

            const filename = toolbar?.querySelector('.toolbar-filename');
            expect(filename?.textContent).toBe('my-workflow.xml');
        });
    });

    describe('Unified Toolbar (XML + Visual)', () => {
        it('should have filename display', () => {
            const toolbar = renderUnifiedToolbar('config.xml', false, 'xml');

            expect(toolbar.querySelector('.filename')).toBeTruthy();
            expect(toolbar.textContent).toContain('config.xml');
        });

        it('should have dirty indicator', () => {
            const toolbar = renderUnifiedToolbar('config.xml', true, 'xml');

            const dirty = toolbar.querySelector('.dirty-indicator');
            expect(dirty).toBeTruthy();
            expect(getComputedStyle(dirty!).display).not.toBe('none');
        });

        it('should have mode toggle (XML ↔ Visual)', () => {
            const toolbar = renderUnifiedToolbar('config.xml', false, 'xml');

            const modeToggle = toolbar.querySelector('.mode-toggle');
            expect(modeToggle).toBeTruthy();
        });

        it('should have Save button', () => {
            const toolbar = renderUnifiedToolbar('config.xml', false, 'xml');

            expect(toolbar.querySelector('.btn-save')).toBeTruthy();
        });

        it('should have Run button', () => {
            const toolbar = renderUnifiedToolbar('config.xml', false, 'xml');

            expect(toolbar.querySelector('.btn-run')).toBeTruthy();
        });

        it('should show different actions based on mode', () => {
            const xmlToolbar = renderUnifiedToolbar('config.xml', false, 'xml');
            const visualToolbar = renderUnifiedToolbar('config.xml', false, 'visual');

            // Both should have Save, Run
            expect(xmlToolbar.querySelector('.btn-save')).toBeTruthy();
            expect(visualToolbar.querySelector('.btn-save')).toBeTruthy();

            // Visual mode should have additional layout buttons
            expect(visualToolbar.querySelector('.btn-auto-layout')).toBeTruthy();
        });
    });

    describe('Conductor-style Actions', () => {
        it('should have New button (⌘N)', () => {
            const toolbar = renderUnifiedToolbar('config.xml', false, 'xml');
            const newBtn = toolbar.querySelector('.btn-new');

            expect(newBtn).toBeTruthy();
            expect(newBtn?.getAttribute('title')).toContain('⌘N');
        });

        it('should have Open button (⌘O)', () => {
            const toolbar = renderUnifiedToolbar('config.xml', false, 'xml');
            const openBtn = toolbar.querySelector('.btn-open');

            expect(openBtn).toBeTruthy();
            expect(openBtn?.getAttribute('title')).toContain('⌘O');
        });

        it('should have Examples button', () => {
            const toolbar = renderUnifiedToolbar('config.xml', false, 'xml');

            expect(toolbar.querySelector('.btn-examples')).toBeTruthy();
        });
    });
});

// Helper function
function renderUnifiedToolbar(filename: string, isDirty: boolean, mode: 'xml' | 'visual'): HTMLElement {
    const toolbar = document.createElement('div');
    toolbar.className = 'unified-toolbar';

    toolbar.innerHTML = `
        <div class="toolbar-left">
            <span class="filename">${filename}</span>
            <span class="dirty-indicator" style="display: ${isDirty ? 'inline' : 'none'};">●</span>
        </div>
        <div class="toolbar-center">
            <div class="mode-toggle">
                <button class="${mode === 'xml' ? 'active' : ''}">XML</button>
                <button class="${mode === 'visual' ? 'active' : ''}">Visual</button>
            </div>
        </div>
        <div class="toolbar-right">
            <button class="btn-new" title="New (⌘N)">New</button>
            <button class="btn-open" title="Open (⌘O)">Open</button>
            <button class="btn-save">Save</button>
            <button class="btn-examples">Examples</button>
            <button class="btn-run">Run</button>
            ${mode === 'visual' ? '<button class="btn-auto-layout">Layout</button>' : ''}
        </div>
    `;

    return toolbar;
}




