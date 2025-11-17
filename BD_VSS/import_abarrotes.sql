
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
--FROM '/Users/claudioandressanmartinconcha/Desktop/abarrotes.csv' 
\COPY temp_abarrotes FROM '/Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava/BD_VSS/Excel_productos_csv/abarrotes.csv' WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');

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

-- **********************************************************************************
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

\COPY temp_bebestibles FROM '/Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava/BD_VSS/Excel_productos_csv/bebestibles.csv' WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');

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


--- ************************************************************************************
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
\COPY temp_carnes FROM '/Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava/BD_VSS/Excel_productos_csv/carnes.csv' WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');


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


-- ************************************************************************************

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

\COPY temp_congelados FROM '/Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava/BD_VSS/Excel_productos_csv/congelados.csv' WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');

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


--************************************************************************************

-- SCRIPT PARA CARGA CSV indu : USUARIO DITTO
DROP TABLE IF EXISTS temp_indu;

CREATE TEMP TABLE temp_indu (
    familia_path TEXT,
    descripcion_en TEXT,
    unidad_medida TEXT,
    valor_pesos TEXT,
    precio_costo_neto TEXT,
    tipo_cambio TEXT,
    porcentaje TEXT,
    precio_venta_neto TEXT
);

\COPY temp_indu FROM '/Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava/BD_VSS/Excel_productos_csv/indu.csv' WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');

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
    (SELECT id FROM familia_producto WHERE nombre = 'Indu'),
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
FROM temp_indu
WHERE TRIM(descripcion_en) != '' 
  AND TRIM(descripcion_en) != 'DESCRIPCIÓN'
  AND familia_path ILIKE '%INDU%';

DROP TABLE temp_indu;

--************************************************************************************

-- SCRIPT PARA CARGA CSV lacteos : USUARIO DITTO
DROP TABLE IF EXISTS temp_lacteos;

CREATE TEMP TABLE temp_lacteos (
    familia_path TEXT,
    descripcion_en TEXT,
    unidad_medida TEXT,
    valor_pesos TEXT,
    precio_costo_neto TEXT,
    tipo_cambio TEXT,
    porcentaje TEXT,
    precio_venta_neto TEXT
);

\COPY temp_lacteos FROM '/Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava/BD_VSS/Excel_productos_csv/lacteos.csv' WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');

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
    (SELECT id FROM familia_producto WHERE nombre = 'Lacteos'),
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
FROM temp_lacteos
WHERE TRIM(descripcion_en) != '' 
  AND TRIM(descripcion_en) != 'DESCRIPCIÓN'
  AND familia_path ILIKE '%LACTEOS%';

DROP TABLE temp_lacteos;

--************************************************************************************

-- SCRIPT PARA CARGA CSV tostaduria : USUARIO DITTO

DROP TABLE IF EXISTS temp_tostaduria;

CREATE TEMP TABLE temp_tostaduria (
    familia_path TEXT,
    descripcion_en TEXT,
    unidad_medida TEXT,
    valor_pesos TEXT,
    precio_costo_neto TEXT,
    tipo_cambio TEXT,
    porcentaje TEXT,
    precio_venta_neto TEXT
);

\COPY temp_tostaduria FROM '/Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava/BD_VSS/Excel_productos_csv/tostaduria.csv' WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');

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
    (SELECT id FROM familia_producto WHERE nombre = 'Tostaduria'),
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
FROM temp_tostaduria
WHERE TRIM(descripcion_en) != '' 
  AND TRIM(descripcion_en) != 'DESCRIPCIÓN'
  AND familia_path ILIKE '%TOSTADURIA%';

DROP TABLE temp_tostaduria;

--************************************************************************************

-- SCRIPT PARA CARGA CSV frutas_y_verduras : USUARIO DITTO

-- 1. Crear tabla temporal con 8 columnas
DROP TABLE IF EXISTS temp_frutas_verduras;

CREATE TEMP TABLE temp_frutas_verduras (
    familia_path TEXT,
    descripcion_en TEXT,
    unidad_medida TEXT,
    valor_pesos TEXT,
    precio_costo_neto TEXT,
    tipo_cambio TEXT,
    porcentaje TEXT,
    precio_venta_neto TEXT
);

-- 2. Cargar CSV
\COPY temp_frutas_verduras FROM '/Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava/BD_VSS/Excel_productos_csv/frutas_y_verduras.csv' WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');

-- 3. Insertar en la tabla principal usando el ID 7
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
    7,  -- id de la familia 'Frutas y Verduras'
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
FROM temp_frutas_verduras
WHERE TRIM(descripcion_en) != '' 
  AND TRIM(descripcion_en) != 'DESCRIPCIÓN';

-- 4. Eliminar tabla temporal
DROP TABLE temp_frutas_verduras;

-- SCRIPT PARA CARGA CSV pescados_y_mariscos : USUARIO DITTO

-- 1. Crear tabla temporal con 8 columnas
DROP TABLE IF EXISTS temp_pescados_mariscos;

CREATE TEMP TABLE temp_pescados_mariscos (
    familia_path TEXT,
    descripcion_en TEXT,
    unidad_medida TEXT,
    valor_pesos TEXT,
    precio_costo_neto TEXT,
    tipo_cambio TEXT,
    porcentaje TEXT,
    precio_venta_neto TEXT
);

-- 2. Cargar CSV
\COPY temp_pescados_mariscos FROM '/Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava/BD_VSS/Excel_productos_csv/pescados_y_mariscos.csv' WITH (FORMAT csv, DELIMITER ';', HEADER true, ENCODING 'UTF-8');

-- 3. Insertar en la tabla principal
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
    (SELECT id FROM familia_producto WHERE nombre = 'Pescados y mariscos'),
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
FROM temp_pescados_mariscos
WHERE TRIM(descripcion_en) != '' 
  AND TRIM(descripcion_en) != 'DESCRIPCIÓN';

-- 4. Eliminar tabla temporal
DROP TABLE temp_pescados_mariscos;

