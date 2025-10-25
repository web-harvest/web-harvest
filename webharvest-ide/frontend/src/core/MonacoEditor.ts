/**
 * MonacoEditor - TypeScript wrapper for Monaco Editor
 */

declare const monaco: any;

export class MonacoEditor {
  private editor: any = null;
  private containerId: string;

  constructor(containerId: string) {
    this.containerId = containerId;
  }

  /**
   * Initialize Monaco Editor
   */
  async initialize(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (typeof monaco === 'undefined') {
        // Load Monaco
        (window as any).require.config({
          paths: {
            vs: 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs',
          },
        });

        (window as any).require(['vs/editor/editor.main'], () => {
          this.createEditor();
          resolve();
        });
      } else {
        this.createEditor();
        resolve();
      }
    });
  }

  /**
   * Create Monaco editor instance
   */
  private createEditor(): void {
    this.editor = monaco.editor.create(document.getElementById(this.containerId), {
      value: '<?xml version="1.0" encoding="UTF-8"?>\n<config>\n    \n</config>',
      language: 'xml',
      theme: 'vs',
      automaticLayout: true,
      minimap: { enabled: true },
      fontSize: 14,
      lineNumbers: 'on',
      renderWhitespace: 'selection',
      scrollBeyondLastLine: false,
    });

    console.log('✅ Monaco Editor initialized');
  }

  /**
   * Get editor value
   */
  getValue(): string {
    return this.editor ? this.editor.getValue() : '';
  }

  /**
   * Set editor value
   */
  setValue(value: string): void {
    if (this.editor) {
      this.editor.setValue(value);
    }
  }

  /**
   * On change handler
   */
  onChange(callback: (value: string) => void): void {
    if (this.editor) {
      this.editor.onDidChangeModelContent(() => {
        callback(this.getValue());
      });
    }
  }

  /**
   * Get editor instance (for advanced usage)
   */
  getEditor(): any {
    return this.editor;
  }
}

