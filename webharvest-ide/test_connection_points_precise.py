#!/usr/bin/env python3
"""
Dokładny test connection points - sprawdza czy są dokładnie na środku krawędzi
i czy linie rzeczywiście z nich wychodzą
"""

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
import time
import json

options = Options()
# options.add_argument('--headless')
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
    
    # Get detailed connection point information
    result = driver.execute_script('''
        const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
        const modelManager = designer?._modelManager || designer?.modelManager;
        if (!modelManager) return {error: 'No manager'};
        
        const model = modelManager.getModel();
        if (!model) return {error: 'No model'};
        
        const nodes = Array.from(model.workflow.allNodes.values());
        const connections = Array.from(model.workflow.connections.values());
        
        if (nodes.length === 0) return {error: 'No nodes'};
        
        // Get first node
        const firstNode = nodes[0];
        const nodePosition = firstNode.position;
        const bounds = firstNode.computedBounds;
        const inputs = firstNode.connectionPoints.inputs;
        const outputs = firstNode.connectionPoints.outputs;
        
        // Detailed analysis of each connection point
        const inputAnalysis = inputs.map(p => {
            const relativePos = p.position;
            const absolutePos = {
                x: nodePosition.x + relativePos.x,
                y: nodePosition.y + relativePos.y
            };
            
            // Calculate expected center for this side
            let expectedCenterX = null;
            let expectedCenterY = null;
            let actualCoord = null;
            let isAtCenter = false;
            
            if (p.side === 'left' || p.side === 'right') {
                expectedCenterY = bounds.top + bounds.height / 2;
                actualCoord = absolutePos.y;
                isAtCenter = Math.abs(actualCoord - expectedCenterY) < 0.5; // 0.5px tolerance
            } else if (p.side === 'top' || p.side === 'bottom') {
                expectedCenterX = bounds.left + bounds.width / 2;
                actualCoord = absolutePos.x;
                isAtCenter = Math.abs(actualCoord - expectedCenterX) < 0.5; // 0.5px tolerance
            }
            
            return {
                id: p.id,
                side: p.side,
                relative: relativePos,
                absolute: absolutePos,
                expectedCenter: {x: expectedCenterX, y: expectedCenterY},
                actualCoord: actualCoord,
                isAtCenter: isAtCenter,
                edgeCoordinate: p.edgePosition.edgeCoordinate,
                diff: expectedCenterX !== null ? Math.abs(actualCoord - expectedCenterX) : 
                      expectedCenterY !== null ? Math.abs(actualCoord - expectedCenterY) : null
            };
        });
        
        const outputAnalysis = outputs.map(p => {
            const relativePos = p.position;
            const absolutePos = {
                x: nodePosition.x + relativePos.x,
                y: nodePosition.y + relativePos.y
            };
            
            let expectedCenterX = null;
            let expectedCenterY = null;
            let actualCoord = null;
            let isAtCenter = false;
            
            if (p.side === 'left' || p.side === 'right') {
                expectedCenterY = bounds.top + bounds.height / 2;
                actualCoord = absolutePos.y;
                isAtCenter = Math.abs(actualCoord - expectedCenterY) < 0.5;
            } else if (p.side === 'top' || p.side === 'bottom') {
                expectedCenterX = bounds.left + bounds.width / 2;
                actualCoord = absolutePos.x;
                isAtCenter = Math.abs(actualCoord - expectedCenterX) < 0.5;
            }
            
            return {
                id: p.id,
                side: p.side,
                relative: relativePos,
                absolute: absolutePos,
                expectedCenter: {x: expectedCenterX, y: expectedCenterY},
                actualCoord: actualCoord,
                isAtCenter: isAtCenter,
                edgeCoordinate: p.edgePosition.edgeCoordinate,
                diff: expectedCenterX !== null ? Math.abs(actualCoord - expectedCenterX) : 
                      expectedCenterY !== null ? Math.abs(actualCoord - expectedCenterY) : null
            };
        });
        
        // Check connections - do they start/end at connection points?
        const connectionAnalysis = connections.slice(0, 2).map(conn => {
            const fromAbsolute = modelManager.getConnectionPointAbsolutePosition(
                conn.fromNodeId,
                conn.anchors?.from?.connectionPointId
            );
            const toAbsolute = modelManager.getConnectionPointAbsolutePosition(
                conn.toNodeId,
                conn.anchors?.to?.connectionPointId
            );
            
            const pathStart = conn.path?.points?.[0];
            const pathEnd = conn.path?.points?.[conn.path.points.length - 1];
            
            const startMatches = fromAbsolute && pathStart && 
                Math.abs(pathStart.x - fromAbsolute.x) < 1 && 
                Math.abs(pathStart.y - fromAbsolute.y) < 1;
            const endMatches = toAbsolute && pathEnd && 
                Math.abs(pathEnd.x - toAbsolute.x) < 1 && 
                Math.abs(pathEnd.y - toAbsolute.y) < 1;
            
            return {
                id: conn.connectionId,
                fromNodeId: conn.fromNodeId,
                toNodeId: conn.toNodeId,
                fromAbsolute: fromAbsolute,
                toAbsolute: toAbsolute,
                pathStart: pathStart,
                pathEnd: pathEnd,
                startMatches: startMatches,
                endMatches: endMatches,
                startDiff: fromAbsolute && pathStart ? {
                    x: Math.abs(pathStart.x - fromAbsolute.x),
                    y: Math.abs(pathStart.y - fromAbsolute.y)
                } : null,
                endDiff: toAbsolute && pathEnd ? {
                    x: Math.abs(pathEnd.x - toAbsolute.x),
                    y: Math.abs(pathEnd.y - toAbsolute.y)
                } : null
            };
        });
        
        return {
            node: {
                id: firstNode.nodeId,
                position: nodePosition,
                bounds: bounds,
                width: bounds.width,
                height: bounds.height
            },
            inputs: inputAnalysis,
            outputs: outputAnalysis,
            connections: connectionAnalysis
        };
    ''')
    
    print('='*80)
    print('DOKŁADNA ANALIZA CONNECTION POINTS')
    print('='*80)
    
    if result.get('error'):
        print(f"❌ Error: {result['error']}")
        driver.quit()
        exit(1)
    
    node = result.get('node', {})
    print(f"\nNode: {node.get('id', 'unknown')}")
    print(f"Position: ({node.get('position', {}).get('x', 0):.1f}, {node.get('position', {}).get('y', 0):.1f})")
    print(f"Bounds: left={node.get('bounds', {}).get('left', 0):.1f}, top={node.get('bounds', {}).get('top', 0):.1f}, width={node.get('width', 0):.1f}, height={node.get('height', 0):.1f}")
    print(f"Expected center: X={node.get('bounds', {}).get('centerX', 0):.1f}, Y={node.get('bounds', {}).get('centerY', 0):.1f}")
    
    print(f"\n{'='*80}")
    print("INPUT POINTS:")
    print(f"{'='*80}")
    inputs = result.get('inputs', [])
    for inp in inputs:
        print(f"\n{inp['side']:8s} - {inp['id']}")
        print(f"  Relative: ({inp['relative']['x']:6.1f}, {inp['relative']['y']:6.1f})")
        print(f"  Absolute: ({inp['absolute']['x']:6.1f}, {inp['absolute']['y']:6.1f})")
        if inp['expectedCenter']['x'] is not None:
            print(f"  Expected X: {inp['expectedCenter']['x']:.1f}, Actual: {inp['actualCoord']:.1f}, Diff: {inp['diff']:.3f}px")
        if inp['expectedCenter']['y'] is not None:
            print(f"  Expected Y: {inp['expectedCenter']['y']:.1f}, Actual: {inp['actualCoord']:.1f}, Diff: {inp['diff']:.3f}px")
        print(f"  Edge coordinate: {inp['edgeCoordinate']:.3f}")
        print(f"  {'✅ AT CENTER' if inp['isAtCenter'] else '❌ NOT AT CENTER'}")
    
    print(f"\n{'='*80}")
    print("OUTPUT POINTS:")
    print(f"{'='*80}")
    outputs = result.get('outputs', [])
    for out in outputs:
        print(f"\n{out['side']:8s} - {out['id']}")
        print(f"  Relative: ({out['relative']['x']:6.1f}, {out['relative']['y']:6.1f})")
        print(f"  Absolute: ({out['absolute']['x']:6.1f}, {out['absolute']['y']:6.1f})")
        if out['expectedCenter']['x'] is not None:
            print(f"  Expected X: {out['expectedCenter']['x']:.1f}, Actual: {out['actualCoord']:.1f}, Diff: {out['diff']:.3f}px")
        if out['expectedCenter']['y'] is not None:
            print(f"  Expected Y: {out['expectedCenter']['y']:.1f}, Actual: {out['actualCoord']:.1f}, Diff: {out['diff']:.3f}px")
        print(f"  Edge coordinate: {out['edgeCoordinate']:.3f}")
        print(f"  {'✅ AT CENTER' if out['isAtCenter'] else '❌ NOT AT CENTER'}")
    
    print(f"\n{'='*80}")
    print("CONNECTIONS - DO PATHS START/END AT CONNECTION POINTS?")
    print(f"{'='*80}")
    connections = result.get('connections', [])
    for conn in connections:
        print(f"\nConnection: {conn['id']}")
        print(f"  From: {conn['fromNodeId']} → To: {conn['toNodeId']}")
        if conn['fromAbsolute']:
            print(f"  From connection point: ({conn['fromAbsolute']['x']:.1f}, {conn['fromAbsolute']['y']:.1f})")
        if conn['pathStart']:
            print(f"  Path start: ({conn['pathStart']['x']:.1f}, {conn['pathStart']['y']:.1f})")
        if conn['startDiff']:
            print(f"  Start diff: X={conn['startDiff']['x']:.3f}px, Y={conn['startDiff']['y']:.3f}px")
        print(f"  {'✅ START MATCHES' if conn['startMatches'] else '❌ START DOES NOT MATCH'}")
        
        if conn['toAbsolute']:
            print(f"  To connection point: ({conn['toAbsolute']['x']:.1f}, {conn['toAbsolute']['y']:.1f})")
        if conn['pathEnd']:
            print(f"  Path end: ({conn['pathEnd']['x']:.1f}, {conn['pathEnd']['y']:.1f})")
        if conn['endDiff']:
            print(f"  End diff: X={conn['endDiff']['x']:.3f}px, Y={conn['endDiff']['y']:.3f}px")
        print(f"  {'✅ END MATCHES' if conn['endMatches'] else '❌ END DOES NOT MATCH'}")
    
    # Summary
    allInputsAtCenter = all(inp['isAtCenter'] for inp in inputs)
    allOutputsAtCenter = all(out['isAtCenter'] for out in outputs)
    allConnectionsMatch = all(conn['startMatches'] and conn['endMatches'] for conn in connections)
    
    print(f"\n{'='*80}")
    print("PODSUMOWANIE:")
    print(f"{'='*80}")
    print(f"Inputs at center: {sum(1 for inp in inputs if inp['isAtCenter'])}/{len(inputs)}")
    print(f"Outputs at center: {sum(1 for out in outputs if out['isAtCenter'])}/{len(outputs)}")
    print(f"Connections match: {sum(1 for conn in connections if conn['startMatches'] and conn['endMatches'])}/{len(connections)}")
    
    if allInputsAtCenter and allOutputsAtCenter and allConnectionsMatch:
        print("\n✅ ✅ ✅ WSZYSTKO JEST DOKŁADNIE NA MIEJSCU!")
    else:
        print("\n⚠️  ZNALEZIONO PROBLEMY:")
        if not allInputsAtCenter:
            print("  - Nie wszystkie input points są na środku krawędzi")
        if not allOutputsAtCenter:
            print("  - Nie wszystkie output points są na środku krawędzi")
        if not allConnectionsMatch:
            print("  - Nie wszystkie ścieżki zaczynają/kończą się na connection points")
    print('='*80)
    
    # Keep browser open for visual inspection
    print("\nBrowser pozostaje otwarty dla wizualnej inspekcji. Naciśnij Enter aby zamknąć...")
    input()
            
finally:
    driver.quit()





