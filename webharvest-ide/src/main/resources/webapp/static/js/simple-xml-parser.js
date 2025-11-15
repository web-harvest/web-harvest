/**
 * Simple XML Parser for WebHarvest
 * Uses native DOMParser (no dependencies)
 * TDD: Test first, then integrate
 */

class SimpleXMLParser {
    /**
     * Parse XML string to simple node structure
     */
    parse(xmlString) {
        const parser = new DOMParser();
        const doc = parser.parseFromString(xmlString, 'text/xml');

        // Check for parse errors
        const parseError = doc.querySelector('parsererror');
        if (parseError) {
            throw new Error('XML Parse Error: ' + parseError.textContent);
        }

        // Get root element (skip <?xml?> declaration)
        const root = doc.documentElement;

        // Parse recursively
        const nodes = this.parseElement(root);

        return nodes;
    }

    /**
     * Parse single XML element
     */
    parseElement(element) {
        const nodes = [];

        // Iterate through child elements
        for (const child of element.children) {
            const node = {
                id: 'node-' + Math.random().toString(36).substr(2, 9),
                type: child.tagName,
                attributes: {},
                body: null,
                children: []
            };

            // Get attributes
            for (const attr of child.attributes) {
                node.attributes[attr.name] = attr.value;
            }

            // Get body content (text or CDATA)
            const textContent = child.textContent.trim();
            if (textContent && child.children.length === 0) {
                node.body = textContent;
            }

            // Parse children recursively
            if (child.children.length > 0) {
                node.children = this.parseElement(child);
            }

            nodes.push(node);
        }

        return nodes;
    }
}

class SimpleXMLGenerator {
    /**
     * Generate XML from node structure
     */
    generate(nodes, indent = 0) {
        let xml = '';
        const indentStr = '    '.repeat(indent);

        for (const node of nodes) {
            // Open tag
            xml += indentStr + '<' + node.type;

            // Add attributes
            for (const [key, value] of Object.entries(node.attributes || {})) {
                xml += ` ${key}="${value}"`;
            }

            // Self-closing or with content
            if (!node.body && node.children.length === 0) {
                xml += '/>\n';
            } else {
                xml += '>';

                // Add body content
                if (node.body) {
                    xml += node.body;
                }

                // Add children
                if (node.children.length > 0) {
                    xml += '\n';
                    xml += this.generate(node.children, indent + 1);
                    xml += indentStr;
                }

                // Close tag
                xml += '</' + node.type + '>\n';
            }
        }

        return xml;
    }
}

// Export for use in app.js
window.SimpleXMLParser = SimpleXMLParser;
window.SimpleXMLGenerator = SimpleXMLGenerator;

console.log('✅ Simple XML Parser/Generator loaded');









