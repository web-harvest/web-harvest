# 🔑 Konfiguracja SourceForge dla wgrania dokumentacji

## 📋 Krok 1: Dodaj klucz SSH do SourceForge

### 1.1 Skopiuj klucz publiczny
```bash
cat ~/.ssh/sourceforge_rsa.pub
```

### 1.2 Dodaj klucz do SourceForge
1. Zaloguj się na https://sourceforge.net/
2. Przejdź do **Account Settings** → **SSH Keys**
3. Kliknij **Add Key**
4. Wklej cały klucz publiczny (zaczyna się od `ssh-rsa`)
5. Nadaj nazwę kluczowi (np. "Web-Harvest Docs Upload")
6. Kliknij **Add Key**

## 📋 Krok 2: Sprawdź uprawnienia do projektu

### 2.1 Sprawdź czy masz dostęp do web-harvest
1. Przejdź do https://sourceforge.net/projects/web-harvest/
2. Sprawdź czy jesteś członkiem zespołu projektu
3. Jeśli nie, poproś administratora o dodanie Cię do zespołu

### 2.2 Sprawdź uprawnienia do htdocs
1. Przejdź do **Project Admin** → **Web Services**
2. Sprawdź czy masz dostęp do **Project Web**
3. Jeśli nie, poproś administratora o uprawnienia

## 📋 Krok 3: Przetestuj połączenie

### 3.1 Test połączenia SSH
```bash
ssh -i ~/.ssh/sourceforge_rsa rbala@web.sourceforge.net
```

### 3.2 Test katalogu docelowego
```bash
sftp -i ~/.ssh/sourceforge_rsa rbala@web.sourceforge.net
cd /home/project-web/web-harvest/htdocs
ls -la
quit
```

## 📋 Krok 4: Wgraj dokumentację

### 4.1 Automatyczne wgranie
```bash
./upload-docs.sh
```

### 4.2 Ręczne wgranie
```bash
sftp -i ~/.ssh/sourceforge_rsa rbala@web.sourceforge.net
cd /home/project-web/web-harvest/htdocs
put -r web-harvest-docs-upload/* .
quit
```

## 🔧 Rozwiązywanie problemów

### Problem: "Permission denied (publickey)"
- **Rozwiązanie**: Sprawdź czy klucz został dodany do SourceForge
- **Sprawdź**: `ssh -i ~/.ssh/sourceforge_rsa rbala@web.sourceforge.net`

### Problem: "No such file or directory" dla htdocs
- **Rozwiązanie**: Sprawdź uprawnienia do projektu web-harvest
- **Sprawdź**: Czy jesteś członkiem zespołu projektu

### Problem: "Permission denied" przy wgrywaniu
- **Rozwiązanie**: Sprawdź uprawnienia do katalogu htdocs
- **Sprawdź**: Czy masz uprawnienia do Project Web

## 📞 Wsparcie

Jeśli nadal masz problemy:
1. Sprawdź logi: `ssh -v -i ~/.ssh/sourceforge_rsa rbala@web.sourceforge.net`
2. Skontaktuj się z administratorem projektu web-harvest
3. Sprawdź dokumentację SourceForge: https://sourceforge.net/p/forge/documentation/

## ✅ Weryfikacja po wgraniu

1. Otwórz https://web-harvest.sourceforge.net/
2. Sprawdź czy strona główna się ładuje
3. Przetestuj nawigację (Processors, Examples, API)
4. Sprawdź czy wszystkie pliki CSS i JS się ładują

---

**Klucz SSH został wygenerowany i jest gotowy do dodania do SourceForge!** 🔑


