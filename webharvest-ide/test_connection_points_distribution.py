#!/usr/bin/env python3
"""
Test rozłożenia connection points - sprawdza czy różne połączenia używają różnych punktów
"""

import logging
import sys
import time
import json
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import websocket
import threading

# Konfiguracja loggera
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger('ConnectionPointsDistributionTest')

class WebSocketClient:
    """Klient WebSocket do komunikacji z backendem"""
    
    def __init__(self, url, on_message=None):
        self.url = url
        self.ws = None
        self.on_message = on_message
        self.received_messages = []
        self.connected = False
        
    def connect(self):
        """Połącz z WebSocket"""
        try:
            self.ws = websocket.WebSocketApp(
                self.url,
                on_open=self._on_open,
                on_message=self._on_message,
                on_error=self._on_error,
                on_close=self._on_close
            )
            wst = threading.Thread(target=self.ws.run_forever)
            wst.daemon = True
            wst.start()
            
            timeout = 5
            start = time.time()
            while not self.connected and (time.time() - start) < timeout:
                time.sleep(0.1)
            
            if not self.connected:
                raise Exception("WebSocket connection timeout")
                
            logger.info("✅ WebSocket połączony")
            return True
        except Exception as e:
            logger.error(f"❌ Błąd połączenia WebSocket: {e}")
            return False
    
    def _on_open(self, ws):
        self.connected = True
        logger.info("WebSocket: Połączenie otwarte")
    
    def _on_message(self, ws, message):
        try:
            data = json.loads(message)
            self.received_messages.append(data)
            msg_type = data.get('type') or data.get('action', {}).get('type') if isinstance(data.get('action'), dict) else 'unknown'
            if msg_type == 'error':
                logger.error(f"WebSocket: Błąd: {data.get('error', 'Unknown error')}")
            else:
                logger.debug(f"WebSocket: Otrzymano wiadomość typu: {msg_type}")
            if self.on_message:
                self.on_message(data)
        except json.JSONDecodeError as e:
            logger.warning(f"WebSocket: Nie można sparsować JSON: {e}")
    
    def _on_error(self, ws, error):
        logger.error(f"WebSocket: Błąd: {error}")
    
    def _on_close(self, ws, close_status_code, close_msg):
        self.connected = False
        logger.info("WebSocket: Połączenie zamknięte")
    
    def disconnect(self):
        if self.ws:
            self.ws.close()
            self.connected = False

def setup_driver():
    """Konfiguracja Chrome driver"""
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
    driver.execute_cdp_cmd('Network.setCacheDisabled', {'cacheDisabled': True})
    driver.execute_cdp_cmd('Network.enable', {})
    
    return driver

def wait_for_app_initialization(driver, timeout=30):
    """Czekaj na inicjalizację aplikacji"""
    logger.info("Czekam na inicjalizację aplikacji...")
    start = time.time()
    while time.time() - start < timeout:
        try:
            result = driver.execute_script('''
                return {
                    hasApp: !!window.app,
                    hasDesigner: !!(window.app?._instance?.visualDesigner || window.app?.visualDesigner),
                    hasPluginManager: !!(window.app?._instance?.pluginManager || window.app?.pluginManager)
                };
            ''')
            if result.get('hasApp') and result.get('hasDesigner'):
                logger.info("✅ Aplikacja zainicjalizowana")
                return True
        except Exception as e:
            logger.debug(f"Sprawdzanie inicjalizacji: {e}")
        time.sleep(0.5)
    logger.warning("⚠️ Timeout inicjalizacji aplikacji")
    return False

def load_config_via_selenium(driver, config_xml):
    """Załaduj konfigurację przez Selenium"""
    logger.info("Ładowanie konfiguracji przez Selenium...")
    driver.execute_script(f'''
        const xmlContent = {repr(config_xml)};
        const parser = window.app?._instance?.xmlParser;
        if (parser) {{
            const config = parser.parse(xmlContent);
            const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
            if (designer) {{
                designer.loadNodes(config);
                designer.render();
                
                if (designer.modelManager) {{
                    designer.modelManager.notifyUpdate();
                }}
            }}
        }}
    ''')
    time.sleep(5)
    logger.info("✅ Konfiguracja załadowana")

