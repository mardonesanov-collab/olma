#!/bin/bash
# MenuBot — to'g'ri Java bilan ishga tushirish
set -e

cd "$(dirname "$0")"

# IntelliJ dagi JDK (openjdk-26)
export JAVA_HOME="${JAVA_HOME:-/Users/m1max/Library/Java/JavaVirtualMachines/openjdk-26.0.1/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

if [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "❌ Java topilmadi: $JAVA_HOME"
  echo "IntelliJ IDEA → Project Structure → SDK da JDK yo'lini tekshiring."
  exit 1
fi

echo "✓ Java: $($JAVA_HOME/bin/java -version 2>&1 | head -1)"

# Barcha MenuBot / 8080 jarayonlarini to'xtatish (IntelliJ + terminal ikki nusxa bo'lmasin)
stop_menubot() {
  echo "⏹ Eski MenuBot jarayonlari to'xtatilmoqda..."
  pkill -9 -f "${PWD}.*spring-boot:run" 2>/dev/null || true
  pkill -9 -f "${PWD}.*MenubotApplication" 2>/dev/null || true
  local pids
  pids=$(lsof -ti :8080 2>/dev/null || true)
  if [ -n "$pids" ]; then
    echo "   8080 port: $pids"
    kill -9 $pids 2>/dev/null || true
  fi
  rm -f data/menubot.lock.db 2>/dev/null || true
  sleep 2
  if lsof -ti :8080 >/dev/null 2>&1; then
    echo "❌ 8080 hali band. IntelliJ IDEA da Run/Stop tugmasini bosing, keyin qayta urining."
    exit 1
  fi
}
stop_menubot

# Frontend build (agar node mavjud bo'lsa)
if command -v npm >/dev/null 2>&1 && [ -d frontend ]; then
  echo "📦 Frontend build..."
  (cd frontend && npm run build --silent) || echo "⚠ Frontend build o'tmadi, davom etilmoqda..."
fi

# Vaqtinchalik H2 (in-memory) — Docker/PostgreSQL o'tkazib yuboriladi
echo "💾 H2 in-memory bazasi (Docker kerak emas)"

echo "🚀 MenuBot SaaS ishga tushmoqda (http://localhost:8080)..."
exec ./mvnw spring-boot:run -DskipTests
