# 🧪 Testing Guide - Phase 2 Features

## 🌐 Uruchomienie IDE
1. Otwórz przeglądarkę: **http://localhost:8080**
2. **Hard Refresh**: `Cmd+Shift+R` (Mac) lub `Ctrl+Shift+R` (Windows/Linux) - **WAŻNE!**
3. IDE powinno się uruchomić w trybie **Visual** (domyślnie)

---

## ✅ TEST 1: IconLibrary - Font Awesome Icons

**Co testować:**
- Nodes mają ikony Font Awesome zamiast emoji
- Kolory kategorii (Network=Blue, Extraction=Green, Control=Orange, etc.)
- Border color nodes zgodny z kategorią

**Jak testować:**
1. Przełącz na **Visual mode** (jeśli nie jesteś)
2. **Drag & Drop** plugins z palety po lewej stronie:
   - `http` → powinien mieć ikonę 🌐 i niebieski border
   - `loop` → powinien mieć ikonę 🔄 i pomarańczowy border
   - `xpath` → powinien mieć ikonę 🔀 i zielony border
   - `var` → powinien mieć ikonę 🧊 i fioletowy border
3. Sprawdź czy każdy node ma **ikona + tekst** w headerze
4. Sprawdź czy border color odpowiada kategorii

**Oczekiwany rezultat:**
- ✅ Wszystkie nodes mają ikony Font Awesome
- ✅ Kolory są spójne z kategoriami
- ✅ Border color zmienia się w zależności od typu

---

## ✅ TEST 2: Marquee Selection (Multi-Select)

**Co testować:**
- Drag rectangle na pustym canvas
- Real-time selection podczas drag
- Visual highlight dla selected nodes
- Multi-select support

**Jak testować:**
1. Dodaj kilka nodes na canvas (np. 3-4 nodes)
2. **Kliknij i przeciągnij** na pustym obszarze canvas (nie na node!)
3. Powinien pojawić się **różowy przerywany rectangle** (marquee)
4. Podczas drag - nodes wewnątrz rectangle powinny być automatycznie zaznaczone
5. Puść myszkę - selected nodes powinny mieć **różowy border**
6. Kliknij na pustym canvas → selection powinno się wyczyścić

**Oczekiwany rezultat:**
- ✅ Marquee rectangle pojawia się podczas drag
- ✅ Nodes są zaznaczane w real-time
- ✅ Selected nodes mają różowy border i tło
- ✅ Można zaznaczyć wiele nodes jednocześnie

---

## ✅ TEST 3: Grid Toggle

**Co testować:**
- Toggle grid overlay
- Snap-to-grid podczas move nodes

**Jak testować:**
1. Kliknij przycisk **Grid** w toolbar (ikonka: ▦)
2. Powinien pojawić się **grid overlay** na canvas
3. Przenieś node (drag & drop) → powinien "snapować" do grid
4. Kliknij Grid ponownie → grid powinien zniknąć

**Oczekiwany rezultat:**
- ✅ Grid overlay pojawia się/znika
- ✅ Nodes snapują do grid podczas move
- ✅ Grid ma 20px spacing

---

## ✅ TEST 4: Auto-Layout Buttons

**Co testować:**
- Auto-Layout: Vertical
- Auto-Layout: Horizontal  
- Auto-Layout: Grid

**Jak testować:**
1. Dodaj kilka nodes na canvas (np. 5-6 nodes)
2. Kliknij **Auto-Layout: Vertical** (ikonka: ┃)
   - Nodes powinny ułożyć się pionowo
3. Kliknij **Auto-Layout: Horizontal** (ikonka: ━)
   - Nodes powinny ułożyć się poziomo
4. Kliknij **Auto-Layout: Grid** (ikonka: ▦)
   - Nodes powinny ułożyć się w grid

**Oczekiwany rezultat:**
- ✅ Każdy layout działa poprawnie
- ✅ Nodes są równomiernie rozmieszczone
- ✅ Nie nakładają się na siebie

---

## ✅ TEST 5: Zoom Controls

**Co testować:**
- Zoom In (+)
- Zoom Out (-)
- Reset Zoom (100%)
- Zoom to Fit

