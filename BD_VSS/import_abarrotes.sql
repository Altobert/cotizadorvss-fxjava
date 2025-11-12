-- Crear familia de productos Abarrotes si no existe
INSERT INTO familia_producto (nombre, descripcion) 
VALUES ('Abarrotes', 'Provisiones y abarrotes varios')
ON CONFLICT (nombre) DO NOTHING;

-- Obtener el ID de la familia Abarrotes
DO $$
DECLARE
    familia_abarrotes_id BIGINT;
BEGIN
    SELECT id INTO familia_abarrotes_id FROM familia_producto WHERE nombre = 'Abarrotes';
    
    -- Crear tabla temporal para importar CSV
    CREATE TEMP TABLE temp_abarrotes (
        familia_path TEXT,
        descripcion_en TEXT,
        unidad_medida TEXT,
        valor_pesos TEXT,
        precio_costo_neto TEXT,
        tipo_cambio TEXT,
        porcentaje TEXT,
        precio_venta_neto TEXT,
        col9 TEXT,
        col10 TEXT,
        col11 TEXT
    );
END $$;

-- Copiar datos del CSV (ejecutar desde psql)
\COPY temp_abarrotes FROM '/Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava/BD_VSS/Abarrotes.csv' WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');

-- Insertar productos desde la tabla temporal a la tabla producto
INSERT INTO producto (familia_id, descripcion_en, descripcion_es, unidad_medida, valor_pesos, fecha_actualizacion, usuario_editor_id)
SELECT 
    (SELECT id FROM familia_producto WHERE nombre = 'Abarrotes'),
    TRIM(descripcion_en),
    NULL, -- No hay descripción en español en el CSV
    TRIM(unidad_medida),
    -- Limpiar el valor en pesos: remover $, espacios y convertir coma a punto
    CASE 
        WHEN TRIM(valor_pesos) = '' OR TRIM(valor_pesos) = '$ 0' THEN 0
        ELSE CAST(REPLACE(REPLACE(REPLACE(TRIM(valor_pesos), '$', ''), ' ', ''), '.', '') AS DECIMAL(12,2))
    END,
    NOW(),
    NULL
FROM temp_abarrotes
WHERE TRIM(descripcion_en) != '' 
  AND TRIM(descripcion_en) != 'DESCRIPCIÓN'
  AND familia_path LIKE '%ABARROTES%';

-- Limpiar tabla temporal
DROP TABLE temp_abarrotes;
