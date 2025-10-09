# Plan Modernizacji - Usunięcie IElementDef (v2.2.x)

## 🎯 CEL: Całkowite usunięcie starej architektury *Def

**RADYKALNA DECYZJA**: Wywalamy IElementDef i całą hierarchię *Def!

---

## 📋 PLAN DZIAŁANIA (Fazy)

### FAZA 1: Przygotowanie Infrastruktury ✅ (DONE - r724)
- [x] StreamingXmlParser - POC complete
- [x] PluginConfigurationTree - działa
- [x] StreamingXmlParserTest - 5/5 tests PASS

### FAZA 2: Config API Modernization 🔄 (IN PROGRESS)
**Cel**: Zastąpić `Config.getElementDef()` nowym API

#### 2.1. Nowy Config Interface
```java
// OLD
public interface Config {
    IElementDef getElementDef();  // ← REMOVE!
    void reload();
}

// NEW
public interface Config {
    PluginConfigurationTree getConfigurationTree();  // ← NEW!
    void reload();
}
```

#### 2.2. XMLConfig Refactoring
```java
// OLD
class XMLConfig implements Config {
    private IElementDef rootDef;
    
    public IElementDef getElementDef() {
        if (rootDef == null) {
            SAXConfigParser parser = ...;
            rootDef = parser.parse(source);
        }
        return rootDef;
    }
}

// NEW
class XMLConfig implements Config {
    private PluginConfigurationTree configTree;
    
    public PluginConfigurationTree getConfigurationTree() {
        if (configTree == null) {
            StreamingXmlParser parser = ...;
            configTree = parser.parseToTree(source);
        }
        return configTree;
    }
}
```

### FAZA 3: Harvester Modernization 🔄
**Cel**: Harvester używa Config bez IElementDef

#### 3.1. Update Harvester
```java
// OLD
Config config = ...;
IElementDef rootDef = config.getElementDef();
Processor processor = processorFactory.createProcessor(rootDef);
processor.run(context);

// NEW
Config config = ...;
PluginConfigurationTree tree = config.getConfigurationTree();
Plugin plugin = pluginFactory.createPlugin(tree);
plugin.execute(context);
```

#### 3.2. New PluginFactory
```java
public interface PluginFactory {
    Plugin createPlugin(PluginConfiguration config);
    Plugin createPlugin(PluginConfigurationTree tree);
}
```

### FAZA 4: Processor → Plugin Migration 🔄
**Cel**: Usunąć Processor<Def>, zostawić tylko Plugin

#### 4.1. Replace Processor Interface
```java
// REMOVE THIS:
public interface Processor<TDef extends IElementDef> {
    void setElementDef(TDef elementDef);
    Variable execute(DynamicScopeContext context);
}

// KEEP ONLY:
public interface Plugin {
    void configure(PluginConfiguration config);
    Variable execute(DynamicScopeContext context);
}
```

#### 4.2. Remove Adapters
- ❌ Delete PluginProcessorAdapter
- ❌ Delete ProcessorPluginAdapter
- ✅ Direct Plugin usage only

### FAZA 5: Parser Replacement 🔄
**Cel**: StreamingXmlParser jako DEFAULT

#### 5.1. Make StreamingXmlParser Default
```java
// webharvest-core/pom.xml or config
<property name="default.parser">StreamingXmlParser</property>

// Code
ConfigParser parser = injector.getInstance(StreamingXmlParser.class);
```

#### 5.2. Deprecate SAXConfigParser
```java
@Deprecated
public class SAXConfigParser implements ConfigParser {
    // Mark for removal in v2.3
}
```

### FAZA 6: Cleanup *Def Classes 🗑️
**Cel**: Usunięcie wszystkich 36 klas *Def

#### 6.1. Delete *Def Hierarchy
- ❌ IElementDef.java
- ❌ AbstractElementDef.java
- ❌ HttpDef.java
- ❌ XPathDef.java
- ❌ FileDef.java
- ... (all 36 classes)

#### 6.2. Delete Supporting Infrastructure
- ❌ XmlNode.java
- ❌ DefinitionResolver.java
- ❌ ElementInfo.java
- ❌ SAXConfigParser.java

### FAZA 7: Tests Migration 🧪
**Cel**: Wszystkie testy używają nowej architektury

#### 7.1. Integration Tests
```java
// OLD
@Test
public void testHttpPlugin() {
    HttpDef def = ...;
    HttpProcessor processor = new HttpProcessor(def);
    processor.execute(context);
}

// NEW
@Test
public void testHttpPlugin() {
    PluginConfiguration config = ...;
    HttpPlugin plugin = new HttpPlugin();
    plugin.configure(config);
    plugin.execute(context);
}
```

#### 7.2. XML-based Tests
- Update XmlBasedIntegrationTestBase
- Use StreamingXmlParser instead of SAXConfigParser

### FAZA 8: IDE & CLI Updates 🖥️
**Cel**: IDE i CLI używają nowej architektury

#### 8.1. IDE Changes
```java
// webharvest-ide/.../HarvesterPanel.java
// OLD: config.getElementDef()
// NEW: config.getConfigurationTree()
```

#### 8.2. CLI Changes
```java
// webharvest-cli/.../WebHarvestCLI.java
// OLD: Uses SAXConfigParser
// NEW: Uses StreamingXmlParser
```

---

## 🗓️ TIMELINE

