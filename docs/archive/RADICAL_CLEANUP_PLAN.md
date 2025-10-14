# Radykalne Odchudzenie Core v2.2.x

## 🎯 CEL: Maksymalnie odchudzony, szybki, wydajny core

**KONIEC z backward compatibility!**  
**USUWAMY stary system całkowicie!**

---

## 🗑️ DO USUNIĘCIA (Radykalnie):

### 1. CAŁY SYSTEM *DEF (36 klas, ~3000 linii)
```
❌ DELETE: webharvest-core/src/main/java/org/webharvest/definition/*Def.java
```

**Lista do usunięcia:**
- IElementDef.java
- AbstractElementDef.java  
- ConfigDef.java
- HttpDef.java
- XPathDef.java
- FileDef.java
- LoopDef.java
- IfDef.java
- TryDef.java
... (ALL 36 files!)

### 2. STARY PARSER (800+ linii)
```
❌ DELETE: SAXConfigParser.java
❌ DELETE: XmlNode.java
❌ DELETE: DefinitionResolver.java
❌ DELETE: ConfigurableResolver.java
❌ DELETE: CoreDefinitionResolver.java
❌ DELETE: ElementInfo.java
❌ DELETE: CorePluginElementInfo.java
```

### 3. PROCESSOR SYSTEM (500+ linii)
```
❌ DELETE: Processor.java interface
❌ DELETE: ProcessorFactory.java
❌ DELETE: DefaultProcessorFactory.java
❌ DELETE: AbstractProcessor.java
❌ DELETE: AbstractProcessorDecorator.java
❌ DELETE: BodyProcessor.java
❌ DELETE: EmptyProcessor.java
```

### 4. ADAPTERY (300+ linii)
```
❌ DELETE: PluginProcessorAdapter.java
❌ DELETE: ProcessorPluginAdapter.java
```

### 5. @Definition ANNOTATION
```
❌ DELETE: @Definition annotation (old processors)
```

---

## ✅ ZOSTAJE (Minimum):

### Core Execution
- ✅ Plugin interface
- ✅ AbstractPlugin (base class)
- ✅ @CorePlugin annotation
- ✅ PluginConfiguration
- ✅ DynamicScopeContext

### New Parser
- ✅ StreamingXmlParser
- ✅ PluginConfigurationTree
- ✅ ConfigParser interface (only StreamingXmlParser impl)

### Config System
- ✅ Config interface (only getConfigurationTree())
- ✅ XMLConfig (only new parser)
- ✅ ConfigSource

### Execution
- ✅ Scraper (only new path!)
- ✅ WebScraper interface
- ✅ Harvester

---

## 🚀 NOWA ARCHITEKTURA (Ultra-Lean):

```
XML → StreamingXmlParser → PluginConfigurationTree → Plugin
```

**That's it!** Koniec. Proste!

---

## 📊 IMPACT:

| Komponent | Przed | Po | Redukcja |
|-----------|-------|-----|----------|
| Parser | 800 linii | 350 | -56% |
| *Def classes | 3000 linii | 0 | -100% |
| Processor system | 500 linii | 0 | -100% |
| Adapters | 300 linii | 0 | -100% |
| **TOTAL** | **4600** | **350** | **-92%** |

**Performance**: +30-40% szybciej (szacunek)  
**Memory**: -50% mniej obiektów  
**Complexity**: 3 layers zamiast 6

---

## ⚡ EXECUTION ORDER:

### 1. NAJPIERW: Scraper - TYLKO nowa ścieżka
```java
// DELETE fallback to old path
// ONLY: executeWithNewArchitecture()
```

### 2. Config - TYLKO nowe API
```java
// DELETE: getElementDef()
// ONLY: getConfigurationTree()
```

### 3. XMLConfig - TYLKO StreamingXmlParser
```java
// DELETE: SAXConfigParser support
// ONLY: StreamingXmlParser
```

### 4. BULK DELETE
```bash
rm webharvest-core/src/main/java/org/webharvest/definition/*Def.java
rm SAXConfigParser.java XmlNode.java DefinitionResolver.java
rm Processor*.java *ProcessorAdapter.java
```

### 5. Fix Tests
- Update wszystkie testy
- Remove testy dla old system
- Add coverage dla new system

### 6. Final Cleanup
- Remove imports
- Remove @Deprecated
- Documentation

---

## ⚠️ BREAKING CHANGES:

**TO BĘDZIE BREAKING RELEASE!**

- ❌ Stary API usunięty całkowicie
- ❌ Custom plugins z @Definition nie zadziałają
- ❌ Code używający IElementDef się nie skompiluje

**ALE:**
- ✅ XML configs bez zmian (backward compatible!)
- ✅ @CorePlugin plugins działają
- ✅ Wszystko prostsze i szybsze

---

## 🎯 STRATEGY:

**Nie modernizujemy - USUWAMY!**

1. Scraper - tylko nowa ścieżka (remove fallback)
2. Config - tylko nowe API (remove getElementDef)
3. XMLConfig - tylko nowy parser (remove SAXConfigParser)
4. Bulk delete *Def classes
5. Bulk delete old parser
6. Bulk delete Processor system
7. Fix imports & tests
8. Ship it! 🚀

---

**Status**: Ready to execute  
**Impact**: -92% code  
**Risk**: High (breaking changes)  
**Reward**: Ultra-lean, fast core  

**NEXT**: Execute deletion plan?
