/**
 * WebHarvest IDE - Main Entry Point (TypeScript + TDD)
 */

import { App } from '@core/App';
import '@/styles/main.css';

console.log('🚀 WebHarvest IDE v2.2.0 - TypeScript Edition');

// Initialize application
let app: App;

document.addEventListener('DOMContentLoaded', () => {
    try {
        app = new App();

        // Expose ALL methods to window for HTML onclick handlers
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

            // Session
            loadSessionHistory: () => app.loadSessionHistory(),

            // UI
            hideTooltip: () => app.hideTooltip(),
            hideExamplesModal: () => app.hideExamplesModal(),

            // For debugging
            _instance: app
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

