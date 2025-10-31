/**
 * ConnectionPropertyPanel - Implementation
 * Edit connection labels, conditions, and transformations
 */

import type { Connection } from './ConnectionManager';

export class ConnectionPropertyPanel {
    private container: HTMLElement | null = null;
    private currentConnection: Connection | null = null;
    private onChangeCallback: ((connection: Connection) => void) | null = null;

    show(container: HTMLElement, connection: Connection): void {
        this.container = container;
        this.currentConnection = connection;

        container.innerHTML = '';
        container.innerHTML = `
            <div style="padding: 16px;">
                <h3>Connection Properties</h3>
                <label>Label:
                    <input type="text" value="${connection.label || ''}" class="conn-label" 
                           style="width: 100%; padding: 8px; margin-top: 4px;" />
                </label>
                <label style="margin-top: 12px; display: block;">Condition:
                    <textarea class="conn-condition" rows="3" 
                              style="width: 100%; padding: 8px; margin-top: 4px;">${connection.condition || ''}</textarea>
                </label>
            </div>
        `;

        const labelInput = container.querySelector('.conn-label') as HTMLInputElement;
        const conditionInput = container.querySelector('.conn-condition') as HTMLTextAreaElement;

        labelInput?.addEventListener('input', () => {
            if (this.currentConnection && this.onChangeCallback) {
                this.currentConnection.label = labelInput.value;
                this.onChangeCallback(this.currentConnection);
            }
        });

        conditionInput?.addEventListener('input', () => {
            if (this.currentConnection && this.onChangeCallback) {
                this.currentConnection.condition = conditionInput.value;
                this.onChangeCallback(this.currentConnection);
            }
        });
    }

    onChange(callback: (connection: Connection) => void): void {
        this.onChangeCallback = callback;
    }

    hide(): void {
        if (this.container) {
            this.container.innerHTML = '';
        }
    }
}


