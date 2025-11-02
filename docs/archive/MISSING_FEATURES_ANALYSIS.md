# 🔍 Analysis: Missing Features in Visual Designer

**Date:** October 30, 2025  
**Purpose:** Identify gaps between planned and implemented features

---

## ❌ **BRAKUJĄCE FUNKCJONALNOŚCI**

### 1. **Zaawansowany Edytor Właściwości** ⚠️ **CRITICAL**

#### ✅ **CO JEST:**
- Podstawowe text inputs dla wszystkich atrybutów
- Przyciski 🔧 które otwierają modal z zaawansowanymi edytorami (XPath, Regex, Expression)
- ExpressionBuilder, XPathTester, RegexValidator (jako osobne komponenty)

#### ❌ **CZEGO BRAKUJE:**

**A. Schema-Based Form Generation**
- ❌ `SchemaEditor` nie jest zintegrowany z `PropertyPanel`
- ❌ Brak automatycznego generowania form na podstawie plugin schemas z API
- ❌ Wszystkie pola są `type="text"` zamiast odpowiednich typów
- ❌ Brak rozpoznawania typów z AttributeDefinition (url, number, boolean, enum)

**B. Type-Specific Editors**
- ❌ **URL Editor** - brak walidacji URL, brak browse button
- ❌ **Number Editor** - brak min/max validation, brak spinner controls
- ❌ **Boolean Editor** - brak toggle switch, tylko text input
- ❌ **Enum Editor** - brak dropdown/select, tylko text input
- ❌ **Date/Time Editor** - brak date picker
- ❌ **Color Picker** - brak color picker

**C. Real-Time Validation**
- ❌ Brak walidacji podczas wpisywania
- ❌ Brak wizualnych wskaźników błędów (czerwony border, ikona ❌)
- ❌ Brak wyświetlania komunikatów o błędach pod polami
- ❌ Brak walidacji required fields
- ❌ Brak pattern validation (regex patterns z schemas)

**D. Variable Autocomplete/Picker**
- ❌ ExpressionBuilder istnieje ale tylko jako modal
- ❌ Brak inline autocomplete w głównych polach tekstowych
- ❌ Brak przycisku do otwierania variable picker
- ❌ Brak listy dostępnych zmiennych w kontekście
- ❌ Brak ${var} syntax highlighting w inputach

**E. Advanced Features**
- ❌ Brak "Advanced Properties" toggle (show/hide advanced fields)
- ❌ Brak "Required Fields" indicators (*)
- ❌ Brak help tooltips (ikonka ? obok każdego pola)
- ❌ Brak linków do dokumentacji z schemas
- ❌ Brak default value indicators

**F. Integration Issues**
- ❌ PropertyPanel nie używa plugin metadata z `/api/plugins/metadata`
- ❌ Brak mapowania AttributeDefinition → form fields
- ❌ Nie korzysta z schema validation rules

---

### 2. **Drag & Drop do Containers** ⚠️ **CRITICAL**

#### ✅ **CO JEST:**
- Metoda `addChildToContainer()` 
- Wykrywanie drop nad `.container-children`
- Logika dodawania dzieci do container

#### ❌ **CZEGO BRAKUJE:**
- ❌ **OFFSET X/Y Problem** - używa `e.offsetX/Y` zamiast obliczeń względem canvas
- ❌ **Container Highlight** - highlight pojawia się ale może nie działać poprawnie
- ❌ **Visual Feedback** - brak wyraźnego wskaźnika że drop jest nad container
- ❌ **Prevent Drop Outside** - można przypadkiem dodać node poza container

---

### 3. **Przesuwanie Nodes po Canvas** ⚠️ **CRITICAL**

#### ✅ **CO JEST:**
- Metoda `setupNodeDragHandlers()` 
- Event handlery: mousedown, mousemove, mouseup

#### ❌ **CZEGO BRAKUJE:**
- ❌ **Integracja** - `portSystem` i `connectionManager` dodane ale nie używane
- ❌ **Port Rendering** - porty nie są renderowane (brak `.node-port` elements)
- ❌ **Offset Calculation** - może być niepoprawny offset przy przesuwaniu
- ❌ **Visual Feedback** - brak cursor change podczas drag

---

### 4. **Połączenia między Komponentami** ⚠️ **CRITICAL**

#### ✅ **CO JEST:**
- `ConnectionManager` class
- `PortSystem` class
- `startConnectionDrag()`, `endConnectionDrag()` methods
- `renderConnections()` method

#### ❌ **CZEGO BRAKUJE:**

**A. Port System Integration**
- ❌ Porty nie są renderowane na nodes (brak `.node-port` elements)
- ❌ Brak event handlerów na portach (mousedown do rozpoczęcia connection)
- ❌ Brak visual feedback podczas drag connection (temporary line)

