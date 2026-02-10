# 📋 Exportación con Formato BSM Catering

## Información del Formato

### Tablas Utilizadas
| Tabla | Descripción | Registros BSM |
|-------|-------------|---------------|
| `brokers` | Información básica del broker | 1 registro (broker_id=6) |
| `broker_formatos` | Configuración del formato (header_row, versión) | 1 registro (formato_id=7) |
| `formato_columnas` | Definición de columnas con estilos | 19 columnas |
| `broker_metadata` | Metadata del formato (RFQ info, company, vendor) | 29 campos en 3 secciones |

## Proceso de Exportación

### 1. **Carga de Configuración**
Cuando seleccionas BSM Catering en el ComboBox:

```java
// Se carga el formato con todas las columnas y metadata
formatoActual = formatoDAO.obtenerFormatoPorBrokerId(6);  // broker_id=6
metadataActual = metadataDAO.obtenerMetadataPorSeccion(7); // formato_id=7
```

**Datos cargados:**
- ✅ 19 columnas con: nombre_columna_original, indice_columna, letra_columna
- ✅ Estilos: color_fondo, color_texto, es_negrita, es_cursiva, tiene_borde
- ✅ Tipos de dato: tipo_dato (TEXT, DECIMAL, INTEGER, etc.)
- ✅ 29 campos de metadata con posiciones: fila_origen, columna_origen

### 2. **Exportación de Metadata (Paso 1)**
Coloca cada campo de metadata en su posición exacta, usando el formato `nombreCampo=valor`:

```java
for (BrokerMetadata metadata : metadataActual) {
    int filaExcel = metadata.getFilaOrigen() - 1;  // Convertir a 0-indexed
    int colExcel = metadata.getColumnaOrigen();
    
    // Crear celda en posición exacta
    Row row = sheet.getRow(filaExcel);
    if (row == null) {
        row = sheet.createRow(filaExcel);
    }
    Cell cell = row.createCell(colExcel);
    // Formato: nombreCampo=valor
    String valorFormateado = metadata.getCampoNombre() + "=" + metadata.getCampoValor();
    cell.setCellValue(valorFormateado);
}
```

**Ejemplo:** Si metadata tiene `campo_nombre="Company Name Line 1"` y `campo_valor="BERNHARD SCHULTE..."`,
se colocará `"Company Name Line 1=BERNHARD SCHULTE..."` en la celda.

### 3. **Encabezados con Formato (Paso 2)**
Crea los encabezados en `header_row` con estilos específicos:

```java
int headerRowIndex = formatoActual.getHeaderRow() - 1;  // Fila 18 para BSM
Row headerRow = sheet.createRow(headerRowIndex);

for (FormatoColumna columna : formatoActual.getColumnas()) {
    Cell headerCell = headerRow.createCell(columna.getIndiceColumna());
    headerCell.setCellValue(columna.getNombreColumnaOriginal());
    
    // Aplicar estilos
    CellStyle estilo = workbook.createCellStyle();
    
    // ✓ Negrita
    if (columna.getEsNegrita()) font.setBold(true);
    
    // ✓ Cursiva
    if (columna.getEsCursiva()) font.setItalic(true);
    
    // ✓ Color de texto
    if (columna.getColorTexto() != null) {
        font.setColor(convertirColorAIndex(columna.getColorTexto()));
    }
    
    // ✓ Color de fondo
    if (columna.getColorFondo() != null) {
        estilo.setFillForegroundColor(convertirColorAIndex(columna.getColorFondo()));
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }
    
    // ✓ Bordes
    if (columna.getTieneBorde()) {
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
    }
    
    headerCell.setCellStyle(estilo);
}
```

### 4. **Datos de Productos (Paso 3)**
Inserta los datos en las columnas correctas respetando el tipo de dato:

