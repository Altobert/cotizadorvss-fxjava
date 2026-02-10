-- Consultar información del broker BSM Catering
\echo '=== BROKER BSM CATERING ==='
SELECT broker_id, broker_name, descripcion, contacto 
FROM brokers 
WHERE broker_id = 6;

\echo ''
\echo '=== FORMATO BSM (formato_id=7) ==='
SELECT formato_id, version, header_row, descripcion 
FROM broker_formatos 
WHERE formato_id = 7;

\echo ''
\echo '=== COLUMNAS (primeras 5) ==='
SELECT columna_id, campo_estandar, nombre_columna_original, indice_columna, 
       letra_columna, color_fondo, color_texto, es_negrita
FROM formato_columnas 
WHERE formato_id = 7 
ORDER BY indice_columna 
LIMIT 5;

\echo ''
\echo '=== METADATA (primeras 10) ==='
SELECT metadata_id, seccion, campo_nombre, campo_valor, 
       fila_origen, columna_origen, letra_columna
FROM broker_metadata 
WHERE formato_id = 7 
ORDER BY seccion, metadata_id
LIMIT 10;

\echo ''
\echo '=== RESUMEN DE REGISTROS ==='
SELECT 'Columnas totales' as tipo, COUNT(*) as cantidad 
FROM formato_columnas WHERE formato_id = 7
UNION ALL
SELECT 'Metadata totales', COUNT(*) 
FROM broker_metadata WHERE formato_id = 7;
