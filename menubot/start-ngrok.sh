#!/bin/bash
# Ngrok — Telegram WebApp uchun tashqi URL
set -e

if ! command -v ngrok >/dev/null 2>&1; then
  echo "❌ ngrok o'rnatilmagan. brew install ngrok"
  exit 1
fi

# Eski ngrok ni to'xtatish
pkill -f "ngrok http" 2>/dev/null || true
sleep 1

echo "🌐 Ngrok ishga tushmoqda → localhost:8080"
echo "   URL ni application.properties dagi app.base-url ga yozing!"
exec ngrok http 8080
