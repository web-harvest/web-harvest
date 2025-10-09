# Plan Usuwania Starej Architektury

## 🎯 CEL: Usunięcie IElementDef i całej hierarchii *Def

### KROK 1: Inwentaryzacja użyć ✅ (TERAZ)
Sprawdzamy kto jeszcze używa getElementDef()

### KROK 2: Zamiana użyć na nowe API 🔄
Replace: config.getElementDef() → config.getConfigurationTree()

### KROK 3: Usunięcie *Def classes 🗑️
Delete: HttpDef, XPathDef, FileDef... (all 36 classes)

### KROK 4: Usunięcie IElementDef 🗑️
Delete: IElementDef.java, AbstractElementDef.java

### KROK 5: Usunięcie infrastructure 🗑️
Delete: SAXConfigParser, XmlNode, DefinitionResolver

### KROK 6: Cleanup 🧹
Remove imports, dead code, adapters

---

## STATUS: Szukamy użyć getElementDef()...
