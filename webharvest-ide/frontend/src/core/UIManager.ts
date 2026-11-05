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
        const canvas = document.getElementById('visual-canvas');
        
        if (editor) {
            if (show) {
                editor.style.display = 'flex';
                editor.style.flex = '1';
                editor.style.minWidth = '0';
                editor.style.minHeight = '0';
                editor.style.width = '100%';
                editor.style.height = '100%';
            } else {
                editor.style.display = 'none';
            }
        }
        
        // Ensure canvas is hidden when editor is shown
        if (show && canvas) {
            canvas.style.display = 'none';
        }
    }

    /**
     * Show/hide Visual canvas
     */
    showVisualCanvas(show: boolean): void {
        const canvas = document.getElementById('visual-canvas');
        const editor = document.getElementById('editor');
        const editorSection = document.querySelector('.editor-section') as HTMLElement;
        
        console.log(`[UIManager] showVisualCanvas(${show}) called`);
        console.log(`[UIManager] Canvas element found:`, !!canvas);
        console.log(`[UIManager] Editor element found:`, !!editor);
        console.log(`[UIManager] Editor section found:`, !!editorSection);
        
        if (canvas) {
            if (show) {
                // Remove any conflicting inline styles first
                canvas.removeAttribute('style');
                
                // Set all required styles
                canvas.style.cssText = `
                    display: flex !important;
                    flex: 1 !important;
                    flex-direction: column !important;
                    min-width: 0 !important;
                    min-height: 0 !important;
                    width: 100% !important;
                    height: 100% !important;
                    overflow: hidden !important;
                    position: relative !important;
                `;
                
                console.log(`[UIManager] Canvas styles applied. Display:`, canvas.style.display);
                console.log(`[UIManager] Canvas computed display:`, window.getComputedStyle(canvas).display);
                
                // Force reflow to ensure canvas is visible
                void canvas.offsetHeight;
                
                // Double-check visibility
                const computedDisplay = window.getComputedStyle(canvas).display;
                if (computedDisplay === 'none') {
                    console.error('[UIManager] WARNING: Canvas display is still "none" after setting to flex!');
                }
            } else {
                canvas.style.display = 'none';
            }
        } else {
            console.error('[UIManager] ERROR: Canvas element not found!');
        }
        
        // Ensure editor is hidden when canvas is shown
        if (show && editor) {
            editor.style.display = 'none';
            console.log(`[UIManager] Editor hidden`);
        }
        
        // Ensure editor-section has proper height
        if (show && editorSection) {
            const sectionHeight = window.getComputedStyle(editorSection).height;
            console.log(`[UIManager] Editor section height:`, sectionHeight);
            if (sectionHeight === '0px' || sectionHeight === 'auto') {
                editorSection.style.height = '100%';
                editorSection.style.minHeight = '100%';
                console.log(`[UIManager] Fixed editor section height`);
            }
        }
    }

    /**
     * Show/hide Plugin sidebar
     */
    showPluginSidebar(show: boolean): void {
        const sidebar = document.getElementById('plugin-sidebar');
        if (sidebar) {
            sidebar.style.display = show ? 'flex' : 'none';
            // Update toggle button state
            const toggleBtn = document.getElementById('togglePaletteBtn');
            if (toggleBtn) {
                toggleBtn.classList.toggle('active', show);
                toggleBtn.setAttribute('aria-pressed', show.toString());
            }
        }
    }

    /**
     * Toggle Plugin sidebar visibility
     */
    togglePluginSidebar(): void {
        const sidebar = document.getElementById('plugin-sidebar');
        if (sidebar) {
            const isVisible = sidebar.style.display !== 'none' && sidebar.style.display !== '';
            this.showPluginSidebar(!isVisible);
        }
    }

    /**
     * Show/hide Right sidebar
     */
    showRightSidebar(show: boolean): void {
        // Try new RightSidebar first (#rightSidebar), fallback to old (.sidebar-section)
        const sidebar = document.getElementById('rightSidebar') as HTMLElement || 
                       document.querySelector('.sidebar-section') as HTMLElement;
        if (sidebar) {
            sidebar.style.display = show ? 'flex' : 'none';
            // Update toggle button state
            const toggleBtn = document.getElementById('toggleSidebarBtn');
            if (toggleBtn) {
                toggleBtn.classList.toggle('active', show);
                toggleBtn.setAttribute('aria-pressed', show.toString());
            }
        }
    }

    /**
     * Toggle Right sidebar visibility
     */
    toggleRightSidebar(): void {
        // Try new RightSidebar first (#rightSidebar), fallback to old (.sidebar-section)
        const sidebar = document.getElementById('rightSidebar') as HTMLElement || 
                       document.querySelector('.sidebar-section') as HTMLElement;
        if (sidebar) {
            const isVisible = sidebar.style.display !== 'none' && sidebar.style.display !== '';
            this.showRightSidebar(!isVisible);
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
                this.activateSidebarTabElement(tab, panel);
            } else {
                // Hide Properties tab in XML mode
                tab.setAttribute('aria-hidden', 'true');
                tab.classList.remove('active');
                panel.classList.remove('active');
                panel.style.display = 'none';
                panel.setAttribute('aria-hidden', 'true');

                // Prefer Run & Test tab when Properties are hidden; fallback to Logs
                const runTab = document.querySelector('[data-panel="run"]') as HTMLElement | null;
                const runPanel = document.getElementById('runPanel');

                if (runTab && runPanel) {
                    this.activateSidebarTabElement(runTab, runPanel);
                } else {
                    const logsTab = document.querySelector('[data-panel="logs"]') as HTMLElement | null;
                    const logsPanel = document.getElementById('logsPanel');
                    if (logsTab && logsPanel) {
                        this.activateSidebarTabElement(logsTab, logsPanel);
                    }
                }
            }
        }
    }

    /**
     * Helper to set specific sidebar panel active given the elements
     */
    private activateSidebarTabElement(tabEl: HTMLElement, panelEl: HTMLElement): void {
        document.querySelectorAll('.sidebar-nav-btn').forEach(btn => {
            btn.classList.remove('active');
            (btn as HTMLElement).setAttribute('aria-selected', 'false');
        });
        document.querySelectorAll('.sidebar-panel').forEach(panel => {
            panel.classList.remove('active');
            const element = panel as HTMLElement;
            element.style.display = 'none';
            element.setAttribute('aria-hidden', 'true');
        });
        tabEl.classList.add('active');
        tabEl.setAttribute('aria-selected', 'true');
        panelEl.classList.add('active');
        panelEl.style.display = 'flex';
        panelEl.setAttribute('aria-hidden', 'false');
    }

    /**
     * Public helper to activate a sidebar panel by name (properties/run/logs/results/variables)
     */
    activateSidebarPanel(panelName: string): void {
        const panelIdMap: Record<string, string> = {
            properties: 'propertyPanel',
            run: 'runPanel',
            logs: 'logsPanel',
            results: 'resultsPanel',
            variables: 'variablesPanel'
        };

        const tab = document.querySelector(`.sidebar-nav-btn[data-panel="${panelName}"]`) as HTMLElement | null;
        const panelId = panelIdMap[panelName] ?? `${panelName}Panel`;
        const panel = document.getElementById(panelId);

        if (!tab || !panel) {
            console.warn(`[UIManager] Sidebar panel "${panelName}" not found`);
            return;
        }

        this.activateSidebarTabElement(tab, panel);
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
        
        // First ensure ide-main has proper flex layout
        const ideMain = document.querySelector('.ide-main') as HTMLElement;
        if (ideMain) {
            ideMain.style.display = 'flex';
            ideMain.style.flexDirection = 'row';
            ideMain.style.width = '100%';
            ideMain.style.height = 'calc(100vh - 120px)';
            ideMain.style.overflow = 'hidden';
            console.log(`[UIManager] ide-main layout fixed`);
        }
        
        // Ensure editor-section has proper flex properties
        const editorSection = document.querySelector('.editor-section') as HTMLElement;
        if (editorSection) {
            editorSection.style.flex = '1 1 auto';
            editorSection.style.minWidth = '0';
            editorSection.style.minHeight = '0';
            editorSection.style.width = 'auto';
            editorSection.style.display = 'flex';
            editorSection.style.flexDirection = 'column';
            editorSection.style.overflow = 'hidden';
            
            const rect = editorSection.getBoundingClientRect();
            console.log(`[UIManager] Editor section dimensions after fix:`, rect.width, 'x', rect.height);
            
            if (rect.width < 100) {
                console.error(`[UIManager] WARNING: Editor section width is still too small: ${rect.width}px`);
                // Force recalculation
                editorSection.style.flex = '1';
                editorSection.style.minWidth = '200px';
            }
        }
        
        // Ensure sidebar has fixed width (reduced to 20% to give more space to canvas)
        const sidebar = document.querySelector('.sidebar-section') as HTMLElement;
        if (sidebar) {
            sidebar.style.flex = '0 0 20%';
            sidebar.style.minWidth = '280px';
            sidebar.style.maxWidth = '20%';
            console.log(`[UIManager] Sidebar width set to 20% (min 280px)`);
        }
        
        this.showEditor(false);
        
        // Small delay to ensure editor is hidden before showing canvas
        setTimeout(() => {
            this.showVisualCanvas(true);
        }, 10);
        
        this.showPluginSidebar(true);
        this.showRightSidebar(true);    // ← ALWAYS visible
        this.showPropertyPanel(true);   // ← SHOW Properties tab as FIRST + make active
        this.showExecutionControls(true); // Show canvas controls (Run/Validate) in Visual mode
        this.showExecutionActionButtons(false); // Hide execution action buttons initially
        this.setExecutionState('idle');

        // Ensure toggle buttons reflect correct state
        const paletteBtn = document.getElementById('togglePaletteBtn');
        const sidebarBtn = document.getElementById('toggleSidebarBtn');
        if (paletteBtn) {
            paletteBtn.classList.add('active');
            paletteBtn.setAttribute('aria-pressed', 'true');
        }
        if (sidebarBtn) {
            sidebarBtn.classList.add('active');
            sidebarBtn.setAttribute('aria-pressed', 'true');
        }

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

