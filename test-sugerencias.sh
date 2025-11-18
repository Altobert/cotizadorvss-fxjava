#!/bin/bash

# Script para probar las sugerencias de productos
echo "🚀 Probando sugerencias de productos..."

cd /Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava

# Compilar el proyecto
mvn compile -q

# Ejecutar con el classpath completo
mvn exec:java -Dexec.mainClass="cl.vss.cotizador.demo.DemoSugerenciasProductos" -Dexec.args="" -q