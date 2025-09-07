# 📤 Instrukcje wgrania dokumentacji na SourceForge

## 🚀 Szybkie wgranie (automatyczne)

```bash
./upload-docs.sh
```

## 🔧 Ręczne wgranie przez SFTP

### 1. Połącz się z serwerem SFTP
```bash
sftp rbala@web.sourceforge.net
```

### 2. Przejdź do katalogu docelowego
```bash
cd /home/project-web/web-harvest/htdocs
```

### 3. Wgraj pliki
```bash
# Wgraj wszystkie pliki z katalogu web-harvest-docs-upload/
put -r web-harvest-docs-upload/* .
```

### 4. Sprawdź czy pliki zostały wgrane
```bash
ls -la
```

### 5. Zakończ połączenie
```bash
quit
```

## 📁 Struktura plików do wgrania

```
web-harvest-docs-upload/
├── index.html              # Strona główna
├── css/                    # Style CSS
│   ├── style.css
│   ├── processors.css
│   └── prism.css
├── js/                     # JavaScript
│   ├── script.js
│   └── prism.js
├── processors/             # Referencja procesorów
│   └── index.html
├── examples/               # Przykłady
│   └── index.html
└── api/                    # API dokumentacja
    └── index.html
```

## 🌐 Dostęp do dokumentacji

Po wgraniu dokumentacja będzie dostępna pod adresem:
**https://web-harvest.sourceforge.net/**

## ✅ Weryfikacja

1. Otwórz https://web-harvest.sourceforge.net/ w przeglądarce
2. Sprawdź czy wszystkie strony się ładują
3. Przetestuj funkcje wyszukiwania i filtrowania
4. Sprawdź czy przykłady kodu się kopiują

## 🔧 Rozwiązywanie problemów

### Problem: "Permission denied"
- Sprawdź czy masz uprawnienia do katalogu `/home/project-web/web-harvest/htdocs`
- Skontaktuj się z administratorem SourceForge

### Problem: "No such file or directory"
- Upewnij się, że katalog `web-harvest-docs-upload` istnieje
- Sprawdź czy wszystkie pliki zostały skopiowane

### Problem: Strona się nie ładuje
- Sprawdź czy wszystkie pliki CSS i JS zostały wgrane
- Sprawdź uprawnienia do plików (powinny być 644)

## 📞 Wsparcie

W przypadku problemów:
1. Sprawdź logi SFTP
2. Zweryfikuj uprawnienia do katalogu
3. Skontaktuj się z zespołem Web-Harvest

