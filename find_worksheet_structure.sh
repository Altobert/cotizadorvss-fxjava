#!/bin/bash

# Extrae la estructura completa del worksheet1.xml para encontrar los encabezados

EXCEL_FILE="$1"
TEMP_DIR=$(mktemp -d)

# Descomprime el XLSX (que es un ZIP)
unzip -q "$EXCEL_FILE" -d "$TEMP_DIR"

# Extrae el worksheet1.xml y lo analiza
if [ -f "$TEMP_DIR/xl/worksheets/sheet1.xml" ]; then
    echo "=== ESTRUCTURA DEL WORKSHEET (sheet1.xml) ==="
    echo ""
    
    # Extrae todas las filas con sus referencias de celda
    grep -o '<row r="[0-9]*".*</row>' "$TEMP_DIR/xl/worksheets/sheet1.xml" | head -50 | while read row; do
        row_num=$(echo "$row" | grep -o 'r="[0-9]*"' | cut -d'"' -f2)
        # Extrae los valores de celda de esta fila
        echo "ROW $row_num:"
        echo "$row" | grep -o '<c r="[A-Z]*[0-9]*"[^>]*>.*?</c>' | sed 's/></>\n</g' | head -20
        echo ""
    done
    
    echo ""
    echo "=== BÚSQUEDA DE ENCABEZADOS ==="
    grep -i "ITEM DESCRIPTION\|UNIT\|PRICE\|QUANTITY" "$TEMP_DIR/xl/worksheets/sheet1.xml" 2>/dev/null || echo "No se encontraron encabezados por texto directo"
    
fi

# Limpia el directorio temporal
rm -rf "$TEMP_DIR"