def get_connection_points_distribution(driver):
    """Pobierz informacje o rozłożeniu connection points"""
    logger.info("Pobieranie informacji o rozłożeniu connection points...")
    result = driver.execute_script('''
        const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
        const modelManager = designer?.modelManager;
        if (!modelManager) {
            return {error: 'No modelManager'};
        }
        
        const model = modelManager.getModel();
        if (!model) {
            return {error: 'No model'};
        }
        
        // Pobierz wszystkie połączenia
        const connections = Array.from(model.workflow.connections.values());
        
        // Grupuj połączenia według węzłów (source i target)
        const nodeConnections = {
            inputs: {},  // nodeId -> [connectionIds]
            outputs: {}  // nodeId -> [connectionIds]
        };
        
        const connectionPointsInfo = [];
        
        for (const conn of connections) {
            // Source node
            if (!nodeConnections.outputs[conn.fromNodeId]) {
                nodeConnections.outputs[conn.fromNodeId] = [];
            }
            nodeConnections.outputs[conn.fromNodeId].push(conn.connectionId);
            
            // Target node
            if (!nodeConnections.inputs[conn.toNodeId]) {
                nodeConnections.inputs[conn.toNodeId] = [];
            }
            nodeConnections.inputs[conn.toNodeId].push(conn.connectionId);
            
            // Pobierz informacje o connection points
            const fromPointId = conn.anchors?.from?.connectionPointId;
            const toPointId = conn.anchors?.to?.connectionPointId;
            const fromSide = conn.path?.spatialMetadata?.startSide;
            const toSide = conn.path?.spatialMetadata?.endSide;
            
            connectionPointsInfo.push({
                connectionId: conn.connectionId,
                fromNodeId: conn.fromNodeId,
                toNodeId: conn.toNodeId,
                fromPointId: fromPointId,
                toPointId: toPointId,
                fromSide: fromSide,
                toSide: toSide
            });
        }
        
        // Sprawdź czy różne połączenia używają różnych punktów
        const distributionCheck = {
            nodesWithMultipleOutputs: {},
            nodesWithMultipleInputs: {},
            conflicts: []
        };
        
        // Sprawdź outputs
        for (const [nodeId, connIds] of Object.entries(nodeConnections.outputs)) {
            if (connIds.length > 1) {
                const points = connIds.map(cid => {
                    const conn = connections.find(c => c.connectionId === cid);
                    return {
                        connectionId: cid,
                        pointId: conn?.anchors?.from?.connectionPointId,
                        side: conn?.path?.spatialMetadata?.startSide
                    };
                });
                
                const uniquePoints = new Set(points.map(p => p.pointId));
                const uniqueSides = new Set(points.map(p => p.side).filter(s => s));
                
                distributionCheck.nodesWithMultipleOutputs[nodeId] = {
                    connectionCount: connIds.length,
                    uniquePointIds: Array.from(uniquePoints),
                    uniqueSides: Array.from(uniqueSides),
                    points: points,
                    allPointsDifferent: uniquePoints.size === connIds.length,
                    allSidesDifferent: uniqueSides.size === connIds.length
                };
                
                if (uniquePoints.size < connIds.length) {
                    distributionCheck.conflicts.push({
                        nodeId: nodeId,
                        type: 'output',
                        issue: 'Multiple connections use same point',
                        connections: connIds,
                        points: points
                    });
                }
            }
        }
        
        // Sprawdź inputs
        for (const [nodeId, connIds] of Object.entries(nodeConnections.inputs)) {
            if (connIds.length > 1) {
                const points = connIds.map(cid => {
                    const conn = connections.find(c => c.connectionId === cid);
                    return {
                        connectionId: cid,
                        pointId: conn?.anchors?.to?.connectionPointId,
                        side: conn?.path?.spatialMetadata?.endSide
                    };
                });
                
                const uniquePoints = new Set(points.map(p => p.pointId));
                const uniqueSides = new Set(points.map(p => p.side).filter(s => s));
                
                distributionCheck.nodesWithMultipleInputs[nodeId] = {
                    connectionCount: connIds.length,
                    uniquePointIds: Array.from(uniquePoints),
                    uniqueSides: Array.from(uniqueSides),
                    points: points,
                    allPointsDifferent: uniquePoints.size === connIds.length,
                    allSidesDifferent: uniqueSides.size === connIds.length
                };
                
                if (uniquePoints.size < connIds.length) {
                    distributionCheck.conflicts.push({
                        nodeId: nodeId,
                        type: 'input',
                        issue: 'Multiple connections use same point',
                        connections: connIds,
                        points: points
                    });
                }
            }
        }
        
        return {
            connectionsCount: connections.length,
            connectionPointsInfo: connectionPointsInfo,
            distributionCheck: distributionCheck,
            hasConflicts: distributionCheck.conflicts.length > 0
        };
    ''')
    
    if result.get('error'):
        logger.error(f"❌ Błąd: {result.get('error')}")
        return None
    
    logger.info(f"✅ Pobrano informacje o {result.get('connectionsCount', 0)} połączeniach")
    return result

