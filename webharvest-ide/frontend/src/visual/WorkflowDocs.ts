/**
 * WorkflowDocs - Implementation
 * Generate workflow documentation with diagram export
 */

import type { VisualNode } from '../types';

export interface WorkflowDocumentation {
    title: string;
    description: string;
    nodeCount: number;
    pluginTypes: Record<string, number>;
    markdown: string;
    diagram?: string;
}

export class WorkflowDocs {
    generateDocumentation(nodes: VisualNode[], title: string, description: string): WorkflowDocumentation {
        const pluginTypes: Record<string, number> = {};

        nodes.forEach(node => {
            pluginTypes[node.type] = (pluginTypes[node.type] || 0) + 1;
        });

        const markdown = this.generateMarkdown(nodes, title, description, pluginTypes);

        return {
            title,
            description,
            nodeCount: nodes.length,
            pluginTypes,
            markdown
        };
    }

    private generateMarkdown(
        nodes: VisualNode[],
        title: string,
        description: string,
        pluginTypes: Record<string, number>
    ): string {
        let md = `# ${title}\n\n`;
        md += `${description}\n\n`;
        md += `## Workflow Statistics\n\n`;
        md += `- Total Nodes: ${nodes.length}\n`;
        md += `- Plugin Types Used:\n`;

        Object.entries(pluginTypes).forEach(([type, count]) => {
            md += `  - ${type}: ${count}\n`;
        });

        md += `\n## Nodes\n\n`;

        nodes.forEach((node, index) => {
            md += `### ${index + 1}. ${node.type}\n\n`;
            
            if (Object.keys(node.attributes).length > 0) {
                md += `**Attributes:**\n`;
                Object.entries(node.attributes).forEach(([key, value]) => {
                    md += `- ${key}: \`${value}\`\n`;
                });
                md += `\n`;
            }
        });

        return md;
    }

    exportDiagram(canvas: HTMLElement): string {
        // Simple SVG export (placeholder)
        const svg = `<svg width="800" height="600">
            <!-- Workflow diagram -->
        </svg>`;
        return svg;
    }
}


