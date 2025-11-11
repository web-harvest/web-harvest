/**
 * UIManager - Manages ALL UI rendering and interactions
 * Apple HIG compliant
 */

import type { Tab, Plugin } from '../types';

export class UIManager {
    /**
     * Show/hide Monaco editor
     */
    showEditor(show: boolean): void {
        const editor = document.getElementById('editor');
        if (editor) {
            editor.style.display = show ? 'block' : 'none';
        }
    }

    /**
     * Show/hide Visual canvas
     */
    showVisualCanvas(show: boolean): void {
        const canvas = document.getElementById('visual-canvas');
        if (canvas) {
            canvas.style.display = show ? 'flex' : 'none';
        }
    }

    /**
     * Show/hide Plugin sidebar
     */
    showPluginSidebar(show: boolean): void {
        const sidebar = document.getElementById('plugin-sidebar');
        if (sidebar) {
            sidebar.style.display = show ? 'flex' : 'none';
        }
    }

    /**
     * Show/hide Right sidebar
     */
    showRightSidebar(show: boolean): void {
        const sidebar = document.querySelector('.sidebar-section') as HTMLElement;
        if (sidebar) {
            sidebar.style.display = show ? 'flex' : 'none';
        }
    }

    /**
     * Show/hide Property panel tab + content in sidebar
     */
    showPropertyPanel(show: boolean): void {
        const tab = document.getElementById('propertiesTabBtn');
        const panel = document.getElementById('propertyPanel');

        if (tab && panel) {
            tab.style.display = show ? 'flex' : 'none';

            if (show) {
                // Show Properties tab and make it active (FIRST tab in Visual mode)
                tab.setAttribute('aria-hidden', 'false');

                // Auto-activate Properties tab when entering Visual mode
                const allTabs = document.querySelectorAll('.sidebar-nav-btn');
                const allPanels = document.querySelectorAll('.sidebar-panel');

                allTabs.forEach(t => t.classList.remove('active'));
                allPanels.forEach(p => p.classList.remove('active'));

                tab.classList.add('active');
                panel.classList.add('active');
                panel.style.display = 'block';
            } else {
                // Hide Properties tab in XML mode
                tab.setAttribute('aria-hidden', 'true');
                tab.classList.remove('active');
                panel.classList.remove('active');
                panel.style.display = 'none';

                // Switch to Logs tab when hiding Properties
                const logsTab = document.querySelector('[data-panel="logs"]') as HTMLElement;
                const logsPanel = document.getElementById('logsPanel');

                if (logsTab && logsPanel) {
                    logsTab.classList.add('active');
                    logsPanel.classList.add('active');
                }
            }
        }
    }

    /**
     * Show/hide execution controls (Run/Validate buttons)
     */
    showExecutionControls(show: boolean): void {
        const controls = document.getElementById('executionControls');
        if (controls) {
            controls.style.display = show ? 'flex' : 'none';
        }
    }

    /**
     * Show/hide execution action buttons (Pause/Resume/Stop during execution)
     */
    showExecutionActionButtons(show: boolean): void {
        const actionButtons = document.getElementById('executionActionButtons');
        if (actionButtons) {
            actionButtons.style.display = show ? 'flex' : 'none';
            // Also ensure parent executionControls is visible in Visual mode
            if (show) {
                const controls = document.getElementById('executionControls');
                if (controls) {
                    controls.style.display = 'flex';
                }
            }
        }
    }

    /**
     * Update execution control buttons to reflect workflow state
     */
    setExecutionState(state: 'idle' | 'running' | 'paused'): void {
        const runBtn = document.getElementById('canvasRunBtn') as HTMLButtonElement | null;
        const validateBtn = document.getElementById('canvasValidateBtn') as HTMLButtonElement | null;
        const pauseBtn = document.getElementById('pauseBtn') as HTMLButtonElement | null;
        const resumeBtn = document.getElementById('resumeBtn') as HTMLButtonElement | null;

        const isRunning = state === 'running' || state === 'paused';
        const isPaused = state === 'paused';

        if (runBtn) {
            runBtn.disabled = isRunning;
            runBtn.style.opacity = isRunning ? '0.5' : '1';
            runBtn.style.cursor = isRunning ? 'not-allowed' : 'pointer';
            runBtn.title = isRunning ? 'Execution in progress…' : 'Run Configuration (⌘R)';
        }

        if (validateBtn) {
            validateBtn.disabled = isRunning;
            validateBtn.style.opacity = isRunning ? '0.5' : '1';
            validateBtn.style.cursor = isRunning ? 'not-allowed' : 'pointer';
            validateBtn.title = isRunning ? 'Validation disabled during execution' : 'Validate Configuration (⌘K)';
        }

        if (pauseBtn) {
            pauseBtn.disabled = !isRunning;
            pauseBtn.style.opacity = !isRunning ? '0.5' : '1';
            pauseBtn.style.cursor = !isRunning ? 'not-allowed' : 'pointer';
        }

        if (resumeBtn) {
            resumeBtn.style.display = isPaused ? 'inline-flex' : 'none';
        }
    }

