#!/bin/bash

# Analiza el worksheet.xml de forma más detallada

EXCEL_FILE="$1"
TEMP_DIR=$(mktemp -d)

# Descomprime
unzip -q "$EXCEL_FILE" -d "$TEMP_DIR"

if [ -f "$TEMP_DIR/xl/worksheets/sheet1.xml" ]; then
    echo "=== ANÁLISIS DE PRIMERAS 30 FILAS ==="
    
    # Extrae referencia a sharedStrings
    cat "$TEMP_DIR/xl/worksheets/sheet1.xml" | \
    grep -o '<c r="[A-Z]*[0-9]*"[^>]*>[^<]*</c>' | \
    head -200 | \
    while read cell; do
        # Extrae referencia de celda y valor
        cell_ref=$(echo "$cell" | grep -o 'r="[^"]*"' | cut -d'"' -f2)
        # Busca si tiene referencia a sharedString (t="s") y el índice
        shared_idx=$(echo "$cell" | grep -o '<v>[0-9]*</v>' | head -1 | sed 's/<v>//; s/<\/v>//')
        
        if [ ! -z "$shared_idx" ]; then
            # Obtiene el string del índice compartido
            string_val=$(grep -m $((shared_idx + 1)) '<si>' "$TEMP_DIR/xl/sharedStrings.xml" | \
                         tail -1 | grep -o '<t[^>]*>[^<]*</t>' | sed 's/<t[^>]*>//; s/<\/t>//')
            
            if [ ! -z "$string_val" ]; then
                echo "Cell $cell_ref = $string_val"
            fi
        fi
    done
    
fi

rm -rf "$TEMP_DIR"
