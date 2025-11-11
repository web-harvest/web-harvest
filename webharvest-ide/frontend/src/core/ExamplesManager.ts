/**
 * ExamplesManager - Manages example configurations
 */

export interface Example {
    file: string;
    name: string;
    description?: string;
}

export const DEFAULT_EXAMPLES: Example[] = [
    {
        file: 'advanced_search_example.xml',
        name: 'Advanced Search',
        description: 'Complex web scraping workflow using XPath selectors.',
    },
    {
        file: 'amazon_test.xml',
        name: 'Amazon Test',
        description: 'Sample Amazon product scraping configuration.',
    },
    {
        file: 'canon_with_namespace.xml',
        name: 'Canon (Namespaces)',
        description: 'Demonstrates namespace-aware scraping with XPath.',
    },
    {
        file: 'crawler.xml',
        name: 'Crawler',
        description: 'Iterative crawler with pagination support.',
    },
    {
        file: 'data_processing_pipeline.xml',
        name: 'Processing Pipeline',
        description: 'Chained processors for complex data transformation.',
    },
    {
        file: 'database_example.xml',
        name: 'Database Example',
        description: 'Connects to a database and processes query results.',
    },
    {
        file: 'expekt.xml',
        name: 'Expekt',
        description: 'Legacy sportsbook scraping example.',
    },
    {
        file: 'ftp_plugin_demo.xml',
        name: 'FTP Plugin',
        description: 'Shows FTP download and upload operations.',
    },
    {
        file: 'functions.xml',
        name: 'Functions',
        description: 'User-defined functions with parameters and return values.',
    },
    {
        file: 'google_images.xml',
        name: 'Google Images',
        description: 'Scrapes Google Images search results.',
    },
    {
        file: 'mail_plugin_demo.xml',
        name: 'Mail Plugin',
        description: 'Sending e-mail notifications from a workflow.',
    },
    {
        file: 'nytimes.xml',
        name: 'NYTimes',
        description: 'New York Times headline scraping example.',
    },
    {
        file: 'product_catalog.xml',
        name: 'Product Catalog',
        description: 'Builds a catalogue by aggregating multiple sources.',
    },
    {
        file: 'social_media_analytics.xml',
        name: 'Social Media Analytics',
        description: 'Collects and normalises social media metrics.',
    },
    {
        file: 'webbrowser_plugin_demo.xml',
        name: 'Web Browser',
        description: 'Headless browser automation through the webbrowser plugin.',
    },
    {
        file: 'xquery.xml',
        name: 'XQuery',
        description: 'Transforms XML responses using XQuery.',
    },
    {
        file: 'yahoomail.xml',
        name: 'Yahoo Mail',
        description: 'Mail scraping and processing example.',
    },
];

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
            this.examples = [...DEFAULT_EXAMPLES];
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

