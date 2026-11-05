/**
 * VariableDependencyAnalyzer - Analiza zależności zmiennych
 * 
 * Analizuje użycie i definicje zmiennych w workflow:
 * - Wykrywa definicje zmiennych (DEF, VAR nodes)
 * - Wykrywa użycie zmiennych (${variable} w atrybutach i body)
 * - Waliduje zależności (użycie przed definicją = błąd)
 * - Określa scope zmiennych (root vs nested)
 */

import type { VisualNode } from '../types';
import type { VariableDependency } from './VisualSemanticModel';

export class VariableDependencyAnalyzer {
    private readonly VARIABLE_REGEX = /\$\{([^}]+)\}/g;
    private readonly VARIABLE_DEFINITION_TYPES = new Set(['def', 'var', 'set-var']);

    /**
     * Analizuj zależności zmiennych dla wszystkich węzłów
     */
    analyze(nodes: VisualNode[]): Map<string, VariableDependency> {
        const dependencies = new Map<string, VariableDependency>();

        // Krok 1: Znajdź wszystkie definicje zmiennych
        const definitions = this.findVariableDefinitions(nodes);

        // Krok 2: Znajdź wszystkie użycia zmiennych
        const usages = this.findVariableUsages(nodes);

        // Krok 3: Zbuduj mapę zależności
        const allVariables = new Set<string>([
            ...definitions.keys(),
            ...usages.keys()
        ]);

        allVariables.forEach(variableName => {
            const definedAt = definitions.get(variableName) || null;
            const usedAt = usages.get(variableName) || [];
            const scope = this.findVariableScope(variableName, nodes, definedAt);

            // Walidacja: czy użycie jest po definicji?
            const isValid = this.validateDependency(variableName, definedAt, usedAt, nodes);
            const errors: string[] = [];

            if (!isValid) {
                if (definedAt === null && usedAt.length > 0) {
                    errors.push(`Variable '${variableName}' is used but never defined`);
                } else if (definedAt && usedAt.length > 0) {
                    // Sprawdź czy użycie jest przed definicją (w kolejności wykonania)
                    const definitionDepth = this.getNodeDepth(definedAt, nodes);
                    const invalidUsages = usedAt.filter(nodeId => {
                        const usageDepth = this.getNodeDepth(nodeId, nodes);
                        // Jeśli użycie jest na tym samym lub wyższym poziomie przed definicją
                        return usageDepth <= definitionDepth && this.isNodeBefore(nodeId, definedAt, nodes);
                    });
                    if (invalidUsages.length > 0) {
                        errors.push(`Variable '${variableName}' is used before definition in nodes: ${invalidUsages.join(', ')}`);
                    }
                }
            }

            dependencies.set(variableName, {
                variableName,
                definedAt,
                usedAt,
                scope,
                isValid: errors.length === 0,
                errors: errors.length > 0 ? errors : undefined
            });
        });

        return dependencies;
    }

    /**
     * Wyciągnij zdefiniowane zmienne z węzła (DEF, VAR nodes)
     */
    extractDefinedVariables(node: VisualNode): string[] {
        const variables: string[] = [];
        
        if (this.VARIABLE_DEFINITION_TYPES.has(node.type.toLowerCase())) {
            // DEF/VAR node - sprawdź atrybut 'var' lub 'name'
            const varName = node.attributes.var || node.attributes.name;
            if (varName) {
                variables.push(varName);
            }
        }
        
        return variables;
    }

    /**
     * Wyciągnij referencje do zmiennych z węzła (${variable})
     */
    extractVariableReferences(node: VisualNode): string[] {
        const variables: string[] = [];

        // Sprawdź atrybuty
        Object.values(node.attributes).forEach(value => {
            if (typeof value === 'string') {
                let match;
                this.VARIABLE_REGEX.lastIndex = 0; // Reset regex
                while ((match = this.VARIABLE_REGEX.exec(value)) !== null) {
                    variables.push(match[1].trim());
                }
            }
        });

        // Sprawdź body
        if (node.body && typeof node.body === 'string') {
            let match;
            this.VARIABLE_REGEX.lastIndex = 0; // Reset regex
            while ((match = this.VARIABLE_REGEX.exec(node.body)) !== null) {
                variables.push(match[1].trim());
            }
        }

        return [...new Set(variables)]; // Unique
    }

    /**
     * Znajdź definicje zmiennych (DEF, VAR nodes)
     */
    findVariableDefinitions(nodes: VisualNode[]): Map<string, string> {
        const definitions = new Map<string, string>();

        const traverse = (nodeList: VisualNode[]) => {
            nodeList.forEach(node => {
                if (this.VARIABLE_DEFINITION_TYPES.has(node.type.toLowerCase())) {
                    // DEF/VAR node - sprawdź atrybut 'var' lub 'name'
                    const varName = node.attributes.var || node.attributes.name;
                    if (varName) {
                        definitions.set(varName, node.id);
                    }
                }

                // Rekurencyjnie przeszukaj dzieci
                if (node.children && node.children.length > 0) {
                    traverse(node.children);
                }
            });
        };

        traverse(nodes);
        return definitions;
    }

    /**
     * Znajdź użycia zmiennych (${variable} w atrybutach i body)
     */
    findVariableUsages(nodes: VisualNode[]): Map<string, string[]> {
        const usages = new Map<string, string[]>();

        const traverse = (nodeList: VisualNode[]) => {
            nodeList.forEach(node => {
                const referencedVars = this.extractVariableReferences(node);
                referencedVars.forEach(varName => {
                    if (!usages.has(varName)) {
                        usages.set(varName, []);
                    }
                    usages.get(varName)!.push(node.id);
                });

                // Rekurencyjnie przeszukaj dzieci
                if (node.children && node.children.length > 0) {
                    traverse(node.children);
                }
            });
        };

        traverse(nodes);
        return usages;
    }

    /**
     * Określ scope zmiennej (root vs nested)
     */
    private findVariableScope(
        variableName: string,
        nodes: VisualNode[],
        definitionNodeId: string | null
    ): string {
        if (!definitionNodeId) {
            return 'root';
        }

        const definitionNode = this.findNodeById(definitionNodeId, nodes);
        if (!definitionNode) {
            return 'root';
        }

        const parent = this.findParentNode(definitionNode, nodes);
        return parent ? parent.id : 'root';
    }

    /**
     * Waliduj zależność - czy użycie jest po definicji?
     */
    private validateDependency(
        variableName: string,
        definedAt: string | null,
        usedAt: string[],
        nodes: VisualNode[]
    ): boolean {
        if (!definedAt) {
            // Zmienna nie jest zdefiniowana - błąd jeśli jest używana
            return usedAt.length === 0;
        }

        if (usedAt.length === 0) {
            // Zmienna jest zdefiniowana ale nieużywana - OK
            return true;
        }

        // Sprawdź czy wszystkie użycia są po definicji
        // (w uproszczeniu - sprawdzamy głębokość i kolejność)
        const definitionNode = this.findNodeById(definedAt, nodes);
        if (!definitionNode) {
            return false;
        }

        const definitionDepth = this.getNodeDepth(definedAt, nodes);

        // Sprawdź każde użycie
        for (const usageNodeId of usedAt) {
            const usageDepth = this.getNodeDepth(usageNodeId, nodes);
            
            // Jeśli użycie jest na wyższym poziomie niż definicja, może być problem
            if (usageDepth < definitionDepth) {
                // Użycie jest na wyższym poziomie - sprawdź czy jest przed definicją w kolejności
                if (this.isNodeBefore(usageNodeId, definedAt, nodes)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Znajdź węzeł po ID (rekurencyjnie)
     */
    private findNodeById(nodeId: string, nodes: VisualNode[]): VisualNode | null {
        for (const node of nodes) {
            if (node.id === nodeId) {
                return node;
            }
            if (node.children && node.children.length > 0) {
                const found = this.findNodeById(nodeId, node.children);
                if (found) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Znajdź rodzica węzła
     */
    private findParentNode(node: VisualNode, nodes: VisualNode[]): VisualNode | null {
        for (const rootNode of nodes) {
            if (rootNode.children && rootNode.children.includes(node)) {
                return rootNode;
            }
            if (rootNode.children && rootNode.children.length > 0) {
                const parent = this.findParentNode(node, rootNode.children);
                if (parent) {
                    return parent;
                }
            }
        }
        return null;
    }

    /**
     * Oblicz głębokość węzła (0 = root)
     */
    private getNodeDepth(nodeId: string, nodes: VisualNode[]): number {
        const findDepth = (nodeList: VisualNode[], targetId: string, currentDepth: number): number | null => {
            for (const node of nodeList) {
                if (node.id === targetId) {
                    return currentDepth;
                }
                if (node.children && node.children.length > 0) {
                    const depth = findDepth(node.children, targetId, currentDepth + 1);
                    if (depth !== null) {
                        return depth;
                    }
                }
            }
            return null;
        };

        return findDepth(nodes, nodeId, 0) ?? 0;
    }

    /**
     * Sprawdź czy węzeł A jest przed węzłem B w kolejności wykonania
     */
    private isNodeBefore(nodeAId: string, nodeBId: string, nodes: VisualNode[]): boolean {
        // Proste sprawdzenie - jeśli są na tym samym poziomie, sprawdź kolejność w tablicy
        const nodeA = this.findNodeById(nodeAId, nodes);
        const nodeB = this.findNodeById(nodeBId, nodes);
        
        if (!nodeA || !nodeB) {
            return false;
        }

        const parentA = this.findParentNode(nodeA, nodes);
        const parentB = this.findParentNode(nodeB, nodes);

        // Jeśli mają różnych rodziców, nie możemy określić kolejności
        if (parentA?.id !== parentB?.id) {
            return false;
        }

        // Jeśli są root nodes, sprawdź kolejność w tablicy root nodes
        if (!parentA && !parentB) {
            const indexA = nodes.findIndex(n => n.id === nodeAId);
            const indexB = nodes.findIndex(n => n.id === nodeBId);
            return indexA < indexB;
        }

        // Jeśli mają tego samego rodzica, sprawdź kolejność w children
        if (parentA && parentB && parentA.id === parentB.id) {
            const children = parentA.children || [];
            const indexA = children.findIndex(n => n.id === nodeAId);
            const indexB = children.findIndex(n => n.id === nodeBId);
            return indexA < indexB;
        }

        return false;
    }

    /**
     * Waliduj wszystkie zależności i zwróć listę błędów
     */
    validateDependencies(dependencies: Map<string, VariableDependency>): Array<{
        variableName: string;
        error: string;
    }> {
        const errors: Array<{ variableName: string; error: string }> = [];

        dependencies.forEach((dep, varName) => {
            if (!dep.isValid && dep.errors) {
                dep.errors.forEach(error => {
                    errors.push({
                        variableName: varName,
                        error
                    });
                });
            }
        });

        return errors;
    }
}