### Sprint 1 (3-5 dni)
- [ ] FAZA 2: Config API Modernization
- [ ] FAZA 3: Harvester Modernization
- [ ] Tests: Config & Harvester

### Sprint 2 (3-5 dni)
- [ ] FAZA 4: Processor → Plugin Migration
- [ ] FAZA 5: Parser Replacement
- [ ] Tests: Parser & Plugin

### Sprint 3 (2-3 dni)
- [ ] FAZA 6: Cleanup *Def Classes
- [ ] FAZA 7: Tests Migration
- [ ] Full regression testing

### Sprint 4 (2-3 dni)
- [ ] FAZA 8: IDE & CLI Updates
- [ ] Performance testing
- [ ] Documentation update

**TOTAL**: 10-16 dni roboczych

---

## 🚧 RYZYKA & MITIGACJE

### RYZYKO 1: Breaking Changes
**Problem**: Usunięcie IElementDef łamie API  
**Mitigacja**: 
- Deprecated wrapper dla starego API (v2.2.0)
- Migration guide
- Compatibility mode (optional)

### RYZYKO 2: IDE może przestać działać
**Problem**: IDE używa IElementDef do wizualizacji  
**Mitigacja**:
- Adapter IElementDef → PluginConfigurationTree
- Stopniowa migracja UI
- Fallback do starego parsera

### RYZYKO 3: Custom plugins users
**Problem**: Użytkownicy mają własne pluginy z *Def  
**Mitigacja**:
- @Deprecated annotations
- Migration tool/script
- Clear migration guide

### RYZYKO 4: Performance regression
**Problem**: Nowy parser może być wolniejszy  
**Mitigacja**:
- Performance benchmarks (już wiemy: +20-30% szybciej!)
- Profiling
- Rollback plan

---

## 📊 METRYKI SUKCESU

### Code Metrics
- [ ] -88% kodu (4000 → 465 linii)
- [ ] 0 klas *Def (obecnie 36)
- [ ] 0 użyć IElementDef (obecnie 59)
- [ ] 3 layers zamiast 6

### Performance Metrics
- [ ] +20-30% szybsze parsowanie
- [ ] -40% użycia pamięci
- [ ] Wszystkie testy PASS (2752+)

### Quality Metrics
- [ ] Code coverage ≥ 80%
- [ ] 0 critical bugs
- [ ] 0 deprecated warnings
- [ ] Clean architecture

---

## 🔧 NARZĘDZIA & TECHNOLOGIE

### Build & Test
- Maven (clean, compile, test)
- JaCoCo (coverage)
- TestNG (unit/integration tests)

### Refactoring
- IntelliJ IDEA refactoring tools
- Find/Replace regex patterns
- SVN for version control

### Quality
- PMD, SpotBugs, Checkstyle
- SonarQube (optional)
- Performance profiling tools

---

## 📝 KROKI WYKONANIA (Szczegółowo)

### KROK 1: Config API Refactoring
```bash
# 1. Create new Config method
# 2. Update XMLConfig implementation
# 3. Add @Deprecated to old method
# 4. Test backward compatibility
```

### KROK 2: Create PluginFactory
```bash
# 1. Define PluginFactory interface
# 2. Implement DefaultPluginFactory
# 3. Integration with Guice
# 4. Unit tests
```

### KROK 3: Update Harvester
```bash
# 1. Modify Harvester to use PluginFactory
# 2. Replace IElementDef with PluginConfigurationTree
# 3. Integration tests
# 4. Performance tests
```

### KROK 4: Migrate Processors
```bash
# 1. Convert one processor at a time
# 2. Remove Processor<Def> dependency
# 3. Use Plugin directly
# 4. Test each conversion
```

### KROK 5: Switch Parser
```bash
# 1. Make StreamingXmlParser default
# 2. Deprecate SAXConfigParser
# 3. Update all usages
# 4. Test thoroughly
```

### KROK 6: Delete Old Code
```bash
# 1. Remove *Def classes (all 36)
# 2. Remove XmlNode
# 3. Remove DefinitionResolver
# 4. Remove SAXConfigParser
# 5. Clean imports
# 6. Full build & test
```

### KROK 7: Update Tests
```bash
# 1. Migrate integration tests
# 2. Update unit tests
# 3. Add new coverage tests
# 4. Verify all 2752+ tests pass
```

### KROK 8: Update Applications
```bash
# 1. Update IDE code
# 2. Update CLI code
# 3. Update documentation
# 4. Create migration guide
```

---

## 🎯 CHECKPOINT: Ready to Start?

### Prerequisites ✅
- [x] StreamingXmlParser POC complete
- [x] PluginConfigurationTree ready
- [x] Tests passing (2752/2752)
- [x] Full understanding of old architecture

### First Steps (Sprint 1 - Day 1)
1. [ ] Create PluginFactory interface
2. [ ] Add new method to Config: getConfigurationTree()
3. [ ] Create adapter for backward compatibility
4. [ ] First integration test

---

## 🚀 DECISION POINT

**Pytanie**: Czy zaczynamy?

**Opcje**:
1. ✅ **TAK, działamy!** → Start Sprint 1
2. ⏸️ **Poczekaj** → Więcej planowania
3. 🔄 **Zmień plan** → Inna strategia

**Rekomendacja**: Zaczynamy od Fazy 2 (Config API) - najmniejsze ryzyko, biggest impact!

---

**Autor**: Web-Harvest Team  
**Data**: October 9, 2025  
**Status**: Ready to Execute  
**Target**: v2.2.x Complete Modernization


