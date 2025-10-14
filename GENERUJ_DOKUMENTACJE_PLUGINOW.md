# Generowanie Dokumentacji Pluginów WebHarvest

## 🚀 Szybki Start

### Wygeneruj całą dokumentację jedną komendą:

```bash
python3 generate_all_plugin_docs.py
```

### Obejrzyj dokumentację:

```bash
cd docs/web
python3 -m http.server 8000
# Otwórz: http://localhost:8000/documentation/2.2.0/
```

## 📊 Co zostanie wygenerowane?

- ✅ **46 stron core plugins** (http, xpath, loop, itp.)
- ✅ **8 stron external plugins** (mail, ftp, zip, web-browser)
- ✅ **1 strona indeksowa** ze wszystkimi 54 pluginami
- ✅ **Razem: 55 stron HTML**

## 📂 Struktura

```
docs/web/documentation/2.2.0/
├── index.html              # Strona główna z wszystkimi pluginami
├── http.html               # Core plugins (46 plików)
├── xpath.html
├── loop.html
├── ...
└── 2.2/
    ├── mail.html           # External plugins (8 plików)
    ├── ftp.html
    └── ...
```

## 🔧 Skrypty Generujące

### 1. Master Skrypt (REKOMENDOWANE)

```bash
python3 generate_all_plugin_docs.py
```

Uruchamia wszystkie generatory w poprawnej kolejności.

### 2. Indywidualne Skrypty

```bash
# Tylko core plugins (46)
python3 generate_core_plugin_docs.py

# Tylko external plugins (8)
python3 generate_external_plugin_docs.py

# Tylko strona indeksowa
python3 generate_plugins_index.py
```

## 📋 Kategorie Pluginów

### Core Plugins (46)

- **CRITICAL** (9): http, xpath, regexp, tokenize, while, function, call, html-to-xml, file
- **IMPORTANT** (11): def, get-var, set-var, loop, script, template, json-to-xml, xml-to-json, if, xquery, include
- **COMMON** (10): empty, sleep, exit, try, catch, return, text, case, constant, var
- **ADVANCED** (16): config, value-of, xml, xq-expression, xq-param, xslt, stylesheet, http-header, http-param, call-param, else, regexp-pattern, regexp-source, regexp-result, list, body-processor

### External Plugins (8)

- mail, ftp, ftp-get, ftp-put, ftp-list, zip, zip-entry, web-browser

## 🎯 Jak to działa?

1. **Czyta kod źródłowy** pluginów z `webharvest-core/src/` i modułów external
2. **Wyciąga JavaDoc** z komentarzy w kodzie Java
3. **Parsuje przykłady** użycia z sekcji `<h4>Example`
4. **Generuje HTML** z profesjonalnym stylem
5. **Tworzy stronę indeksową** ze wszystkimi 54 pluginami

## 📝 Dodawanie Nowego Pluginu

### Core Plugin

1. Dodaj klasę pluginu do kategorii w `generate_core_plugin_docs.py`:

```python
CATEGORIES = {
    'CRITICAL': [..., 'TwojNowyPlugin'],
}

PLUGIN_NAMES = {
    'TwojNowyPlugin': 'twoj-nowy-plugin',
}
```

2. Uruchom: `python3 generate_all_plugin_docs.py`

### External Plugin

1. Dodaj do `generate_external_plugin_docs.py`:

```python
PLUGINS = {
    'webharvest-twojmodul': ['TwojPlugin'],
}
```

2. Dodaj do `generate_plugins_index.py`:

```python
EXTERNAL_PLUGINS = {
    'twoj-plugin': {
        'title': 'Twój Plugin',
        'description': 'Opis pluginu',
        'icon': 'ikona-font-awesome',
        'path': '2.2/twoj-plugin.html'
    },
}
```

3. Uruchom: `python3 generate_all_plugin_docs.py`

## 🆘 Problemy?

### Plugin nie został wygenerowany

Sprawdź czy:
- ✅ Plik Java istnieje w odpowiednim katalogu
- ✅ Klasa ma adnotację `@CorePlugin`
- ✅ Plugin jest dodany do `CATEGORIES` lub `PLUGINS`
- ✅ Nazwa w `PLUGIN_NAMES` jest poprawna

### Brak przykładów w dokumentacji

Upewnij się że JavaDoc zawiera:

```java
/**
 * <h4>Example 1: Tytuł</h4>
 * <pre>{@code
 * <twoj-plugin attr="wartość"/>
 * }</pre>
 */
```

## 📖 Więcej Informacji

- **Pełna dokumentacja** (EN): `PLUGIN_DOCS_GENERATION.md`
- **Guide pluginów** (PL/EN): `PLUGINS.md`
- **Inwentarz pluginów**: `CORE_PLUGINS_INVENTORY.md`

## ✨ Przykład Wyjścia

```
============================================================
🚀 WebHarvest Complete Plugin Documentation Generator
============================================================

📌 This will generate documentation for:
   • 46 CORE plugins (built-in, @CorePlugin)
   • 8 EXTERNAL plugins (mail, ftp, zip, web-browser)
   • TOTAL: 54 plugin pages

[... generuje dokumentację ...]

✅ ALL PLUGIN DOCUMENTATION GENERATED SUCCESSFULLY!
============================================================

📂 Output locations:
   • Index page: docs/web/documentation/2.2.0/index.html
   • Core plugins: docs/web/documentation/2.2.0/*.html (46 files)
   • External plugins: docs/web/documentation/2.2.0/external/*.html (8 files)

🌐 View documentation:
   cd docs/web && python3 -m http.server 8000
   Then open: http://localhost:8000/documentation/2.2.0/

📋 Complete plugin reference:
   • CRITICAL: 9 plugins
   • IMPORTANT: 11 plugins
   • COMMON: 10 plugins
   • ADVANCED: 16 plugins
   • EXTERNAL: 8 plugins
```

---

**Ostatnia aktualizacja**: 14 października 2025  
**WebHarvest**: v2.2.0

