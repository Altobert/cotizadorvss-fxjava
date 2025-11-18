#!/bin/bash

# Script para crear aplicación nativa de macOS con jpackage

echo "📦 Creando aplicación nativa de macOS para Cotizador VSS..."

cd "$(dirname "$0")"

# Verificar que tenemos Java 17+
java_version=$(java -version 2>&1 | grep "version" | awk -F'"' '{print $2}' | cut -d'.' -f1)
if [ "$java_version" -lt 17 ]; then
    echo "❌ Se requiere Java 17 o superior para jpackage"
    exit 1
fi

# Asegurar que el JAR está compilado
if [ ! -f "target/cotizador-vss.jar" ]; then
    echo "🔨 Compilando proyecto..."
    mvn clean package -DskipTests
fi

# Crear directorio para la aplicación si no existe
mkdir -p dist

# Crear aplicación nativa de macOS
echo "🖥️ Generando aplicación nativa de macOS..."

jpackage \
    --input target \
    --main-jar cotizador-vss.jar \
    --main-class cl.vss.cotizador.Main \
    --name "Cotizador VSS" \
    --app-version "1.0.0" \
    --description "Sistema de Cotizaciones VSS" \
    --vendor "VSS Solutions" \
    --copyright "© 2024 VSS Solutions" \
    --icon src/main/resources/images/cotizador.icns \
    --dest dist \
    --type app-image \
    --java-options "-Dapple.awt.application.name=Cotizador VSS"

if [ $? -eq 0 ]; then
    echo "✅ Aplicación nativa creada exitosamente en: dist/Cotizador VSS.app"
    echo "🚀 Para ejecutar: open \"dist/Cotizador VSS.app\""
    
    # Opcional: Crear alias en el dock
    echo "💡 Para agregar al dock, arrastra la aplicación desde dist/ al dock"
else
    echo "❌ Error creando la aplicación nativa"
    exit 1
fi