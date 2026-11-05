#!/usr/bin/env python3
"""
Kompleksowy test renderowania połączeń z Selenium Chrome i WebSockets
Sprawdza:
1. Renderowanie wizualne w przeglądarce (Selenium)
2. Model semantyczny przez WebSockets
3. Korelację między modelem a renderowaniem
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
logger = logging.getLogger('ConnectionRenderingTest')

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
            # Uruchom w osobnym wątku
            wst = threading.Thread(target=self.ws.run_forever)
            wst.daemon = True
            wst.start()
            
            # Czekaj na połączenie
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
        """Callback przy otwarciu połączenia"""
        self.connected = True
        logger.info("WebSocket: Połączenie otwarte")
    
    def _on_message(self, ws, message):
        """Callback przy otrzymaniu wiadomości"""
        try:
            data = json.loads(message)
            self.received_messages.append(data)
            msg_type = data.get('type') or data.get('action', {}).get('type') if isinstance(data.get('action'), dict) else 'unknown'
            logger.info(f"WebSocket: Otrzymano wiadomość typu: {msg_type}")
            if msg_type == 'error':
                logger.error(f"WebSocket: Błąd: {data.get('error', 'Unknown error')}")
            else:
                logger.debug(f"WebSocket: Pełna wiadomość: {json.dumps(data, indent=2)[:500]}")
            if self.on_message:
                self.on_message(data)
        except json.JSONDecodeError as e:
            logger.warning(f"WebSocket: Nie można sparsować JSON: {e}, message: {message[:200]}")
    
    def _on_error(self, ws, error):
        """Callback przy błędzie"""
        logger.error(f"WebSocket: Błąd: {error}")
    
    def _on_close(self, ws, close_status_code, close_msg):
        """Callback przy zamknięciu"""
        self.connected = False
        logger.info("WebSocket: Połączenie zamknięte")
    
    def send(self, message):
        """Wyślij wiadomość"""
        if self.ws and self.connected:
            self.ws.send(json.dumps(message))
            logger.debug(f"WebSocket: Wysłano: {message.get('action', 'unknown')}")
        else:
            logger.warning("WebSocket: Nie można wysłać - brak połączenia")
    
    def disconnect(self):
        """Rozłącz"""
        if self.ws:
            self.ws.close()
            self.connected = False

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
                
                // Upewnij się, że model manager jest zaktualizowany
                if (designer.modelManager) {{
                    designer.modelManager.notifyUpdate();
                }}
            }}
        }}
    ''')
    time.sleep(5)
    logger.info("✅ Konfiguracja załadowana")

def get_semantic_model_via_selenium(driver):
    """Pobierz model semantyczny przez Selenium"""
    logger.info("Pobieranie modelu semantycznego przez Selenium...")
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
        
        // Konwertuj model na JSON-serializable format
        const connections = Array.from(model.workflow.connections.values()).map(c => ({
            connectionId: c.connectionId,
            fromNodeId: c.fromNodeId,
            toNodeId: c.toNodeId,
            type: c.type,
            pathPoints: c.path?.points || [],
            pathStartPoint: c.path?.points?.[0],
            pathEndPoint: c.path?.points?.[c.path.points?.length - 1],
            hasArrowMetadata: !!c.path?.arrowMetadata,
            routingType: c.path?.routingType
        }));
        
        const nodes = Array.from(model.workflow.allNodes.values()).map(n => ({
            nodeId: n.nodeId,
            type: n.type,
            position: n.position
        }));
        
        return {
            connectionsCount: connections.length,
            nodesCount: nodes.length,
            connections: connections,
            nodes: nodes,
            hasModel: true
        };
    ''')
    
    if result.get('error'):
        logger.error(f"❌ Błąd pobierania modelu: {result.get('error')}")
        return None
    
    logger.info(f"✅ Model pobrany: {result.get('connectionsCount', 0)} połączeń, {result.get('nodesCount', 0)} węzłów")
    return result

def get_rendered_connections_via_selenium(driver):
    """Pobierz informacje o renderowanych połączeniach przez Selenium"""
    logger.info("Pobieranie informacji o renderowanych połączeniach...")
    result = driver.execute_script('''
        const svg = document.querySelector('svg');
        if (!svg) {
            return {error: 'No SVG element'};
        }
        
        // Znajdź wszystkie ścieżki połączeń
        const paths = Array.from(svg.querySelectorAll('path[data-connection-id]'));
        
        const renderedConnections = paths.map(path => {
            const connectionId = path.getAttribute('data-connection-id');
            const pathData = path.getAttribute('d');
            
            // Parsuj path data - znajdź pierwszy i ostatni punkt
            let startPoint = null;
            let endPoint = null;
            if (pathData) {
                // Regex dla M (move) i L (line) commands
                const matches = pathData.match(/[ML]\\s*([\\d.]+),([\\d.]+)/g);
                if (matches && matches.length > 0) {
                    const startMatch = matches[0].match(/[ML]\\s*([\\d.]+),([\\d.]+)/);
                    if (startMatch) {
                        startPoint = {x: parseFloat(startMatch[1]), y: parseFloat(startMatch[2])};
                    }
                    if (matches.length > 1) {
                        const endMatch = matches[matches.length - 1].match(/[ML]\\s*([\\d.]+),([\\d.]+)/);
                        if (endMatch) {
                            endPoint = {x: parseFloat(endMatch[1]), y: parseFloat(endMatch[2])};
                        }
                    }
                }
            }
            
            return {
                connectionId: connectionId,
                hasPath: true,
                pathData: pathData,
                startPoint: startPoint,
                endPoint: endPoint,
                pathLength: pathData ? pathData.length : 0
            };
        });
        
        // Znajdź strzałki
        const arrows = Array.from(svg.querySelectorAll('polygon[data-connection-id], path[data-connection-id]'));
        const hasMarkers = svg.querySelector('defs marker') !== null;
        
        return {
            pathsCount: paths.length,
            arrowsCount: arrows.length,
            hasMarkers: hasMarkers,
            renderedConnections: renderedConnections
        };
    ''')
    
    if result.get('error'):
        logger.error(f"❌ Błąd pobierania renderowanych połączeń: {result.get('error')}")
        return None
    
    logger.info(f"✅ Renderowanie: {result.get('pathsCount', 0)} ścieżek, {result.get('arrowsCount', 0)} strzałek")
    return result

def get_model_via_websocket(ws_client, timeout=15):
    """Pobierz model semantyczny przez WebSocket"""
    logger.info("Pobieranie modelu przez WebSocket...")
    
    # Backend wymaga, aby frontend najpierw wysłał modelUpdate
    # Agent otrzyma model snapshot po połączeniu, jeśli frontend już wysłał model
    # Czekaj na odpowiedź - backend może wysłać model automatycznie lub po żądaniu
    start = time.time()
    while time.time() - start < timeout:
        # Sprawdź ostatnie wiadomości
        for msg in reversed(ws_client.received_messages):
            msg_type = msg.get('type', '')
            logger.debug(f"Sprawdzam wiadomość typu: {msg_type}")
            
            if msg_type in ['modelUpdate', 'modelSnapshot', 'model']:
                logger.info("✅ Model otrzymany przez WebSocket")
                # Model może być w różnych miejscach w zależności od formatu odpowiedzi
                model_data = msg.get('model') or msg.get('data') or msg.get('workflow') or msg
                if isinstance(model_data, dict) and 'workflow' in model_data:
                    return model_data
                elif isinstance(model_data, dict):
                    return model_data
                else:
                    return msg
        time.sleep(0.2)
    
    logger.warning("⚠️ Timeout oczekiwania na model przez WebSocket")
    logger.info(f"Otrzymane wiadomości: {len(ws_client.received_messages)}")
    if ws_client.received_messages:
        logger.info(f"Typy wiadomości: {[m.get('type', 'unknown') for m in ws_client.received_messages]}")
        logger.debug(f"Ostatnia wiadomość: {json.dumps(ws_client.received_messages[-1], indent=2)[:500]}")
    return None

def compare_model_with_rendering(semantic_model, rendered_connections):
    """Porównaj model semantyczny z renderowaniem"""
    logger.info("Porównywanie modelu semantycznego z renderowaniem...")
    
    if not semantic_model or not rendered_connections:
        logger.error("❌ Brak danych do porównania")
        return False
    
    model_connections = semantic_model.get('connections', [])
    rendered = rendered_connections.get('renderedConnections', [])
    
    # Stwórz mapę connectionId -> rendered connection
    rendered_map = {r.get('connectionId'): r for r in rendered}
    
    all_match = True
    matches_count = 0
    
    for model_conn in model_connections:
        conn_id = model_conn.get('connectionId')
        rendered_conn = rendered_map.get(conn_id)
        
        if not rendered_conn:
            logger.warning(f"⚠️ Połączenie {conn_id} nie jest renderowane w SVG")
            all_match = False
            continue
        
        # Porównaj start point
        model_start = model_conn.get('pathStartPoint')
        rendered_start = rendered_conn.get('startPoint')
        
        start_match = True
        if model_start and rendered_start:
            diff_x = abs(model_start.get('x', 0) - rendered_start.get('x', 0))
            diff_y = abs(model_start.get('y', 0) - rendered_start.get('y', 0))
            if diff_x > 1 or diff_y > 1:
                logger.warning(f"⚠️ Połączenie {conn_id}: Start point nie pasuje (diff: X={diff_x:.2f}px, Y={diff_y:.2f}px)")
                start_match = False
                all_match = False
        
        # Porównaj end point
        model_end = model_conn.get('pathEndPoint')
        rendered_end = rendered_conn.get('endPoint')
        
        end_match = True
        if model_end and rendered_end:
            diff_x = abs(model_end.get('x', 0) - rendered_end.get('x', 0))
            diff_y = abs(model_end.get('y', 0) - rendered_end.get('y', 0))
            if diff_x > 1 or diff_y > 1:
                logger.warning(f"⚠️ Połączenie {conn_id}: End point nie pasuje (diff: X={diff_x:.2f}px, Y={diff_y:.2f}px)")
                end_match = False
                all_match = False
        
        if start_match and end_match:
            matches_count += 1
            logger.debug(f"✅ Połączenie {conn_id}: Model i renderowanie pasują")
    
    logger.info(f"Wynik porównania: {matches_count}/{len(model_connections)} połączeń pasuje")
    return all_match and matches_count == len(model_connections)

def test_connection_rendering_complete():
    """Kompleksowy test renderowania połączeń"""
    logger.info("="*80)
    logger.info("START TESTU RENDEROWANIA POŁĄCZEŃ")
    logger.info("="*80)
    
    driver = None
    ws_client = None
    
    try:
        # 1. Setup Selenium
        logger.info("\n[1/6] Konfiguracja Selenium Chrome...")
        driver = setup_driver()
        timestamp = int(time.time())
        driver.get(f'http://localhost:8080/?nocache={timestamp}&t={timestamp}&v={timestamp}')
        time.sleep(10)
        
        # 2. Czekaj na inicjalizację
        logger.info("\n[2/6] Oczekiwanie na inicjalizację aplikacji...")
        if not wait_for_app_initialization(driver):
            logger.error("❌ Aplikacja nie zainicjalizowała się")
            return False
        
        # Przełącz na tryb wizualny
        driver.execute_script('if (window.app && window.app.switchToVisualMode) { window.app.switchToVisualMode(); }')
        time.sleep(3)
        
        # 3. Załaduj konfigurację
        logger.info("\n[3/6] Ładowanie konfiguracji...")
        config_xml = '''<config xmlns="http://org.webharvest/schema/2.1/core">
            <def var="url1">https://httpbin.org/html</def>
            <def var="html1"><http url="${url1}"/></def>
            <def var="url2">https://httpbin.org/json</def>
            <def var="json1"><http url="${url2}"/></def>
        </config>'''
        load_config_via_selenium(driver, config_xml)
        
        # 4. Pobierz model przez Selenium
        logger.info("\n[4/6] Pobieranie modelu semantycznego przez Selenium...")
        semantic_model_selenium = get_semantic_model_via_selenium(driver)
        
        # 5. Pobierz renderowane połączenia
        logger.info("\n[5/6] Pobieranie informacji o renderowaniu...")
        rendered_connections = get_rendered_connections_via_selenium(driver)
        
        # 6. Połącz przez WebSocket i pobierz model
        logger.info("\n[6/6] Pobieranie modelu przez WebSocket...")
        ws_client = WebSocketClient('ws://localhost:8080/ws/visual/model?clientType=agent')
        
        if ws_client.connect():
            # Czekaj chwilę na synchronizację - frontend powinien wysłać modelUpdate
            logger.info("Czekam na synchronizację modelu (frontend -> backend)...")
            time.sleep(5)  # Daj czas frontendowi na wysłanie modelUpdate
            
            # Pobierz model przez WebSocket
            semantic_model_websocket = get_model_via_websocket(ws_client)
            
            if semantic_model_websocket:
                logger.info("✅ Model otrzymany przez WebSocket")
                # Porównaj z modelem z Selenium
                # Model może mieć różną strukturę - sprawdź różne możliwości
                ws_workflow = semantic_model_websocket.get('workflow') if isinstance(semantic_model_websocket, dict) else semantic_model_websocket
                if isinstance(ws_workflow, dict):
                    ws_connections = ws_workflow.get('connections', {})
                    if isinstance(ws_connections, dict):
                        ws_connections = list(ws_connections.values()) if ws_connections else []
                    elif isinstance(ws_connections, list):
                        pass  # już lista
                    else:
                        ws_connections = []
                else:
                    ws_connections = []
                
                selenium_connections = semantic_model_selenium.get('connections', []) if semantic_model_selenium else []
                
                logger.info(f"WebSocket: {len(ws_connections)} połączeń")
                logger.info(f"Selenium: {len(selenium_connections)} połączeń")
                
                if len(ws_connections) == len(selenium_connections):
                    logger.info("✅ Liczba połączeń się zgadza między WebSocket a Selenium")
                else:
                    logger.warning(f"⚠️ Różnica w liczbie połączeń: WebSocket={len(ws_connections)}, Selenium={len(selenium_connections)}")
            else:
                logger.warning("⚠️ Nie udało się pobrać modelu przez WebSocket - kontynuuję z testem Selenium")
        
        # 7. Porównaj model z renderowaniem
        logger.info("\n[7/7] Porównywanie modelu z renderowaniem...")
        comparison_result = compare_model_with_rendering(semantic_model_selenium, rendered_connections)
        
        # Podsumowanie
        logger.info("\n" + "="*80)
        logger.info("PODSUMOWANIE TESTU")
        logger.info("="*80)
        logger.info(f"Model semantyczny (Selenium): {semantic_model_selenium.get('connectionsCount', 0) if semantic_model_selenium else 0} połączeń")
        logger.info(f"Renderowanie (SVG): {rendered_connections.get('pathsCount', 0) if rendered_connections else 0} ścieżek")
        logger.info(f"Korelacja model-renderowanie: {'✅ PASS' if comparison_result else '❌ FAIL'}")
        logger.info("="*80)
        
        return comparison_result
        
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
    success = test_connection_rendering_complete()
    sys.exit(0 if success else 1)

