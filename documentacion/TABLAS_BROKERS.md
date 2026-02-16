# Tablas de Formato y Metadata de Brokers

## Descripción General

El sistema utiliza 4 tablas principales para gestionar la configuración de formatos y metadata de los diferentes brokers. Estas tablas permiten definir cómo leer, interpretar y visualizar las cotizaciones de cada broker.

---

## 1. Tabla: `brokers`

**Descripción:** Almacena la información básica de cada broker.

### Estructura

| Columna | Tipo | Nulable | Default | Descripción |
|---------|------|---------|---------|-------------|
| `broker_id` | integer | NO | nextval('brokers_broker_id_seq') | **PK** - ID único del broker |
| `broker_name` | varchar(255) | NO | - | Nombre del broker |
| `descripcion` | text | YES | - | Descripción del broker |
| `contacto` | varchar(255) | YES | - | Información de contacto |
| `email` | varchar(255) | YES | - | Email del broker |
| `activo` | boolean | YES | true | Estado activo/inactivo |
| `fecha_creacion` | timestamp | YES | CURRENT_TIMESTAMP | Fecha de creación del registro |
| `fecha_actualizacion` | timestamp | YES | CURRENT_TIMESTAMP | Fecha de última actualización |

### Índices
- **PK**: `brokers_pkey` (broker_id)
- **UNIQUE**: `brokers_broker_name_key` (broker_name)
- **INDEX**: `idx_broker_nombre` (broker_name)

### Ejemplo de Datos
```sql
INSERT INTO brokers (broker_name, descripcion, activo) 
VALUES ('CMA CGM', 'Broker de provisiones marítimas', true);
```

---

## 2. Tabla: `broker_formatos`

**Descripción:** Define los formatos de Excel de cada broker. Un broker puede tener múltiples versiones de formato.

### Estructura

| Columna | Tipo | Nulable | Default | Descripción |
|---------|------|---------|---------|-------------|
| `formato_id` | integer | NO | nextval('broker_formatos_formato_id_seq') | **PK** - ID único del formato |
| `broker_id` | integer | NO | - | **FK** → brokers(broker_id) |
| `version` | varchar(50) | YES | '1.0' | Versión del formato |
| `header_row` | integer | NO | - | Fila donde están los encabezados (0-based) |
| `descripcion` | text | YES | - | Descripción del formato |
| `archivo_ejemplo` | varchar(500) | YES | - | Ruta/nombre del archivo ejemplo |
| `activo` | boolean | YES | true | Estado activo/inactivo |
| `fecha_creacion` | timestamp | YES | CURRENT_TIMESTAMP | Fecha de creación |
| `fecha_actualizacion` | timestamp | YES | CURRENT_TIMESTAMP | Fecha de última actualización |

### Índices
- **PK**: `broker_formatos_pkey` (formato_id)
- **INDEX**: `idx_formato_broker` (broker_id)
- **INDEX**: `idx_formato_activo` (activo)

### Relaciones
- **FK**: `broker_id` → `brokers.broker_id`

### Ejemplo de Datos
```sql
INSERT INTO broker_formatos (broker_id, version, header_row, descripcion) 
VALUES (3, '1.0', 19, 'Formato estándar CMA CGM 2025');
```

---

## 3. Tabla: `formato_columnas`

**Descripción:** Define las columnas de datos del formato de cada broker, incluyendo mapeo de campos estándar y estilos visuales.

### Estructura

