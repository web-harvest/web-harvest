import type { VisualNode } from '../../types';
import type { Connection } from '../VisualModelHelpers';

export function planConnections(nodes: VisualNode[], startNodeId: string, endNodeId: string): Connection[] {
    if (!nodes.length) {
        return [];
    }

    const connections: Connection[] = [];

    // Dla prostych workflow zawsze używaj kolejności z tablicy (sekwencyjne)
    // Sortowanie według pozycji może powodować równoległe połączenia
    const isSimpleWorkflow = nodes.length <= 5;
    
    let ordered: VisualNode[];
    if (isSimpleWorkflow) {
        // Prosty workflow - użyj kolejności z tablicy (sekwencyjne)
        ordered = [...nodes];
    } else {
        // Złożony workflow - sortuj według pozycji
        const nodesWithPosition = nodes.filter(node => node.position);
        ordered = nodesWithPosition.length > 0
            ? [...nodesWithPosition].sort((a, b) => {
                // Najpierw sortuj po Y (pionowo), potem po X (poziomo)
                const ay = a.position?.y ?? 0;
                const by = b.position?.y ?? 0;
                if (ay !== by) {
                    return ay - by;
                }
                const ax = a.position?.x ?? 0;
                const bx = b.position?.x ?? 0;
                return ax - bx;
            })
            : [...nodes];
    }

    // Dla sekwencyjnego workflow: start -> node1 -> node2 -> ... -> nodeN -> end
    if (ordered.length > 0) {
        // Połączenie start -> pierwszy węzeł
        connections.push({ from: startNodeId, to: ordered[0].id, type: 'sequential' });
        
        // Połączenia między węzłami (sekwencyjne)
        for (let i = 0; i < ordered.length - 1; i++) {
            connections.push({ 
                from: ordered[i].id, 
                to: ordered[i + 1].id, 
                type: 'sequential' 
            });
        }
        
        // Połączenie ostatni węzeł -> end
        const last = ordered[ordered.length - 1];
        connections.push({ from: last.id, to: endNodeId, type: 'sequential' });
    }

    return connections;
}
