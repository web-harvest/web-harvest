/**
 * Visual Renderer - Displays parsed XML nodes as visual boxes
 * Phase 2: Basic rendering before full drag & drop
 */

class VisualRenderer {
    constructor(canvasId) {
        this.canvas = document.getElementById(canvasId);
        if (!this.canvas) {
            throw new Error('Canvas element not found: ' + canvasId);
        }
    }

    /**
     * Render parsed nodes as visual boxes
     */
    render(nodes) {
        if (!nodes || nodes.length === 0) {
            this.showEmpty();
            return;
        }

        let html = '<div class="visual-nodes-list">';

        for (let i = 0; i < nodes.length; i++) {
            html += this.renderNode(nodes[i], i);
        }

        html += '</div>';
        this.canvas.innerHTML = html;
    }

    /**
     * Render single node as box
     */
    renderNode(node, index) {
        const icon = this.getNodeIcon(node.type);
        const attrCount = Object.keys(node.attributes || {}).length;
        const childCount = node.children ? node.children.length : 0;

        let html = `
            <div class="visual-node-box" data-node-id="${node.id}">
                <div class="node-header">
                    <div class="node-icon">${icon}</div>
                    <div class="node-info">
                        <h4 class="node-type">${node.type}</h4>
                        <p class="node-meta">${attrCount} attributes, ${childCount} children</p>
                    </div>
                </div>
        `;

        // Show attributes
        if (attrCount > 0) {
            html += '<div class="node-attributes">';
            for (const [key, value] of Object.entries(node.attributes)) {
                const displayValue = value.length > 30 ? value.substring(0, 30) + '...' : value;
                html += `<div class="node-attr"><strong>${key}:</strong> ${displayValue}</div>`;
            }
            html += '</div>';
        }

        // Show body content
        if (node.body) {
            const displayBody = node.body.length > 50 ? node.body.substring(0, 50) + '...' : node.body;
            html += `<div class="node-body">${displayBody}</div>`;
        }

        // Show children recursively
        if (node.children && node.children.length > 0) {
            html += '<div class="node-children">';
            html += '<div class="children-label">Children:</div>';
            for (const child of node.children) {
                html += this.renderNode(child, 0);
            }
            html += '</div>';
        }

        html += '</div>';
        return html;
    }

    /**
     * Get Font Awesome icon for node type
     */
    getNodeIcon(type) {
        const icons = {
            'http': 'fa-globe',
            'def': 'fa-tag',
            'var': 'fa-box',
            'xpath': 'fa-code-branch',
            'html-to-xml': 'fa-file-code',
            'loop': 'fa-repeat',
            'if': 'fa-question',
            'template': 'fa-file-alt',
            'get': 'fa-arrow-right',
            'file': 'fa-file'
        };

        const iconClass = icons[type] || 'fa-puzzle-piece';
        return `<i class="fas ${iconClass}"></i>`;
    }

    /**
     * Show empty state
     */
    showEmpty() {
        this.canvas.innerHTML = `
            <div class="visual-empty">
                <i class="fas fa-inbox"></i>
                <p>No nodes to display</p>
            </div>
        `;
    }
}

// Export
window.VisualRenderer = VisualRenderer;
console.log('✅ Visual Renderer loaded');