def test_connection_points_distribution():
    """Test rozłożenia connection points"""
    logger.info("="*80)
    logger.info("START TESTU ROZŁOŻENIA CONNECTION POINTS")
    logger.info("="*80)
    
    driver = None
    ws_client = None
    
    try:
        # 1. Setup Selenium
        logger.info("\n[1/5] Konfiguracja Selenium Chrome...")
        driver = setup_driver()
        timestamp = int(time.time())
        driver.get(f'http://localhost:8080/?nocache={timestamp}&t={timestamp}&v={timestamp}')
        time.sleep(10)
        
        # 2. Czekaj na inicjalizację
        logger.info("\n[2/5] Oczekiwanie na inicjalizację aplikacji...")
        if not wait_for_app_initialization(driver):
            logger.error("❌ Aplikacja nie zainicjalizowała się")
            return False
        
        driver.execute_script('if (window.app && window.app.switchToVisualMode) { window.app.switchToVisualMode(); }')
        time.sleep(3)
        
        # 3. Załaduj konfigurację z wieloma połączeniami do tego samego węzła
        logger.info("\n[3/5] Ładowanie konfiguracji z wieloma połączeniami...")
        config_xml = '''<config xmlns="http://org.webharvest/schema/2.1/core">
            <def var="url1">https://httpbin.org/html</def>
            <def var="html1"><http url="${url1}"/></def>
            <def var="url2">https://httpbin.org/json</def>
            <def var="json1"><http url="${url2}"/></def>
            <def var="url3">https://httpbin.org/xml</def>
            <def var="xml1"><http url="${url3}"/></def>
            <def var="url4">https://httpbin.org/uuid</def>
            <def var="uuid1"><http url="${url4}"/></def>
            <def var="combined">
                <get var="html1"/>
                <get var="json1"/>
                <get var="xml1"/>
                <get var="uuid1"/>
            </def>
        </config>'''
        load_config_via_selenium(driver, config_xml)
        
        # 4. Pobierz rozłożenie connection points
        logger.info("\n[4/5] Analiza rozłożenia connection points...")
        distribution = get_connection_points_distribution(driver)
        
        if not distribution:
            logger.error("❌ Nie udało się pobrać informacji o rozłożeniu")
            return False
        
        # 5. Połącz przez WebSocket i sprawdź model
        logger.info("\n[5/5] Pobieranie modelu przez WebSocket...")
        ws_client = WebSocketClient('ws://localhost:8080/ws/visual/model?clientType=agent')
        
        if ws_client.connect():
            time.sleep(5)
            
            # Pobierz model przez WebSocket
            start = time.time()
            while time.time() - start < 15:
                for msg in reversed(ws_client.received_messages):
                    msg_type = msg.get('type', '')
                    if msg_type in ['modelUpdate', 'modelSnapshot', 'model']:
                        model_data = msg.get('model') or msg.get('data') or msg.get('workflow') or msg
                        if isinstance(model_data, dict) and 'workflow' in model_data:
                            logger.info("✅ Model otrzymany przez WebSocket")
                            break
                time.sleep(0.2)
        
        # Analiza wyników
        logger.info("\n" + "="*80)
        logger.info("WYNIKI ANALIZY ROZŁOŻENIA CONNECTION POINTS")
        logger.info("="*80)
        
        dist_check = distribution.get('distributionCheck', {})
        nodes_with_multiple_outputs = dist_check.get('nodesWithMultipleOutputs', {})
        nodes_with_multiple_inputs = dist_check.get('nodesWithMultipleInputs', {})
        conflicts = dist_check.get('conflicts', [])
        
        logger.info(f"\nWęzły z wieloma połączeniami wychodzącymi: {len(nodes_with_multiple_outputs)}")
        for node_id, info in nodes_with_multiple_outputs.items():
            logger.info(f"  Node {node_id}:")
            logger.info(f"    Liczba połączeń: {info.get('connectionCount', 0)}")
            logger.info(f"    Unikalne point IDs: {info.get('uniquePointIds', [])}")
            logger.info(f"    Unikalne strony: {info.get('uniqueSides', [])}")
            logger.info(f"    Wszystkie punkty różne: {'✅' if info.get('allPointsDifferent') else '❌'}")
            logger.info(f"    Wszystkie strony różne: {'✅' if info.get('allSidesDifferent') else '❌'}")
            for point in info.get('points', []):
                logger.info(f"      - {point.get('connectionId')}: pointId={point.get('pointId')}, side={point.get('side')}")
        
        logger.info(f"\nWęzły z wieloma połączeniami przychodzącymi: {len(nodes_with_multiple_inputs)}")
        for node_id, info in nodes_with_multiple_inputs.items():
            logger.info(f"  Node {node_id}:")
            logger.info(f"    Liczba połączeń: {info.get('connectionCount', 0)}")
            logger.info(f"    Unikalne point IDs: {info.get('uniquePointIds', [])}")
            logger.info(f"    Unikalne strony: {info.get('uniqueSides', [])}")
            logger.info(f"    Wszystkie punkty różne: {'✅' if info.get('allPointsDifferent') else '❌'}")
            logger.info(f"    Wszystkie strony różne: {'✅' if info.get('allSidesDifferent') else '❌'}")
            for point in info.get('points', []):
                logger.info(f"      - {point.get('connectionId')}: pointId={point.get('pointId')}, side={point.get('side')}")
        
        if conflicts:
            logger.warning(f"\n⚠️ Znaleziono {len(conflicts)} konfliktów:")
            for conflict in conflicts:
                logger.warning(f"  {conflict.get('type')} dla node {conflict.get('nodeId')}: {conflict.get('issue')}")
        else:
            logger.info("\n✅ Brak konfliktów - wszystkie połączenia używają różnych punktów!")
        
        logger.info("\n" + "="*80)
        logger.info("PODSUMOWANIE")
        logger.info("="*80)
        logger.info(f"Łączna liczba połączeń: {distribution.get('connectionsCount', 0)}")
        logger.info(f"Węzły z wieloma outputs: {len(nodes_with_multiple_outputs)}")
        logger.info(f"Węzły z wieloma inputs: {len(nodes_with_multiple_inputs)}")
        logger.info(f"Konflikty: {len(conflicts)}")
        logger.info(f"Status: {'✅ PASS' if len(conflicts) == 0 else '❌ FAIL'}")
        logger.info("="*80)
        
        return len(conflicts) == 0
        
    except Exception as e:
        logger.error(f"❌ Błąd podczas testu: {e}", exc_info=True)
        return False
        
    finally:
        logger.info("\nBrowser pozostaje otwarty. Naciśnij Enter aby zamknąć...")
        try:
            input()
        except EOFError:
            pass
        if driver:
            driver.quit()
        if ws_client:
            ws_client.disconnect()

if __name__ == '__main__':
    success = test_connection_points_distribution()
    sys.exit(0 if success else 1)





