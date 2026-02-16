# Guía de Uso: INSERTS_BROKERS.sql

## Descripción

El archivo `INSERTS_BROKERS.sql` contiene todos los INSERT statements necesarios para recrear las 4 tablas principales del sistema de brokers:

1. **brokers** - 11 registros
2. **broker_formatos** - 6 registros  
3. **formato_columnas** - ~120+ registros
4. **broker_metadata** - ~80+ registros

**Total**: 311 líneas SQL

---

## Contenido del Archivo

### 1. Tabla `brokers`
Brokers registrados:
- OCEANIC CATERING LTD
- MCTC MARINE LTD
- CMA CGM
- PROCURESHIP
- GARRETS INTERNATIONAL LTD
- BSM CATERING
- MSC SHIPMANAGEMENT
- ANGLO EASTERN
- UMAR
- BERNHARD SCHULTE
- OPERATION3

### 2. Tabla `broker_formatos`
Formatos configurados con sus versiones y fila de encabezado:
- Formato 1: OCEANIC CATERING LTD (header_row: 13)
- Formato 2: MCTC MARINE LTD (header_row: 10)
- Formato 3: CMA CGM (header_row: 19)
- Formato 4: PROCURESHIP (header_row: 14)
- Formato 5: GARRETS INTERNATIONAL LTD (header_row: 25)
- Formato 7: BSM CATERING (header_row: 18)

### 3. Tabla `formato_columnas`
Definiciones de columnas para cada formato, incluyendo:
- Campo estándar
- Nombre de columna original
- Índice y letra de columna
- Estilos (colores, negrita, bordes)

### 4. Tabla `broker_metadata`
Metadata de cada broker (RFQ Info, Company Details, Vendor Details, etc.)

---

## Cómo Usar el Archivo

### Opción 1: Importar en Base de Datos Vacía

```bash
# Conectar a PostgreSQL y ejecutar el archivo
psql -U postgres -d sistema_cotizacion_2025 -f INSERTS_BROKERS.sql
```

### Opción 2: Importar en Base de Datos Existente

Si la base de datos ya tiene datos y quieres **reemplazarlos**:

```bash
# 1. Primero, limpiar las tablas (CUIDADO: esto elimina todos los datos)
psql -U postgres -d sistema_cotizacion_2025 <<EOF
DELETE FROM broker_metadata;
DELETE FROM formato_columnas;
DELETE FROM broker_formatos;
DELETE FROM brokers;
EOF

# 2. Luego importar el archivo
psql -U postgres -d sistema_cotizacion_2025 -f INSERTS_BROKERS.sql
```

### Opción 3: Importar Solo Algunas Tablas

Puedes editar el archivo `INSERTS_BROKERS.sql` y comentar las secciones que no necesites.

Por ejemplo, para solo importar `brokers` y `broker_formatos`:
1. Abrir el archivo
2. Comentar (con `--`) las líneas de `formato_columnas` y `broker_metadata`
3. Ejecutar el archivo modificado

---

## Regenerar el Archivo

Si has hecho cambios en la base de datos y quieres regenerar el archivo:

```bash
pg_dump -U postgres -d sistema_cotizacion_2025 \
  -t brokers \
  -t broker_formatos \
  -t formato_columnas \
  -t broker_metadata \
  --data-only \
  --inserts \
  --column-inserts \
  -f INSERTS_BROKERS.sql
```

### Opciones de pg_dump Utilizadas

- `-t tabla`: Especifica qué tabla exportar
- `--data-only`: Solo datos, sin estructura de tablas
- `--inserts`: Usa INSERT statements (en lugar de COPY)
- `--column-inserts`: Incluye nombres de columnas en los INSERT
- `-f archivo`: Archivo de salida

---

## Verificación Post-Importación

Después de importar, verifica que todo se haya cargado correctamente:

```sql
-- Contar registros en cada tabla
SELECT 
    'brokers' as tabla,
    COUNT(*) as registros
FROM brokers
UNION ALL
SELECT 
    'broker_formatos',
    COUNT(*)
FROM broker_formatos
UNION ALL
SELECT 
    'formato_columnas',
    COUNT(*)
FROM formato_columnas
UNION ALL
SELECT 
    'broker_metadata',
    COUNT(*)
FROM broker_metadata;
```

Resultado esperado:
```
     tabla       | registros 
-----------------+-----------
 brokers         |        11
 broker_formatos |         6
 formato_columnas|       120+
 broker_metadata |        80+
```

### Verificar Integridad de Relaciones

```sql
SELECT 
    b.broker_name,
    COUNT(DISTINCT bf.formato_id) as formatos,
    COUNT(DISTINCT fc.columna_id) as columnas,
    COUNT(DISTINCT bm.metadata_id) as metadata
FROM brokers b
LEFT JOIN broker_formatos bf ON b.broker_id = bf.broker_id
LEFT JOIN formato_columnas fc ON bf.formato_id = fc.formato_id
LEFT JOIN broker_metadata bm ON bf.formato_id = bm.formato_id
GROUP BY b.broker_id, b.broker_name
ORDER BY b.broker_name;
```

---

## Resetear Secuencias (Opcional)

Si importas en una base de datos nueva, es recomendable resetear las secuencias:

