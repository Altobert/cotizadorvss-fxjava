#!/bin/bash

# Script para ejecutar la aplicación nativa de Cotizador VSS

echo "🚀 Ejecutando Cotizador VSS (Aplicación Nativa)..."

cd "$(dirname "$0")"

# Verificar que la aplicación existe
if [ ! -d "dist/Cotizador VSS.app" ]; then
    echo "❌ Aplicación nativa no encontrada. Ejecuta ./create-native-app.sh primero."
    exit 1
fi

# Ejecutar la aplicación nativa
open "dist/Cotizador VSS.app"

echo "✅ Cotizador VSS iniciado correctamente"
echo "💡 El icono personalizado debería aparecer en el dock"