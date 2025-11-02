# 🔧 Plan Naprawy: Zaawansowany Edytor Właściwości

**Problem:** PropertyPanel używa tylko prostych text inputs zamiast zaawansowanych edytorów opartych na schema.

**Status:** SchemaEditor istnieje ale nie jest używany.

---

## 🎯 **CO NAPRAWIĆ:**

### **1. Integracja SchemaEditor z PropertyPanel** (2-3h)

**Obecny stan:**
```typescript
// PropertyPanel.ts:107
// TODO: Integrate SchemaEditor when plugin metadata is available
Object.entries(this.currentNode.attributes).forEach(([key, value]) => {
    const input = document.createElement('input');
    input.type = 'text'; // ❌ Zawsze text!
    // ...
});
```

**Po naprawie:**
```typescript
// PropertyPanel.ts
import { SchemaEditor } from './SchemaEditor';

async render(): Promise<void> {
    // 1. Fetch plugin metadata
    const metadata = await fetch('/api/plugins/metadata').then(r => r.json());
    const plugin = metadata.plugins.find(p => p.elementName === this.currentNode.type);
    
    // 2. Użyj SchemaEditor jeśli schema dostępne
    if (plugin?.schema) {
        const schemaEditor = new SchemaEditor();
        schemaEditor.generateEditor(this.container, plugin, this.currentNode.attributes);
        schemaEditor.onChange((attrs) => {
            this.currentNode.attributes = attrs;
            this.onChangeCallback(this.currentNode);
        });
    } else {
        // Fallback: proste inputs (jak teraz)
    }
}
```

---

### **2. Type-Specific Editors** (2h)

**Dodać do SchemaEditor lub PropertyPanel:**

- ✅ **URL Editor** - `inputington type="url"` + validation
- ✅ **Number Editor** - `input type="number"` + min/max
- ✅ **Boolean Editor** - checkbox/toggle switch
- ✅ **Enum Editor** - `<select>` z options
- ✅ **Textarea** - dla długich wartości

---

### **3. Real-Time Validation** (1-2h)

**Dodać:**
- Walidacja podczas wpisywania (`input` event)
- Wizualne wskaźniki błędów (czerwony border, ikona ❌)
- Komunikaty błędów pod polami
- Required field validation
- Pattern validation (regex patterns)

---

### **4. Variable Autocomplete Inline** (2h)

**Dodać:**
- Autocomplete dropdown w text inputs (detect `${` → pokaż dostępne zmienne)
- Variable picker button obok pól które mogą zawierać zmienne
- Syntax highlighting dla `${variable}` w inputach

---

### **5. Advanced Properties Toggle** (1h)

**Dodać:**
- Sekcja "Basic Properties" (always visible)
- Sekcja "Advanced Properties" (collapsed by default)
- Toggle button do pokazywania/ukrywania

---

### **6. Help Tooltips** (1h)

**Dodać:**
- Ikona `?` obok każdego pola
- Tooltip z opisem z AttributeDefinition.description
- Link do dokumentacji jeśli dostępny w schema

---

## 📋 **IMPLEMENTACJA KROK PO KROKU:**

### **Step 1: PropertyPanel - Fetch Metadata**
```typescript
private pluginMetadata: any[] = [];

async loadPluginMetadata(): Promise<void> {
    const response = await fetch('/api/plugins/metadata');
    const data = await response.json();
    this.pluginMetadata = data.plugins || [];
}

private getPluginSchema(nodeType: string): Plugin | null {
    return this.pluginMetadata.find(p => p.elementName === nodeType) || null;
}
```

### **Step 2: PropertyPanel - Use SchemaEditor**
```typescript
private render(): void {
    const plugin = this.getPluginSchema(this.currentNode.type);
    
    if (plugin?.schema) {
        // Use SchemaEditor
        const schemaEditor = new SchemaEditor一个新();
        schemaEditor.generateEditor(this.container, plugin, this.currentNode.attributes);
        schemaEditor.onChange((attrs) => {
            if (this.currentNode) {
                this.currentNode.attributes = attrs;
                this.onChangeCallback?.(this.currentNode);
            }
        });
    } else {
        // Fallback to simple inputs
        this.renderSimpleInputs();
    }
}
```

### **Step 3: SchemaEditor - Fix Type Mapping**
```typescript
// SchemaEditor.ts powinien już mieć:
case 'boolean': return createCheckbox();
case 'enum': return createSelect();
case 'number': input.type = 'number';
case 'url': input.type = 'url';
// ✅ To już jest zaimplementowane!
```

### **Step 4: Add Validation**
```typescript
// W SchemaEditor.createField():
input.addEventListener('input', () => {
    const validation = this.validateField(attrDef, input.value);
    if (!validation.valid) {
        input.style.borderColor = 'var(--magenta)';
        this.showError(fieldContainer, validation.error);
    } else {
        input.style.borderColor = 'var(--gray-300)';
        this.hideError(fieldContainer);
    }
});
```

---

## ✅ **CHECKLIST:**

- [ ] PropertyPanel.loadPluginMetadata() - fetch z API
- [ ] PropertyPanel.getPluginSchema() - znajdź schema dla node type
- [ ] PropertyPanel.render() - użyj SchemaEditor jeśli schema dostępne
- [ ] SchemaEditor.generateEditor() - już działa (sprawdź czy wszystkie typy)
- [ ] Validation - dodać real-time validation
- [ ] Error Display - dodać komunikaty błędów
- [ ] Variable Autocomplete - dodać inline autocomplete
- [ ] Help Tooltips - dodać tooltips z opisami
- [ ] Required Indicators - dodać * przy required fields

---

**Estimated Time:** 8-12 hours

**Priority:** 🔴 CRITICAL

