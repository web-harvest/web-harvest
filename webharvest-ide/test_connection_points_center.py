#!/usr/bin/env python3
"""
Test sprawdzający czy connection points są na środku krawędzi węzłów
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
        if (nodes.length === 0) return {error: 'No nodes'};
        
        const firstNode = nodes[0];
        const bounds = firstNode.computedBounds;
        const nodePosition = firstNode.position;
        const inputs = firstNode.connectionPoints.inputs;
        const outputs = firstNode.connectionPoints.outputs;
        
        // Check if points are at the center of edges
        const inputChecks = inputs.map(p => {
            const isOnEdge = p.edgePosition.isOnEdge;
            const edgeCoord = p.edgePosition.edgeCoordinate;
            const isAtCenter = Math.abs(edgeCoord - 0.5) < 0.1; // Within 10% of center (0.5)
            
            // Connection points are in RELATIVE coordinates, convert to absolute
            const relativePos = p.position;
            const absolutePos = {
                x: nodePosition.x + relativePos.x,
                y: nodePosition.y + relativePos.y
            };
            
            // Calculate expected center position for each side (absolute)
            let expectedCenter = null;
            let actualCoord = null;
            let isAtCenterPrecise = false;
            
            if (p.side === 'left' || p.side === 'right') {
                expectedCenter = bounds.top + bounds.height / 2;
                actualCoord = absolutePos.y;
                isAtCenterPrecise = Math.abs(actualCoord - expectedCenter) < 1;
            } else if (p.side === 'top' || p.side === 'bottom') {
                expectedCenter = bounds.left + bounds.width / 2;
                actualCoord = absolutePos.x;
                isAtCenterPrecise = Math.abs(actualCoord - expectedCenter) < 1;
            }
            
            return {
                side: p.side,
                relativePosition: {x: relativePos.x, y: relativePos.y},
                absolutePosition: absolutePos,
                edgeCoordinate: edgeCoord,
                isOnEdge: isOnEdge,
                isAtCenter: isAtCenter,
                isAtCenterPrecise: isAtCenterPrecise,
                expectedCenter: expectedCenter,
                actualCoord: actualCoord,
                difference: expectedCenter !== null ? Math.abs(actualCoord - expectedCenter) : null
            };
        });
        
        const outputChecks = outputs.map(p => {
            const isOnEdge = p.edgePosition.isOnEdge;
            const edgeCoord = p.edgePosition.edgeCoordinate;
            const isAtCenter = Math.abs(edgeCoord - 0.5) < 0.1;
            
            // Connection points are in RELATIVE coordinates, convert to absolute
            const relativePos = p.position;
            const absolutePos = {
                x: nodePosition.x + relativePos.x,
                y: nodePosition.y + relativePos.y
            };
            
            let expectedCenter = null;
            let actualCoord = null;
            let isAtCenterPrecise = false;
            
            if (p.side === 'left' || p.side === 'right') {
                expectedCenter = bounds.top + bounds.height / 2;
                actualCoord = absolutePos.y;
                isAtCenterPrecise = Math.abs(actualCoord - expectedCenter) < 1;
            } else if (p.side === 'top' || p.side === 'bottom') {
                expectedCenter = bounds.left + bounds.width / 2;
                actualCoord = absolutePos.x;
                isAtCenterPrecise = Math.abs(actualCoord - expectedCenter) < 1;
            }
            
            return {
                side: p.side,
                relativePosition: {x: relativePos.x, y: relativePos.y},
                absolutePosition: absolutePos,
                edgeCoordinate: edgeCoord,
                isOnEdge: isOnEdge,
                isAtCenter: isAtCenter,
                isAtCenterPrecise: isAtCenterPrecise,
                expectedCenter: expectedCenter,
                actualCoord: actualCoord,
                difference: expectedCenter !== null ? Math.abs(actualCoord - expectedCenter) : null
            };
        });
        
        return {
            nodeId: firstNode.nodeId,
            bounds: bounds,
            inputChecks: inputChecks,
            outputChecks: outputChecks,
            inputsAtCenter: inputChecks.filter(c => c.isAtCenter).length,
            outputsAtCenter: outputChecks.filter(c => c.isAtCenter).length,
            inputsAtCenterPrecise: inputChecks.filter(c => c.isAtCenterPrecise).length,
            outputsAtCenterPrecise: outputChecks.filter(c => c.isAtCenterPrecise).length,
            totalInputs: inputs.length,
            totalOutputs: outputs.length
        };
    ''')
    
    print('='*60)
    print('TEST: Connection Points na środku krawędzi')
    print('='*60)
    print(f"Node: {result.get('nodeId', 'unknown')}")
    print(f"Bounds: left={result['bounds']['left']:.1f}, top={result['bounds']['top']:.1f}, width={result['bounds']['width']:.1f}, height={result['bounds']['height']:.1f}")
    print(f"\nInputs at center (edgeCoordinate ~0.5): {result.get('inputsAtCenter', 0)}/{result.get('totalInputs', 0)}")
    print(f"Inputs at center (precise): {result.get('inputsAtCenterPrecise', 0)}/{result.get('totalInputs', 0)}")
    print(f"Outputs at center (edgeCoordinate ~0.5): {result.get('outputsAtCenter', 0)}/{result.get('totalOutputs', 0)}")
    print(f"Outputs at center (precise): {result.get('outputsAtCenterPrecise', 0)}/{result.get('totalOutputs', 0)}")
    
    if result.get('inputChecks'):
        print(f"\nInput points details:")
        for check in result['inputChecks']:
            if check['side'] in ['left', 'right', 'top', 'bottom']:
                rel = check['relativePosition']
                abs = check['absolutePosition']
                print(f"  {check['side']:6s}: rel=({rel['x']:6.1f}, {rel['y']:6.1f}), abs=({abs['x']:6.1f}, {abs['y']:6.1f}), edgeCoord={check['edgeCoordinate']:.3f}, atCenter={check['isAtCenter']}, precise={check['isAtCenterPrecise']}")
                if check['expectedCenter'] is not None:
                    print(f"           expected={check['expectedCenter']:.1f}, actual={check['actualCoord']:.1f}, diff={check['difference']:.2f}")
    
    if result.get('outputChecks'):
        print(f"\nOutput points details:")
        for check in result['outputChecks']:
            if check['side'] in ['left', 'right', 'top', 'bottom']:
                rel = check['relativePosition']
                abs = check['absolutePosition']
                print(f"  {check['side']:6s}: rel=({rel['x']:6.1f}, {rel['y']:6.1f}), abs=({abs['x']:6.1f}, {abs['y']:6.1f}), edgeCoord={check['edgeCoordinate']:.3f}, atCenter={check['isAtCenter']}, precise={check['isAtCenterPrecise']}")
                if check['expectedCenter'] is not None:
                    print(f"           expected={check['expectedCenter']:.1f}, actual={check['actualCoord']:.1f}, diff={check['difference']:.2f}")
    
    # Summary
    allInputsAtCenter = result.get('inputsAtCenterPrecise', 0) == result.get('totalInputs', 0)
    allOutputsAtCenter = result.get('outputsAtCenterPrecise', 0) == result.get('totalOutputs', 0)
    
    print(f"\n{'='*60}")
    if allInputsAtCenter and allOutputsAtCenter:
        print("✅ ✅ ✅ WSZYSTKIE PUNKTY SĄ NA ŚRODKU KRAWĘDZI!")
    else:
        print("⚠️  Nie wszystkie punkty są na środku krawędzi")
        if not allInputsAtCenter:
            print(f"   - Inputs: {result.get('inputsAtCenterPrecise', 0)}/{result.get('totalInputs', 0)} na środku")
        if not allOutputsAtCenter:
            print(f"   - Outputs: {result.get('outputsAtCenterPrecise', 0)}/{result.get('totalOutputs', 0)} na środku")
    print('='*60)
            
finally:
    driver.quit()

