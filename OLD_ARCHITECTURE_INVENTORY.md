# Inwentaryzacja Starej Architektury (v2.2.x)

## 🔍 CO ZOSTAŁO ZE STAREJ ARCHITEKTURY?

### 📊 Statystyki

**Pliki *Def**: 36 klas  
**Użycia *Def**: 59 odniesień  
**Procesorów @Definition**: 3 klasy

---

## 1️⃣ RDZEŃ STAREJ ARCHITEKTURY (NIEZBĘDNY)

### **IElementDef** - Główny interfejs
```
Location: webharvest-core/src/main/java/org/webharvest/definition/IElementDef.java
Purpose: Marker interface dla wszystkich definicji elementów
Status: ✅ MUSI ZOSTAĆ - używany przez SAXConfigParser
```

**Dlaczego musi zostać:**
- `SAXConfigParser` zwraca `IElementDef` jako wynik parsowania
- `Config.getElementDef()` wymaga `IElementDef`
- `Processor<TDef extends IElementDef>` - generyczny interface

### **AbstractElementDef** - Klasa bazowa
```
Location: webharvest-core/src/main/java/org/webharvest/definition/AbstractElementDef.java
Purpose: Bazowa implementacja dla wszystkich *Def
Status: ✅ MUSI ZOSTAĆ - baza dla 30+ klas
Dependencies: XmlNode (stara architektura parsera)
```

---

## 2️⃣ 36 KLAS *Def (Definicje Elementów)

### Core Definitions (Podstawowe)
1. ✅ **ConfigDef** - Główny config
2. ✅ **ConstantDef** - Stałe wartości
3. ✅ **TextDef** - Tekst
4. ✅ **TemplateDef** - Szablony
5. ✅ **EmptyDef** - Puste elementy
6. ✅ **SimpleElementDef** - Proste elementy

### HTTP & Web
7. ✅ **HttpDef** - Żądania HTTP
8. ✅ **HttpParamDef** - Parametry HTTP
9. ✅ **HttpHeaderDef** - Nagłówki HTTP

### XML Processing
10. ✅ **XPathDef** - XPath queries
11. ✅ **XQueryDef** - XQuery
12. ✅ **XQueryExternalParamDef** - Parametry XQuery
13. ✅ **XsltDef** - Transformacje XSLT
14. ✅ **HtmlToXmlDef** - Konwersja HTML→XML

### Control Flow
15. ✅ **IfDef** - Warunki
16. ✅ **LoopDef** - Pętle
17. ✅ **WhileDef** - While loops
18. ✅ **CaseDef** - Switch/case
19. ✅ **TryDef** - Obsługa błędów
20. ✅ **ExitDef** - Wyjście
21. ✅ **ReturnDef** - Return

### Variables & Functions
22. ✅ **VarDef** - Odczyt zmiennych
23. ✅ **VarDefDef** - Definicja zmiennych
24. ✅ **CallDef** - Wywołania funkcji
25. ✅ **CallParamDef** - Parametry wywołań
26. ✅ **FunctionDef** - Definicje funkcji

### I/O Operations
27. ✅ **FileDef** - Operacje plikowe
28. ✅ **IncludeDef** - Include plików

### Scripting & Regex
29. ✅ **ScriptDef** - Kod JavaScript/Groovy
30. ✅ **RegexpDef** - Wyrażenia regularne

### Plugin Definitions
31. ✅ **WebHarvestPluginDef** - Pluginy Web-Harvest
32. ✅ **CorePluginDef** - Core plugins
33. ✅ **CorePluginWebHarvestPluginDef** - Core plugin wrapper

### Other
34. ✅ **ElementInfo** - Informacje o elementach
35. ✅ **CorePluginElementInfo** - Info o core plugins
36. ✅ **DefaultPluginRegistrationStrategy** - Strategia rejestracji

---

## 3️⃣ PROCESORÓW SYSTEM (Stary)

### **Processor<TDef extends IElementDef>**
```
Location: webharvest-core/src/main/java/org/webharvest/runtime/processors/Processor.java
Purpose: Główny interface dla procesorów (stary system)
Status: ✅ MUSI ZOSTAĆ - używany przez 50+ procesorów
```

### Kluczowe Procesory
1. ✅ **AbstractProcessor** - Bazowa klasa
2. ✅ **BodyProcessor** - Procesowanie body
3. ✅ **EmptyProcessor** - Pusty procesor
4. ✅ **ProcessorFactory** - Fabryka procesorów
5. ✅ **DefaultProcessorFactory** - Domyślna fabryka
6. ✅ **PluginProcessorAdapter** - Adapter Plugin→Processor

---

## 4️⃣ PARSER SYSTEM (Stary)

### **SAXConfigParser**
```
Location: webharvest-core/src/main/java/org/webharvest/definition/SAXConfigParser.java
Purpose: Stary parser XML → *Def → Processor
Status: ✅ MUSI ZOSTAĆ - używany przez całą aplikację
Flow: XML → XmlNode → DefinitionResolver → *Def → Processor
```

