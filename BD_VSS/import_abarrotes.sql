
-- SCRIPT PARA CARGA CSV ABARROTES CON CAMPOS CORREGIDOS : USUARIO DITTO

-- Eliminar productos mal cargados recientemente si es que se necesita
DELETE FROM producto
WHERE familia_id = (
    SELECT id FROM familia_producto WHERE nombre = 'Abarrotes'
)
AND fecha_actualizacion >= NOW() - INTERVAL '10 minutes';

-- Crear tabla temporal con 11 columnas
DROP TABLE IF EXISTS temp_abarrotes;

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

-- Cargar CSV desde terminal psql
\COPY temp_abarrotes 
FROM '/Users/claudioandressanmartinconcha/Desktop/abarrotes.csv' 
WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');

-- Insertar productos con limpieza de formato y derivación de descripcion_es
INSERT INTO producto (
    familia_id,
    descripcion_en,
    descripcion_es,
    unidad_medida,
    valor_pesos,
    fecha_actualizacion,
    usuario_editor_id
)
SELECT 
    (SELECT id FROM familia_producto WHERE nombre = 'Abarrotes'),
    TRIM(SPLIT_PART(descripcion_en, '-', 1)),
    TRIM(SPLIT_PART(descripcion_en, '-', 2)),
    TRIM(unidad_medida),
    CASE 
        WHEN TRIM(valor_pesos) = '' OR TRIM(valor_pesos) ILIKE '$ 0' THEN 0
        ELSE CAST(
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(TRIM(valor_pesos), '$', ''), ' ', ''
                    ), '.', ''
                ), ',', '.'
            ) AS DECIMAL(12,2)
        )
    END,
    NOW(),
    NULL
FROM temp_abarrotes
WHERE TRIM(descripcion_en) != '' 
  AND TRIM(descripcion_en) != 'DESCRIPCIÓN'
  AND familia_path ILIKE '%ABARROTES%';

-- Eliminar tabla temporal
DROP TABLE temp_abarrotes;



**********************************************************************************
-- SCRIPT PARA CARGA CSV bebestibles : USUARIO DITTO
DROP TABLE IF EXISTS temp_bebestibles;

CREATE TEMP TABLE temp_bebestibles (
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

\COPY temp_bebestibles 
FROM '/Users/claudioandressanmartinconcha/Desktop/bebestibles.csv' 
WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');

INSERT INTO producto (
    familia_id,
    descripcion_en,
    descripcion_es,
    unidad_medida,
    valor_pesos,
    fecha_actualizacion,
    usuario_editor_id
)
SELECT 
    (SELECT id FROM familia_producto WHERE nombre = 'Bebestibles'),
    TRIM(SPLIT_PART(descripcion_en, '-', 1)),
    TRIM(SPLIT_PART(descripcion_en, '-', 2)),
    TRIM(unidad_medida),
    CASE 
        WHEN TRIM(valor_pesos) = '' OR TRIM(valor_pesos) ILIKE '$ 0' THEN 0
        ELSE CAST(
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(TRIM(valor_pesos), '$', ''), ' ', ''
                    ), '.', ''
                ), ',', '.'
            ) AS DECIMAL(12,2)
        )
    END,
    NOW(),
    NULL
FROM temp_bebestibles
WHERE TRIM(descripcion_en) != '' 
  AND TRIM(descripcion_en) != 'DESCRIPCIÓN'
  AND familia_path ILIKE '%BEBESTIBLES%';

DROP TABLE temp_bebestibles;


************************************************************************************
-- SCRIPT PARA CARGA CSV carnes : USUARIO DITTO


-- 2. Crear tabla temporal con 11 columnas (estructura del CSV)


DROP TABLE IF EXISTS temp_carnes;

CREATE TEMP TABLE temp_carnes (
    familia_path TEXT,
    descripcion_en TEXT,
    unidad_medida TEXT,
    valor_pesos TEXT,
    precio_costo_neto TEXT,
    tipo_cambio TEXT,
    porcentaje TEXT,
    precio_venta_neto TEXT
);


-- 3. Cargar CSV desde terminal psql
\COPY temp_carnes 
FROM '/Users/claudioandressanmartinconcha/Desktop/carnes.csv' 
WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');


INSERT INTO producto (
    familia_id,
    descripcion_en,
    descripcion_es,
    unidad_medida,
    valor_pesos,
    fecha_actualizacion,
    usuario_editor_id
)
SELECT 
    (SELECT id FROM familia_producto WHERE nombre = 'Carnes'),
    TRIM(SPLIT_PART(descripcion_en, '-', 1)),
    TRIM(SPLIT_PART(descripcion_en, '-', 2)),
    TRIM(unidad_medida),
    CASE 
        WHEN TRIM(valor_pesos) = '' OR TRIM(valor_pesos) ILIKE '$ 0' THEN 0
        ELSE CAST(
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(TRIM(valor_pesos), '$', ''), ' ', ''
                    ), '.', ''
                ), ',', '.'
            ) AS DECIMAL(12,2)
        )
    END,
    NOW(),
    NULL
FROM temp_carnes
WHERE TRIM(descripcion_en) != '' 
  AND TRIM(descripcion_en) != 'DESCRIPCIÓN'
  AND familia_path ILIKE '%CARNES%';


************************************************************************************

-- SCRIPT PARA CARGA CSV congelados : USUARIO DITTO



DROP TABLE IF EXISTS temp_congelados;

CREATE TEMP TABLE temp_congelados (
    familia_path TEXT,
    descripcion_en TEXT,
    unidad_medida TEXT,
    valor_pesos TEXT,
    precio_costo_neto TEXT,
    tipo_cambio TEXT,
    porcentaje TEXT,
    precio_venta_neto TEXT
);

\COPY temp_congelados 
FROM '/Users/claudioandressanmartinconcha/Desktop/congelados.csv' 
WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');

INSERT INTO producto (
    familia_id,
    descripcion_en,
    descripcion_es,
    unidad_medida,
    valor_pesos,
    fecha_actualizacion,
    usuario_editor_id
)
SELECT 
    (SELECT id FROM familia_producto WHERE nombre = 'Congelados'),
    TRIM(SPLIT_PART(descripcion_en, '-', 1)),
    TRIM(SPLIT_PART(descripcion_en, '-', 2)),
    TRIM(unidad_medida),
    CASE 
        WHEN TRIM(valor_pesos) = '' OR TRIM(valor_pesos) ILIKE '$ 0' THEN 0
        ELSE CAST(
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(TRIM(valor_pesos), '$', ''), ' ', ''
                    ), '.', ''
                ), ',', '.'
            ) AS DECIMAL(12,2)
        )
    END,
    NOW(),
    NULL
FROM temp_congelados
WHERE TRIM(descripcion_en) != '' 
  AND TRIM(descripcion_en) != 'DESCRIPCIÓN'
  AND familia_path ILIKE '%CONGELADOS%';
