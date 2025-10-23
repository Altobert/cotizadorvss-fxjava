#!/bin/bash

# Script simple para ejecutar JavaFX
# Uso: ./start.sh

echo "🚀 Iniciando Cotizador JavaFX..."

# Verificar si el proyecto está compilado
if [ ! -d "target/classes" ]; then
    echo "⚙️  Compilando proyecto primero..."
    mvn compile -q
    if [ $? -ne 0 ]; then
        echo "❌ Error al compilar"
        exit 1
    fi
fi

# Ejecutar aplicación
echo "🏃 Ejecutando aplicación..."
mvn javafx:run