**Jak testować:**
1. Dodaj kilka nodes na canvas
2. Kliknij **Zoom In** (ikonka: 🔍+)
   - Canvas powinien się przybliżyć
3. Kliknij **Zoom Out** (ikonka: 🔍-)
   - Canvas powinien się oddalić
4. Kliknij **Reset Zoom** (100%)
   - Zoom powinien wrócić do 100%
5. Kliknij **Zoom to Fit** (ikonka: ⇄)
   - Canvas powinien dostosować zoom do wszystkich nodes

**Keyboard shortcuts:**
- `Cmd/Ctrl + =` → Zoom In
- `Cmd/Ctrl + -` → Zoom Out
- `Cmd/Ctrl + 0` → Reset Zoom

**Oczekiwany rezultat:**
- ✅ Zoom działa płynnie
- ✅ Keyboard shortcuts działają
- ✅ Zoom to Fit dostosowuje widok

---

## ✅ TEST 6: Minimap

**Co testować:**
- Toggle minimap
- Navigate canvas przez kliknięcie w minimap

**Jak testować:**
1. Dodaj wiele nodes na canvas (np. 10+) rozłożonych szeroko
2. Kliknij przycisk **Minimap** w toolbar (ikonka: 🗺️)
3. W prawym dolnym rogu powinien pojawić się **minimap** (200x150px)
4. Minimap powin歪 pokazywać wszystkie nodes jako małe kropki
5. **Kliknij w minimap** → canvas powinien się przesunąć w to miejsce
6. Kliknij Minimap ponownie → minimap powinien zniknąć

**Oczekiwany rezultat:**
- ✅ Minimap pojawia się w prawym dolnym rogu
- ✅ Pokazuje wszystkie nodes
- ✅ Kliknięcie w minimap przesuwa canvas
- ✅ Toggle działa poprawnie

---

## ✅ TEST 7: ContainerRenderer - Expand/Collapse

**Co testować:**
- Renderowanie container nodes (loop, if, try)
- Expand/Collapse functionality
- Child count badges
- Gradient headers

**Jak testować:**
1. **Utwórz container node:**
   - Drag `loop` z palety
   - Container powinien mieć **gradient header** (fioletowy/różowy)
   - Header powinien pokazywać: `▼ LOOP` + badge z liczbą children
2. **Dodaj children do container:**
   - Drag `var` do środka container → powinien być zagnieżdżony
   - Drag kolejne `var` → powinny być w środku container
   - Badge powinien pokazywać: `2 plugins` lub `3 plugins`
3. **Collapse container:**
   - Kliknij na header container → powinien się **zwinąć**
   - W collapsed state: tylko header z `▶ LOOP` i badge
   - Children nie są widoczne
4. **Expand container:**
   - Kliknij ponownie na header → powinien się **rozwinąć**
   - Children są znowu widoczne

**Oczekiwany rezultat:**
- ✅ Containers mają gradient headers
- ✅ Expand/Collapse działa płynnie
- ✅ Child count badges są aktualne
- ✅ Children są poprawnie zagnieżdżone

---

## ✅ TEST 8: Context Menu (Right-Click)

**Co testować:**
- Right-click menu na nodes
- Edit Properties
- Delete

**Jak testować:**
1. Dodaj node na canvas (np. `http`)
2. **Right-click** na node
3. Powinien pojawić się **context menu** z opcjami:
   - Edit Properties
   - Delete
   - Duplicate (placeholder)
4. Kliknij **Edit Properties** → Property Panel powinien się otworzyć z właściwościami node
5. Kliknij **Delete** → node powinien zostać usunięty

**Oczekiwany rezultat:**
- ✅ Context menu pojawia się na right-click
- ✅ Edit Properties otwiera Property Panel
- ✅ Delete usuwa node
- ✅ Menu znika po kliknięciu gdzie indziej

---

## ✅ TEST 9: Keyboard Shortcuts

**Co testować:**
- Select All (`Cmd/Ctrl + A`)
- Delete Selected (`Delete` / `Backspace`)
- Zoom shortcuts (`Cmd/Ctrl + 0, +, -`)

