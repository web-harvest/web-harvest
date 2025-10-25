/**
 * ExamplesManager - Manages example configurations
 */

export interface Example {
    file: string;
    name: string;
    description?: string;
}

export class ExamplesManager {
    private examples: Example[] = [];
    private loaded: boolean = false;

    /**
     * Load examples list
     */
    async loadExamples(): Promise<Example[]> {
        if (this.loaded) {
            return this.examples;
        }

        try {
            // Hardcoded examples for now (API can be added later)
            this.examples = [
                {
                    file: 'basic_variables_example.xml',
                    name: 'Basic Variables',
                    description: 'Simple variable definition and usage',
                },
                {
                    file: 'advanced_search_example.xml',
                    name: 'Advanced Search',
                    description: 'Complex web scraping with XPath',
                },
                {
                    file: 'database_example.xml',
                    name: 'Database Operations',
                    description: 'Database connection and queries',
                },
                {
                    file: 'database_plugin_demo.xml',
                    name: 'Database Plugin',
                    description: 'Database plugin demonstration',
                },
                {
                    file: 'ftp_plugin_demo.xml',
                    name: 'FTP Plugin',
                    description: 'FTP operations example',
                },
                {
                    file: 'mail_plugin_demo.xml',
                    name: 'Mail Plugin',
                    description: 'Email operations example',
                },
                {
                    file: 'webbrowser_plugin_demo.xml',
                    name: 'Web Browser',
                    description: 'Browser automation example',
                },
                {
                    file: 'xquery.xml',
                    name: 'XQuery',
                    description: 'XQuery transformation',
                },
                {
                    file: 'simple_test.xml',
                    name: 'Simple Test',
                    description: 'Basic HTTP request test',
                },
                {
                    file: 'modern_web_scraping.xml',
                    name: 'Modern Web Scraping',
                    description: 'Advanced scraping techniques',
                },
            ];

            this.loaded = true;
            console.log(`✅ Loaded ${this.examples.length} examples`);
            return this.examples;
        } catch (error) {
            console.error('❌ Failed to load examples:', error);
            throw error;
        }
    }

    /**
     * Load example content
     */
    async loadExampleContent(filename: string): Promise<string> {
        try {
            const response = await fetch(`/examples/${filename}`);
            if (!response.ok) {
                throw new Error(`Failed to load example: ${filename}`);
            }
            const content = await response.text();
            console.log(`✅ Loaded example: ${filename}`);
            return content;
        } catch (error) {
            console.error(`❌ Failed to load example ${filename}:`, error);
            throw error;
        }
    }

    /**
     * Get all examples
     */
    getExamples(): Example[] {
        return [...this.examples];
    }

    /**
     * Find example by filename
     */
    findExample(filename: string): Example | null {
        return this.examples.find(e => e.file === filename) || null;
    }
}

