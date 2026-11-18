#!/bin/bash
# Script de inicio para Api QuS (Calidad de Software)
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

if [ ! -d ".venv" ]; then
    echo "⚙️  Creando entorno virtual Python..."
    python3 -m venv .venv
    echo "📦 Instalando dependencias..."
    ./.venv/bin/pip install -r requirements.txt
fi

echo "🚀 Iniciando Api QuS en http://localhost:8000 ..."
echo "📖 Swagger UI disponible en http://localhost:8000/docs"
./.venv/bin/python3 -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

