> **Archived historical manual — original mixed-language source.** This document preserves the former root-level WebHarvest IDE v2.3 user guide as a historical record. It contains Polish prose and version-specific UI, API, session, token-tracking and Visual Designer claims that have not been revalidated against the current implementation. Do not use it as current operational guidance. The original content is preserved verbatim for traceability; a current English user manual will be published only after the IDE workflows and release status have been verified.
>
> Original source: `USER_GUIDE.md`. Historical version label: v2.3 / v2.3.1. The version label is not proof of a published release.

---

# WebHarvest User Guide

**Modern Web Scraping & Data Extraction Environment v2.3**

---

## 📖 Spis Treści

1. [Wprowadzenie](#wprowadzenie)
2. [Uruchomienie IDE](#uruchomienie-ide)
3. [Interfejs Użytkownika](#interfejs-użytkownika)
4. [Praca z Zakładkami](#praca-z-zakładkami)
5. [Edytor XML](#edytor-xml)
6. [Visual Designer (v2.3)](#visual-designer-v23)
7. [Wykonywanie Konfiguracji](#wykonywanie-konfiguracji)
8. [Panel Wyników](#panel-wyników)
9. [Session Management (v2.2)](#session-management-v22)
10. [Token Tracking (v2.2)](#token-tracking-v22)
11. [Skróty Klawiszowe](#skróty-klawiszowe)
12. [Przykłady Użycia](#przykłady-użycia)
13. [Rozwiązywanie Problemów](#rozwiązywanie-problemów)

---

## Wprowadzenie

Web-Harvest IDE to nowoczesne, webowe środowisko programistyczne do:
- **Tworzenia** konfiguracji XML dla web scrapingu
- **Testowania** i debugowania scraperów
- **Monitorowania** wykonania w czasie rzeczywistym
- **Analizowania** wyników i metryk (v2.2)

### Nowości w v2.3

🆕 **Visual Designer** - drag-and-drop edytor konfiguracji  
🆕 **Zmiana kolejności pluginów** - przyciski ⬆️⬇️ w kontenerach  
🆕 **Zagnieżdżone kontenery** - wielopoziomowa hierarchia wizualna  
🆕 **Semantic validation** - automatyczna walidacja poprawności  
🆕 **Nested drop targets** - inteligentne upuszczanie do zagnieżdżonych kontenerów  

### Nowości w v2.2

✨ **Per-Tab State** - każda zakładka ma własne logi i wyniki (Apple HIG)  
✨ **Session Management** - śledzenie sesji z UUID  
✨ **Token Tracking** - monitoring zużycia zasobów  
✨ **Real-time Metrics** - czas wykonania, elementy, rate  
✨ **Multi-Tab Execution** - równoległe uruchamianie konfiguracji  

---

## Uruchomienie IDE

### Wymagania

- **Java**: 11 lub nowsza
- **RAM**: Min. 512MB (zalecane: 1GB+)
- **Przeglądarka**: Chrome, Firefox, Edge, Safari (najnowsza wersja)

### Start

```bash
# Uruchomienie IDE
java -jar webharvest-ide-2.3.1.jar

# IDE startuje na porcie 8080
# Otwórz w przeglądarce: http://localhost:8080
```

### Własny Port

```bash
# Uruchomienie na innym porcie (np. 9000)
java -Dport=9000 -jar webharvest-ide-2.3.1.jar
```

### Zatrzymanie

```bash
# Linux/Mac
pkill -f "webharvest-ide"

# Windows
# Ctrl+C w terminalu lub zamknij okno
```

---

## Interfejs Użytkownika

### Layout

```
┌────────────────────────────────────────────────────────┐
│  [🌐 Web-Harvest IDE]  [New] [Open] [Save]            │ ← Navbar
├────────────────────────────────────────────────────────┤
│  [Tab 1] [Tab 2] [Tab 3] ...                 [+]      │ ← Tab Bar
├──────────────────────────┬─────────────────────────────┤
│                          │                             │
│  XML Editor (Monaco)     │   Execution Results         │
│  - Syntax highlighting   │   [Output│Variables│Log│    │
│  - Auto-complete         │    Session]                 │
│  - Error detection       │                             │
│                          │   • Output preview          │
│  [Run] [Validate]        │   • Variables list          │
│                          │   • Execution log           │
│                          │   • Session info (v2.2)     │
│                          │                             │
└──────────────────────────┴─────────────────────────────┘
│  Status: Ready  │  Ln 1, Col 1  │  XML  │  UTF-8     │ ← Status Bar
└────────────────────────────────────────────────────────┘
```

### Komponenty

#### 1. Navbar (Górny pasek)
- **Logo + Nazwa** - branding projektu
- **New Tab** - nowa zakładka z pustą konfiguracją
- **Open** - otwieranie pliku XML (WIP)
- **Save** - zapis konfiguracji (WIP)

#### 2. Tab Bar (Pasek zakładek)
- **Zakładki** - każda reprezentuje jedną konfigurację XML
- **Przycisk [+]** - szybkie dodawanie nowej zakładki
- **Przycisk [×]** - zamykanie zakładek (z potwierdzeniem jeśli zmienione)
- **Kropka (•)** - oznaczenie niezapisanych zmian

#### 3. Editor Panel (Lewy panel)
- **Monaco Editor** - ten sam silnik co w VS Code
- **Syntax Highlighting** - kolorowanie składni XML
- **Line Numbers** - numeracja linii
- **Minimap** - mapa kodu (prawy margines)
- **Actions** - przyciski Run i Validate

#### 4. Results Panel (Prawy panel)
- **Zakładki wyników**: Output, Variables, Log, Session
- **Progress Bar** - pasek postępu wykonania
- **Execution Controls** - Pause/Stop podczas wykonania

#### 5. Status Bar (Dolny pasek)
- **Status Message** - aktualny stan (Ready, Running, Completed, etc.)
- **Cursor Position** - pozycja kursora (Ln X, Col Y)
- **Language** - XML
- **Encoding** - UTF-8

---

## Praca z Zakładkami

### Tworzenie Zakładek

**Metoda 1: Przycisk [+]**
```
Kliknij przycisk [+] w tab bar
```

**Metoda 2: Skrót klawiszowy**
```
Ctrl+T  (Windows/Linux)
Cmd+T   (Mac)
```

**Metoda 3: Menu**
```
Kliknij [New Tab] w navbar
```

### Przełączanie Zakładek

**Myszką:**
```
Kliknij na nazwę zakładki
```

**Wynik:**
- ✅ Edytor ładuje XML z tej zakładki
- ✅ **Panel wyników pokazuje LOGI tej zakładki** (v2.2!)
- ✅ **Wyniki są zachowane per-tab** (v2.2!)

### Zamykanie Zakładek

**Przycisk [×]:**
```
Kliknij [×] na zakładce → Potwierdź jeśli są zmiany
```

**Zachowanie:**
- Jeśli zakładka ma **kropkę (•)** = niezapisane zmiany
- IDE pyta: "Close anyway?" przed zamknięciem
- Po zamknięciu wszystkich → auto-tworzy nową pustą

### 🆕 v2.2: Per-Tab State (Apple HIG)

**WAŻNE**: Każda zakładka to **niezależny dokument**!

```
Tab 1: Scrapes Wikipedia
  ├─ Własne logi ✅
  ├─ Własne wyniki ✅
  └─ Własna session info ✅

Tab 2: Scrapes GitHub API
  ├─ Własne logi ✅
  ├─ Własne wyniki ✅
  └─ Własna session info ✅

Switching tabs → Restores COMPLETE state!
```

**Korzyści:**
- ✅ Zachowanie historii wykonania per-tab
- ✅ Możliwość porównania wyników między tabami
- ✅ Równoległe uruchamianie konfiguracji
- ✅ Brak gubienia logów przy przełączaniu

---

## Edytor XML

### Monaco Editor

IDE używa **Monaco Editor** - tego samego silnika co Visual Studio Code!

**Funkcje:**
- ✅ **Syntax Highlighting** - kolorowanie XML
- ✅ **Auto-Indentation** - automatyczne wcięcia
- ✅ **Bracket Matching** - dopasowywanie nawiasów <>
- ✅ **Line Numbers** - numeracja linii
- ✅ **Minimap** - nawigacja po kodzie
- ✅ **Word Wrap** - zawijanie długich linii
- ✅ **Find & Replace** - wyszukiwanie (Ctrl+F)

### Podstawowa Edycja

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    
    <!-- Define variable -->
    <def var="url">https://example.com</def>
    
    <!-- HTTP request -->
    <def var="html">
        <http url="${url}"/>
    </def>
    
    <!-- XPath extraction -->
    <def var="title">
        <xpath expression="//title/text()">
            <html-to-xml>
                <get var="html"/>
            </html-to-xml>
        </xpath>
    </def>
    
</config>
```

### Auto-Save

**Każda zmiana w edytorze jest automatycznie zapisywana w zakładce!**

- Edytujesz kod → Zakładka dostaje kropkę (•)
- Przełączasz tab → Kod zostaje zachowany
- Wracasz do tab → Widzisz dokładnie to co edytowałeś

### Kursor Position

Status bar pokazuje aktualną pozycję:
```
Ln 15, Col 23    ← Linia 15, Kolumna 23
```

---

## Wykonywanie Konfiguracji

### Uruchomienie

**Metoda 1: Przycisk Run**
```
Kliknij [Run] w editor panel
```

**Metoda 2: Skrót klawiszowy**
```
Ctrl+Enter  (Windows/Linux)
Cmd+Enter   (Mac)
```

### Proces Wykonania

```
1. IDE wysyła XML do backendu
   Status: "Connecting..."

2. Backend tworzy Execution Job
   Generuje: Execution ID, Session ID

3. WebSocket connection
   Status: "Running..."
   Progress Bar: 0% → 100%

4. Real-time log updates
   Logi w zakładce Log

5. Completion
   Status: "Completed!"
   Results → Output/Variables tabs (execution telemetry moves to Run & Test → History)
```

### Progress Tracking

**Progress Bar:**
```
[████████████████    ] 75%
```

**Log Messages:**
```
[10:15:23] ▶ Preparing to execute: config.xml
[10:15:24] ⚙ Execution ID: a7b3c4d5...
[10:15:24] 🔌 Creating new WebSocket connection...
[10:15:25] ✓ WebSocket connected, execution starting...
[10:15:27] ⚙ Processing HTTP request...
[10:15:28] ⚙ Extracting data with XPath...
[10:15:29] ✓ Execution finished!
[10:15:29] ✓ Extracted 12 variables
[10:15:29] SUCCESS Results sent to browser!
```

### Execution Controls

**Podczas wykonania dostępne:**

```
[Pause]  - Wstrzymanie (WIP dla v2.3)
[Stop]   - Przerwanie natychmiastowe
```

**Po zakończeniu:**
- Controls ukryte
- WebSocket zamknięty
- Wyniki dostępne w tabsach

---

## Panel Wyników

### Zakładki Wyników

#### 1. Output

**Co pokazuje:**
- Główny wynik scraping (zmienna `configResult`)
- Lub pierwsza istotna zmienna (html, response, data)
- Lub pierwszy element z results

**Format:**
```
┌─[○○○]─ output.txt ─────────────────┐
│                                     │
│  Title: Example Domain              │
│  Content: ...                       │
│                                     │
└─────────────────────────────────────┘
```

**Code Window:**
- Symuluje terminal/edytor
- Kontrolki (○○○) jak w macOS
- Mono-spaced font
- Scroll dla długich wyników

#### 2. Variables

**Co pokazuje:**
- Wszystkie zdefiniowane zmienne w konfiguracji
- Format: Nazwa → Wartość

**Layout:**
```
┌──────────────────────────┐
│  url                     │
│  https://example.com     │
└──────────────────────────┘

┌──────────────────────────┐
│  html                    │
│  <!DOCTYPE html>...      │
│  (1234 chars, truncated) │
└──────────────────────────┘

┌──────────────────────────┐
│  title                   │
│  Example Domain          │
└──────────────────────────┘
```

**Truncation:**
- Wartości > 300 znaków → obcięte
- Pokazuje: `(N total chars, truncated)`

**Variable Cards:**
- Każda zmienna w osobnej karcie
- Nazwa pogrubiona (bold)
- Wartość w `<pre>` (zachowane białe znaki)
- Hover effect (podniesienie karty)

#### 3. Log

**Co pokazuje:**
- Chronologiczny dziennik wykonania
- Real-time updates przez WebSocket
- Persisted per-tab (v2.2!)

**Poziomy logów:**
```
[10:15:23] INFO    ▶ Preparing to execute...
[10:15:24] SUCCESS ✓ WebSocket connected
[10:15:25] WARNING ⚠ Retrying HTTP request...
[10:15:26] ERROR   ✗ Connection timeout
```

**Kolory:**
- 🔵 **INFO** - niebieski
- 🟢 **SUCCESS** - zielony
- 🟡 **WARNING** - żółty
- 🔴 **ERROR** - czerwony

**Auto-scroll:**
- Logi auto-scrollują do końca
- Nowe wiadomości zawsze widoczne

**Empty State:**
```
No logs yet. Click Run to execute this configuration.
```

#### 4. Session (🆕 v2.2)

**Co pokazuje:**
- Metadane sesji wykonania
- Token usage (zużycie zasobów)
- Metryki wydajności

**Layout:**

##### Session Information Card

```
╭────────────────────────────────────────╮
│ ℹ️  Session Information                │
├────────────────────────────────────────┤
│ Session ID:     a7b3c4d5...            │
│ Client ID:      ide-12345...           │
│ Status:         COMPLETED ✅           │
│ Duration:       2.34s                  │
│ Elements:       156                    │
│ Rate:           66.67 elem/s           │
╰────────────────────────────────────────╯
```

##### Token Usage Card

```
╭────────────────────────────────────────╮
│ 💰 Token Usage                         │
├────────────────────────────────────────┤
│ HTTP Requests:  5                      │
│ HTTP Bytes:     142.58 KB              │
│ CPU Time:       1250 ms                │
│ Memory Peak:    34.12 MB               │
╰────────────────────────────────────────╯
```

**Status Badges:**
- `PENDING` - żółty gradient
- `RUNNING` - niebieski gradient
- `COMPLETED` - zielony gradient
- `FAILED` - czerwony gradient
- `CANCELLED` - szary gradient

**Empty State:**
```
No session information available.
Session data will appear here after running a configuration.
```

---

## Session Management (v2.2)

### Co to jest Session?

**Session** = pojedyncze wykonanie konfiguracji XML

Każde kliknięcie [Run] tworzy nową sesję z:
- ✅ Unique ID (UUID)
- ✅ Client ID (IDE)
- ✅ Lifecycle tracking (PENDING → RUNNING → COMPLETED)
- ✅ Metrics (czas, elementy, rate)
- ✅ Token usage (zasoby)

### Lifecycle

```
PENDING   → Czeka w kolejce
    ↓
RUNNING   → Wykonuje się
    ↓
COMPLETED → Sukces!
FAILED    → Błąd
CANCELLED → Zatrzymane przez użytkownika
```

### Session ID

**Format:** UUID
```
a7b3c4d5-1234-5678-90ab-cdef12345678
```

**Wyświetlane:**
- W logach: `a7b3c4d5...` (skrócone)
- W Run & Test → History: pełne UUID i metryki sesji
- Backend: pełne UUID

**Cel:**
- Korelacja logów
- Debugowanie
- Późniejsze odtworzenie (WIP v2.3)

### Client ID

**Format:** `ide-{timestamp}-{random}`
```
ide-1728662400-a7b3c
```

**Generowany:**
- Przy starcie IDE
- Identyfikuje tę instancję IDE
- Persystowany w session storage (WIP)

**Cel:**
- Multi-tenant support
- Quota tracking (v2.3)
- User attribution

### Metrics

**Zbierane automatycznie:**

| Metryka | Opis | Jednostka |
|---------|------|-----------|
| Start Time | Kiedy rozpoczęto | ISO timestamp |
| End Time | Kiedy zakończono | ISO timestamp |
| Duration | Czas trwania | ms |
| Processed Elements | Ilość przetworzonych węzłów XML | liczba |
| Processing Rate | Elementy na sekundę | elem/s |

**Przykład:**
```
Duration:        2345 ms  (2.35s)
Elements:        156
Rate:            66.52 elem/s
```

---

## Token Tracking (v2.2)

### Co to są Tokeny?

**Token** = jednostka zużytego zasobu podczas scrapingu

Śledzone typy:
1. **HTTP_REQUEST** - liczba zapytań HTTP
2. **HTTP_BYTES** - pobrane bajty
3. **CPU_TIME** - czas procesora (ms)
4. **MEMORY_PEAK** - szczyt pamięci (bytes)

### Dlaczego Tokens?

- 📊 **Monitoring** - widzisz ile zasobów używasz
- 💰 **Billing** - fundament dla monetyzacji (v2.3)
- 🎯 **Optimization** - identyfikacja wąskich gardeł
- 🚦 **Quota** - limity zasobów (v2.3)

### Jak Są Zbierane?

**Automatycznie podczas wykonania:**

```
1. HTTP request:
   → +1 HTTP_REQUEST token
   → +N HTTP_BYTES tokens (rozmiar response)

2. Processor execution:
   → +T CPU_TIME tokens (czas wykonania)

3. Memory allocation:
   → Track MEMORY_PEAK (max używana pamięć)
```

**Lock-free tracking:**
- AtomicLong counters
- Thread-safe
- Zero overhead

### Wyświetlanie w UI

**Session Tab:**
```
💰 Token Usage
──────────────────────
HTTP Requests:  5
HTTP Bytes:     142.58 KB
CPU Time:       1250 ms
Memory Peak:    34.12 MB
```

**Interpretacja:**
- **5 HTTP Requests** = scraper wykonał 5 zapytań
- **142.58 KB** = pobrał łącznie 146 KB danych
- **1250 ms CPU** = procesor pracował 1.25s
- **34.12 MB Memory** = szczyt: 35.8 MB RAM

### Przyszłe Użycie (v2.3)

```
- Quota limits: "Max 100 HTTP requests/day"
- Billing: "0.001 USD per HTTP request"
- Throttling: "Max 10 req/s"
- Alerts: "Memory > 1GB → notify"
```

---

## Visual Designer (v2.3)

### Wprowadzenie

**Visual Designer** to nowy, wizualny edytor konfiguracji WebHarvest wprowadzony w wersji 2.3. Umożliwia tworzenie workflow'ów poprzez przeciąganie i upuszczanie pluginów na canvas zamiast pisania XML ręcznie.

### Przełączanie Trybów

W górnym pasku narzędzi znajdziesz przyciski **"XML"** i **"Visual"**.

**Jak przełączyć:**
1. Kliknij przycisk **"Visual"**, aby przełączyć na visual designer
2. Kliknij przycisk **"XML"**, aby wrócić do edytora kodu
3. IDE automatycznie synchronizuje zmiany między trybami

**Ważne:**
- Zmiany są zachowywane automatycznie
- XML jest generowany w czasie rzeczywistym
- Walidacja semantyczna chroni przed błędami

### Podstawowe Operacje

#### Dodawanie Pluginów

**Metoda 1: Drag & Drop z Palety**
1. Znajdź plugin w **Plugin Palette** po lewej stronie
2. **Kliknij i przeciągnij** plugin do obszaru Canvas
3. **Puść** mysz - węzeł pojawi się w miejscu upuszczenia
4. **Kliknij** węzeł, aby go wybrać

#### Wygląd kafelków pluginów

- Wszystkie pluginy (zarówno `def`, `empty`, `case`, `constant`, itp.) korzystają z **jednego spójnego wzorca kafelka** – bez dodatkowych ramek, etykiet roli czy kolorowych badge’y.
- Nagłówek wyświetla **tylko nazwę typu** (np. `constant`), natomiast właściwości pojawiają się poniżej w formie skróconych par `klucz: wartość` (pomijamy `name`/`id`, aby nie dublować tytułu).
- Kafelki zachowują jednolitą siatkę i marginesy, dzięki czemu pierwszy element między markerami Start/End nie ma już niebieskiej obwódki ani wyróżnień.
- Domyślna szerokość/wysokość kafelka wynosi teraz **320×160 px**, więc pasek metadanych `Children / Containers / Leaves` mieści się w jednej linii bez zawijania.
- **Ramka zaznaczenia = kafelek** – obszar podświetlenia z przycisku **Details** ma dokładnie takie same wymiary jak karta; podczas zmiany rozmiaru kafelka jednocześnie rośnie zarówno widoczna karta, jak i ramka zaznaczenia oraz punkty portów.
- Dzięki ujednoliceniu łatwiej porównywać węzły wzrokowo – różnice wynikają wyłącznie z ikon i treści konfiguracji, a nie z niestandardowych styli.

#### 🔧 Zmiana rozmiaru i wyróżnienia

1. **Zaznacz kartę** – kliknięcie powoduje, że cały kafelek rozświetla się na niebiesko (ten sam efekt pojawia się po wybraniu **Details** w panelu po prawej).
2. **Uchwyt = rzeczywisty obrys** – osiem kropek pojawia się dokładnie na krawędzi widocznej karty. Przeciągnięcie któregokolwiek uchwytu jednocześnie:
   - aktualizuje zewnętrzny kontener `.visual-node`,
   - dopasowuje wewnętrzną powierzchnię `.visual-node__surface` (czyli to, co widać jako biały kafelek),
   - przelicza porty i punkty zaczepienia połączeń.
3. **Minimalne wymiary** – po puszczeniu myszy karta nigdy nie będzie mniejsza niż **320 × 160 px**. Większe wartości są zapisywane w konfiguracji i odtwarzane przy każdej sesji.
4. **Czyszczenie zaznaczenia** – kliknięcie pustego miejsca na canvasie natychmiast usuwa niebieskie podświetlenie, dzięki czemu łatwo zobaczyć, które elementy biorą udział w wielokrotnym zaznaczeniu.

> Dzięki wspólnym wymiarom dla karty, obszaru zaznaczenia i portów skończyły się problemy z “unsyncowanym” highlightem (np. pierwsza karta między Start/Stop miała kiedyś dodatkową niebieską ramkę – teraz już nie).

**Metoda 2: Wyszukiwanie**
1. Wpisz nazwę plugin w **wyszukiwarce** (`[🔍]`)
2. Paleta się filtruje
3. Przeciągnij znaleziony plugin

#### Praca z Kontenerami

**Kontener** to plugin, który może zawierać inne pluginy jako dzieci (np. `try`, `catch`, `if`, `loop`, `while`).

**Jak dodać plugin do kontenera:**
1. Przeciągnij kontener na canvas
2. Rozwiń kontener (kliknij nagłówek), jeśli jest zwinięty
3. Przeciągnij kolejne pluginy **do środka** kontenera
4. Pluginy pojawią się jako dzieci

> **Uwaga (HTTP):** Węzeł `http` posiada własny panel z przyciskami „Add Header” / „Add Parameter”. `http-header` i `http-param` są ukryte w palecie – dodajesz je bezpośrednio z poziomu węzła HTTP (toolbar, menu kontekstowe lub panel właściwości), zgodnie z UX znanym z [Orkes Conductor HTTP task](https://orkes.io/content/reference-docs/system-tasks/http).

#### HTTP Task (nagłówki i parametry)

- Kliknij węzeł `http`, aby otworzyć dedykowany edytor (Method, URL, Body, Query Parameters, Headers)
- Sekcja kontenera pokazuje dwie listy („Query Parameters” i „Headers”) z podsumowaniem, przyciskami dodawania, usuwania oraz szybkiego przejścia do właściwości
- Menu kontekstowe węzła `http` zawiera opcje **Add Header** / **Add Query Parameter** (tworzą puste elementy, które wypełniasz w edytorze)
- Próba przeciągnięcia `http-header` na canvas wyświetli komunikat, że należy użyć panelu HTTP – nie jest to samodzielny kafelek na płótnie
- Interfejs został zestrojony z [Orkes Conductor HTTP task](https://orkes.io/content/reference-docs/system-tasks/http) dla znajomego przepływu konfiguracji

#### If/Else oraz Try/Catch

- Węzły `if` i `try` zawierają wbudowane karty z gałęziami `else` i `catch`
- Kliknij przycisk **Add Else** / **Add Catch**, aby utworzyć nową gałąź – kafelki detail nie są wystawione na palecie
- Każda gałąź ma własną strefę „Drop plugins here” – przeciągnij w nią elementy opisujące zachowanie ścieżki
- Przyciski na pasku kart umożliwiają otwarcie właściwości, usunięcie gałęzi oraz (dla `catch`) zmianę kolejności
- Drag & drop `else` lub `catch` poza kartą wyświetli wskazówkę z informacją, żeby użyć narzędzi dostępnych w węźle nadrzędnym

#### Zmiana Kolejności Pluginów ⬆️⬇️

Każdy plugin wewnątrz kontenera ma **przyciski strzałek** w prawym górnym rogu:

```
┌─────────────────────────────┐
│ 📦 log    ⬆️⬇️              │ ← Reorder buttons
└─────────────────────────────┘
```

**Instrukcja:**
1. Znajdź plugin w kontenerze
2. **Kliknij ⬆️**, aby przenieść w górę
3. **Kliknij ⬇️**, aby przenieść w dół
4. Przyciski są automatycznie wyłączane na granicach

**Przykład:**
```
Początkowa kolejność:
1. def var="x" value="1"
2. log message="X=${x}"
3. def var="y" value="2"

Po kliknięciu ⬆️ na "log":
1. log message="X=${x}"     ← Moved up!
2. def var="x" value="1"
3. def var="y" value="2"
```

#### Zagnieżdżone Kontenery

Visual Designer obsługuje **wielopoziomowe zagnieżdżenie**:

```
┌──────────────────────────────┐
│ 🛡️  try                      │ ← Root container
│ ┌──────────────────────────┐ │
│ │ ⚠️  catch               │ │ ← Nested container
│ │ ┌──────────────────────┐ │ │
│ │ │ ❓ if               │ │ │ ← Deeply nested
│ │ │ ┌──────────────────┐ │ │ │
│ │ │ │ 🌐 http         │ │ │ │ ← Child node
│ │ │ └──────────────────┘ │ │ │
│ │ └──────────────────────┘ │ │
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

**Jak działa:**
1. Przeciągnij plugin do obszaru kontenera
2. System wykrywa najgłębszy kontener na pozycji myszy
3. Plugin jest dodawany do właściwego kontenera
4. **Zielone podświetlenie** wskazuje prawidłowy target

#### Focus Mode (Drill-down)

**"Focus on this container"** pozwala wejść w kontener i pracować z jego zawartością na pełnym ekranie:

**Zachowanie w Focus Mode:**
- ✅ **Markery Start/Stop** – wyświetlają się jako "Start" i "Stop" (nie "workflow-start"/"workflow-end"), są interaktywne i można je przeciągać
- ✅ **Dodawanie pluginów** – możesz dodawać normalne pluginy (nie detail) do kontenera przez drag & drop z palety
- ✅ **Master/Detail** – jeśli kontener jest master/detail (np. `http`), detail nodes (`http-header`, `http-param`) **nie są widoczne w przepływie**. Zamiast tego pojawia się overlay `focused-detail-summary` z listą wszystkich detail items
- ✅ **Wygląd** – wszystkie pluginy wyglądają i zachowują się tak samo jak na poziomie głównym (unified styling)
- ✅ **Połączenia** – linie łączą tylko "prawdziwe" kroki workflow, nie detail nodes
- ✅ **Breadcrumbs** – na górze widoczna ścieżka nawigacji z przyciskiem "Back" do powrotu na poziom główny

**Przykład:**
```
Główny poziom:
┌─────────────────┐
│ 🌐 http         │ ← Kliknij "Focus on this container"
│   Headers: 2    │
│   Params: 1     │
└─────────────────┘

Focus mode (wewnątrz http):
┌─────────────────────────────────────┐
│ ← Back to root                      │
│                                     │
│ [Start]                             │
│                                     │
│ ┌──────────┐                        │
│ │ 📝 def   │                        │
│ └──────────┘                        │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ Detail items                    │ │ ← Overlay zamiast detail nodes
│ │ Headers: Authorization, Content │ │
│ │ Params: page                    │ │
│ └─────────────────────────────────┘ │
│                                     │
│ [Stop]                              │
└─────────────────────────────────────┘
```

### Master/detail pluginy

- **Paleta:** Pokazuje jedynie pluginy nadrzędne (`http`, `call`, `zip`, `ftp`, `xquery`, ...). Elementy zależne (`http-header`, `call-param`, `zip-entry`) są ukryte w palecie i tworzymy je z menu akcji na karcie nadrzędnej lub z panelu właściwości. Dzięki temu każdy poziom płótna (również wewnątrz *Focus mode*) wygląda identycznie i łatwo go będzie odwzorować np. w rozszerzeniu VS Code.
- **Inline summary:** Każda karta zarejestrowana w `MasterDetailRegistry` renderuje wewnątrz **Detail items** – listę detali z krótkim opisem (`children`, `containers`, `leaves` + nazwy szczegółów). Kliknięcie pozycji otwiera jej właściwości, a przycisk „Add …” dodaje nowy element dokładnie w tym kontenerze.
- **Edycja:** Edycja, duplikowanie, przenoszenie oraz kasowanie detali odbywa się w panelu **Properties** poprzez edytor schematowy (`MasterDetailEditor`). Obsługuje on wszystkie typy pól (tekst, number, URL, enum, checkbox, body), więc nie trzeba już szukać specjalnych formularzy w innych zakładkach.
- **Połączenia:** Linie i punkty połączeń nigdy nie zaczepiają się o karty detali – wszystkie anchor points znajdują się na pluginach nadrzędnych, co eliminuje „pływające” niebieskie obramowania i utrzymuje porządek wizualny.
- **Focus mode:** Gdy wchodzisz w kontener master/detail przez "Focus on this container", detail nodes **nie są widoczne w przepływie** – zamiast tego pojawia się overlay `focused-detail-summary` z listą wszystkich detail items. Możesz nadal dodawać normalne pluginy (nie detail) do kontenera, ale detail items zarządzasz wyłącznie przez panel właściwości lub inline summary.

### Semantic Validation

**Walidacja semantyczna** zapewnia, że pluginy są używane zgodnie ze swoim przeznaczeniem.

**Zasady walidacji:**

**1. Zależne Pluginy:**
- `catch` - **wymaga** rodzica `try`
- `ftp-put` - **wymaga** rodzica `ftp`
- `call-param` - **wymaga** rodzica `call`
- `http-header` - **wymaga** rodzica `http` (dodaj z panelu HTTP, a nie przez drag & drop)

**2. Wizualna Informacja Zwrotna:**
- **🟢 Zielony border** = dopuszczalne upuszczenie
- **🔴 Czerwony border** = niedopuszczalne upuszczenie
- **⚫ Szary plugin w palecie** = wymaga rodzica

**Przykład błędu:**
```
❌ BŁĄD: catch bez try
┌──────────────────┐
│ ⚠️  catch        │ ← Błąd! catch wymaga try
│ (empty)          │
└──────────────────┘

Toast: "❌ catch can only be used inside a try container"
```

### Generowanie XML

Visual Designer automatycznie generuje XML na podstawie wizualnej reprezentacji.

**Instrukcja:**
1. Edytuj workflow wizualnie na canvas
2. Przełącz na tryb **XML**
3. XML zostanie zaktualizowany zgodnie z węzłami

**Zachowanie:**
- Kolejność węzłów jest zachowywana
- Poziomy zagnieżdżenia są zachowywane
- Właściwości węzłów są zachowywane

### Skróty Klawiszowe (Visual Designer)

| Skrót | Akcja |
|-------|-------|
| **Ctrl+A** / **Cmd+A** | Select all |
| **Ctrl+C** / **Cmd+C** | Copy selected |
| **Ctrl+V** / **Cmd+V** | Paste |
| **Ctrl+Z** / **Cmd+Z** | Undo |
| **Ctrl+Y** / **Cmd+Y** | Redo |
| **Delete** / **Backspace** | Delete selected |
| **Ctrl+F** / **Cmd+F** | Search plugins |

### Rozwiązywanie Problemów

**Problem: Węzły nie pojawiają się**
- Sprawdź Console (F12) dla błędów
- Upewnij się, że drop jest nad Canvas
- Sprawdź walidację semantyczną

**Problem: Nie mogę zmienić kolejności**
- Upewnij się, że plugin jest wewnątrz kontenera
- Węzły główne nie mają przycisków
- Hard refresh (Cmd+Shift+R) jeśli cache

**Problem: Walidacja zawodzi niepotrzebnie**
- Sprawdź komunikat toast
- Upewnij się, że przeciągasz nad właściwym targetem
- Użyj zielonego podświetlenia jako wskazówki

**Problem: Canvas jest pusty**
- Dodaj węzły przeciągając z palety
- Sprawdź, czy XML jest prawidłowy
- Błędy parsowania są w Console (F12)

### Dokumentacja Techniczna

Aby uzyskać więcej informacji o Visual Designerze, zobacz:
- `VISUAL_DESIGNER_USER_GUIDE.md` - kompletny przewodnik użytkownika
- `SEMANTIC_METADATA_IMPLEMENTATION_COMPLETE.md` - implementacja techniczna
- `DEVELOPER_GUIDE.md` - dokumentacja dla deweloperów

---

## Skróty Klawiszowe

### Globalne

| Skrót | Akcja |
|-------|-------|
| `Ctrl+Enter` (Mac: `Cmd+Enter`) | Uruchom konfigurację |
| `Ctrl+S` (Mac: `Cmd+S`) | Zapisz konfigurację |
| `Ctrl+T` (Mac: `Cmd+T`) | Nowa zakładka |

### Edytor Monaco

| Skrót | Akcja |
|-------|-------|
| `Ctrl+F` | Find (wyszukiwanie) |
| `Ctrl+H` | Find & Replace |
| `Ctrl+G` | Go to Line |
| `Ctrl+/` | Toggle comment |
| `Ctrl+]` | Indent |
| `Ctrl+[` | Outdent |
| `Ctrl+Z` | Undo |
| `Ctrl+Y` | Redo |
| `Ctrl+D` | Add selection to next find match |
| `Alt+Up/Down` | Move line up/down |
| `Shift+Alt+Up/Down` | Copy line up/down |

### Przeglądarka

| Skrót | Akcja |
|-------|-------|
| `F5` | Refresh (reload) |
| `Ctrl+Shift+R` (Mac: `Cmd+Shift+R`) | Hard refresh (bypass cache) |
| `F12` | DevTools |

---

## Przykłady Użycia

### Biblioteka przykładów (v2.3.1)

Wszystkie przykładowe konfiguracje IDE znajdują się w `webharvest-ide/src/main/resources/webapp/examples/` _i_ są dostępne bezpośrednio z panelu **Examples** w Visual Designerze / edytorze XML.

- 📂 **Kategorie**  
  - _Core HTTP/XPath_ (`simple_test.xml`, `amazon_test.xml`, `functions.xml`)  
  - _Pipelines_ (`data_processing_pipeline.xml`, `social_media_analytics.xml`)  
  - _Symulacje usług zewnętrznych_ – FTP/Mail/Database zostały zmodernizowane tak, aby działały offline (np. `ftp_plugin_demo.xml`, `mail_plugin_demo.xml`, `database_example.xml`). Zamiast legacy pluginów używają JSON/XML i core'owych węzłów.
- ▶️ **Jak używać:**  
  1. Otwórz panel **Examples** → kliknij wybrany plik, aby załadować go do nowej zakładki.  
  2. Naciśnij **Validate** (lub `Ctrl/Cmd + Shift + V`), aby sprawdzić schemat. Wszystkie przykłady przechodzą `/api/config/validate`.  
  3. Naciśnij **Run** (`Ctrl/Cmd + Enter`), żeby uruchomić konfigurację – UI podświetli logi, a wyniki trafią do paneli Output/Variables.  
  4. Zapisz własną kopię (Save / Save As), jeśli chcesz zmodyfikować przykład.
- 📝 **Wskazówki:**  
  - Przykłady z prefiksem „simulated” (FTP/Mail/DB) generują realistyczne dane bez wykonywania połączeń sieciowych.  
  - Przed uruchomieniem kilku scenariuszy z rzędu skorzystaj z per-tab logów/history, aby porównać wyniki.

### Przykład 1: Simple HTTP + XPath

**Cel:** Pobrać tytuł strony

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    
    <def var="url">https://example.com</def>
    
    <def var="html">
        <http url="${url}"/>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <def var="title">
        <xpath expression="//title/text()">
            <get var="page"/>
        </xpath>
    </def>
    
</config>
```

**Wynik:**
- Output: `Example Domain`
- Variables: url, html, page, title
- Session: ~1s, 4 elements, 1 HTTP request

### Przykład 2: Multi-Tab Workflow

**Scenariusz:** Testowanie różnych selektorów

```
Tab 1: config-xpath.xml
  → Używa XPath: //div[@class='content']
  → Run → Zobacz wyniki

Tab 2: config-regex.xml
  → Używa Regex: <div class="content">(.*?)</div>
  → Run → Zobacz wyniki

Porównanie:
  Tab 1 Results: 15 elementów
  Tab 2 Results: 12 elementów
  → XPath lepszy!
```

**Korzyść v2.2:**
- Oba wyniki zachowane
- Możesz przełączać tabs i porównywać
- Nie tracisz logów

### Przykład 3: Debugging z Session Info

**Problem:** Scraper wolny

**Analiza Session Tab:**
```
Duration:     45.23s      ← Bardzo długo!
Elements:     25
Rate:         0.55 elem/s  ← Bardzo wolno!

Token Usage:
HTTP Requests:  50        ← Dużo requestów!
HTTP Bytes:     5.2 MB
```

**Diagnoza:**
- 50 HTTP requests dla 25 elementów = 2 req/element
- Możliwa przyczyna: scraper pobiera każdy element osobno

**Rozwiązanie:**
- Zoptymalizować: pobierz wszystko jednym requestem
- Użyj XPath do ekstrakcji wielu elementów naraz

---

## Rozwiązywanie Problemów

### Problem: Widzę starą wersję IDE

**Przyczyna:** Cache przeglądarki

**Rozwiązanie:**
```
1. Hard Refresh:
   - Chrome/Edge: Ctrl+Shift+R (Mac: Cmd+Shift+R)
   - Firefox: Ctrl+F5 (Mac: Cmd+Shift+R)
   - Safari: Cmd+Option+R

2. Lub otwórz z timestamp:
   http://localhost:8080?v=1728662400
```

### Problem: Logi znikają przy przełączeniu tab

**To już NIE jest problem w v2.2!**

- Każda zakładka ma własne logi
- Logi są persystowane w tab state
- Przełączenie tab → automatyczne restore

**Weryfikacja:**
1. Tab 1: Run config → Zobacz logi
2. Przełącz na Tab 2
3. Wróć do Tab 1 → **Logi nadal tam są!** ✅

### Problem: WebSocket disconnect

**Symptomy:**
```
⚠️ WebSocket disconnected: Connection lost
```

**Przyczyny:**
1. Timeout (wykonanie > 10 min)
2. Network error
3. Backend restart

**Rozwiązanie:**
1. Sprawdź backend logs
2. Zwiększ timeout (WIP v2.3)
3. Re-run konfigurację

### Problem: Session info nie wyświetla się

**Możliwe przyczyny:**

1. **Nie uruchomiono jeszcze:** Session info pojawia się TYLKO po wykonaniu
   - Rozwiązanie: Kliknij [Run] aby utworzyć sesję

2. **Stara wersja core:** Session API wymaga webharvest-core 2.2.0+
   - Rozwiązanie: Sprawdź `java -jar ... --version`

3. **Cache:** Stare pliki CSS/JS
   - Rozwiązanie: Hard refresh (Cmd+Shift+R)

### Problem: IDE nie startuje

**Error:** `Address already in use: 8080`

**Przyczyna:** Port zajęty

**Rozwiązanie:**
```bash
# 1. Kill stary proces
pkill -f "webharvest-ide"

# 2. Lub użyj innego portu
java -Dport=9000 -jar webharvest-ide-2.2.0.jar
```

### Problem: Validation fails

**Symptomy:**
```
✗ Validation failed: Element 'xxx' not allowed
```

**Przyczyny:**
1. Błąd składni XML
2. Niepoprawny namespace
3. Nieznany processor

**Debugowanie:**
1. Sprawdź namespace:
   ```xml
   <config xmlns="http://org.webharvest/schema/2.1/core">
   ```

2. Sprawdź czy procesor istnieje:
   - `<http>` ✅
   - `<htttp>` ❌ (typo!)

3. Sprawdź zamknięcie tagów:
   ```xml
   <def var="x">
       <http url="..."/>  ← Musi być zamknięty!
   </def>
   ```

---

## Najlepsze Praktyki

### 1. Organizacja Zakładek

```
✅ Dobre:
  - Tab 1: production-scraper.xml
  - Tab 2: test-selectors.xml
  - Tab 3: debug-xpath.xml

❌ Złe:
  - Tab 1: Untitled-1
  - Tab 2: Untitled-2
  - Tab 3: Untitled-3
```

**Tip:** Nadaj opisowe nazwy zakładkom

### 2. Sprawdzanie Session Info

**Po każdym wykonaniu:**
1. Sprawdź Duration - czy rozsądny?
2. Sprawdź Rate - czy wydajny?
3. Sprawdź HTTP Requests - czy nie za dużo?

**Przykład:**
```
Duration: 0.5s      ✅ Szybko
Rate:     100 el/s  ✅ Wydajnie
HTTP:     1 request ✅ Optymalne
```

vs

```
Duration: 30s       ❌ Wolno!
Rate:     0.3 el/s  ❌ Niewydajne!
HTTP:     100 req   ❌ Za dużo!
```

### 3. Monitorowanie Tokenów

**Śledź zużycie:**
- HTTP Bytes > 10 MB → możliwy scraping obrazów (duży transfer)
- HTTP Requests > 50 → możliwe po 1 req per element (nieoptymalne)
- CPU Time > Duration → możliwe heavy processing (regex, xslt)
- Memory Peak > 500 MB → możliwy memory leak

### 4. Backup Konfiguracji

**IDE nie zapisuje automatycznie na dysk!**

Regularnie:
1. Kopiuj XML z edytora
2. Zapisz lokalnie: `my-scraper.xml`
3. Commit do git (optional)

### 5. Testing Workflow

```
1. Nowa zakładka → Test
2. Small XML → Quick iteration
3. Validate → Check syntax
4. Run → Check results
5. Iterate → Fix issues
6. Run & Test → History → Check performance
7. Optimize → Reduce tokens
8. Production → Deploy
```

---

## Appendix A: Architecture

### Frontend

- **Monaco Editor** - VS Code editor engine
- **WebSocket** - real-time communication
- **Vanilla JS** - no framework overhead
- **CSS3** - modern styling with gradients

### Backend

- **Jetty 11** - embedded web server
- **Jakarta Servlet** - HTTP/WebSocket handling
- **webharvest-core 2.2** - scraping engine
- **Session API** - tracking & metrics

### Communication

```
Browser                         Backend
   │                               │
   ├─ POST /api/execute ──────────>│ Create job
   │                               │ Return execution ID
   │<───────────────────────────────┤
   │                               │
   ├─ WS /ws/execution?id=xxx ────>│ Connect WebSocket
   │                               │
   │<─── {status:"RUNNING"} ────────┤ Real-time updates
   │<─── {type:"log"} ──────────────┤ Log messages
   │<─── {status:"COMPLETED"} ──────┤ Final result
   │                               │
```

---

## Appendix B: File Structure

```
webharvest-ide/
├── src/main/
│   ├── java/
│   │   └── org/webharvest/server/
│   │       ├── WebHarvestServer.java     # Main entry point
│   │       ├── IDEClientContext.java     # Client ID generator
│   │       ├── api/
│   │       │   ├── ConfigServlet.java    # /api/config
│   │       │   ├── ExecutionServlet.java # /api/execute
│   │       │   └── ExecutionInfoServlet.java # /api/execution/{id}
│   │       └── manager/
│   │           ├── ExecutionManager.java # Job orchestration
│   │           ├── ExecutionJob.java     # Job state + logs
│   │           └── LogEntry.java         # Log storage
│   └── resources/webapp/
│       ├── index.html                    # Main HTML
│       ├── static/
│       │   ├── css/
│       │   │   ├── style.css            # Base styles
│       │   │   ├── ide.css              # IDE-specific
│       │   │   └── session-info.css     # Session panel (v2.2)
│       │   └── js/
│       │       └── app.js               # Main application
│       └── examples/
│           ├── 01_simple_api.xml
│           ├── 02_quotes_scraping.xml
│           └── 03_wikipedia.xml
└── target/
    └── webharvest-ide-2.3.1.jar         # Shaded JAR
```

---

## Appendix C: API Endpoints

### REST API

#### POST /api/execute
```
Request:
{
  "configXml": "<?xml...",
  "configName": "my-config.xml",
  "waitForWebSocket": true
}

Response:
{
  "executionId": "a7b3c4d5-..."
}
```

#### GET /api/execution/{id}/info
```
Response:
{
  "executionId": "a7b3c4d5",
  "status": "COMPLETED",
  "logCount": 12,
  "resultCount": 5,
  "sessionInfo": "Session[...]",
  "tokenUsage": "Tokens[...]"
}
```

#### GET /api/execution/{id}/logs
```
Response:
{
  "executionId": "a7b3c4d5",
  "logs": [
    {"timestamp": 1728662400, "level": "INFO", "message": "..."},
    ...
  ],
  "logCount": 12
}
```

### WebSocket API

#### WS /ws/execution?id={executionId}

**Messages (Server → Client):**

```javascript
// Status update
{
  "type": "status",
  "status": "RUNNING",
  "progress": 50,
  "message": "Processing..."
}

// Log message
{
  "type": "log",
  "level": "INFO",
  "message": "HTTP request completed",
  "timestamp": 1728662400
}

// Completion with results
{
  "type": "status",
  "status": "COMPLETED",
  "progress": 100,
  "results": {
    "url": "https://example.com",
    "title": "Example Domain",
    ...
  }
}
```

---

## Changelog v2.2

### ✨ New Features

- **Per-Tab State** - każda zakładka zachowuje własne logi i wyniki
- **Session Management** - UUID tracking, lifecycle, metrics
- **Token Tracking** - HTTP requests, bytes, CPU time, memory
- **Session Info Panel** - dedykowana zakładka z metadanymi
- **Real-time Metrics** - duration, rate, elements processed
- **Client ID** - auto-generated IDE identifier
- **Enhanced Logging** - stored per-job, persisted in memory

### 🎨 UI Improvements

- **Gradient Styles** - modern purple/pink accents
- **Session Panel** - beautiful cards with info/tokens
- **Status Badges** - color-coded execution states
- **Animations** - smooth fade-in for session cards
- **Empty States** - helpful messages when no data

### 🔧 Technical Changes

- **Removed database plugin** - IDE uses ONLY core now
- **ExecutionJob enhanced** - stores logs, results, session
- **ExecutionManager** - keeps completed jobs in memory
- **New API endpoints** - /api/execution/{id}/logs, /info, /session
- **LogEntry class** - structured log storage
- **ExecutionInfoServlet** - REST API for job data

### 🐛 Bug Fixes

- Fixed: Logs disappearing on tab switch
- Fixed: Results overwritten by new execution
- Fixed: No way to see previous execution results
- Fixed: Session info not available

---

## Support & Resources

- **Documentation:** `/webharvest-ide/IDE_USER_GUIDE_v2.2.md`
- **Examples:** `/webharvest-ide/src/main/resources/webapp/examples/`
- **API Reference:** `/webharvest-core/SESSION_API_REFERENCE.md`
- **Architecture:** `/webharvest-core/SESSION_TOKEN_ARCHITECTURE.md`

---

**Web-Harvest IDE v2.2** - Modern Web Scraping Environment  
© 2025 Web-Harvest Project | Open Source | Apache License 2.0






