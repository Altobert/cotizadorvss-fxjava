-- 1. Crear tabla temporal
DROP TABLE IF EXISTS productos_csv;

CREATE TEMP TABLE productos_csv (
  familia TEXT,
  descripcion_completa TEXT,
  unidad_medida TEXT,
  valor_pesos_raw TEXT,
  precio_costo TEXT,
  tipo_cambio TEXT,
  porcentaje TEXT,
  precio_venta TEXT
);

-- 2. Cargar desde archivo limpio
-- IMPORTANTE: Ajustar la ruta del archivo según tu sistema
-- Ejecutar esto dentro de psql (sin punto y coma)
-- \copy productos_csv FROM '/ruta/completa/al/archivo/abarrotes_limpio.csv' DELIMITER ';' CSV HEADER
-- 
-- Ejemplo para tu caso:
-- \copy productos_csv FROM '/Users/claudioandressanmartinconcha/Desktop/abarrotes_limpio.csv' DELIMITER ';' CSV HEADER

-- 3. Insertar en tabla producto
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
  9,
  TRIM(SPLIT_PART(descripcion_completa, ' - ', 1)),
  TRIM(SPLIT_PART(descripcion_completa, ' - ', 2)),
  unidad_medida,
  CASE
    WHEN valor_pesos_raw ~ '^[\$\s]*[0-9,\.]+[\s]*$' THEN
      REPLACE(REPLACE(REPLACE(TRIM(valor_pesos_raw), '$', ''), '.', ''), ',', '.')::NUMERIC
    WHEN valor_pesos_raw IS NULL OR TRIM(valor_pesos_raw) = '' THEN 0
    ELSE 0
  END,
  NOW(),
  NULL
FROM productos_csv
WHERE descripcion_completa IS NOT NULL 
  AND TRIM(descripcion_completa) != ''
  AND unidad_medida IS NOT NULL
  AND TRIM(unidad_medida) != ''
  AND descripcion_completa LIKE '%-%';  -- Asegurar que tiene el formato esperado











