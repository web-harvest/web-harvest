#!/usr/bin/env python3
"""
Test sprawdzający korelację między modelem semantycznym a renderowaniem:
1. Czy connection points są na środku krawędzi w modelu
2. Czy path.points używa poprawnych pozycji absolutnych
3. Czy ConnectionRenderer renderuje z poprawnych pozycji
"""

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
import time
import json

options = Options()
options.add_argument('--headless')
options.add_argument('--no-sandbox')
options.add_argument('--disable-dev-shm-usage')
driver = webdriver.Chrome(options=options)

try:
    driver.get('http://localhost:8080')
    time.sleep(5)
    driver.execute_script('if (window.app && window.app.switchToVisualMode) { window.app.switchToVisualMode(); }')
    time.sleep(3)
    
    config_xml = '''<config xmlns="http://org.webharvest/schema/2.1/core">
        <def var="url1">https://httpbin.org/html</def>
        <def var="html1"><http url="${url1}"/></def>
        <def var="url2">https://httpbin.org/json</def>
        <def var="json1"><http url="${url2}"/></def>
    </config>'''
    
    driver.execute_script(f'''
        const xmlContent = {repr(config_xml)};
        const parser = window.app?._instance?.xmlParser;
        if (parser) {{
            const nodes = parser.parse(xmlContent);
            window.app.visualDesigner.loadNodes(nodes);
        }}
    ''')
    time.sleep(5)
    
    result = driver.execute_script('''
        const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
        const modelManager = designer?._modelManager || designer?.modelManager;
        if (!modelManager) return {error: 'No manager'};
        
        const model = modelManager.getModel();
        if (!model) return {error: 'No model'};
        
        const nodes = Array.from(model.workflow.allNodes.values());
        const connections = Array.from(model.workflow.connections.values());
        
        if (nodes.length < 2 || connections.length === 0) {
            return {error: 'Not enough nodes or connections'};
        }
        
        // Get first connection that doesn't use workflow-start/end
        let firstConn = null;
        let fromNode = null;
        let toNode = null;
        
        for (const conn of connections) {
            // Skip workflow-start/end connections for now
            if (conn.fromNodeId === 'workflow-start' || conn.toNodeId === 'workflow-end') {
                continue;
            }
            fromNode = nodes.find(n => n.nodeId === conn.fromNodeId);
            toNode = nodes.find(n => n.nodeId === conn.toNodeId);
            if (fromNode && toNode) {
                firstConn = conn;
                break;
            }
        }
        
        if (!firstConn || !fromNode || !toNode) {
            return {
                error: 'No suitable connection found (all use workflow-start/end or nodes not found)',
                debug: {
                    connectionsCount: connections.length,
                    connections: connections.map(c => ({
                        id: c.connectionId,
                        from: c.fromNodeId,
                        to: c.toNodeId
                    })),
                    availableNodeIds: nodes.map(n => n.nodeId),
                    nodesCount: nodes.length
                }
            };
        }
        
        // Check connection points in model
        const fromInputs = fromNode.connectionPoints.inputs;
        const fromOutputs = fromNode.connectionPoints.outputs;
        const toInputs = toNode.connectionPoints.inputs;
        const toOutputs = toNode.connectionPoints.outputs;
        
        // Check if connection points are at center (edgeCoordinate ~0.5)
        const fromOutputAtCenter = fromOutputs.some(p => 
            Math.abs(p.edgePosition.edgeCoordinate - 0.5) < 0.1
        );
        const toInputAtCenter = toInputs.some(p => 
            Math.abs(p.edgePosition.edgeCoordinate - 0.5) < 0.1
        );
        
        // Check path.points - should be absolute positions
        const pathPoints = firstConn.path.points;
        const pathStart = pathPoints[0];
        const pathEnd = pathPoints[pathPoints.length - 1];
        
        // Get absolute positions of connection points
        const fromAbsolute = modelManager.getConnectionPointAbsolutePosition(
            firstConn.fromNodeId,
            firstConn.anchors.from.connectionPointId
        );
        const toAbsolute = modelManager.getConnectionPointAbsolutePosition(
            firstConn.toNodeId,
            firstConn.anchors.to.connectionPointId
        );
        
        // Check if path.points match absolute connection point positions
        const pathStartMatches = fromAbsolute && 
            Math.abs(pathStart.x - fromAbsolute.x) < 1 && 
            Math.abs(pathStart.y - fromAbsolute.y) < 1;
        const pathEndMatches = toAbsolute && 
            Math.abs(pathEnd.x - toAbsolute.x) < 1 && 
            Math.abs(pathEnd.y - toAbsolute.y) < 1;
        
        // Check rendered SVG path
        const svg = document.querySelector('svg');
        const renderedPath = svg ? svg.querySelector(`path[data-connection-id="${firstConn.connectionId}"]`) : null;
        const renderedPathD = renderedPath ? renderedPath.getAttribute('d') : null;
        
        // Check if rendered path starts/ends at correct positions
        let renderedPathMatches = false;
        if (renderedPathD && pathStart && pathEnd) {
            // Parse SVG path to get first and last points
            const pathMatch = renderedPathD.match(/M\\s*([\\d.]+)\\s*,?\\s*([\\d.]+)/);
            if (pathMatch) {
                const renderedStartX = parseFloat(pathMatch[1]);
                const renderedStartY = parseFloat(pathMatch[2]);
                renderedPathMatches = Math.abs(renderedStartX - pathStart.x) < 1 && 
                                     Math.abs(renderedStartY - pathStart.y) < 1;
            }
        }
        
        return {
            connectionId: firstConn.connectionId,
            fromNode: {
                id: fromNode.nodeId,
                position: fromNode.position,
                bounds: fromNode.computedBounds,
                outputsCount: fromOutputs.length,
                outputsAtCenter: fromOutputs.filter(p => Math.abs(p.edgePosition.edgeCoordinate - 0.5) < 0.1).length
            },
            toNode: {
                id: toNode.nodeId,
                position: toNode.position,
                bounds: toNode.computedBounds,
                inputsCount: toInputs.length,
                inputsAtCenter: toInputs.filter(p => Math.abs(p.edgePosition.edgeCoordinate - 0.5) < 0.1).length
            },
            connectionPoints: {
                fromAbsolute: fromAbsolute,
                toAbsolute: toAbsolute,
                fromAtCenter: fromOutputAtCenter,
                toAtCenter: toInputAtCenter
            },
            path: {
                pointsCount: pathPoints.length,
                start: pathStart,
                end: pathEnd,
                startMatches: pathStartMatches,
                endMatches: pathEndMatches
            },
            rendering: {
                hasRenderedPath: !!renderedPath,
                renderedPathD: renderedPathD ? renderedPathD.substring(0, 100) : null,
                renderedPathMatches: renderedPathMatches
            },
            correlation: {
                modelPointsAtCenter: fromOutputAtCenter && toInputAtCenter,
                pathMatchesConnectionPoints: pathStartMatches && pathEndMatches,
                renderingMatchesModel: renderedPathMatches
            }
        };
    ''')
    
    print('='*70)
    print('TEST: Korelacja Model Semantyczny - Renderowanie')
    print('='*70)
    
    if result.get('error'):
        print(f"❌ Error: {result['error']}")
        if result.get('debug'):
            print(f"Debug info: {json.dumps(result['debug'], indent=2)}")
        driver.quit()
        exit(1)
    
    print(f"\nConnection: {result.get('connectionId', 'unknown')}")
    print(f"\nFrom Node:")
    from_node = result.get('fromNode', {})
    print(f"   Position: ({from_node.get('position', {}).get('x', 0):.1f}, {from_node.get('position', {}).get('y', 0):.1f})")
    print(f"   Bounds: {from_node.get('bounds', {})}")
    print(f"   Outputs: {from_node.get('outputsCount', 0)} total, {from_node.get('outputsAtCenter', 0)} at center")
    
    print(f"\nTo Node:")
    to_node = result.get('toNode', {})
    print(f"   Position: ({to_node.get('position', {}).get('x', 0):.1f}, {to_node.get('position', {}).get('y', 0):.1f})")
    print(f"   Bounds: {to_node.get('bounds', {})}")
    print(f"   Inputs: {to_node.get('inputsCount', 0)} total, {to_node.get('inputsAtCenter', 0)} at center")
    
    cp = result.get('connectionPoints', {})
    print(f"\nConnection Points:")
    print(f"   From absolute: {cp.get('fromAbsolute', {})}")
    print(f"   To absolute: {cp.get('toAbsolute', {})}")
    print(f"   From at center: {cp.get('fromAtCenter', False)}")
    print(f"   To at center: {cp.get('toAtCenter', False)}")
    
    path = result.get('path', {})
    print(f"\nPath (from model):")
    print(f"   Points count: {path.get('pointsCount', 0)}")
    print(f"   Start: {path.get('start', {})}")
    print(f"   End: {path.get('end', {})}")
    print(f"   Start matches connection point: {path.get('startMatches', False)}")
    print(f"   End matches connection point: {path.get('endMatches', False)}")
    
    rendering = result.get('rendering', {})
    print(f"\nRendering:")
    print(f"   Has rendered path: {rendering.get('hasRenderedPath', False)}")
    print(f"   Rendered path D: {rendering.get('renderedPathD', 'N/A')}")
    print(f"   Rendered path matches model: {rendering.get('renderedPathMatches', False)}")
    
    correlation = result.get('correlation', {})
    print(f"\n{'='*70}")
    print("KORELACJA:")
    print(f"   Model points at center: {'✅' if correlation.get('modelPointsAtCenter') else '❌'}")
    print(f"   Path matches connection points: {'✅' if correlation.get('pathMatchesConnectionPoints') else '❌'}")
    print(f"   Rendering matches model: {'✅' if correlation.get('renderingMatchesModel') else '❌'}")
    
    all_ok = (
        correlation.get('modelPointsAtCenter') and
        correlation.get('pathMatchesConnectionPoints') and
        correlation.get('renderingMatchesModel')
    )
    
    print(f"\n{'='*70}")
    if all_ok:
        print("✅ ✅ ✅ WSZYSTKO JEST SKORELOWANE!")
        print("   - Connection points są na środku krawędzi w modelu")
        print("   - Path.points używa poprawnych pozycji absolutnych")
        print("   - Renderowanie używa poprawnych pozycji z modelu")
    else:
        print("⚠️  Niektóre elementy nie są skorelowane:")
        if not correlation.get('modelPointsAtCenter'):
            print("   - Connection points nie są na środku krawędzi w modelu")
        if not correlation.get('pathMatchesConnectionPoints'):
            print("   - Path.points nie pasuje do connection points")
        if not correlation.get('renderingMatchesModel'):
            print("   - Renderowanie nie pasuje do modelu")
    print('='*70)
            
finally:
    driver.quit()