| Columna | Tipo | Nulable | Default | Descripción |
|---------|------|---------|---------|-------------|
| `columna_id` | integer | NO | nextval('formato_columnas_columna_id_seq') | **PK** - ID único de la columna |
| `formato_id` | integer | NO | - | **FK** → broker_formatos(formato_id) |
| `campo_estandar` | varchar(100) | NO | - | Nombre del campo estandarizado |
| `nombre_columna_original` | varchar(255) | YES | - | Nombre original en el Excel del broker |
| `indice_columna` | integer | NO | - | Índice numérico de la columna (0-based) |
| `letra_columna` | varchar(10) | YES | - | Letra de la columna (A, B, C, etc.) |
| `tipo_dato` | varchar(50) | YES | - | Tipo de dato esperado |
| `requerido` | boolean | YES | false | Si el campo es obligatorio |
| `descripcion` | text | YES | - | Descripción del campo |
| `fecha_creacion` | timestamp | YES | CURRENT_TIMESTAMP | Fecha de creación |
| **Campos de Estilo** |
| `color_fondo` | varchar(20) | YES | - | Color de fondo (formato hex: #RRGGBB) |
| `color_texto` | varchar(20) | YES | - | Color del texto (formato hex: #RRGGBB) |
| `es_negrita` | boolean | YES | false | Si tiene formato negrita |
| `es_cursiva` | boolean | YES | false | Si tiene formato cursiva |
| `tiene_borde` | boolean | YES | false | Si tiene bordes |

### Índices
- **PK**: `formato_columnas_pkey` (columna_id)
- **INDEX**: `idx_columna_formato` (formato_id)
- **INDEX**: `idx_columna_campo` (campo_estandar)
- **UNIQUE**: `uk_formato_campo` (formato_id, campo_estandar)

### Relaciones
- **FK**: `formato_id` → `broker_formatos.formato_id` (ON DELETE CASCADE)

### Campos Estándar Comunes

| Campo Estándar | Descripción |
|----------------|-------------|
| `LINE_NO` | Número de línea/ítem |
| `ITEM_CODE` | Código del producto |
| `ITEM_NAME` | Nombre del producto |
| `DESCRIPTION` | Descripción detallada |
| `CATEGORY` | Categoría del producto |
| `BRAND` | Marca |
| `QUANTITY` | Cantidad solicitada |
| `UOM` | Unidad de medida |
| `UNIT_PRICE` | Precio unitario |
| `DISCOUNT` | Descuento (%) |
| `VAT` | IVA (%) |
| `TOTAL` | Total de línea |
| `SUPPLIER_COMMENTS` | Comentarios del proveedor |
| `VESSEL_COMMENTS` | Comentarios del vessel |
| `PRECIO_VSS` | Precio VSS (columna adicional) |

### Ejemplo de Datos
```sql
INSERT INTO formato_columnas 
    (formato_id, campo_estandar, nombre_columna_original, indice_columna, letra_columna, color_fondo)
VALUES 
    (3, 'ITEM_CODE', 'Item Code', 2, 'C', NULL),
    (3, 'DESCRIPTION', 'Description', 3, 'D', NULL),
    (3, 'PRECIO_VSS', 'Precio VSS', 20, 'U', '#FFE0B2');
```

---

## 4. Tabla: `broker_metadata`

**Descripción:** Almacena metadata adicional del formato (información de encabezados, datos del RFQ, información de contacto, etc.).

### Estructura

| Columna | Tipo | Nulable | Default | Descripción |
|---------|------|---------|---------|-------------|
| `metadata_id` | integer | NO | nextval('broker_metadata_metadata_id_seq') | **PK** - ID único del registro |
| `formato_id` | integer | YES | - | **FK** → broker_formatos(formato_id) |
| `seccion` | varchar(100) | YES | - | Sección/categoría de la metadata |
| `campo_nombre` | varchar(100) | YES | - | Nombre del campo |
| `campo_valor` | text | YES | - | Valor del campo (puede ser texto de ejemplo) |
| `fila_origen` | integer | YES | - | Fila donde se encuentra en el Excel (0-based) |
| `columna_origen` | integer | YES | - | Índice de columna donde se encuentra (0-based) |
| `letra_columna` | varchar(5) | YES | - | Letra de columna en Excel |
| `fecha_creacion` | timestamp | YES | CURRENT_TIMESTAMP | Fecha de creación |

### Índices
- **PK**: `broker_metadata_pkey` (metadata_id)
- **INDEX**: `idx_broker_metadata_formato` (formato_id)
- **INDEX**: `idx_broker_metadata_seccion` (seccion)

### Relaciones
- **FK**: `formato_id` → `broker_formatos.formato_id` (ON DELETE CASCADE)

### Secciones Comunes

| Sección | Descripción | Ejemplos de Campos |
|---------|-------------|-------------------|
| `Company Details` | Información de la compañía del broker | Company Name, Address, Phone |
| `RFQ Information` | Información del RFQ/cotización | RFQ Number, Vessel, IMO, Port, Date |
| `Buyer Information` | Información del comprador | Buyer Name, Contact, Email |
| `Supplier Information` | Información del proveedor (VSS) | Supplier Name, Address, Contact |
| `Vendor Details` | Detalles del vendor | Vendor Name, Email, Phone |
| `Quotation Header` | Encabezado de cotización | Quotation Number, Vessel Name, IMO |

### Ejemplo de Datos
```sql
INSERT INTO broker_metadata 
    (formato_id, seccion, campo_nombre, campo_valor, fila_origen, columna_origen, letra_columna)
VALUES 
    (3, 'RFQ Information', 'Vessel', 'CMA CGM MEKONG', 1, 2, 'C'),
    (3, 'RFQ Information', 'RFQ No.', '2679-2025R-0341', 2, 2, 'C'),
    (3, 'RFQ Information', 'Port of Delivery', 'SAN ANTONIO', 6, 2, 'C');
```

---

## Diagrama de Relaciones

```
┌─────────────────┐
│    brokers      │
│  (broker_id PK) │
└────────┬────────┘
         │ 1:N
         ▼
┌─────────────────────┐
│  broker_formatos    │
│  (formato_id PK)    │
│  (broker_id FK)     │
└──────────┬──────────┘
           │ 1:N
     ┌─────┴─────┐
     ▼           ▼
┌──────────────────┐  ┌──────────────────┐
│formato_columnas  │  │ broker_metadata  │
│ (columna_id PK)  │  │ (metadata_id PK) │
│ (formato_id FK)  │  │ (formato_id FK)  │
└──────────────────┘  └──────────────────┘
```

### Descripción de Relaciones

1. **brokers → broker_formatos** (1:N)
   - Un broker puede tener múltiples formatos (versiones)
   - FK: `broker_formatos.broker_id` → `brokers.broker_id`

2. **broker_formatos → formato_columnas** (1:N)
   - Un formato tiene múltiples columnas definidas
   - FK: `formato_columnas.formato_id` → `broker_formatos.formato_id`
   - ON DELETE CASCADE: Si se elimina un formato, se eliminan sus columnas

3. **broker_formatos → broker_metadata** (1:N)
   - Un formato tiene múltiples registros de metadata
   - FK: `broker_metadata.formato_id` → `broker_formatos.formato_id`
   - ON DELETE CASCADE: Si se elimina un formato, se elimina su metadata

---

## Flujo de Uso

### 1. Registrar un Nuevo Broker

```sql
-- Paso 1: Crear el broker
INSERT INTO brokers (broker_name, descripcion, activo)
VALUES ('NUEVO BROKER', 'Descripción del broker', true)
RETURNING broker_id;

-- Paso 2: Crear el formato del broker
INSERT INTO broker_formatos (broker_id, version, header_row, descripcion)
VALUES (12, '1.0', 15, 'Formato estándar 2025')
RETURNING formato_id;

-- Paso 3: Definir las columnas del formato
INSERT INTO formato_columnas (formato_id, campo_estandar, nombre_columna_original, indice_columna, letra_columna)
VALUES 
    (8, 'ITEM_CODE', 'Item Code', 0, 'A'),
    (8, 'DESCRIPTION', 'Description', 1, 'B'),
    (8, 'QUANTITY', 'Qty', 2, 'C');

-- Paso 4: Agregar metadata del formato
INSERT INTO broker_metadata (formato_id, seccion, campo_nombre, campo_valor, fila_origen, columna_origen, letra_columna)
VALUES 
    (8, 'RFQ Information', 'RFQ Number', 'RFQ-12345', 0, 5, 'F'),
    (8, 'RFQ Information', 'Vessel', 'EXAMPLE VESSEL', 1, 5, 'F');
```

### 2. Consultar Formato Completo de un Broker

```sql
-- Obtener toda la configuración de un broker
SELECT 
    b.broker_name,
    bf.version,
    bf.header_row,
    fc.indice_columna,
    fc.letra_columna,
    fc.campo_estandar,
    fc.nombre_columna_original,
    fc.color_fondo
FROM brokers b
JOIN broker_formatos bf ON b.broker_id = bf.broker_id
JOIN formato_columnas fc ON bf.formato_id = fc.formato_id
WHERE b.broker_name = 'CMA CGM'
    AND bf.activo = true
ORDER BY fc.indice_columna;
```

### 3. Obtener Metadata de un Broker

```sql
-- Obtener metadata organizada por sección
SELECT 
    bm.seccion,
    bm.campo_nombre,
    bm.campo_valor,
    bm.fila_origen,
    bm.letra_columna
FROM brokers b
JOIN broker_formatos bf ON b.broker_id = bf.broker_id
JOIN broker_metadata bm ON bf.formato_id = bm.formato_id
WHERE b.broker_name = 'GARRETS INTERNATIONAL LTD'
    AND bf.activo = true
ORDER BY bm.seccion, bm.fila_origen;
```

---

## Vistas Útiles

### Vista: Formatos Activos con Detalles
```sql
CREATE VIEW v_formatos_activos AS
SELECT 
    b.broker_id,
    b.broker_name,
    bf.formato_id,
    bf.version,
    bf.header_row,
    COUNT(fc.columna_id) as num_columnas,
    COUNT(bm.metadata_id) as num_metadata
FROM brokers b
JOIN broker_formatos bf ON b.broker_id = bf.broker_id
LEFT JOIN formato_columnas fc ON bf.formato_id = fc.formato_id
LEFT JOIN broker_metadata bm ON bf.formato_id = bm.formato_id
WHERE bf.activo = true
GROUP BY b.broker_id, b.broker_name, bf.formato_id, bf.version, bf.header_row;
```

### Vista: Columnas Detalladas
```sql
CREATE VIEW v_columnas_detalladas AS
SELECT 
    b.broker_name,
    bf.version as formato_version,
    fc.indice_columna,
    fc.letra_columna,
    fc.campo_estandar,
    fc.nombre_columna_original,
    fc.tipo_dato,
    fc.requerido,
    fc.color_fondo,
    fc.es_negrita
FROM brokers b
JOIN broker_formatos bf ON b.broker_id = bf.broker_id
JOIN formato_columnas fc ON bf.formato_id = fc.formato_id
WHERE bf.activo = true
ORDER BY b.broker_name, fc.indice_columna;
```

---

## Notas de Implementación

### Índices de Columna (0-based vs 1-based)
- **Base de datos**: Los índices se almacenan en base 0 (primera columna = 0)
- **Excel**: Las columnas se numeran desde A (columna 0)
- **Filas**: También en base 0 (primera fila = 0)

### Convenciones de Nombres
- Campos estándar: MAYÚSCULAS con guion bajo (ej: `ITEM_CODE`, `UNIT_PRICE`)
- Nombres de tabla: minúsculas con guion bajo (ej: `broker_formatos`)
- Índices: prefijo `idx_` (ej: `idx_formato_broker`)

### Cascadas
- Al eliminar un `broker_formato`, se eliminan automáticamente:
  - Todas sus `formato_columnas` (ON DELETE CASCADE)
  - Toda su `broker_metadata` (ON DELETE CASCADE)
- Al eliminar un `broker`, NO se eliminan automáticamente sus formatos (requiere eliminación manual)

---

## Brokers Configurados (Enero 2026)

| Broker | Columnas | Metadata | Estado |
|--------|----------|----------|--------|
| CMA CGM | 16 | 17 | ✅ Completo |
| GARRETS INTERNATIONAL LTD | 16 | 33 | ✅ Completo |
| MCTC MARINE LTD | 12 | 8 | ✅ Completo |
| OCEANIC CATERING LTD | 20 | 6 | ✅ Completo |
| PROCURESHIP | 17 | 19 | ✅ Completo |
| MSC SHIPMANAGEMENT | 0 | 0 | ⚠️ Sin archivos |
| UMAR | 0 | 0 | ⚠️ Sin archivos |

---

## Referencias

- Base de datos: `sistema_cotizacion_2025`
- Usuario: `postgres`
- Puerto: `5432`
- Host: `localhost`

### Archivos Relacionados
- `ESTRUCTURA_BD.md` - Esquema completo de la base de datos
- `README.md` - Documentación general del proyecto
- `WORKFLOW_VIEWER.md` - Flujo de trabajo del visor de cotizaciones
