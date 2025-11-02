# Build & Test Guide

## 📌 Najnowsze usprawnienia (31.10.2025)

- 🔁 **Usunięto zduplikowaną zależność `org.jsoup`** w `webharvest-core/pom.xml` (pozostawiono wpis w sekcji „Bug #13”), co eliminuje ostrzeżenia Mavena o powielonych deklaracjach.
- 🧼 **Czyścimy frontendowy `dist/` przed buildem** – zapobiega to błędom `EPERM` przy nadpisywaniu plików przez Vite.
- 🔄 **Pełny build bez testów** (`mvn clean install -DskipTests`) przechodzi poprawnie i buduje wszystkie moduły.
- 🚪 **Port 8080** – przed uruchomieniem IDE warto zabić starą instancję (`pkill -f webharvest-ide-2.3.1.jar`), by uniknąć komunikatu „Port 8080 is already in use!”.

### Szybka sekwencja poleceń

```bash
cd /Users/robertbala/Documents/Git/web-harvest-git

# 1. Upewnij się, że nic nie blokuje portu 8080
pkill -f webharvest-ide-2.3.1.jar || true

# 2. Pełny build backendu (bez testów)
mvn clean install -DskipTests

# 3. (Opcjonalnie) ręczny rebuild frontendu
cd webharvest-ide/frontend
npm install
npm run build

# 4. Uruchom IDE
cd ../..
java -jar webharvest-ide/target/webharvest-ide-2.3.1.jar
```

> 💡 Po starcie log Jetty potwierdzi adres `http://localhost:8080`. Jeśli port był zajęty, serwer spróbuje połączyć się z istniejącą instancją i wypisze błąd.

## 🔧 Co zostało zmienione?

### 1. PropertyPanel.ts
- ✅ Dodano import SchemaEditor, ExpressionBuilder, XPathTester, RegexValidator
- ✅ Zmieniono render() aby używać zaawansowanych edytorów
- ✅ Dodano automatyczną detekcję typu pola (url, xpath, regex, expression)
- ✅ Dodano przyciski do otwierania zaawansowanych edytorów w modalu
- ✅ Dodano metody addAdvancedEditorButton() i openAdvancedEditor()

### 2. advanced-editors.css (NOWY)
- ✅ Style dla SchemaEditor
- ✅ Style dla ExpressionBuilder  
- ✅ Style dla XPathTester
- ✅ Style dla RegexValidator
- ✅ Style dla modalu

### 3. index.html
- ✅ Dodano link do advanced-editors.css

---

## 🚀 Jak zbudować?

### Metoda 1: Maven (Automatyczna)

```bash
cd /Users/robertbala/Documents/Git/web-harvest-git
mvn clean package -DskipTests -pl webharvest-ide -am
```

Maven wykona automatycznie:
1. `npm install` (zainstaluje zależności)
2. `npm run build` (TypeScript → JavaScript przez Vite)
3. Skopiuje `dist/` do `webapp/dist/`
4. Zbuduje JAR

**Czas budowania:** ~2-3 minuty

---

## 🧪 Jak przetestować?

### 1. Uruchom IDE

```bash
# Zatrzymaj poprzednią instancję
pkill -f "webharvest-ide"

# Uruchom nową
java -jar webharvest-ide/target/webharvest-ide-2.3.1.jar
```

### 2. Otwórz w przeglądarce

```
http://localhost:8080
```

**WAŻNE:** Użyj **hard refresh** aby wyczyścić cache:
- **Mac:** Cmd + Shift + R
- **Windows:** Ctrl + Shift + F5
- **Lub:** Otwórz w trybie incognito

### 3. Test manualny

#### Test 1: PropertyPanel z SchemaEditor

1. Przełącz na tryb **Visual** (przycisk w toolbarze)
2. Przeciągnij plugin **HTTP** na canvas
3. Kliknij w node HTTP
4. **Oczekiwany rezultat:**
   - Panel "Properties" pojawia się po prawej stronie
   - Pole "url" ma typ URL (z walidacją)
   - Pole "method" ma dropdown (jeśli jest enum)
   - Pola są ładnie ostylowane (nie proste inputy)

#### Test 2: XPath Tester Button

1. Przeciągnij plugin **XPath** na canvas
2. Kliknij w node
3. Znajdź pole "expression"
4. **Oczekiwany rezultat:**
   - Pod polem "expression" pojawia się przycisk "🔧 XPath Tester"
   - Kliknięcie przycisku otwiera modal
   - Modal zawiera:
     - Pole "XPath Expression"
     - Pole "Sample XML"
     - Sekcję "Results"
     - Przycisk "Apply & Close"

#### Test 3: Expression Builder

1. Przeciągnij plugin **Def** na canvas
2. W body wpisz: `${myVar}`
3. **Oczekiwany rezultat:**
   - Pod textarea pojawia się przycisk "🔧 Expression Builder"
   - Kliknięcie otwiera modal z:
     - Textarea dla wyrażenia
     - Lista dostępnych zmiennych
     - Lista funkcji (concat, replace, etc.)
     - Live preview
     - Przycisk "Apply & Close"

#### Test 4: Regex Validator

1. Przeciągnij plugin **Regexp** na canvas
2. Kliknij w node
3. Znajdź pole "pattern"
4. **Oczekiwany rezultat:**
   - Pod polem "pattern" pojawia się przycisk "🔧 Regex Validator"
   - Kliknięcie otwiera modal z:
     - Pole "Test Text"
     - Pole "Regex Pattern"
     - Checkboxy dla flag (g, i, m)
     - Sekcję "Matches" z highlighted results
     - Pattern library (Email, URL, Phone, etc.)

---

## 🐛 Troubleshooting

### Problem: Nie widzę zmian

**Rozwiązanie:**
```bash
# 1. Wyczyść cache npm
cd webharvest-ide/frontend
npm cache clean --force

# 2. Przebuduj
cd ../..
mvn clean package -DskipTests -pl webharvest-ide -am

# 3. Hard refresh w przeglądarce
# Mac: Cmd+Shift+R
# Windows: Ctrl+Shift+F5
```

### Problem: Brak przycisków "🔧 XPath Tester"

**Możliwe przyczyny:**
1. Frontend nie został zbudowany
2. Cache przeglądarki
3. Błąd w TypeScript (sprawdź browser console)

**Sprawdź:**
```bash
# Czy dist/ istnieje?
ls -la webharvest-ide/frontend/dist/

# Czy main.js jest aktualny?
stat webharvest-ide/frontend/dist/assets/main-*.js
```

### Problem: Modal się nie otwiera

**Sprawdź console:**
```
F12 → Console
```

**Szukaj błędów:**
- `Uncaught TypeError...` - Problem z importem
- `Cannot read property...` - Null pointer
- `Module not found...` - Missing dependency

**Rozwiązanie:**
```bash
# Sprawdź czy wszystkie pliki istnieją
ls -la webharvest-ide/frontend/src/visual/SchemaEditor.ts
ls -la webharvest-ide/frontend/src/visual/ExpressionBuilder.ts
ls -la webharvest-ide/frontend/src/visual/XPathTester.ts
ls -la webharvest-ide/frontend/src/visual/RegexValidator.ts

# Sprawdź czy nie ma błędów TypeScript
cd webharvest-ide/frontend
npm run type-check
```

### Problem: Style nie działają

**Sprawdź czy CSS załadowany:**
```
F12 → Network → Filter: CSS
```

**Szukaj:**
- `advanced-editors.css` - status 200 OK

**Jeśli 404:**
```bash
# Sprawdź czy plik istnieje
ls -la webharvest-ide/src/main/resources/webapp/static/css/advanced-editors.css

# Sprawdź czy jest w JARze
unzip -l webharvest-ide/target/webharvest-ide-2.3.1.jar | grep advanced-editors.css
```

---

## 📊 Checklist dla developerów

### Pre-Build:
- [ ] Wszystkie pliki TypeScript istnieją
- [ ] Brak błędów w `npm run type-check`
- [ ] PropertyPanel.ts importuje wszystkie advanced editors
- [ ] index.html ma link do advanced-editors.css

### Build:
- [ ] `mvn clean package` kończy się SUCCESS
- [ ] `dist/` folder istnieje
- [ ] `main-*.js` jest w dist/assets/
- [ ] JAR zawiera wszystkie pliki CSS

### Post-Build:
- [ ] IDE startuje bez błędów
- [ ] http://localhost:8080 otwiera się
- [ ] Browser console nie pokazuje błędów
- [ ] Przyciski "🔧" są widoczne
- [ ] Modals otwierają się i zamykają
- [ ] Zmiany są zapisywane

---

## 🎯 Expected Output

### Console (No errors):
```
🚀 WebHarvest IDE v2.2.0 - TypeScript Edition
✅ PluginPalette initialized with 27 plugins
✅ VisualDesigner initialized
✅ PropertyPanel initialized
✅ SchemaEditor ready
```

### Browser Network (All 200 OK):
```
main-*.js          200 OK  (JS bundle)
advanced-editors.css  200 OK  (CSS styles)
node-states.css    200 OK  (Node styles)
enterprise.css     200 OK  (Brand colors)
```

### Visual Test:
```
1. Przeciągnij HTTP → Canvas
2. Kliknij node
3. PropertyPanel pojawia się ✓
4. Pole URL z walidacją ✓
5. Przycisk "🔧 XPath Tester" (jeśli xpath) ✓
6. Modal otwiera się ✓
7. Apply & Close działa ✓
```

---

## 🏁 Success Criteria

Integracja jest **kompletna** gdy:

✅ PropertyPanel używa SchemaEditor  
✅ Przyciski "🔧" pojawiają się dla xpath/regex/expression  
✅ Modals otwierają się i działają  
✅ XPathTester waliduje XPath wyrażenia  
✅ RegexValidator testuje regex  
✅ ExpressionBuilder pokazuje autocomplete  
✅ Apply & Close aktualizuje wartości  
✅ Zmiany są widoczne po re-kliknięciu w node  

---

## 📞 Support

Jeśli nadal nie działa po wykonaniu wszystkich kroków:

1. **Sprawdź Maven output:**
   ```bash
   mvn clean package -DskipTests -pl webharvest-ide -am -X > build.log 2>&1
   tail -100 build.log
   ```

2. **Sprawdź Browser Console:**
   ```
   F12 → Console → Copy all errors
   ```

3. **Sprawdź Network Tab:**
   ```
   F12 → Network → Filter failed requests (status != 200)
   ```

4. **Sprawdź czy frontend buildnął się:**
   ```bash
   cat webharvest-ide/frontend/dist/index.html | grep "main"
   # Powinno pokazać: <script type="module" src="/dist/assets/main-*.js">
   ```

---

**Powodzenia! 🚀**