**B. Connection Creation**
- ❌ Brak możliwości rozpoczęcia connection z output port
- ❌ Brak możliwości zakończenia connection na input port
- ❌ Brak visual feedback podczas przeciągania connection

**C. Connection Rendering**
- ❌ `renderConnections()` renderuje tylko `this.connections` (automatic)
- ❌ Nie renderuje `this.manualConnections` ani `connectionManager.getAllConnections()`
- ❌ Połączenia nie są widoczne między nodes

**D. Connection Validation**
- ❌ Brak walidacji typów portów (sequential/conditional/error)
- ❌ Brak sprawdzania czy port może przyjąć connection
- ❌ `wouldCreateCycle()` istnieje ale nie jest używany podczas tworzenia connection

---

### 5. **SchemaEditor Integration** ⚠️ **HIGH PRIORITY**

#### ✅ **CO JEST:**
- `SchemaEditor.ts` file exists
- Tests: `SchemaEditor.test.ts`

#### ❌ **CZEGO BRAKUJE:**
- ❌ **PropertyPanel** nie używa `SchemaEditor`
- ❌ TODO w kodzie: `// TODO: Integrate SchemaEditor when plugin metadata is available`
- ❌ Brak fetch plugin metadata w PropertyPanel
- ❌ Brak generowania form na podstawie schema

**Expected Integration:**
```typescript
// PropertyPanel.ts powinien:
1. Fetch plugin metadata: GET /api/plugins/metadata
2. Find schema for PK current node type
3. Use SchemaEditor.generateEditor(schema, currentAttributes)
4. Replace manual input creation with schema-driven form
```

---

### 6. **Port System Rendering** ⚠️ **HIGH PRIORITY**

#### ✅ **CO JEST:**
- `PortSystem.ts` class
- `addPorts()` method
- Port styling and positioning logic

#### ❌ **CZEGO BRAKUJE:**
- ❌ `addPorts()` jest wywoływany w `createBasicNodeElement()` ale:
  - Porty mogą nie być widoczne (CSS missing?)
  - Brak event handlerów na portach
  - Brak visual feedback podczas hover

**Expected:**
- Porty powinny być widoczne jako kółka na górze/dole node
- Click na output port → zaczyna connection drag
- Hover nad input port podczas drag → highlight port
- Drop na input port → tworzy connection

---

### 7. **Connection Manager Integration** ⚠️ **HIGH PRIORITY**

#### ✅ **CO JEST:**
- `ConnectionManager.ts` class
- Methods: `createConnection()`, `deleteConnection()`, `getAllConnections()`

#### ❌ **CZEGO BRAKUJE:**
- ❌ `VisualDesigner` ma `connectionManager` ale nie używa go
- ❌ `renderConnections()` nie używa `connectionManager.getAllConnections()`
- ❌ Brak synchronizacji między `this.manualConnections` a `connectionManager`
- ❌ `startConnectionDrag()` nie jest wywoływany z portów

---

## 📋 **SUMMARY - Co Naprawić (Priority Order)**

### **🔴 CRITICAL - Działa Ale Nie Jest Widoczne/Używane:**

1. **Port System** - porty są renderowane ale brak event handlerów i visual feedback
2. **Connection Manager** - istnieje ale nie jest używany w renderConnections()
3. **SchemaEditor** - istnieje ale nie jest zintegrowany z PropertyPanel
4. **Manual Connections** - logika istnieje ale brak UI do tworzenia (drag z portu)

### **🟠 HIGH PRIORITY - Częściowo Zaimplementowane:**

5. **Real-Time Validation** - brak walidacji w PropertyPanel
6. **Variable Autocomplete** - ExpressionBuilder istnieje ale brak inline autocomplete
7. **Type-Specific Editors** - wszystkie pola są text, brak URL/number/boolean/enum editors

### **🟡 MEDIUM PRIORITY - Zaplanowane Ale Nie Zaimplementowane:**

8. **Advanced Properties Toggle** - brak sekcji advanced/collapsed
9. **Help Tooltips** - brak tooltips z dokumentacją
10. **Required Field Indicators** - brak * przy required fields

---

## 🎯 **NATYCHMIASTOWE DZIAŁANIA**

### **Fix 1: Port System - Renderowanie + Event Handlery** (1-2h)
```typescript
// W createBasicNodeElement():
1. ✅ addPorts() jest wywoływany
2. ❌ Dodać CSS dla .node-port (sprawdź czy istnieje)
3. ❌ Dodać event handler na output port (mousedown → startConnectionDrag)
4. ❌ Dodać event handler na input port (mouseup → endConnectionDrag)
5. ❌ Dodać visual feedback (hover styles)
```

