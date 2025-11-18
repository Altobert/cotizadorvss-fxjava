#!/bin/bash

# Script mejorado para ejecutar Cotizador VSS con icono en macOS

echo "🚀 Iniciando Cotizador VSS con configuración de iconos para macOS..."

cd "$(dirname "$0")"

# Verificar que el JAR existe
if [ ! -f "target/cotizador-vss.jar" ]; then
    echo "❌ JAR no encontrado. Ejecutando build primero..."
    mvn clean package -DskipTests
fi

# Crear archivo temporal de propiedades para macOS
cat > /tmp/cotizador-vss.properties << EOF
apple.awt.application.name=Cotizador VSS
com.apple.mrj.application.apple.menu.about.name=Cotizador VSS
java.awt.Window.locationByPlatform=true
EOF

# Ejecutar con configuración completa de macOS
java \
    -Dfile.encoding=UTF-8 \
    -Djava.system.class.loader=java.lang.ClassLoader \
    -Djava.awt.headless=false \
    -Dcom.apple.macos.useScreenMenuBar=true \
    -Dcom.apple.mrj.application.apple.menu.about.name="Cotizador VSS" \
    -Dapple.awt.application.name="Cotizador VSS" \
    -Dapple.laf.useScreenMenuBar=true \
    -Dapple.awt.graphics.EnableQ2DX=true \
    -Dapple.awt.graphics.UseQuartz=true \
    --add-opens java.desktop/com.apple.eawt=ALL-UNNAMED \
    --add-opens java.desktop/java.awt=ALL-UNNAMED \
    --add-opens java.desktop/sun.awt=ALL-UNNAMED \
    --add-exports java.desktop/com.apple.eawt=ALL-UNNAMED \
    -jar target/cotizador-vss.jar

echo "✅ Cotizador VSS finalizado"