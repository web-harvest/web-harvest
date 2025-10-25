/**
 * Visual Node Type Definition
 * Shared across all components
 */

import type { PluginMetadata } from '../core/PluginRegistry';

export interface VisualNode {
    id: string;
    type: string;
    plugin: PluginMetadata;
    x: number;
    y: number;
    config: Record<string, any>;
    body: string | null;  // Body content (for template, script, etc.)

    // Hierarchy
    parentId: string | null;
    children: string[];

    // Execution tracking
    executionState: 'pending' | 'running' | 'completed' | 'error' | 'paused';
    executionTime?: number;
    dataSize?: number;
    itemCount?: number;
    errorMessage?: string;
}

