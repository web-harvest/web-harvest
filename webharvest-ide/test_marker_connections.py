#!/usr/bin/env python3
"""
Test sprawdzający czy połączenia z markerami używają connection points
"""

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
import time
import json

options = Options()
# options.add_argument('--headless')
options.add_argument('--no-sandbox')
options.add_argument('--disable-dev-shm-usage')
# Disable ALL cache to ensure fresh code
options.add_argument('--disable-application-cache')
options.add_argument('--disable-cache')
options.add_argument('--aggressive-cache-discard')
options.add_argument('--disable-gpu')
options.add_argument('--disable-software-rasterizer')
# Force reload
prefs = {
    'profile.default_content_setting_values': {
        'images': 2,  # Block images to speed up
    },
    'profile.managed_default_content_settings': {
        'images': 2
    },
    'profile.default_content_settings.popups': 0,
    'profile.managed_default_content_settings.images': 2
}
options.add_experimental_option('prefs', prefs)
# Disable cache via experimental options
options.add_experimental_option('useAutomationExtension', False)
options.add_experimental_option("excludeSwitches", ["enable-automation"])
driver = webdriver.Chrome(options=options)

try:
    # Clear cache and force reload BEFORE any navigation
    driver.execute_cdp_cmd('Network.clearBrowserCache', {})
    driver.execute_cdp_cmd('Network.setCacheDisabled', {'cacheDisabled': True})
    driver.execute_cdp_cmd('Network.enable', {})
    # Force reload with cache bypass
    timestamp = int(time.time())
    driver.get(f'http://localhost:8080?nocache={timestamp}&t={timestamp}')
    # Clear all caches after page load
    driver.execute_script('''
        if ('caches' in window) {
            caches.keys().then(names => {
                names.forEach(name => caches.delete(name));
            });
        }
        // Clear service worker cache
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.getRegistrations().then(registrations => {
                registrations.forEach(reg => reg.unregister());
            });
        }
    ''')
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
        
        const connections = Array.from(model.workflow.connections.values());
        
        // Find marker connections - look for connections TO workflow-end (node -> workflow-end)
        const toEndConnections = connections.filter(c => c.toNodeId === 'workflow-end');
        // Also find connections FROM workflow-start (workflow-start -> node)
        const fromStartConnections = connections.filter(c => c.fromNodeId === 'workflow-start');
        
        // Use connection TO workflow-end if available, otherwise use FROM workflow-start
        const firstMarkerConn = toEndConnections.length > 0 ? toEndConnections[0] : 
                               (fromStartConnections.length > 0 ? fromStartConnections[0] : null);
        
        if (!firstMarkerConn) {
            return {error: 'No marker connections found', totalConnections: connections.length};
        }
        
        // Get marker positions
        const designerAny = designer;
        const startPos = designerAny.startMarkerPosition;
        const endPos = designerAny.endMarkerPosition;
        
        // Get connection point positions
        // Debug: check what connectionPointId is stored
        const fromPointId = firstMarkerConn.anchors?.from?.connectionPointId;
        const toPointId = firstMarkerConn.anchors?.to?.connectionPointId;
        
        // DEBUG: Check method implementation
        const methodStr = modelManager.getConnectionPointAbsolutePosition.toString();
        const hasMarkerHandling = methodStr.includes('workflow-start') || methodStr.includes('workflow-end');
        
        // DEBUG: Check designer state
        const managerDesigner = modelManager.designer;
        const designerStartPos = managerDesigner?.startMarkerPosition;
        const designerEndPos = managerDesigner?.endMarkerPosition;
        
        // DEBUG: Manual test
        let manualResult = null;
        if (firstMarkerConn.fromNodeId === 'workflow-start') {
            const s = managerDesigner;
            const i = s?.startMarkerPosition;
            const r = 30;
            if (i && (fromPointId === 'marker-output' || fromPointId?.includes('output'))) {
                manualResult = {x: i.x, y: i.y + r};
            }
        }
        
        const fromAbsolute = modelManager.getConnectionPointAbsolutePosition(
            firstMarkerConn.fromNodeId,
            fromPointId || 'marker-output'
        );
        const toAbsolute = modelManager.getConnectionPointAbsolutePosition(
            firstMarkerConn.toNodeId,
            toPointId || 'marker-input'
        );
        
        // Get path points
        const pathStart = firstMarkerConn.path?.points?.[0];
        const pathEnd = firstMarkerConn.path?.points?.[firstMarkerConn.path.points.length - 1];
        
        // Check if path matches connection points
        const startMatches = fromAbsolute && pathStart && 
            Math.abs(pathStart.x - fromAbsolute.x) < 1 && 
            Math.abs(pathStart.y - fromAbsolute.y) < 1;
        const endMatches = toAbsolute && pathEnd && 
            Math.abs(pathEnd.x - toAbsolute.x) < 1 && 
            Math.abs(pathEnd.y - toAbsolute.y) < 1;
        
        return {
            connectionId: firstMarkerConn.connectionId,
            fromNodeId: firstMarkerConn.fromNodeId,
            toNodeId: firstMarkerConn.toNodeId,
            fromPointId: fromPointId,
            toPointId: toPointId,
            anchors: {
                from: firstMarkerConn.anchors?.from,
                to: firstMarkerConn.anchors?.to
            },
            markerPositions: {
                start: startPos,
                end: endPos
            },
            debug: {
                hasMarkerHandling: hasMarkerHandling,
                methodLength: methodStr.length,
                designerStartPos: designerStartPos,
                designerEndPos: designerEndPos,
                manualResult: manualResult,
                hasDesigner: !!managerDesigner,
                fromNodeId: firstMarkerConn.fromNodeId,
                fromPointId: fromPointId
            },
            connectionPoints: {
                from: fromAbsolute,
                to: toAbsolute,
                fromPointId: fromPointId,
                toPointId: toPointId
            },
            pathPoints: {
                start: pathStart,
                end: pathEnd
            },
            matches: {
                start: startMatches,
                end: endMatches
            },
            diffs: {
                start: fromAbsolute && pathStart ? {
                    x: Math.abs(pathStart.x - fromAbsolute.x),
                    y: Math.abs(pathStart.y - fromAbsolute.y)
                } : null,
                end: toAbsolute && pathEnd ? {
                    x: Math.abs(pathEnd.x - toAbsolute.x),
                    y: Math.abs(pathEnd.y - toAbsolute.y)
                } : null
            }
        };
    ''')
    
    print('='*80)
    print('TEST: Połączenia z markerami - Connection Points')
    print('='*80)
    
    if result.get('error'):
        print(f"❌ Error: {result['error']}")
        driver.quit()
        exit(1)
    
    print(f"\nConnection: {result.get('connectionId', 'unknown')}")
    print(f"From: {result.get('fromNodeId', 'unknown')} → To: {result.get('toNodeId', 'unknown')}")
    
    markerPos = result.get('markerPositions', {})
    print(f"\nMarker Positions:")
    print(f"  Start: {markerPos.get('start', {})}")
    print(f"  End: {markerPos.get('end', {})}")
    
    debug = result.get('debug', {})
    if debug:
        print(f"\nDEBUG Info:")
        print(f"  Has marker handling: {debug.get('hasMarkerHandling', False)}")
        print(f"  Method length: {debug.get('methodLength', 0)}")
        print(f"  Designer start pos: {debug.get('designerStartPos', {})}")
        print(f"  Designer end pos: {debug.get('designerEndPos', {})}")
        print(f"  Manual result: {debug.get('manualResult', {})}")
        print(f"  Has designer: {debug.get('hasDesigner', False)}")
        print(f"  From nodeId: {debug.get('fromNodeId', 'unknown')}")
        print(f"  From pointId: {debug.get('fromPointId', 'unknown')}")
    
    cp = result.get('connectionPoints', {})
    print(f"\nConnection Points (should be on edges):")
    print(f"  From pointId: {result.get('fromPointId', 'unknown')}")
    print(f"  To pointId: {result.get('toPointId', 'unknown')}")
    print(f"  From: {cp.get('from', {})}")
    print(f"  To: {cp.get('to', {})}")
    
    path = result.get('pathPoints', {})
    print(f"\nPath Points (from routing):")
    print(f"  Start: {path.get('start', {})}")
    print(f"  End: {path.get('end', {})}")
    
    matches = result.get('matches', {})
    diffs = result.get('diffs', {})
    print(f"\n{'='*80}")
    print("WYNIKI:")
    print(f"  Start matches: {'✅' if matches.get('start') else '❌'}")
    if diffs.get('start'):
        print(f"    Diff: X={diffs['start']['x']:.2f}px, Y={diffs['start']['y']:.2f}px")
    print(f"  End matches: {'✅' if matches.get('end') else '❌'}")
    if diffs.get('end'):
        print(f"    Diff: X={diffs['end']['x']:.2f}px, Y={diffs['end']['y']:.2f}px")
    
    if matches.get('start') and matches.get('end'):
        print(f"\n✅ ✅ ✅ POŁĄCZENIA Z MARKERAMI UŻYWAJĄ CONNECTION POINTS!")
    else:
        print(f"\n⚠️  Połączenia z markerami NIE używają connection points")
        print(f"   - Sprawdź czy kod został zaktualizowany")
        print(f"   - Sprawdź czy serwer został zrestartowany")
    print('='*80)
    
    print("\nBrowser pozostaje otwarty. Naciśnij Enter aby zamknąć...")
    input()
            
finally:
    driver.quit()

