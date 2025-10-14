# Inwentaryzacja Pozostałości Starej Architektury (r727)

## 📊 PODSUMOWANIE

**✅ USUNIĘTO użycia w produkcji:**
- DefaultHarvest ✅
- WebHarvestCLI ✅

**❌ POZOSTAŁO:**
- 36 klas *Def
- 58 odniesień do IElementDef  
- 5 użyć .getElementDef() w kodzie produkcyjnym
- Cały system Processor<Def>

---

## 1️⃣ UŻYCIA .getElementDef() (Produkcja - 5 plików)

### A. Kod Produkcyjny (3 pliki)

1. **Scraper.java** - Runtime scraper
2. **AbstractProcessorDecorator.java** - Processor decorator
3. **ProcessorPluginAdapter.java** - Adapter Plugin→Processor

### B. Testy (2 pliki)

4. **ConfigPluginTest.java**
5. **AbstractProcessorDecoratorTest.java**  
6. **ProcessorPluginAdapterTest.java**

### C. Dokumentacja & Build (ignorowane)
- REMOVAL_PLAN.md
- MODERNIZATION_PLAN_v2.2.md
- OLD_ARCHITECTURE_INVENTORY.md
- docs/target/*, web-harvest-docs-upload/*

---

## 2️⃣ 36 KLAS *Def (DO USUNIĘCIA)

### Core Definitions
1. ConfigDef.java
2. ConstantDef.java
3. TextDef.java
4. TemplateDef.java
5. EmptyDef.java
6. SimpleElementDef.java

### HTTP & Web
7. HttpDef.java
8. HttpParamDef.java
9. HttpHeaderDef.java

### XML Processing
10. XPathDef.java
11. XQueryDef.java
12. XQueryExternalParamDef.java
13. XsltDef.java
14. HtmlToXmlDef.java

### Control Flow
15. IfDef.java
16. LoopDef.java
17. WhileDef.java
18. CaseDef.java
19. TryDef.java
20. ExitDef.java
21. ReturnDef.java

### Variables & Functions
22. VarDef.java
23. VarDefDef.java
24. CallDef.java
25. CallParamDef.java
26. FunctionDef.java

### I/O Operations
27. FileDef.java
28. IncludeDef.java

### Scripting & Regex
29. ScriptDef.java
30. RegexpDef.java

### Plugin System
31. WebHarvestPluginDef.java
32. CorePluginDef.java
33. CorePluginWebHarvestPluginDef.java

### Infrastructure  
34. AbstractElementDef.java (BASE CLASS)
35. IElementDef.java (INTERFACE)
36. ElementInfo.java

---

## 3️⃣ PROCESSOR SYSTEM (Stary - 58 użyć)

### Główne Komponenty:

**Interfaces:**
- Processor<TDef extends IElementDef>
- ProcessorFactory

**Base Classes:**
- AbstractProcessor
- AbstractProcessorDecorator

**Adapters:**
- PluginProcessorAdapter (Plugin → Processor)
- ProcessorPluginAdapter (Processor → Plugin)

**Factories:**
- DefaultProcessorFactory
- ProcessorResolver

**Utilities:**
- BodyProcessor
- EmptyProcessor
- RunningStatusController
- StoppedOrExitedProcessor

---

## 4️⃣ PARSER INFRASTRUCTURE (DO USUNIĘCIA)

### Stary System:
1. **SAXConfigParser** - Old XML parser
2. **XmlNode** - XML node representation
3. **DefinitionResolver** - Element name → *Def mapper
4. **ConfigurableResolver** - Interface
5. **CoreDefinitionResolver** - Core elements resolver

### Nowy System (Replacement):
✅ **StreamingXmlParser** - New parser (r724)
✅ **PluginConfigurationTree** - New data structure (r724)

---

## 5️⃣ DEPENDENCY GRAPH

```
UŻYCIA (top-down):

DefaultHarvest (✅ ZMODERNIZOWANE)
  └─ Config.getConfigurationTree() (✅ NOWE API)

Scraper (❌ STARE)
  └─ Config.getElementDef() (❌ STARE API)
      └─ IElementDef
          └─ AbstractElementDef
              └─ 30+ *Def classes

ProcessorPluginAdapter (❌ STARY ADAPTER)
  └─ IElementDef
  └─ Processor<Def>
  
AbstractProcessorDecorator (❌ STARY)
  └─ IElementDef
  └─ Processor<Def>
```

---

## 6️⃣ PLAN USUNIĘCIA (Fazy)

### ✅ FAZA 1-3: DONE (r720-r727)
- [x] POC new parser
- [x] Config API modernization  
- [x] DefaultHarvest modernization

### 🔄 FAZA 4: Replace Scraper (NEXT)
- [ ] Scraper.java - replace getElementDef()
- [ ] Update Scraper to use PluginConfigurationTree

### 🔄 FAZA 5: Remove Adapters
- [ ] ProcessorPluginAdapter
- [ ] PluginProcessorAdapter
- [ ] AbstractProcessorDecorator

### 🔄 FAZA 6: Delete *Def Classes
- [ ] Delete all 36 *Def.java files
- [ ] Delete IElementDef.java
- [ ] Delete AbstractElementDef.java

### 🔄 FAZA 7: Delete Old Parser
- [ ] Delete SAXConfigParser.java
- [ ] Delete XmlNode.java
- [ ] Delete DefinitionResolver.java

### 🔄 FAZA 8: Final Cleanup
- [ ] Remove Processor<Def> interface
- [ ] Remove ProcessorFactory
- [ ] Update all tests
- [ ] Documentation update

---

## 7️⃣ METRYKI POSTĘPU

### Code Lines (szacunkowo):

| Component | Obecnie | Po usunięciu | Redukcja |
|-----------|---------|--------------|----------|
| *Def classes | ~3000 lines | 0 | -100% |
| Parser | ~1200 lines | ~350 | -71% |
| Adapters | ~500 lines | 0 | -100% |
| **TOTAL** | **~4700** | **~350** | **-93%** |

### API Calls:

| API | r720 | r727 | Target |
|-----|------|------|--------|
| getElementDef() | ~25 | ~18 | 0 |
| getConfigurationTree() | 0 | 7 | ALL |

### Progress:

**r720-r727:** 8 commitów  
**Usunięte użycia:** 7 (28%)  
**Pozostałe użycia:** 18 (72%)

---

## 8️⃣ NASTĘPNE KROKI (Priorytet)

### HIGH PRIORITY (Core):
1. **Scraper.java** - główny runtime
2. **AbstractProcessorDecorator.java** - bazowa klasa
3. **ProcessorPluginAdapter.java** - most między systemami

### MEDIUM PRIORITY (Tests):
4. ConfigPluginTest
5. AbstractProcessorDecoratorTest
6. ProcessorPluginAdapterTest

### LOW PRIORITY (Eventually):
7. Delete *Def classes (bulk)
8. Delete old parser infrastructure

---

## 🎯 KLUCZOWY WNIOSEK

**Pozostało do zrobienia:**

### PRODUKCJA (3 pliki):
- Scraper.java
- AbstractProcessorDecorator.java
- ProcessorPluginAdapter.java

### INFRASTRUCTURE (36+ plików):
- 36 klas *Def
- SAXConfigParser
- XmlNode
- DefinitionResolver
- Processor system

### PROGRESS: ~30% complete

**Następny krok**: Modernizacja Scraper.java

---

**Status**: v2.2.x r727  
**Commity**: 8 (r720-r727)  
**Tests**: 2633/2633 PASS ✅  
**Build**: SUCCESS ✅
