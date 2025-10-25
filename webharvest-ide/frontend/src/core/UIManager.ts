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
     * Show/hide Property panel
     */
    showPropertyPanel(show: boolean): void {
        const panel = document.getElementById('propertyPanel');
        if (panel) {
            panel.style.display = show ? 'flex' : 'none';
        }
    }

    /**
     * Switch to XML mode
     */
    switchToXMLMode(): void {
        this.showEditor(true);
        this.showVisualCanvas(false);
        this.showPluginSidebar(false);
        this.showRightSidebar(true);
        this.showPropertyPanel(false);
    }

    /**
     * Switch to Visual mode
     */
    switchToVisualMode(): void {
        this.showEditor(false);
        this.showVisualCanvas(true);
        this.showPluginSidebar(true);
        this.showRightSidebar(false);
        this.showPropertyPanel(true);
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