    /**
     * Switch to XML mode
     */
    switchToXMLMode(): void {
        console.log('🔄 [UIManager] Switching to XML mode (Conductor-style)...');
        this.showEditor(true);
        this.showVisualCanvas(false);
        this.showPluginSidebar(false);
        this.showRightSidebar(true);    // ← ALWAYS visible
        this.showPropertyPanel(false);  // ← HIDE Properties tab + switch to Logs
        this.showExecutionControls(false); // Hide canvas controls in XML mode

        console.log('✅ [UIManager] XML mode: Editor + Right Sidebar (Logs/Results/Variables/Sessions - NO Properties)');
    }

    /**
     * Switch to Visual mode
     */
    switchToVisualMode(): void {
        console.log('🔄 [UIManager] Switching to Visual mode (Conductor-style)...');
        this.showEditor(false);
        this.showVisualCanvas(true);
        this.showPluginSidebar(true);
        this.showRightSidebar(true);    // ← ALWAYS visible
        this.showPropertyPanel(true);   // ← SHOW Properties tab as FIRST + make active
        this.showExecutionControls(true); // Show canvas controls (Run/Validate) in Visual mode
        this.showExecutionActionButtons(false); // Hide execution action buttons initially
        this.setExecutionState('idle');

        console.log('✅ [UIManager] Visual mode: Plugins + Canvas + Right Sidebar (Properties FIRST, then Logs/Results/Variables/Sessions)');
    }

    /**
     * Render plugin palette
     */
    renderPluginPalette(plugins: Plugin[]): void {
        const container = document.getElementById('plugin-list');
        if (!container) return;

        // Group by category
        const grouped: Record<string, Plugin[]> = {};
        plugins.forEach(plugin => {
            const cat = plugin.category || 'Other';
            if (!grouped[cat]) grouped[cat] = [];
            grouped[cat].push(plugin);
        });

        let html = '';
        Object.keys(grouped).sort().forEach(category => {
            html += `
        <div style="margin-bottom: 16px;">
          <div style="padding: 8px 12px; font-size: 11px; font-weight: 700; text-transform: uppercase; color: #999; letter-spacing: 0.5px;">
            ${category}
          </div>
      `;

            grouped[category].forEach(plugin => {
                html += `
          <div class="plugin-item" 
               draggable="true" 
               data-plugin-id="${plugin.id}"
               style="padding: 10px 12px; margin-bottom: 4px; background: #f9f9f9; border: 1px solid transparent; border-radius: 6px; cursor: grab; transition: all 0.15s;">
            <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px;">
              <div style="width: 28px; height: 28px; background: rgba(255, 193, 7, 0.1); border: 2px solid #FFC107; border-radius: 5px; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #1F2937;">
                <i class="fas ${plugin.icon}"></i>
              </div>
              <div style="font-size: 13px; font-weight: 600; color: #1F2937;">${plugin.name}</div>
            </div>
            <div style="font-size: 11px; color: #666; line-height: 1.4;">${plugin.description}</div>
          </div>
        `;
            });

            html += '</div>';
        });

        container.innerHTML = html;
    }

    /**
     * Show examples modal
     */
    showExamplesModal(): void {
        const modal = document.getElementById('examplesModal');
        if (modal) {
            modal.style.display = 'flex';
        }
    }

    /**
     * Hide examples modal
     */
    hideExamplesModal(): void {
        const modal = document.getElementById('examplesModal');
        if (modal) {
            modal.style.display = 'none';
        }
    }

    /**
     * Render examples list (Direct load UX - Apple HIG)
     */
    renderExamplesList(examples: any[]): void {
        const container = document.getElementById('examplesGrid');
        if (!container) return;

        let html = '';
        examples.forEach(example => {
            html += `
        <div class="example-card" 
             data-example="${example.file}" 
             onclick="window.app.loadExampleDirect(event)" 
             style="cursor: pointer; padding: 16px; margin-bottom: 8px; border: 2px solid #e5e5e5; border-radius: 8px; background: white; transition: all 0.15s;">
          <div style="display: flex; align-items: center; gap: 12px;">
            <div class="example-icon" style="width: 40px; height: 40px; background: #FFFBF0; border: 2px solid #FFC107; border-radius: 8px; display: flex; align-items: center; justify-content: center;">
              <i class="fas fa-file-code" style="font-size: 18px; color: #FFC107;"></i>
            </div>
            <div style="flex: 1;">
              <div class="example-name" style="font-weight: 600; margin-bottom: 4px; font-size: 14px;">${example.name}</div>
              <div class="example-description" style="font-size: 12px; color: #666; line-height: 1.4;">${example.description || ''}</div>
            </div>
            <div style="color: #999; font-size: 20px;">
              <i class="fas fa-chevron-right"></i>
            </div>
          </div>
        </div>
      `;
        });

        container.innerHTML = html;
    }

    /**
     * Update current filename
     */
    updateFilename(name: string): void {
        const el = document.getElementById('currentFileName');
        if (el) {
            el.textContent = name;
        }
    }

    /**
     * Show loading indicator
     */
    showLoading(message: string = 'Loading...'): void {
        // TODO: Implement loading overlay
        console.log('Loading:', message);
    }

    /**
     * Hide loading indicator
     */
    hideLoading(): void {
        // TODO: Hide loading overlay
    }

    /**
     * Show notification toast
     */
    showNotification(type: 'success' | 'error' | 'warning' | 'info', message: string): void {
        // TODO: Implement toast notifications (Apple HIG style)
        console.log(`[${type.toUpperCase()}]`, message);
    }
}

