#!/usr/bin/env python3
"""
Test Selenium Chrome dla wizualnego renderowania połączeń
Sprawdza czy połączenia są poprawnie rysowane w przeglądarce
"""

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time
import json

def setup_driver():
    """Konfiguracja Chrome driver z wyłączonym cache"""
    options = Options()
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    options.add_argument('--disable-cache')
    options.add_argument('--disable-application-cache')
    options.add_argument('--disable-gpu')
    prefs = {'profile.default_content_setting_values': {'images': 2}}
    options.add_experimental_option('prefs', prefs)
    options.add_experimental_option('useAutomationExtension', False)
    options.add_experimental_option("excludeSwitches", ["enable-automation"])
    
    driver = webdriver.Chrome(options=options)
    
    # Wyłącz cache
    driver.execute_cdp_cmd('Network.setCacheDisabled', {'cacheDisabled': True})
    driver.execute_cdp_cmd('Network.enable', {})
    
    return driver

def test_connection_rendering():
    """Test renderowania połączeń w przeglądarce"""
    driver = setup_driver()
    
    try:
        timestamp = int(time.time())
        driver.get(f'http://localhost:8080/?nocache={timestamp}&t={timestamp}&v={timestamp}')
        time.sleep(10)
        
        # Przełącz na tryb wizualny
        driver.execute_script('if (window.app && window.app.switchToVisualMode) { window.app.switchToVisualMode(); }')
        time.sleep(3)
        
        # Załaduj konfigurację z markerami
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
                const config = parser.parse(xmlContent);
                const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
                if (designer) {{
                    designer.loadNodes(config);
                    designer.render();
                }}
            }}
        ''')
        time.sleep(5)
        
        # Sprawdź renderowanie połączeń
        result = driver.execute_script('''
            const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
            const modelManager = designer?.modelManager;
            if (!modelManager) return {error: 'No manager'};
            
            // Pobierz model
            const model = modelManager.getModel();
            const connections = Array.from(model.workflow.connections.values());
            
            // Znajdź połączenia z markerami
            const markerConnections = connections.filter(c => 
                c.fromNodeId === 'workflow-start' || c.toNodeId === 'workflow-end' ||
                c.fromNodeId === 'workflow-end' || c.toNodeId === 'workflow-start'
            );
            
            // Sprawdź SVG rendering
            const svg = document.querySelector('svg');
            const svgPaths = svg ? Array.from(svg.querySelectorAll('path.connection-path')) : [];
            
            // Sprawdź czy każda connection ma odpowiadający SVG path
            const renderedConnections = [];
            for (const conn of markerConnections) {
                const pathId = `connection-${conn.connectionId}`;
                const svgPath = svg?.querySelector(`#${pathId}`) || svg?.querySelector(`[data-connection-id="${conn.connectionId}"]`);
                
                if (svgPath) {
                    const pathData = svgPath.getAttribute('d');
                    // Parse SVG path data - extract M (move) and L (line) commands
                    const pathPoints = pathData ? pathData.match(/[ML]\\s*([\\d.]+),([\\d.]+)/g) || [] : [];
                    
                    // Extract coordinates from path points
                    let svgStartPoint = null;
                    let svgEndPoint = null;
                    if (pathPoints.length > 0) {
                        const startMatch = pathPoints[0].match(/[ML]\\s*([\\d.]+),([\\d.]+)/);
                        if (startMatch) {
                            svgStartPoint = {x: parseFloat(startMatch[1]), y: parseFloat(startMatch[2])};
                        }
                    }
                    if (pathPoints.length > 1) {
                        const endMatch = pathPoints[pathPoints.length - 1].match(/[ML]\\s*([\\d.]+),([\\d.]+)/);
                        if (endMatch) {
                            svgEndPoint = {x: parseFloat(endMatch[1]), y: parseFloat(endMatch[2])};
                        }
                    }
                    
                    renderedConnections.push({
                        connectionId: conn.connectionId,
                        fromNodeId: conn.fromNodeId,
                        toNodeId: conn.toNodeId,
                        hasSvgPath: true,
                        pathDataLength: pathData?.length || 0,
                        pathPointsCount: pathPoints.length,
                        modelPathPoints: conn.path?.points?.length || 0,
                        modelStartPoint: conn.path?.points?.[0],
                        modelEndPoint: conn.path?.points?.[conn.path.points.length - 1],
                        svgStartPoint: svgStartPoint,
                        svgEndPoint: svgEndPoint,
                        pathData: pathData
                    });
                } else {
                    renderedConnections.push({
                        connectionId: conn.connectionId,
                        fromNodeId: conn.fromNodeId,
                        toNodeId: conn.toNodeId,
                        hasSvgPath: false,
                        error: 'SVG path not found'
                    });
                }
            }
            
            return {
                totalConnections: connections.length,
                markerConnectionsCount: markerConnections.length,
                svgPathsCount: svgPaths.length,
                renderedConnections: renderedConnections,
                hasSvg: !!svg,
                svgChildrenCount: svg ? svg.children.length : 0
            };
        ''')
        
        print("\n" + "="*80)
        print("WYNIKI TESTU RENDEROWANIA POŁĄCZEŃ")
        print("="*80)
        print(f"\nŁączna liczba połączeń: {result.get('totalConnections', 0)}")
        print(f"Połączenia z markerami: {result.get('markerConnectionsCount', 0)}")
        print(f"Ścieżki SVG w DOM: {result.get('svgPathsCount', 0)}")
        print(f"SVG element istnieje: {result.get('hasSvg', False)}")
        print(f"Liczba dzieci SVG: {result.get('svgChildrenCount', 0)}")
        
        rendered = result.get('renderedConnections', [])
        print(f"\nSzczegóły renderowania ({len(rendered)} połączeń):")
        print("-" * 80)
        
        allRendered = True
        for i, conn in enumerate(rendered, 1):
            print(f"\n{i}. Połączenie: {conn.get('fromNodeId')} -> {conn.get('toNodeId')}")
            print(f"   Connection ID: {conn.get('connectionId')}")
            print(f"   Ma SVG path: {'✅' if conn.get('hasSvgPath') else '❌'}")
            
            if conn.get('hasSvgPath'):
                print(f"   Długość path data: {conn.get('pathDataLength', 0)} znaków")
                print(f"   Punkty w SVG path: {conn.get('pathPointsCount', 0)}")
                print(f"   Punkty w modelu: {conn.get('modelPathPoints', 0)}")
                
                # Sprawdź czy start point się zgadza
                modelStart = conn.get('modelStartPoint')
                svgStart = conn.get('svgStartPoint')
                if modelStart and svgStart:
                    diffX = abs(svgStart.get('x', 0) - modelStart.get('x', 0))
                    diffY = abs(svgStart.get('y', 0) - modelStart.get('y', 0))
                    startMatches = diffX < 1 and diffY < 1
                    print(f"   Start point match: {'✅' if startMatches else '❌'} (diff: X={diffX:.2f}px, Y={diffY:.2f}px)")
                    if not startMatches:
                        print(f"      Model: ({modelStart.get('x', 0):.1f}, {modelStart.get('y', 0):.1f})")
                        print(f"      SVG: ({svgStart.get('x', 0):.1f}, {svgStart.get('y', 0):.1f})")
                
                # Sprawdź czy end point się zgadza
                modelEnd = conn.get('modelEndPoint')
                svgEnd = conn.get('svgEndPoint')
                if modelEnd and svgEnd:
                    diffX = abs(svgEnd.get('x', 0) - modelEnd.get('x', 0))
                    diffY = abs(svgEnd.get('y', 0) - modelEnd.get('y', 0))
                    endMatches = diffX < 1 and diffY < 1
                    print(f"   End point match: {'✅' if endMatches else '❌'} (diff: X={diffX:.2f}px, Y={diffY:.2f}px)")
                    if not endMatches:
                        print(f"      Model: ({modelEnd.get('x', 0):.1f}, {modelEnd.get('y', 0):.1f})")
                        print(f"      SVG: ({svgEnd.get('x', 0):.1f}, {svgEnd.get('y', 0):.1f})")
            else:
                print(f"   Błąd: {conn.get('error', 'Unknown')}")
                allRendered = False
        
        print("\n" + "="*80)
        if allRendered and len(rendered) > 0:
            print("✅ ✅ ✅ WSZYSTKIE POŁĄCZENIA SĄ POPRAWNIE RENDEROWANE W SVG!")
        else:
            print("❌ ❌ ❌ NIEKTÓRE POŁĄCZENIA NIE SĄ RENDEROWANE!")
        print("="*80)
        
        # Sprawdź również wizualnie czy są strzałki
        arrowResult = driver.execute_script('''
            const svg = document.querySelector('svg');
            const arrows = svg ? Array.from(svg.querySelectorAll('polygon.arrow-head, path.arrow-head, marker')) : [];
            return {
                arrowsCount: arrows.length,
                hasMarkers: svg ? svg.querySelector('defs marker') !== null : false
            };
        ''')
        
        print(f"\nStrzałki/Arrowheads:")
        print(f"  Liczba elementów arrow: {arrowResult.get('arrowsCount', 0)}")
        print(f"  Ma definicje markerów: {arrowResult.get('hasMarkers', False)}")
        
        return result
        
    finally:
        print("\nBrowser pozostaje otwarty. Naciśnij Enter aby zamknąć...")
        try:
            input()
        except EOFError:
            pass
        driver.quit()

if __name__ == '__main__':
    test_connection_rendering()

