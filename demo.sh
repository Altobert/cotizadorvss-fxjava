#!/bin/bash

# Script para ejecutar la demostración del modelo genérico
# Uso: ./demo.sh

echo "🚀 Ejecutando Demostración del Modelo Genérico"
echo "=============================================="

# Verificar si Maven está disponible
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven no encontrado"
    exit 1
fi

# Compilar el proyecto
echo "⚙️  Compilando proyecto..."
mvn compile -q
if [ $? -ne 0 ]; then
    echo "❌ Error al compilar"
    exit 1
fi

# Ejecutar la demostración usando Maven
echo "🏃 Ejecutando demostración..."
mvn exec:java -Dexec.mainClass="com.example.demo.CotizacionDemo" -q

echo ""
echo "🎉 Demostración completada!"
