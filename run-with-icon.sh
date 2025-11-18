#!/bin/bash

# Script para ejecutar la aplicación Cotizador VSS con icono personalizado
# Este script configura las propiedades necesarias para mostrar el icono correctamente

echo "🚀 Iniciando Cotizador VSS..."

# Configurar propiedades de macOS para el icono
export _JAVA_OPTIONS="-Dapple.awt.application.name=CotizadorVSS -Dcom.apple.mrj.application.apple.menu.about.name=CotizadorVSS"

# Ir al directorio del proyecto
cd "$(dirname "$0")"

# Ejecutar con configuración específica de JavaFX
mvn javafx:run \
  -Djavafx.application.name="Cotizador VSS" \
  -Dapple.awt.application.name="Cotizador VSS" \
  -Dcom.apple.mrj.application.apple.menu.about.name="Cotizador VSS"

echo "✅ Aplicación finalizada"