### **XmlNode**
```
Status: ✅ MUSI ZOSTAĆ - używany przez SAXConfigParser i *Def
Purpose: Reprezentacja węzła XML
```

### **DefinitionResolver**
```
Status: ✅ MUSI ZOSTAĆ - mapuje elementy XML na *Def
Purpose: element name + namespace → ElementInfo → *Def class
```

---

## 5️⃣ CONFIG SYSTEM

### **Config Interface**
```java
public interface Config {
    IElementDef getElementDef();  // ← Wymaga IElementDef!
    void reload();
}
```
**Status**: ✅ MUSI ZOSTAĆ - używany przez Harvester

### **XMLConfig**
```
Purpose: Implementacja Config dla XML
Status: ✅ MUSI ZOSTAĆ
Dependencies: SAXConfigParser, IElementDef
```

---

## 6️⃣ CO MOŻNA USUNĄĆ? (Przyszłość v2.3+)

### ❌ Po wdrożeniu StreamingXmlParser:
1. ❌ Niektóre metody w IElementDef (deprecated)
2. ❌ XmlNode (gdy nowy parser przejmie)
3. ❌ DefinitionResolver (gdy PluginRegistry przejmie)

### ⚠️ Wymaga migracji (v2.3-v2.4):
1. **SAXConfigParser** → **StreamingXmlParser**
2. **IElementDef tree** → **PluginConfigurationTree**
3. **Processor<Def>** → **Plugin** (już częściowo zrobione)

---

## 🔄 HYBRYDOWA ARCHITEKTURA (Obecnie v2.2.x)

### STARA (SAXConfigParser):
```
XML → SAXConfigParser → XmlNode → DefinitionResolver → *Def → Processor
```
**Status**: ✅ Działająca, używana w produkcji

### NOWA (StreamingXmlParser):
```
XML → StreamingXmlParser → PluginConfigurationTree → Plugin
```
**Status**: ✅ POC Complete (v2.2.x r724)

### ADAPTACJA (Most między systemami):
```
*Def → PluginProcessorAdapter → Plugin
Processor → ProcessorPluginAdapter → *Def
```
**Status**: ✅ Działa, pozwala na koegzystencję

---

## 📈 PLAN MIGRACJI (v2.2.x → v2.3.x → v2.4.x)

### v2.2.x (OBECNA) ✅
- ✅ Stara architektura działa
- ✅ Nowy parser POC gotowy
- ✅ Oba systemy współistnieją

### v2.3.x (NAJBLIŻSZA)
- [ ] Migrate XMLConfig to use StreamingXmlParser (optional)
- [ ] Add PluginRegistry element lookup
- [ ] Deprecate some *Def methods
- [ ] More tests for new parser

### v2.4.x (PÓŹNIEJ)
- [ ] Make StreamingXmlParser default
- [ ] Deprecate SAXConfigParser
- [ ] Start migrating *Def → PluginConfiguration

### v2.5.x+ (DŁUGOTERMINOWO)
- [ ] Remove deprecated *Def methods
- [ ] Optional: Remove XmlNode
- [ ] Cleanup old resolver

---

## 🎯 PODSUMOWANIE

### MUSI ZOSTAĆ (na razie):
1. ✅ **36 klas *Def** - używane przez SAXConfigParser
2. ✅ **IElementDef hierarchy** - API contracts
3. ✅ **SAXConfigParser** - główny parser (default)
4. ✅ **XmlNode** - reprezentacja XML
5. ✅ **DefinitionResolver** - mapowanie elementów
6. ✅ **Processor interface** - stary system procesorów
7. ✅ **Config.getElementDef()** - API

### MOŻE BYĆ USUNIĘTE (przyszłość):
1. ❌ XmlNode (gdy StreamingXmlParser przejmie)
2. ❌ DefinitionResolver (gdy PluginRegistry przejmie)
3. ❌ Niektóre deprecated metody w *Def

### NOWE (v2.2.x r724):
1. ✅ **StreamingXmlParser** - POC
2. ✅ **PluginConfigurationTree** - nowa struktura
3. ✅ **v3 package** - przyszłość

---

## 🔑 KLUCZOWY WNIOSEK

**Stara architektura MUSI POZOSTAĆ w v2.2.x** ponieważ:

1. **SAXConfigParser** jest głównym parserem (default)
2. **Config API** zwraca `IElementDef`
3. **50+ procesorów** używa `Processor<Def>`
4. **Całe IDE** opiera się na `IElementDef` tree
5. **Backward compatibility** - istniejące konfiguracje

**NOWY PARSER** to:
- ✅ POC (Proof of Concept)
- ✅ Alternatywna ścieżka parsowania
- ✅ Przyszłość (v2.3+)
- ✅ Koegzystuje ze starym

**NIE zastępuje** starej architektury w v2.2.x, tylko **dodaje opcję**!

---

**Status**: v2.2.x - Hybrydowa architektura  
**Data**: October 9, 2025  
**Następny krok**: v2.3.x - Deprecations & Optional New Parser