```sql
-- Obtener el máximo ID actual y ajustar la secuencia
SELECT setval('brokers_broker_id_seq', (SELECT MAX(broker_id) FROM brokers));
SELECT setval('broker_formatos_formato_id_seq', (SELECT MAX(formato_id) FROM broker_formatos));
SELECT setval('formato_columnas_columna_id_seq', (SELECT MAX(columna_id) FROM formato_columnas));
SELECT setval('broker_metadata_metadata_id_seq', (SELECT MAX(metadata_id) FROM broker_metadata));
```

---

## Backup y Restauración

### Crear Backup Completo

```bash
# Backup de solo las tablas de brokers
pg_dump -U postgres -d sistema_cotizacion_2025 \
  -t brokers \
  -t broker_formatos \
  -t formato_columnas \
  -t broker_metadata \
  -f backup_brokers_$(date +%Y%m%d).sql
```

### Restaurar desde Backup

```bash
psql -U postgres -d sistema_cotizacion_2025 -f backup_brokers_YYYYMMDD.sql
```

---

## Exportar a Otros Formatos

### CSV

```bash
# Exportar brokers a CSV
psql -U postgres -d sistema_cotizacion_2025 \
  -c "\COPY brokers TO 'brokers.csv' WITH CSV HEADER"

# Exportar broker_formatos a CSV
psql -U postgres -d sistema_cotizacion_2025 \
  -c "\COPY broker_formatos TO 'broker_formatos.csv' WITH CSV HEADER"
```

### JSON

```bash
# Exportar brokers a JSON
psql -U postgres -d sistema_cotizacion_2025 -c \
  "SELECT json_agg(row_to_json(brokers)) FROM brokers" \
  -t -o brokers.json
```

---

## Notas Importantes

### ⚠️ Advertencias

1. **Pérdida de Datos**: Eliminar las tablas antes de importar causará pérdida de datos. Haz backup primero.

2. **IDs Secuenciales**: Los IDs en el archivo son los originales. Si importas en una base que ya tiene datos, podrían haber conflictos de claves primarias.

3. **Relaciones**: El archivo respeta las relaciones entre tablas. Se debe importar en orden:
   - Primero `brokers`
   - Luego `broker_formatos`
   - Finalmente `formato_columnas` y `broker_metadata`

4. **Timestamps**: Los timestamps en el archivo son de cuando se exportaron. Se mantendrán esos valores al importar.

### ✅ Buenas Prácticas

- Siempre hacer backup antes de importar datos
- Verificar los datos después de importar
- Resetear secuencias después de importar
- Documentar cualquier cambio manual realizado

---

## Troubleshooting

### Error: "duplicate key value violates unique constraint"

**Causa**: Ya existen registros con los mismos IDs.

**Solución**: Limpiar las tablas antes de importar, o modificar el archivo para que use IDs diferentes.

### Error: "relation does not exist"

**Causa**: Las tablas no existen en la base de datos.

**Solución**: Crear primero las tablas usando el script de estructura (ver `ESTRUCTURA_BD.md`).

### Error: "foreign key constraint"

**Causa**: Intentando insertar datos en orden incorrecto.

**Solución**: El archivo ya está en el orden correcto. Si editaste el archivo, asegúrate de mantener el orden:
1. brokers
2. broker_formatos  
3. formato_columnas y broker_metadata

---

## Referencias

- Base de datos: `sistema_cotizacion_2025`
- Usuario: `postgres`
- Documentación: Ver `TABLAS_BROKERS.md` para detalles de estructura
- Fecha de generación: 2026-01-26

---

## Ejemplo Completo de Uso

```bash
# 1. Crear backup de datos actuales
pg_dump -U postgres -d sistema_cotizacion_2025 \
  -t brokers -t broker_formatos -t formato_columnas -t broker_metadata \
  -f backup_antes_importar_$(date +%Y%m%d_%H%M%S).sql

# 2. Limpiar tablas (OPCIONAL)
psql -U postgres -d sistema_cotizacion_2025 <<EOF
DELETE FROM broker_metadata;
DELETE FROM formato_columnas;
DELETE FROM broker_formatos;
DELETE FROM brokers;
EOF

# 3. Importar datos
psql -U postgres -d sistema_cotizacion_2025 -f INSERTS_BROKERS.sql

# 4. Resetear secuencias
psql -U postgres -d sistema_cotizacion_2025 <<EOF
SELECT setval('brokers_broker_id_seq', (SELECT MAX(broker_id) FROM brokers));
SELECT setval('broker_formatos_formato_id_seq', (SELECT MAX(formato_id) FROM broker_formatos));
SELECT setval('formato_columnas_columna_id_seq', (SELECT MAX(columna_id) FROM formato_columnas));
SELECT setval('broker_metadata_metadata_id_seq', (SELECT MAX(metadata_id) FROM broker_metadata));
EOF

# 5. Verificar
psql -U postgres -d sistema_cotizacion_2025 -c "
SELECT 
    b.broker_name,
    COUNT(DISTINCT bf.formato_id) as formatos,
    COUNT(DISTINCT fc.columna_id) as columnas,
    COUNT(DISTINCT bm.metadata_id) as metadata
FROM brokers b
LEFT JOIN broker_formatos bf ON b.broker_id = bf.broker_id
LEFT JOIN formato_columnas fc ON bf.formato_id = fc.formato_id
LEFT JOIN broker_metadata bm ON bf.formato_id = bm.formato_id
GROUP BY b.broker_id, b.broker_name
ORDER BY b.broker_name;
"
```
