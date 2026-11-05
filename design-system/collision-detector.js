/**
 * CollisionDetector - Wykrywanie kolizji i automatyczne odsuwanie etykiet
 */

class CollisionDetector {
    constructor(canvasContainer) {
        this.canvasContainer = canvasContainer;
        this.MARGIN = 20; // Minimalna odległość między etykietami
        this.ANIMATION_DURATION = 300; // ms
        this.STEP_SIZE = 10; // Krok przesuwania przy szukaniu wolnego miejsca
    }

    /**
     * Sprawdza czy dwa prostokąty się przecinają
     */
    rectanglesCollide(rect1, rect2) {
        return !(
            rect1.x + rect1.width + this.MARGIN < rect2.x ||
            rect2.x + rect2.width + this.MARGIN < rect1.x ||
            rect1.y + rect1.height + this.MARGIN < rect2.y ||
            rect2.y + rect2.height + this.MARGIN < rect1.y
        );
    }

    /**
     * Zwraca bounding box dla node
     */
    getNodeBounds(node) {
        return {
            x: node.x,
            y: node.y,
            width: node.width || 120,
            height: node.height || 60
        };
    }

    /**
     * Sprawdza kolizję node z innymi nodes
     */
    checkCollision(node, otherNodes, excludeNodeId = null) {
        const bounds = this.getNodeBounds(node);
        const collisions = [];

        for (const [id, otherNode] of otherNodes.entries()) {
            if (excludeNodeId && id === excludeNodeId) continue;
            
            const otherBounds = this.getNodeBounds(otherNode);
            if (this.rectanglesCollide(bounds, otherBounds)) {
                collisions.push({ nodeId: id, node: otherNode, bounds: otherBounds });
            }
        }

        return collisions;
    }

    /**
     * Znajduje wolne miejsce dla node
     * Strategia: przesuń w kierunku najbliższego wolnego miejsca
     */
    findFreePosition(node, otherNodes, excludeNodeId = null) {
        const canvasRect = this.canvasContainer.getBoundingClientRect();
        const canvasWidth = canvasRect.width;
        const canvasHeight = canvasRect.height;
        
        const nodeBounds = this.getNodeBounds(node);
        let bestX = node.x;
        let bestY = node.y;
        let minDistance = Infinity;

        // Sprawdź 8 kierunków (N, NE, E, SE, S, SW, W, NW)
        const directions = [
            { dx: 0, dy: -this.STEP_SIZE },   // N
            { dx: this.STEP_SIZE, dy: -this.STEP_SIZE }, // NE
            { dx: this.STEP_SIZE, dy: 0 },    // E
            { dx: this.STEP_SIZE, dy: this.STEP_SIZE },  // SE
            { dx: 0, dy: this.STEP_SIZE },    // S
            { dx: -this.STEP_SIZE, dy: this.STEP_SIZE }, // SW
            { dx: -this.STEP_SIZE, dy: 0 },   // W
            { dx: -this.STEP_SIZE, dy: -this.STEP_SIZE } // NW
        ];

        // Spróbuj przesunąć w każdym kierunku
        for (const dir of directions) {
            let testX = node.x;
            let testY = node.y;
            let distance = 0;

            // Przesuń aż znajdziesz wolne miejsce lub wyjdziesz poza canvas
            while (distance < Math.max(canvasWidth, canvasHeight)) {
                testX += dir.dx;
                testY += dir.dy;
                distance += this.STEP_SIZE;

                // Sprawdź granice canvas
                if (testX < 0 || testX + nodeBounds.width > canvasWidth ||
                    testY < 0 || testY + nodeBounds.height > canvasHeight) {
                    break;
                }

                // Sprawdź czy to miejsce jest wolne
                const testBounds = {
                    x: testX,
                    y: testY,
                    width: nodeBounds.width,
                    height: nodeBounds.height
                };

                let isFree = true;
                for (const [id, otherNode] of otherNodes.entries()) {
                    if (excludeNodeId && id === excludeNodeId) continue;
                    
                    const otherBounds = this.getNodeBounds(otherNode);
                    if (this.rectanglesCollide(testBounds, otherBounds)) {
                        isFree = false;
                        break;
                    }
                }

                if (isFree) {
                    // Znaleziono wolne miejsce - sprawdź czy jest bliżej niż poprzednie
                    const dist = Math.sqrt(
                        Math.pow(testX - node.x, 2) + Math.pow(testY - node.y, 2)
                    );
                    if (dist < minDistance) {
                        minDistance = dist;
                        bestX = testX;
                        bestY = testY;
                    }
                    break;
                }
            }
        }

        // Jeśli nie znaleziono wolnego miejsca w żadnym kierunku, użyj prostszego algorytmu
        if (minDistance === Infinity) {
            return this.findFreePositionSimple(node, otherNodes, excludeNodeId);
        }

        return { x: bestX, y: bestY };
    }