```java
int dataStartRow = headerRowIndex + 1;

for (RowData rowData : tablaDinamica.getItems()) {
    Row row = sheet.createRow(currentRow++);
    
    for (FormatoColumna columna : formatoActual.getColumnas()) {
        String valor = rowData.get(columna.getCampoEstandar());
        Cell cell = row.createCell(columna.getIndiceColumna());
        
        // Aplicar formato según tipo de dato
        if ("DECIMAL".equals(columna.getTipoDato())) {
            double numValue = Double.parseDouble(valor);
            cell.setCellValue(numValue);
            cell.setCellStyle(estiloDecimal);  // Formato #,##0.00
        } else if ("INTEGER".equals(columna.getTipoDato())) {
            int intValue = Integer.parseInt(valor);
            cell.setCellValue(intValue);
        } else {
            cell.setCellValue(valor);  // TEXT
        }
    }
}
```

## Campos Utilizados de Cada Tabla

### `formato_columnas` (19 registros)
Todos estos campos SE ESTÁN USANDO:
- ✅ `campo_estandar` → Para mapear datos de RowData
- ✅ `nombre_columna_original` → Encabezado visible en Excel
- ✅ `indice_columna` → Posición exacta de la columna (0-18)
- ✅ `letra_columna` → Referencia visual (A-S)
- ✅ `tipo_dato` → Formateo numérico (DECIMAL, INTEGER, TEXT)
- ✅ `color_fondo` → Color de celda de encabezado
- ✅ `color_texto` → Color de fuente
- ✅ `es_negrita` → Estilo de fuente
- ✅ `es_cursiva` → Estilo de fuente
- ✅ `tiene_borde` → Bordes de celda

### `broker_metadata` (29 registros)
Todos estos campos SE ESTÁN USANDO:
- ✅ `seccion` → Para organizar metadata (RFQ Info, Company Details, Vendor Details)
- ✅ `campo_nombre` → Nombre del campo de metadata
- ✅ `campo_valor` → Valor a exportar
- ✅ `fila_origen` → Fila exacta donde colocar el valor
- ✅ `columna_origen` → Columna exacta donde colocar el valor
- ✅ `letra_columna` → Referencia visual de la columna

### `broker_formatos` (1 registro)
- ✅ `header_row` → Fila donde van los encabezados (18 para BSM)
- ✅ `version` → Versión del formato ("1.0")
- ✅ `descripcion` → Descripción del formato

## Conversión de Colores

El método `convertirColorAIndex()` soporta:

### Formatos de entrada:
- **Hexadecimal**: `#FFFFFF`, `#FF0000`, `#00FF00`
- **Números**: `10`, `12`, `64` (índices directos)
- **Español**: `AZUL`, `ROJO`, `VERDE`, `GRIS`, etc.
- **Inglés**: `BLUE`, `RED`, `GREEN`, `GRAY`, etc.
- **IndexedColors**: `GREY_25_PERCENT`, `LIGHT_BLUE`, etc.

### Mapeo extendido:
```
Azules: AZUL, AZUL_CLARO, AZUL_OSCURO, CELESTE, SKY_BLUE
Rojos: ROJO, ROJO_OSCURO, ROSA, PINK
Verdes: VERDE, VERDE_CLARO, VERDE_OSCURO
Amarillos: AMARILLO, NARANJA, ORO, GOLD
Grises: GRIS, GRIS_25, GRIS_40, GRIS_50, GRIS_80
Otros: VIOLETA, TURQUESA, LAVANDA, CORAL, MARRON
```

## Resultado Final

El Excel exportado tendrá:
1. ✅ **Metadata en posiciones exactas** (29 campos distribuidos en filas específicas)
2. ✅ **Encabezados en fila 18** con nombres, colores y estilos del broker
3. ✅ **Datos de productos** a partir de la fila 19 en las 19 columnas
4. ✅ **Formato numérico correcto** según tipo de dato (decimales, enteros, texto)
5. ✅ **Colores y estilos** exactos del formato BSM Catering

## Uso

1. **Seleccionar BSM Catering** en el ComboBox de brokers
2. **Cargar archivo Excel** con datos de productos
3. **Presionar "Exportar Cotización"**
4. **Guardar archivo** → Excel con formato exacto de BSM Catering

---

**Nota:** Este sistema replica exactamente el formato que BSM Catering espera recibir, usando toda la información almacenada en las 4 tablas de configuración.