### **Fix 2: Connection Manager Integration** (30min)
```typescript
// W renderConnections():
1. ❌ Użyć connectionManager.getAllConnections() zamiast tylko this.connections
2. ❌ Połączyć manualConnections z connectionManager
3. ❌ Renderować wszystkie połączenia (automatic + manual)
```

### **Fix 3: SchemaEditor Integration** (2-3h)
```typescript
// W PropertyPanel.render():
1. ❌ Fetch plugin metadata dla current node type
2. ❌ Użyć SchemaEditor.generateEditor(schema, attributes)
3. ❌ Zamienić manual input creation na schema-driven
4. ❌ Dodać validation na podstawie schema rules
```

### **Fix 4: Type-Specific Editors** (2-3h)
```typescript
// W PropertyPanel lub SchemaEditor:
1. ❌ URL Editor - input type="url" + validation
2. ❌ Number Editor - input type="number" + min/max
3. ❌ Boolean Editor - checkbox/toggle switch
4. ❌ Enum Editor - select dropdown z options
```

---

## 🔧 **KONKRETNE BŁĘDY DO NAPRAWY**

### **1. PropertyPanel - Wszystkie pola są text**
```typescript
// OBECNIE (PropertyPanel.ts:125-132):
const input = document.createElement('input');
input.type = 'text'; // ❌ Zawsze text!

// POWINNO BYĆ:
switch (attributeType) {
    case 'url': input.type = 'url'; break;
    case 'number': input.type = 'number'; break;
    case 'boolean': return createCheckbox(); // ❌ Brak
    case 'enum': return createSelect(); // ❌ Brak
    default: input.type = 'text';
}
```

### **2. renderConnections() - Nie renderuje manual connections**
```typescript
// OBECNIE (VisualDesigner.ts:607):
if (!this.svgElement || this.connections.length === 0) return; // ❌ Tylko automatic!

// POWINNO BYĆ:
const allConnections = [
    ...this.connections, // automatic
    ...this.connectionManager.getAllConnections() // manual
];
```

### **3. Port System - Brak event handlerów**
```typescript
// OBECNIE:
this.portSystem.addPorts(nodeElement, node); // ✅ Porty są dodane
// ❌ Ale brak event handlerów!

// POWINNO BYĆ:
portElement.addEventListener('mousedown', (e) => {
    if (direction === 'output') {
        this.startConnectionDrag(node.id, 'output');
    }
});
```

### **4. Container Drop - Offset Calculation**
```typescript
// OBECNIE (VisualDesigner.ts:737-749):
dropX = e.clientX - canvasRect.left; // ✅ OK
dropY = e.clientY - canvasRect.top; // ✅ OK

// Ale w setupNodeDragHandlers (845):
offsetX = e.clientX - rect.left; // ❌ Może być niepoprawny!
// Powinno uwzględniać pozycję node względem canvas
```

---

## 📊 **STATUS CHECKLIST**

| Feature | Planned | Implemented | Integrated | Working |
|---------|---------|-------------|------------|---------|
| SchemaEditor | ✅ | ✅ | ❌ | ❌ |
| PortSystem | ✅ | ✅ | ⚠️ | ❌ |
| ConnectionManager | ✅ | ✅ | ❌ | ❌ |
| Type-Specific Editors | ✅ | ❌ | ❌ | ❌ |
| Real-Time Validation | ✅ | ❌ | ❌ | ❌ |
| Variable Autocomplete | ✅ | ⚠️ | ❌ | ❌ |
| Advanced Properties | ✅ | ❌ | ❌ | ❌ |
| Help Tooltips | ✅ | ❌ | ❌ | ❌ |
| Required Indicators | ✅ | ❌ | ❌ | ❌ |
| Drag Nodes | ✅ | ⚠️ | ⚠️ | ⚠️ |
| Container Drop | ✅ | ⚠️ | ⚠️ | ⚠️ |
| Manual Connections | ✅ | ⚠️ | ❌ | ❌ |

**Legend:**
- ✅ = Done
- ⚠️ = Partially done / Needs integration
- ❌ = Missing

---

## 🎯 **RECOMMENDED NEXT STEPS**

1. **IMMEDIATE:** Naprawić `renderConnections()` żeby renderowało manual connections
2. **IMMEDIATE:** Dodać event handlery na portach (connection drag)
3. **HIGH:** Zintegrować SchemaEditor z PropertyPanel
4. **HIGH:** Dodać type-specific editors (URL, number, boolean, enum)
5. **MEDIUM:** Dodać real-time validation
6. **MEDIUM:** Dodać inline variable autocomplete

**Total Estimated Time:** 8-12 hours

---

**Created:** October 30, 2025  
**Status:** Analysis Complete - Ready for Implementation

