#!/bin/bash

# Script para ejecutar el JAR de Cotizador VSS con configuración de iconos

echo "🚀 Ejecutando Cotizador VSS desde JAR..."

# Ir al directorio del proyecto
cd "$(dirname "$0")"

# Verificar que el JAR existe
if [ ! -f "target/cotizador-vss.jar" ]; then
    echo "❌ No se encontró el archivo JAR. Ejecuta 'mvn clean package' primero."
    exit 1
fi

# Configurar variables de entorno para macOS
export _JAVA_OPTIONS="-Xdock:name='Cotizador VSS' -Xdock:icon=./src/main/resources/images/icon.png"

# Ejecutar el JAR con configuración completa
java \
    -Dapple.awt.application.name="Cotizador VSS" \
    -Dcom.apple.mrj.application.apple.menu.about.name="Cotizador VSS" \
    -Djava.awt.headless=false \
    -jar target/cotizador-vss.jar

echo "✅ Aplicación finalizada"