**Jak testować:**
1. Dodaj kilka nodes na canvas
2. **`Cmd/Ctrl + A`** → wszystkie nodes powinny być zaznaczone (różowy border)
3. **`Delete`** lub **`Backspace`** → wszystkie zaznaczone nodes powinny zostać usunięte
4. Dodaj znowu nodes
5. **`Cmd/Ctrl + =`** → Zoom In
6. **`Cmd/Ctrl + -`** → Zoom Out
7. **`Cmd/Ctrl + 0`** → Reset Zoom

**Oczekiwany rezultat:**
- ✅ Wszystkie keyboard shortcuts działają
- ✅ Select All zaznacza wszystkie nodes
- ✅ Delete usuwa zaznaczone nodes
- ✅ Zoom shortcuts działają

---

## ✅ TEST 10: Property Panel Integration

**Co testować:**
- Property Panel pojawia się po kliknięciu na node
- Advanced editors (XPath, Regex, Expression)

**Jak testować:**
1. Kliknij na node na canvas
2. **Property Panel** powinien się pojawić w prawym sidebarze (zakładka "Properties")
3. Sprawdź czy properties node są widoczne
4. Dla atrybutów typu XPath/Regex/Expression - powinny być przyciski **🔧**
5. Kliknij **🔧** → powinien otworzyć się modal z odpowiednim edytorem:
   - XPath Tester (dla XPath attributes)
   - Regex Validator (dla regex attributes)
   - Expression Builder (dla `${variable}` expressions)

**Oczekiwany rezultat:**
- ✅ Property Panel otwiera się po kliknięciu node
- ✅ Wszystkie properties są widoczne
- ✅ Advanced editors otwierają się w modalach
- ✅ Property Panel jest pierwszą zakładką w Visual mode

---

## 🐛 Znane Problemy / Do Sprawdzenia

1. **Minimap może się nie pojawić** - sprawdź Console (F12) czy są błędy
2. **ContainerRenderer może nie renderować children** - sprawdź czy children są dodawane do container node
3. **Grid może nie być widoczny** - sprawdź czy przycisk Grid jest włączony

---

## 📋 Checklist Testowania

- [ ] IconLibrary - ikony i kolory działają
- [ ] Marquee Selection - rectangle i multi-select działa
- [ ] Grid Toggle - grid overlay działa
- [ ] Auto-Layout - wszystkie 3 typy działają
- [ ] Zoom Controls - wszystkie funkcje działają
- [ ] Minimap - pojawia się i nawigacja działa
- [ ] ContainerRenderer - expand/collapse działa
- [ ] Context Menu - right-click działa
- [ ] Keyboard Shortcuts - wszystkie działają
- [ ] Property Panel - otwiera się i advanced editors działają

---

## 🔍 Debug Info

**Console (F12):**
- Otwórz Developer Tools (F12)
- Sprawdź zakładkę **Console**
- Powinny być logi:
  - `[App] ...` - logi aplikacji
  - `[VisualDesigner] ...` - logi designer
  - `[ContainerRenderer] ...` - logi container renderer
  - `[GridSystem] ...` - logi grid system

**Jeśli coś nie działa:**
1. Hard Refresh (`Cmd+Shift+R`)
2. Sprawdź Console na błędy
3. Sprawdź Network tab - czy `main.js` się załadował
4. Sprawdź czy IDE jest w trybie Visual (nie XML)

---

## 🎯 Priorytet Testów

**NAJWAŻNIEJSZE (przetestuj najpierw):**
1. ✅ IconLibrary - czy ikony się pokazują
2. ✅ Marquee Selection - czy działa drag rectangle
3. ✅ ContainerRenderer - czy containers się renderują i expand/collapse działa
4. ✅ Property Panel - czy się otwiera po kliknięciu node

**POZIOM 2:**
5. ✅ Zoom Controls
6. ✅ Auto-Layout
7. ✅ Grid Toggle
8. ✅ Minimap

**POZIOM 3:**
9. ✅ Context Menu
10. ✅ Keyboard Shortcuts

---

**Powodzenia w testowaniu! 🚀**

