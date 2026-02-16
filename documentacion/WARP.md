# WARP.md

Este archivo proporciona orientación a WARP (warp.dev) al trabajar con código en este repositorio.

## Descripción del Proyecto

Sistema de organización y visualización de cotizaciones para VSS (Valparaiso Ship Services). Procesa archivos Excel de múltiples brokers (MCTC, Oceanic, CMA CGM, Garrets, Procureship, BSM), extrae información de vessels/clientes, organiza cotizaciones por cliente y visualiza datos con formatos originales preservados.

## Compilación

```bash
# Compilar el proyecto
mvn clean compile

# Construir el paquete
mvn clean package
```

## Ejecución de Aplicaciones

### Aplicaciones Principales

1. **Organizador de Cotizaciones** (organiza cotizaciones por cliente)
```bash
mvn exec:java -Dexec.mainClass="cl.vsschile.CotizacionOrganizer" \
  -Dexec.args="<ruta-al-directorio-BROKERS>"
```

2. **Visor de Cotizaciones** (interfaz JavaFX para visualizar cotizaciones)
```bash
mvn exec:java -Dexec.mainClass="cl.vsschile.CotizacionViewerApp"
# O usar el script:
./run-viewer.sh
```

3. **Prueba de Detector de Columnas** (analiza y prueba la detección de columnas)
```bash
mvn exec:java -Dexec.mainClass="cl.vsschile.ColumnDetectorTest" \
  -Dexec.args="<ruta-al-directorio-BROKERS>"
```

4. **Guardador de Formatos** (guarda formatos de brokers en base de datos)
```bash
mvn exec:java -Dexec.mainClass="cl.vsschile.FormatoSaver" \
  -Dexec.args="<ruta-al-directorio-BROKERS> [contraseña-bd]"
```

5. **Guardador de Colores de Archivos** (guarda información de colores de archivos individuales)
```bash
mvn exec:java -Dexec.mainClass="cl.vsschile.ArchivoColorSaver" \
  -Dexec.args="<ruta-al-directorio-BROKERS>"
```

6. **Recreador de Plantillas** (recrea plantillas Excel de brokers)
```bash
mvn exec:java -Dexec.mainClass="cl.vsschile.TemplateRecreator"
```

7. **Generador de Reportes Excel** (genera reportes de análisis de brokers)
```bash
mvn exec:java -Dexec.mainClass="cl.vsschile.ExcelReportGenerator"
```

8. **Generador de Excel de Brokers** (genera archivos de ejemplo de brokers)
```bash
mvn exec:java -Dexec.mainClass="cl.vsschile.BrokerExcelGenerator"
```

## Arquitectura

### Componentes Principales

**1. CotizacionOrganizer**
- Punto de entrada principal para procesar cotizaciones
- Escanea directorios de brokers, extrae metadata (nombre del vessel, IMO, número de cotización)
- Organiza archivos por cliente en el directorio `cotizaciones-por-cliente/`
- Genera `metadata.txt` para cada carpeta de cliente

**2. ColumnDetector**
- Detecta filas de encabezado y mapeo de columnas para cada formato de broker
- Retorna objetos `ColumnMapping` que contienen:
  - `headerRow`: Índice de la fila donde están los encabezados
  - `columns`: Mapa de nombres de campos estándar a índices de columnas
  - `columnStyles`: Información de estilo de celdas (colores, fuentes, bordes)
- Métodos de detección específicos por broker para cada formato soportado

**3. FormatoDatabaseManager**
- Interfaz PostgreSQL para almacenar/recuperar formatos de brokers
- Base de datos: `sistema_cotizacion_2025` (localhost:5432, usuario: postgres)
- Tablas: `brokers`, `broker_formatos`, `formato_columnas`, `cotizaciones_archivos`, `archivo_colores`
- Vistas: `v_formatos_activos`, `v_columnas_detalladas`, `v_archivos_cotizaciones`, etc.

**4. CotizacionViewerApp (JavaFX)**
- Aplicación GUI para visualizar cotizaciones con el formato original del broker
- Carga formatos de broker desde la base de datos
- Aplica colores, fuentes y estilos dinámicamente basado en configuración de BD
- Usa `RowData` para manejo dinámico de columnas

### Flujo de Datos

```
Archivos Excel → CotizacionOrganizer → Organizado por Cliente
                 ↓
            ColumnDetector → Detecta Formato
                 ↓
            FormatoSaver → PostgreSQL
                 ↓
       CotizacionViewerApp → Visualiza con Formato
```

### Soporte de Brokers

El sistema detecta automáticamente estos formatos de brokers:
- **MCTC MARINE LTD**: Encabezado en fila 10, incluye nombre de vessel, IMO, número de cotización
- **OCEANIC CATERING LTD**: Encabezado en fila 13, nombre de vessel y número de solicitud
- **CMA CGM**: Encabezado en fila 19, nombre de vessel
- **GARRETS INTERNATIONAL LTD**: Encabezado en fila 25, número RFQ y vessel
- **PROCURESHIP**: Encabezado en fila 14, vessel, IMO, número de requisición
- **BSM**: Lógica de detección personalizada
- **Genérico**: Auto-detección alternativa para formatos desconocidos

### Mapeo de Campos Estándar

Campos mapeados en todos los formatos de brokers:
- `ITEM_NAME`, `ITEM_CODE`, `CATEGORY`, `DESCRIPTION`
- `QUANTITY`, `UOM` (unidad de medida), `UNIT_PRICE`, `TOTAL`
- `BRAND`, `DISCOUNT`, `SUPPLIER_COMMENTS`

### Clases Clave

- `QuotationInfo`: Contenedor de metadata (vessel, IMO, número de cotización, broker)
- `ColumnMapping`: Estructura de columnas e información de estilo para un formato de broker
- `CellStyleInfo`: Formato de celda (color de fondo, fuente, bordes)
- `RowData`: Datos de fila dinámicos para JavaFX TableView

## Base de Datos

**Base de Datos PostgreSQL:** `sistema_cotizacion_2025`
- **Conexión:** localhost:5432, usuario: postgres, contraseña: (vacía o proporcionada)
- **Tablas Principales:** Ver ESTRUCTURA_BD.md para el esquema completo
- **Vistas:** Consultas pre-construidas para operaciones comunes

**Probar Conexión:**
```bash
psql -U postgres -d sistema_cotizacion_2025
```

## Agregar Soporte para Nuevo Broker

1. Agregar detección de broker en `CotizacionOrganizer.extractQuotationInfo()`
2. Crear método de detección de columnas en `ColumnDetector.detect*Columns()`
3. Agregar lógica de extracción en `CotizacionOrganizer.extract*Info()`
4. Ejecutar `FormatoSaver` para guardar formato en la base de datos

## Estructura de Archivos

- `src/main/java/cl/vsschile/` - Archivos fuente Java
- `cotizaciones-por-cliente/` - Directorio de salida (cotizaciones organizadas)
- `ESTRUCTURA_BD.md` - Documentación del esquema de base de datos
- `README.md` - Documentación principal de uso
- `README_VIEWER.md` - Documentación del visor JavaFX
- `WORKFLOW_VIEWER.md` - Flujo de trabajo detallado del visor

## Dependencias

- Apache POI 3.17 (procesamiento de Excel)
- PostgreSQL 9.4.1212 (driver JDBC)
- JavaFX 17 (framework GUI)
- Java 17 (target del compilador)

## Notas

- Los archivos son **copiados**, nunca movidos o modificados
- El sistema es seguro para ejecutar múltiples veces
- Soporta formatos `.xls` y `.xlsx`
- La salida preserva los nombres de archivo originales con prefijo del broker
