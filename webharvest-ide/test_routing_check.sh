#!/bin/bash
# Automatyczne sprawdzanie routingu po każdej zmianie

echo "🔍 Sprawdzanie orthogonal routing..."

# 1. Sprawdź kompilację
echo "[1/4] Sprawdzanie kompilacji..."
cd frontend
if ! npm run build > /tmp/build.log 2>&1; then
    echo "❌ Błąd kompilacji!"
    tail -20 /tmp/build.log
    exit 1
fi
echo "✅ Kompilacja OK"

# 2. Sprawdź czy IDE działa
echo "[2/4] Sprawdzanie czy IDE działa..."
if ! curl -s http://localhost:8080 > /dev/null; then
    echo "❌ IDE nie działa na porcie 8080"
    exit 1
fi
echo "✅ IDE działa"

# 3. Sprawdź kod routingu
echo "[3/4] Sprawdzanie kodu routingu..."
cd ..
if ! grep -q "ensureOrthogonalSegments" frontend/src/visual/ConnectionPathRouter.ts; then
    echo "❌ ensureOrthogonalSegments nie jest używane!"
    exit 1
fi

if ! grep -q "H \|V " frontend/src/visual/ConnectionRenderer.ts; then
    echo "❌ ConnectionRenderer nie używa H/V dla orthogonal routing!"
    exit 1
fi
echo "✅ Kod routingu OK"

# 4. Sprawdź czy są błędy lintera
echo "[4/4] Sprawdzanie lintera..."
cd frontend
if npm run lint 2>&1 | grep -q "error"; then
    echo "❌ Błędy lintera!"
    npm run lint 2>&1 | grep "error"
    exit 1
fi
echo "✅ Linter OK"

echo ""
echo "✅ Wszystkie sprawdzenia przeszły!"
echo ""
echo "Uwaga: Aby sprawdzić w runtime przez Selenium Chrome, uruchom:"
echo "  python3 test_routing_orthogonal.py"
echo "(wymaga zainstalowanego selenium: pip install selenium)"





