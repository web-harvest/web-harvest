/**
 * WebHarvest IDE - Main Entry Point (TypeScript + TDD)
 */

import { App } from '@core/App';
import '@/styles/main.css';

// Display runtime version from backend (pom) if available
fetch('/static/version.json')
    .then(res => res.ok ? res.json() : Promise.reject(new Error(`HTTP ${res.status}`)))
    .then(info => {
        if (info && info.version) {
            const build = info.buildDate ? ` (build: ${info.buildDate})` : '';
            console.log(`🚀 WebHarvest IDE v${info.version} - TypeScript Edition${build}`);
        } else {
            console.log('🚀 WebHarvest IDE - TypeScript Edition');
        }
    })
    .catch(() => {
        console.log('🚀 WebHarvest IDE - TypeScript Edition');
    });

// Initialize application
let app: App;

document.addEventListener('DOMContentLoaded', () => {
    try {
        app = new App();

        // Expose ALL methods to window for HTML onclick handlers
        const appInstance = app; // Capture reference
        (window as any).app = {
            // View mode (Conductor-style)
            switchMode: (mode: 'xml' | 'visual') => app.switchMode(mode),
            toggleVisualMode: () => app.toggleVisualMode(), // Legacy

            // Configuration management
            newConfiguration: () => app.newConfiguration(),

            // Execution
            runConfig: () => app.runConfig(),
            validateConfig: () => app.validateConfig(),
            pauseExecution: () => app.pauseExecution(),
            resumeExecution: () => app.resumeExecution(),
            stopExecution: () => app.stopExecution(),

            // Files
            openFile: () => app.openFile(),
            saveFile: () => app.saveFile(),

            // Visual Mode - Single Configuration
            saveVisualConfiguration: () => app.saveVisualConfiguration(),
            newVisualConfiguration: () => app.newVisualConfiguration(),
            openFileInVisual: () => app.openFileInVisual(),

            // Examples
            showExamplesModal: () => app.showExamplesModal(),
            loadExampleDirect: (event: Event) => app.loadExampleDirect(event),

            // Tabs (XML mode only)
            addTab: () => app.addTab(),
            closeTab: (tabId: string, event?: Event) => {
                if (event) {
                    event.stopPropagation();
                }
                app.closeTab(tabId);
            },
            switchToTab: (tabId: string) => app.switchToTab(tabId),

            // Panels
            switchSidebarPanel: (panel: string) => app.switchSidebarPanel(panel),
            clearLogs: () => app.clearLogs(),
            exportResults: () => app.exportResults(),

            // UI
            hideTooltip: () => app.hideTooltip(),
            hideExamplesModal: () => app.hideExamplesModal(),

            // Visual: Zoom & Layout & Grid
            zoomIn: () => {
                console.log('[main.ts] zoomIn called');
                appInstance.zoomIn();
            },
            zoomOut: () => {
                console.log('[main.ts] zoomOut called');
                appInstance.zoomOut();
            },
            zoomReset: () => {
                console.log('[main.ts] zoomReset called');
                appInstance.zoomReset();
            },
            zoomToFit: () => {
                console.log('[main.ts] zoomToFit called');
                appInstance.zoomToFit();
            },
            autoLayout: (type: 'grid' | 'vertical' | 'horizontal') => {
                console.log(`[main.ts] autoLayout(${type}) called`);
                appInstance.autoLayout(type);
            },
            toggleMinimap: () => {
                console.log('[main.ts] toggleMinimap called');
                appInstance.toggleMinimap();
            },
            togglePalette: () => {
                console.log('[main.ts] togglePalette called');
                appInstance.togglePalette();
            },
            toggleSidebar: () => {
                console.log('[main.ts] toggleSidebar called');
                appInstance.toggleSidebar();
            },

            // For debugging
            _instance: appInstance
        };

        console.log('✅ Application ready!');
    } catch (error) {
        console.error('❌ Failed to initialize application:', error);

        // Show error message
        const appDiv = document.getElementById('app');
        if (appDiv) {
            appDiv.innerHTML = `
        <div style="display: flex; align-items: center; justify-content: center; height: 100vh; font-family: Inter, sans-serif;">
          <div style="text-align: center; max-width: 600px; padding: 24px;">
            <h1 style="font-size: 32px; margin-bottom: 16px; color: #ef4444;">⚠️ Initialization Error</h1>
            <p style="color: #666; font-size: 16px; margin-bottom: 16px;">Failed to start WebHarvest IDE</p>
            <pre style="text-align: left; background: #f5f5f5; padding: 16px; border-radius: 8px; overflow: auto; font-size: 12px;">${error}</pre>
          </div>
        </div>
      `;
        }
    }
});