    /**
     * Prostszy algorytm - przesuń w dół i w prawo
     */
    findFreePositionSimple(node, otherNodes, excludeNodeId = null) {
        const canvasRect = this.canvasContainer.getBoundingClientRect();
        const nodeBounds = this.getNodeBounds(node);
        
        let offsetX = 0;
        let offsetY = 0;
        const maxAttempts = 50;

        for (let attempt = 0; attempt < maxAttempts; attempt++) {
            const testX = node.x + offsetX;
            const testY = node.y + offsetY;

            // Sprawdź granice
            if (testX + nodeBounds.width > canvasRect.width) {
                offsetX = 0;
                offsetY += nodeBounds.height + this.MARGIN;
            }

            if (testY + nodeBounds.height > canvasRect.height) {
                // Przesuń na początek canvas
                offsetX = 0;
                offsetY = -node.y;
            }

            const testBounds = {
                x: node.x + offsetX,
                y: node.y + offsetY,
                width: nodeBounds.width,
                height: nodeBounds.height
            };

            let isFree = true;
            for (const [id, otherNode] of otherNodes.entries()) {
                if (excludeNodeId && id === excludeNodeId) continue;
                
                const otherBounds = this.getNodeBounds(otherNode);
                if (this.rectanglesCollide(testBounds, otherBounds)) {
                    isFree = false;
                    break;
                }
            }

            if (isFree) {
                return { x: node.x + offsetX, y: node.y + offsetY };
            }

            offsetX += nodeBounds.width + this.MARGIN;
        }

        // Fallback - zwróć oryginalną pozycję
        return { x: node.x, y: node.y };
    }

    /**
     * Animuje przesunięcie node do nowej pozycji
     */
    animateNodeMove(node, targetX, targetY, callback = null) {
        const nodeEl = document.getElementById(`node-${node.id}`);
        if (!nodeEl) {
            console.warn(`[CollisionDetector] Nie znaleziono elementu dla node ${node.id}`);
            return;
        }

        // Sprawdź czy node jest już w docelowej pozycji
        const currentX = parseFloat(nodeEl.style.left) || node.x;
        const currentY = parseFloat(nodeEl.style.top) || node.y;
        
        if (Math.abs(currentX - targetX) < 1 && Math.abs(currentY - targetY) < 1) {
            // Już w docelowej pozycji
            if (callback) callback();
            return;
        }

        // Zaktualizuj pozycję w modelu najpierw
        node.x = targetX;
        node.y = targetY;

        // Ustaw transition dla animacji
        nodeEl.style.transition = `left ${this.ANIMATION_DURATION}ms ease-out, top ${this.ANIMATION_DURATION}ms ease-out`;
        
        // Ustaw nową pozycję (użyj requestAnimationFrame aby zapewnić że transition zadziała)
        requestAnimationFrame(() => {
            nodeEl.style.left = `${targetX}px`;
            nodeEl.style.top = `${targetY}px`;
        });

        // Wywołaj callback po zakończeniu animacji
        if (callback) {
            setTimeout(() => {
                nodeEl.style.transition = ''; // Usuń transition
                callback();
            }, this.ANIMATION_DURATION);
        } else {
            setTimeout(() => {
                nodeEl.style.transition = ''; // Usuń transition
            }, this.ANIMATION_DURATION);
        }
    }

    /**
     * Rozwiązuje wszystkie kolizje dla node (rekurencyjnie dla przesuniętych nodes)
     */
    resolveCollisions(node, allNodes, excludeNodeId = null, visited = new Set()) {
        const nodeId = excludeNodeId || node.id;
        
        if (visited.has(nodeId)) {
            return; // Unikaj nieskończonych pętli
        }
        visited.add(nodeId);

        const collisions = this.checkCollision(node, allNodes, excludeNodeId);
        
        if (collisions.length === 0) {
            return; // Brak kolizji
        }

        console.log(`[CollisionDetector] Kolizja wykryta dla ${nodeId} z ${collisions.length} innymi etykietami`);

        // Znajdź wolne miejsce
        const freePos = this.findFreePosition(node, allNodes, excludeNodeId);
        
        console.log(`[CollisionDetector] Przesuwam ${nodeId} z (${node.x}, ${node.y}) do (${freePos.x}, ${freePos.y})`);
        
        // Animuj przesunięcie
        this.animateNodeMove(node, freePos.x, freePos.y, () => {
            // Po przesunięciu, sprawdź czy nie powstały nowe kolizje z innymi nodes
            // które mogły być przesunięte
            for (const [id, otherNode] of allNodes.entries()) {
                if (id === nodeId) continue;
                if (visited.has(id)) continue;
                
                const otherCollisions = this.checkCollision(otherNode, allNodes, id);
                if (otherCollisions.length > 0) {
                    // Rekurencyjnie rozwiązuj kolizje dla przesuniętych nodes
                    this.resolveCollisions(otherNode, allNodes, id, visited);
                }
            }
        });
    }

    /**
     * Rozwiązuje wszystkie kolizje w całym układzie
     */
    resolveAllCollisions(allNodes) {
        const visited = new Set();
        
        for (const [nodeId, node] of allNodes.entries()) {
            if (visited.has(nodeId)) continue;
            
            const collisions = this.checkCollision(node, allNodes, nodeId);
            if (collisions.length > 0) {
                this.resolveCollisions(node, allNodes, nodeId, visited);
            }
        }
    }
}

