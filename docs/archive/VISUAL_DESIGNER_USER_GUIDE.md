# Visual Designer - Kompletny Przewodnik Użytkownika

**WebHarvest IDE v2.3**  
**Data:** Listopad 2025

---

## 📖 Spis Treści

1. [Wprowadzenie](#wprowadzenie)
2. [Przełączanie Między Trybami](#przełączanie-między-trybami)
3. [Podstawy Visual Designera](#podstawy-visual-designera)
4. [Praca z Pluginami](#praca-z-pluginami)
5. [Praca z Kontenerami](#praca-z-kontenerami)
6. [Zmiana Kolejności Pluginów](#zmiana-kolejności-pluginów)
7. [Zagnieżdżone Kontenery](#zagnieżdżone-kontenery)
8. [Semantic Validation](#semantic-validation)
9. [Generowanie XML](#generowanie-xml)
10. [Zaawansowane Funkcje](#zaawansowane-funkcje)
11. [Rozwiązywanie Problemów](#rozwiązywanie-problemów)

---

## Wprowadzenie

**Visual Designer** to nowy, wizualny edytor konfiguracji WebHarvest, który umożliwia tworzenie workflow'ów poprzez przeciąganie i upuszczanie pluginów na canvas zamiast pisania XML ręcznie.

### Dlaczego Visual Designer?

✅ **Szybkie prototypowanie** - przeciągnij plugin, skonfiguruj, gotowe  
✅ **Wizualna reprezentacja** - zobacz strukturę workflow'u na pierwszy rzut oka  
✅ **Automatyczna walidacja** - system uniemożliwia tworzenie nieprawidłowych konfiguracji  
✅ **Semantyczne wsparcie** - wsparcie dla właściwego zagnieżdżania pluginów  
✅ **Dwukierunkowa synchronizacja** - XML ↔ Visual  

### Tryby Pracy

IDE oferuje **trzy tryby** edycji:

1. **XML** - tradycyjny edytor kodu (Monaco Editor)
2. **Visual** - nowy designer drag-and-drop
3. **Mixed** - widok podzielony (planowany)

---

## Przełączanie Między Trybami

### Przełączanie XML ↔ Visual

W górnym pasku narzędzi znajdziesz przyciski:

```
[XML]  [Visual]
  ↑       ↑
Tryb  Tryb
Kodu  Designer
```

**Instrukcja:**
1. Kliknij przycisk **"Visual"**, aby przełączyć na designer
2. Kliknij przycisk **"XML"**, aby wrócić do edytora kodu
3. IDE automatycznie synchronizuje zmiany między trybami

**Ważne:**
- Zmiany są zachowywane automatycznie
- XML jest generowany w czasie rzeczywistym
- Walidacja semantyczna chroni przed błędami

### Synchronizacja

```
XML Editor → Visual Designer
  ↓
Parse XML → Extract Nodes → Render on Canvas

Visual Designer → XML Editor
  ↓
Collect Nodes → Generate XML → Update Editor
```

---

## Podstawy Visual Designera

### Interfejs

```
┌──────────────────────────────────────────────────────────────┐
│ 🌺 WebHarvest IDE  [XML] [✓ Visual]  [Run] [Validate]       │ ← Toolbar
├─────────┬──────────────────────────────────────┬─────────────┤
│ PLUGINS │         Canvas (Grid)               │ PROPERTIES │
│         │                                      │             │
│ [🔍]___ │    ┌───────────┐                    │ [Empty]    │
│         │    │ 🌐 http   │← Selected          │             │
│ ▼ HTTP  │    └───────────┘                    │ Select node│
│  🌐 http│              ↓                      │ to edit    │
│  🔄 html│    ┌───────────┐                    │             │
│         │    │ 🔍 xpath  │                    │             │
│ ▼ Data  │    └───────────┘                    │             │
│  🔍 xpath│    ┌───────────┐                    │             │
│  📊 xquery│   │ 📦 def    │                    │             │
│  📝 regexp│   └───────────┘                    │             │
│         │                                      │             │
│ ▼ Ctrl  │                                      │             │
│  ❓ if   │                                      │             │
│  🔁 loop │                                      │             │
│  🛡️  try │                                      │             │
│  ⚠️ catch│                                      │             │
└─────────┴──────────────────────────────────────┴─────────────┤
│ Status: Ready │ 3 nodes │ 0 connections │ ✓ Valid          │
└──────────────────────────────────────────────────────────────┘
```

### Komponenty

1. **Plugin Palette** (lewo) - lista dostępnych pluginów, grupowanych kategoriami
2. **Canvas** (środek) - obszar roboczy z węzłami
3. **Properties Panel** (prawo) - edycja właściwości wybranego węzła
4. **Toolbar** (góra) - przełączniki trybu, Run, Validate
5. **Status Bar** (dół) - statystyki i status walidacji

---

## Praca z Pluginami

### Dodawanie Pluginów

**Metoda 1: Drag & Drop z Palety**

1. Znajdź plugin w **Plugin Palette** po lewej stronie
2. **Kliknij i przeciągnij** plugin do obszaru Canvas
3. **Puść** mysz - węzeł pojawi się w miejscu upuszczenia
4. **Kliknij** węzeł, aby go wybrać

**Metoda 2: Wyszukiwanie**

1. Wpisz nazwę plugin w **wyszukiwarce** (`[🔍]`)
2. Paleta się filtruje
3. Przeciągnij znaleziony plugin

### Ruch Węzłów

**Przeciągnij węzeł** myszą, aby zmienić jego położenie na canvas.

**Wskazówki:**
- Węzły są pozycjonowane absolutnie
- Snap-to-grid jest planowany
- Pozycja jest zachowywana w XML

### Usuwanie Węzłów

**Metoda 1: Context Menu**
1. **Kliknij prawym przyciskiem** na węzeł
2. Wybierz **"Delete"**
3. Potwierdź usunięcie

**Metoda 2: Klawiatura**
1. Wybierz węzeł
2. Naciśnij **Delete** lub **Backspace**

**Metoda 3: Multi-Select**
1. **Shift+Click** wielu węzłów lub użyj lasso
2. Naciśnij **Delete**

### Edycja Właściwości

1. **Kliknij** węzeł, aby go wybrać
2. Panel **Properties** zaktualizuje się
3. Edytuj wartości w panelu
4. Zmiany są zapisywane automatycznie

---

## Praca z Kontenerami

### Co to jest Kontener?

**Kontener** to plugin, który może zawierać inne pluginy jako dzieci. Przykłady:

- `try` - obsługa błędów
- `catch` - blok złapania wyjątku
- `if` - warunkowe wykonanie
- `else` - alternatywa dla if
- `loop` - iteracja
- `while` - pętla warunkowa

### Dodawanie Pluginów do Kontenera

1. **Przeciągnij** kontener na canvas
2. **Rozwiń** kontener (kliknij nagłówek), jeśli jest zwinięty
3. **Przeciągnij** kolejne pluginy **do środka** kontenera
4. Pluginy pojawią się jako dzieci

**Wizualizacja:**
```
┌────────────────────────────────┐
│ 🛡️  try                        │ ← Container header
│ ┌────────────────────────────┐ │
│ │ 🌐 http                    │ │ ← Child node
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ 🔍 xpath                   │ │ ← Child node
│ └────────────────────────────┘ │
└────────────────────────────────┘
```

### Zwijanie/Rozwijanie Kontenerów

- **Kliknij nagłówek** kontenera, aby go zwinąć/rozwinąć
- Zwijanie ukrywa dzieci dla czytelności
- Pozycje węzłów są zachowywane

---

## Zmiana Kolejności Pluginów

### Przyciski Strzałek ⬆️⬇️

Każdy plugin wewnątrz kontenera ma **przyciski strzałek** w prawym górnym rogu:

```
┌─────────────────────────────┐
│ 📦 log    ⬆️⬇️              │ ← Reorder buttons
└─────────────────────────────┘
```

### Instrukcja Zmiany Kolejności

1. **Znajdź** plugin w kontenerze
2. **Kliknij ⬆️**, aby przenieść w górę
3. **Kliknij ⬇️**, aby przenieść w dół
4. Przyciski są automatycznie wyłączane na granicach

**Praktyczny przykład:**

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

### Walidacja Kolejności

- Kolejność jest zachowywana przy generowaniu XML
- Kolejność jest zgodna z połączeniami (planowane)
- Undo/Redo działa (Ctrl+Z / Ctrl+Y)

---

## Zagnieżdżone Kontenery

### Hierarchia Kontenerów

Visual Designer **obsługuje wielopoziomowe zagnieżdżenie**:

```
┌──────────────────────────────────────────────┐
│ 🛡️  try                                      │ ← Root container
│ ┌──────────────────────────────────────────┐ │
│ │ ⚠️  catch                                │ │ ← Nested container
│ │ ┌──────────────────────────────────────┐ │ │
│ │ │ ❓ if                                 │ │ │ ← Deeply nested
│ │ │ ┌──────────────────────────────────┐ │ │ │
│ │ │ │ 🌐 http                          │ │ │ │ ← Child node
│ │ │ └──────────────────────────────────┘ │ │ │
│ │ └──────────────────────────────────────┘ │ │
│ └──────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

### Dodawanie do Zagnieżdżonych Kontenerów

1. **Przeciągnij** plugin do obszaru kontenera
2. System wykrywa najgłębszy kontener na pozycji myszy
3. Plugin jest dodawany do właściwego kontenera
4. **Zielone podświetlenie** wskazuje prawidłowy target

---

## Semantic Validation

### Co to jest Walidacja Semantyczna?

**Walidacja semantyczna** zapewnia, że pluginy są używane zgodnie ze swoim przeznaczeniem, np. `catch` tylko wewnątrz `try`.

### Zasady Walidacji

**1. Zależne Pluginy**
- `catch` - **wymaga** rodzica `try`
- `ftp-put` - **wymaga** rodzica `ftp`
- `call-param` - **wymaga** rodzica `call`
- `http-header` - **wymaga** rodzica `http`

**2. Pluginy Kontenerowe**
- `try` - może zawierać **dowolne** dzieci
- `if` - może zawierać **dowolne** dzieci
- `loop` - może zawierać **dowolne** dzieci

**3. Pluginy Rooth**
- Większość pluginów może być na **głównym poziomie**
- Niektóre wymagają **konkretnego rodzica**

### Przykłady Błędów

**❌ BŁĄD: catch bez try**
```
┌──────────────────┐
│ ⚠️  catch        │ ← Błąd! catch wymaga try
│ (empty)          │
└──────────────────┘

Toast: "❌ catch can only be used inside a try container"
```

**✅ PRAWIDŁOWO: catch wewnątrz try**
```
┌──────────────────┐
│ 🛡️  try          │
│ ┌──────────────┐ │
│ │ ⚠️  catch    │ │ ← OK!
│ └──────────────┘ │
└──────────────────┘
```

### Wizualna Informacja Zwrotna

**Podczas przeciągania:**

- **🟢 Zielony border** = dopuszczalne upuszczenie
- **🔴 Czerwony border** = niedopuszczalne upuszczenie
- **🔵 Niebieski border** = zaznaczony target
- **⚫ Szara plugin w palecie** = wymaga rodzica

---

## Generowanie XML

### Automatyczne Generowanie

Visual Designer **automatycznie generuje XML** na podstawie wizualnej reprezentacji.

**Metoda 1: Przełączenie Trybu**
1. Przełącz na **XML**
2. XML jest aktualizowany

**Metoda 2: Run / Validate**
1. Kliknij **"Run"** lub **"Validate"**
2. XML jest używany do walidacji/wykonania

### Zachowanie Kolejności

XML jest generowany z uwzględnieniem:

✅ **Kolejności węzłów** na canvas  
✅ **Poziomów zagnieżdżenia**  
✅ **Połączeń manualnych** (planowane)  
✅ **Właściwości węzłów**  

**Przykład:**

```
Visual:
┌──────────────────┐
│ 🌐 http          │
│   url="..."      │
└──────────────────┘

Generated XML:
<http url="..."/>
```

### Dwukierunkowa Synchronizacja

**XML → Visual:**
1. Przełącz na **Visual**
2. IDE parsuje XML
3. Węzły są renderowane

**Visual → XML:**
1. Edytuj wizualnie
2. Przełącz na **XML**
3. XML jest aktualizowany

---

## Zaawansowane Funkcje

### Multi-Select

**Metoda 1: Shift+Click**
1. Wybierz pierwszy węzeł
2. **Shift+Click** kolejne węzły

**Metoda 2: Marquee Selection** (planowane)
1. **Przeciągnij** prostokąt na canvas
2. Wszystkie węzły w prostokącie są wybrane

**Metoda 3: Select All**
- **Ctrl+A** / **Cmd+A**

**Operacje na wielu węzłach:**
- **Delete** - usuń zaznaczone
- **Copy** (**Ctrl+C**) - skopiuj
- **Paste** (**Ctrl+V**) - wklej
- **Move** - przeciągnij razem

### Undo / Redo

- **Undo:** **Ctrl+Z** / **Cmd+Z**
- **Redo:** **Ctrl+Y** / **Cmd+Shift+Z**

**Obsługiwane operacje:**
✅ Dodawanie węzłów  
✅ Usuwanie węzłów  
✅ Przesuwanie węzłów  
✅ Zmiana kolejności  
✅ Edycja właściwości  
✅ Dodawanie dzieci  

### Tooltip'y

**Plugin w palecie:**
- **Hover** nad pluginem pokazuje opis
- **Zależny plugin** pokazuje wymagany rodzic
- **Kontener** pokazuje, czy akceptuje dzieci

**Węzeł na canvas:**
- **Hover** pokazuje typ i atrybuty
- **Error** pokazuje komunikat błędu

### Skróty Klawiszowe

| Skrót | Akcja |
|-------|-------|
| **Ctrl+N** / **Cmd+N** | New canvas |
| **Ctrl+A** / **Cmd+A** | Select all |
| **Ctrl+C** / **Cmd+C** | Copy selected |
| **Ctrl+V** / **Cmd+V** | Paste |
| **Ctrl+Z** / **Cmd+Z** | Undo |
| **Ctrl+Y** / **Cmd+Y** | Redo |
| **Delete** / **Backspace** | Delete selected |
| **Ctrl+F** / **Cmd+F** | Search plugins |
| **Ctrl+/** / **Cmd+/** | Toggle comment |

---

## Rozwiązywanie Problemów

### Węzły Nie Pojawiają Się

**Problem:** Po przeciągnięciu plugin nie pojawia się.

**Rozwiązanie:**
1. Sprawdź **Console** (F12) dla błędów
2. Upewnij się, że **drop jest nad Canvas**
3. Sprawdź walidację semantyczną (może być odrzucony)

### Nie Mogę Zmienić Kolejności

**Problem:** Przyciski ⬆️⬇️ nie działają.

**Rozwiązanie:**
1. Upewnij się, że plugin jest **wewnątrz kontenera**
2. Węzły główne **nie mają** przycisków (nie ma kontekstu)
3. Hard refresh (Cmd+Shift+R) jeśli cache

### Walidacja Zawodzi Niepotrzebnie

**Problem:** Plugin jest odrzucany mimo poprawności.

**Rozwiązanie:**
1. Sprawdź komunikat toast
2. Upewnij się, że **przeciągasz nad właściwym targetem**
3. Użyj **zielonego podświetlenia** jako wskazówki

### Canvas Jest Pusty

**Problem:** Canvas pusty po przełączeniu na Visual.

**Rozwiązanie:**
1. Dodaj węzły przeciągając z palety
2. Sprawdź, czy XML jest prawidłowy
3. Parsing błędów są w Console (F12)

### XML Nie Synchronizuje

**Problem:** Zmiany wizualne nie są w XML.

**Rozwiązanie:**
1. **Przełącz na XML**, aby wymusić synchronizację
2. Sprawdź **Console** dla błędów parsowania
3. Użyj **Run/Validate**, aby zapisać

---

## Tips & Tricks

### Szybkie Tworzenie Workflow'u

1. **Dodaj główne kontenery** (try, if, loop)
2. **Rozwiń kontenery**
3. **Dodaj dzieci** przeciągając
4. **Zmieniaj kolejność** strzałkami
5. **Run**, aby przetestować

### Organizacja Canvas

- **Grupuj powiązane węzły**
- **Używaj kontenerów** do logicznego grupowania
- **Zwinięte kontenery** dla czytelności

### Debugowanie

- **Console (F12)** - szczegółowe logi
- **Toast** - komunikaty błędów walidacji
- **Status bar** - statystyki
- **Properties** - edycja wartości

---

## Podsumowanie

**Visual Designer** oferuje nowoczesny sposób tworzenia konfiguracji WebHarvest:

✅ **Drag & Drop** - szybkie dodawanie pluginów  
✅ **Zmiana kolejności** - przyciski ⬆️⬇️  
✅ **Zagnieżdżone kontenery** - hierarchia wizualna  
✅ **Walidacja semantyczna** - ochrona przed błędami  
✅ **Generowanie XML** - automatyczna synchronizacja  
✅ **Undo/Redo** - bezpieczna edycja  

**Dalsze informacje:**
- Szczegóły techniczne: `SEMANTIC_METADATA_IMPLEMENTATION_COMPLETE.md`
- Dokumentacja deweloperska: `DEVELOPER_GUIDE.md`
- Przewodnik użytkownika IDE: `webharvest-ide/IDE_USER_GUIDE_v2.2.md`

