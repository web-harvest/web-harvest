# 🔄 Czyszczenie Cache Przeglądarki - Web-Harvest IDE

## ⚠️ Problem
Widzisz **starą wersję IDE** mimo rebuild? To **cache przeglądarki**!

## ✅ Rozwiązanie

### **Opcja 1: Hard Refresh (NAJSZYBSZE)**

#### Chrome / Edge / Chromium
```
Windows/Linux:  Ctrl + Shift + R
Mac:            Cmd + Shift + R
```

#### Firefox
```
Windows/Linux:  Ctrl + F5
Mac:            Cmd + Shift + R
```

#### Safari
```
Mac:            Cmd + Option + R
```

---

### **Opcja 2: URL z Timestamp (100% SKUTECZNE)**

Dodaj timestamp do URL:
```
http://localhost:8080?v=1728662400
```

Lub użyj aktualnego czasu:
```bash
# Linux/Mac
echo "http://localhost:8080?v=$(date +%s)"

# Wynik np: http://localhost:8080?v=1728662454
```

Otwórz wygenerowany URL w przeglądarce - cache zostanie ominięty!

---

### **Opcja 3: DevTools Cache Disable**

1. Otwórz **DevTools** (F12)
2. Przejdź do zakładki **Network**
3. Zaznacz **Disable cache**
4. Odśwież stronę (F5)

---

### **Opcja 4: Wyczyść Cache Ręcznie**

#### Chrome/Edge
1. Otwórz: `chrome://settings/clearBrowserData`
2. Wybierz **Cached images and files**
3. Zakres: **Last hour**
4. Kliknij **Clear data**

#### Firefox
1. Otwórz: `about:preferences#privacy`
2. Sekcja **Cookies and Site Data**
3. Kliknij **Clear Data...**
4. Wybierz **Cached Web Content**

#### Safari
1. Menu: **Safari → Preferences → Advanced**
2. Włącz **Show Develop menu**
3. Menu: **Develop → Empty Caches**

---

## 🧪 Weryfikacja

Po wyczyszczeniu cache powinieneś zobaczyć:

✅ **Nową zakładkę "Session"** w panelu wyników  
✅ **Per-tab logi** (każda zakładka ma własne logi)  
✅ **Gradient style** (fioletowe/różowe akcenty)  
✅ **Session info panel** z metadanymi i tokenami  

---

## 🔧 Dla Developerów

### Disable Cache During Development

#### Chrome DevTools
```javascript
// Console:
location.reload(true);  // Hard reload
```

#### Always Fresh
```bash
# Dodaj timestamp do wszystkich assets
# W production użyj webpack/vite z hash w nazwach plików
```

### Verify Assets Version
```bash
# Check CSS timestamp
curl -I http://localhost:8080/static/css/session-info.css | grep "Last-Modified"

# Check JS timestamp
curl -I http://localhost:8080/static/js/app.js | grep "Last-Modified"
```

---

## 📝 Dlaczego To Się Dzieje?

Przeglądarki **cache'ują pliki statyczne** (CSS, JS) aby:
- ⚡ Przyspieszyć ładowanie
- 📉 Zmniejszyć transfer danych
- 🔋 Oszczędzać zasoby

**Efekt uboczny**: Po rebuild IDE, przeglądarka używa **starych plików z cache**!

---

## 🎯 Best Practice

### Development
```
Zawsze używaj Hard Refresh (Ctrl+Shift+R) po rebuild!
```

### Production
```html
<!-- Dodaj version/hash do assets -->
<link rel="stylesheet" href="/static/css/ide.css?v=2.2.0">
<script src="/static/js/app.js?v=2.2.0"></script>
```

---

**IDE już działa!** Po wyczyszczeniu cache zobaczysz nową wersję z per-tab state i session info! 🎉

