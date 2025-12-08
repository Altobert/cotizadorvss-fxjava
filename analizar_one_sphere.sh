#!/bin/bash

# Script para analizar archivo Excel de M/V One Sphere
ARCHIVO="/Users/albertosanmartin/Documents/VSS-COTIZACIONES/759191248_ONE Sphere_A25008819-02_Valparaiso Ship Services_2025_05_17_(P) Provision_RFQ.xlsx"

echo "🚢 ANÁLISIS RÁPIDO DE COTIZACIÓN M/V ONE SPHERE"
echo "═══════════════════════════════════════════════"

# Verificar si el archivo existe
if [ ! -f "$ARCHIVO" ]; then
    echo "❌ Archivo no encontrado: $ARCHIVO"
    exit 1
fi

echo "📂 Archivo encontrado:"
ls -lh "$ARCHIVO"
echo ""

# Crear directorio temporal
TEMP_DIR=$(mktemp -d)
echo "📁 Directorio temporal: $TEMP_DIR"

# Descomprimir Excel (los .xlsx son archivos ZIP)
cd "$TEMP_DIR"
unzip -q "$ARCHIVO"

echo ""
echo "📄 ESTRUCTURA DEL ARCHIVO EXCEL:"
echo "═════════════════════════════════"
ls -la

echo ""
echo "🔍 HOJAS DEL EXCEL (workbook.xml):"
echo "══════════════════════════════════"
if [ -f "xl/workbook.xml" ]; then
    # Extraer nombres de hojas
    grep -o 'name="[^"]*"' xl/workbook.xml | sed 's/name="//g' | sed 's/"//g' | nl
fi

echo ""
echo "📊 CONTENIDO DE SHARED STRINGS (texto del Excel):"
echo "═════════════════════════════════════════════════"
if [ -f "xl/sharedStrings.xml" ]; then
    # Buscar patrones relevantes en el texto
    echo "🔤 Palabras clave encontradas:"
    grep -o '<t>[^<]*</t>' xl/sharedStrings.xml | sed 's/<t>//g' | sed 's/<\/t>//g' | grep -i -E "(item|description|quantity|price|unit|total|code|vessel|sphere|provision|order|rfq)" | sort | uniq -c | sort -nr | head -20
    
    echo ""
    echo "🚢 Referencias al barco:"
    grep -o '<t>[^<]*</t>' xl/sharedStrings.xml | sed 's/<t>//g' | sed 's/<\/t>//g' | grep -i -E "(one.*sphere|sphere)" | head -5
    
    echo ""
    echo "📋 Posibles encabezados de columnas:"
    grep -o '<t>[^<]*</t>' xl/sharedStrings.xml | sed 's/<t>//g' | sed 's/<\/t>//g' | grep -E "^[A-Z][A-Z ]*$" | grep -v "^[A-Z]$" | sort | uniq | head -15
fi

echo ""
echo "🗂️ ANÁLISIS DE HOJA PRINCIPAL (sheet1.xml):"
echo "════════════════════════════════════════════"
if [ -f "xl/worksheets/sheet1.xml" ]; then
    echo "📏 Total de celdas con datos:"
    grep -o '<c r="[^"]*"[^>]*>[^<]*</c>' xl/worksheets/sheet1.xml | wc -l
    
    echo ""
    echo "📍 Rangos de celdas utilizados:"
    grep -o 'r="[^"]*"' xl/worksheets/sheet1.xml | sed 's/r="//g' | sed 's/"//g' | head -10
fi

echo ""
echo "🧹 Limpiando archivos temporales..."
rm -rf "$TEMP_DIR"

echo "✅ Análisis completado!"