# Dokumentacja - Status i Zadania

**Data:** 2025-11-03  
**Status:** ✅ **SKONSOLIDOWANA**

---

## 📊 Obecny Stan (Po Konsolidacji)

### Pliki .md na głównym poziomie: **24 pliki** ✅

**Kategorie:**
- ✅ **Core docs (6):** README.md, USER_GUIDE.md, DEVELOPER_GUIDE.md, CHANGELOG.md, CONTRIBUTING.md, PLUGINS.md
- ✅ **Reference (3):** BUGS_FIXED.md, COMPONENT_LIBRARY.md, CONDUCTOR_ANALYSIS_AND_ROADMAP.md
- ✅ **Technical guides (6):** HTTPCLIENT5_MIGRATION_GUIDE.md, PAUSE_RESUME_GUIDE.md, ENHANCED_METRICS_GUIDE.md, WORKING_XML_SYNTAX_REFERENCE.md, XPATH_BEST_PRACTICES.md, PLUGIN_DOCS_GENERATION.md
- ✅ **Operations (4):** SFTP-UPLOAD-INSTRUCTIONS.md, SOURCEFORGE-SETUP.md, OCTOBER_2025_RELEASE_NOTES.md, XQUERY_FIX_SUMMARY.md
- ✅ **Utilities (2):** BUILD_AND_TEST.md, DOCS_UPLOAD_SIMPLIFICATION.md
- ✅ **Metadata (3):** DOCUMENTATION_INDEX.md, DOCUMENTATION_STATUS.md, RELEASE_SCRIPTS_ENHANCEMENT.md

### Zarchiwizowane pliki: **~50 plików** → `docs/archive/`

---

## ✅ Zadania Zrealizowane

### Visual Designer (v2.3.1)
- ✅ Drag & drop z palety pluginów
- ✅ Zagnieżdżanie kontenerów
- ✅ Zmiana kolejności pluginów (⬆️⬇️)
- ✅ Walidacja semantyczna
- ✅ Synchronizacja XML ↔ Visual
- ✅ Undo/Redo/Copy/Paste
- ✅ Naprawiony layout canvasu (nie "pływa")
- ✅ Przesuwanie węzłów działa poprawnie z zoom/pan

### Dokumentacja
- ✅ Utworzono USER_GUIDE.md (zunifikowany przewodnik)
- ✅ Utworzono DEVELOPER_GUIDE.md (przewodnik deweloperski)
- ✅ Zaktualizowano DOCUMENTATION_INDEX.md
- ⚠️ **Częściowa konsolidacja** - wiele plików SUMMARY/COMPLETE/PLAN pozostaje na głównym poziomie

---

## 📋 Zadania Planowane (z DEVELOPER_GUIDE.md)

### Visual Designer - Zaawansowane Funkcje
1. **Manual connections (ports)** - Ręczne definiowanie połączeń między komponentami
2. **Execution state visualization** - Wizualizacja stanu wykonania workflow
3. **Property panels with advanced editors** - Zaawansowane edytory właściwości
4. **Minimap and zoom controls** - Minimapa i kontrola zoomu
5. **Multi-select with marquee** - Zaznaczanie wielu elementów

---

## ✅ Wykonane Czynności (2025-11-03)

### 1. Archiwizacja Duplikatów ✅

**Zarchiwizowano ~50 plików:**
- Wszystkie *SUMMARY*.md, *COMPLETE*.md, *PLAN*.md, *STATUS*.md z głównego katalogu (~30 plików)
- Pliki z webharvest-ide/ (~20 plików)
- Przeniesiono do `docs/archive/` i `docs/archive/webharvest-ide/`

### 2. Aktualizacja DOCUMENTATION_INDEX.md ✅

- Zaktualizowano statystyki (61 → 24 pliki)
- Dodano informacje o konsolidacji z listopada 2025
- Zaktualizowano listę core files

### 3. Oczyszczenie Struktury ✅

**Aktualna struktura głównego katalogu (24 pliki):**
- Core (6): README, USER_GUIDE, DEVELOPER_GUIDE, CHANGELOG, CONTRIBUTING, PLUGINS
- Reference (3): BUGS_FIXED, COMPONENT_LIBRARY, CONDUCTOR_ANALYSIS_AND_ROADMAP
- Guides (6): HTTPCLIENT5_MIGRATION, PAUSE_RESUME, ENHANCED_METRICS, XML_SYNTAX, XPATH_BEST_PRACTICES, PLUGIN_DOCS_GENERATION
- Operations (4): SFTP-UPLOAD, SOURCEFORGE-SETUP, RELEASE_NOTES, XQUERY_FIX
- Utilities (2): BUILD_AND_TEST, DOCS_UPLOAD_SIMPLIFICATION
- Metadata (3): DOCUMENTATION_INDEX, DOCUMENTATION_STATUS, RELEASE_SCRIPTS_ENHANCEMENT

---

## 📝 Notatki

- **DOCUMENTATION_CONSOLIDATION_SUMMARY.md** wskazuje, że konsolidacja była wykonana w październiku 2025, ale od tego czasu pojawiło się więcej plików SUMMARY/COMPLETE/PLAN.
- Większość plików do archiwizacji to podsumowania sesji i statusy implementacji - wartościowe historycznie, ale nie powinny być na głównym poziomie.
- Pliki w `webharvest-ide/` też wymagają przeglądu i archiwizacji.
- **2025-11-21:** kompletny przegląd i modernizacja przykładów IDE (`webharvest-ide/src/main/resources/webapp/examples`). Usunięto pseudo-pluginy (`body`, placeholderowe wyniki), legacy moduły (ftp/mail/database) zastąpiono symulacjami bazującymi na core pluginach, a wszystkie pliki przechodzą walidację /api/config/validate.
- **2025-11-21:** Property Panel korzysta z nowego `PropertyPanelShell` + `SchemaDrivenEditor`, więc dokumentacja powinna odsyłać do metadanych (`AttributeUiMetadata`, `ui.tools`) zamiast opisywać stare ręczne formularze.
- **2025-11-21:** dodano skrypt `webharvest-ide/scripts/sweep_examples.ts`, który weryfikuje round-trip XML i raportuje auto-konwersje legacy – wyniki trafiają do logów i powinny być wspomniane w przewodnikach QA/Release.
- **2025-11-21:** Startuje wydzielanie pakietów współdzielonych (`packages/visual-shared/*`) + VS Code harness. Dokumentacja powinna odsyłać do `VSCODE_EXTENSION_COMPATIBILITY_PLAN.md` i `docs/VSCODE_WEBVIEW_HARNESS.md` dla najświeższych wytycznych.

---

## 📈 Wyniki Konsolidacji

**Przed:** 61 plików .md na głównym poziomie  
**Po:** 24 pliki .md na głównym poziomie  
**Redukcja:** 61% (37 plików przeniesionych do archiwum)

**Zarchiwizowane:**
- ~30 plików SUMMARY/COMPLETE/PLAN/STATUS z głównego katalogu
- ~20 plików z webharvest-ide/
- Wszystkie pliki w `docs/archive/` (170+ plików historycznych)

**Status:** ✅ Dokumentacja skonsolidowana i zorganizowana

