#!/bin/bash

# Script simplificado para ejecutar Cotizador VSS con icono en macOS

echo "🚀 Iniciando Cotizador VSS..."

cd "$(dirname "$0")"

# Verificar que el JAR existe
if [ ! -f "target/cotizador-vss.jar" ]; then
    echo "❌ JAR no encontrado. Ejecutando build primero..."
    mvn clean package -DskipTests
fi

# Ejecutar con configuración simplificada de macOS
java \
    -Dapple.awt.application.name="Cotizador VSS" \
    -Dcom.apple.mrj.application.apple.menu.about.name="Cotizador VSS" \
    -Djava.awt.headless=false \
    --add-opens java.desktop/com.apple.eawt=ALL-UNNAMED \
    --add-exports java.desktop/com.apple.eawt=ALL-UNNAMED \
    -jar target/cotizador-vss.jar

echo "✅ Cotizador VSS finalizado"