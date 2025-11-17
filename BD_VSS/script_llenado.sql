
// Script para poblar tabla familia_producto (antes de cargar los productos debido a que es clave foranea de la tabla productos.)

INSERT INTO familia_producto (nombre, descripcion) VALUES
('Tostaduria', 'Productos de frutos secos, semillas y similares'),
('Lacteos', 'Productos lácteos y derivados'),
('Carnes', 'Productos cárnicos y embutidos'),
('Bebestibles', 'Jugos, aguas, bebidas y líquidos'),
('Congelados', 'Productos congelados listos para consumo'),
('Pescados y mariscos', 'Productos del mar, frescos o en conserva'),
('Frutas y verduras', 'Productos vegetales frescos o procesados'),
('Indu', 'Productos industriales o de uso general'),
('Abarrotes', 'Productos de almacén, abarrotes y consumo diario');
*******************************************************************************************
// Script para poblar con 10 productos de prueba.

-- Inserción de 10 productos de prueba en la familia 'Abarrotes' (id = 9)
INSERT INTO producto (
  familia_id,
  descripcion_en,
  descripcion_es,
  unidad_medida,
  valor_pesos,
  fecha_actualizacion,
  usuario_editor_id
) VALUES
(9, 'AFTER EGHT CHOCOLATE 200GR', 'CHOCOLATE 200GR', 'PCS', 0, NOW(), NULL),
(9, 'TEA JASMINE X 20 S', 'TE JAZMIN 100S', 'UNIT', 0, NOW(), NULL),
(9, 'AJINOMOTO', 'AJINOMOTO', 'KGS', 3990, NOW(), NULL),
(9, 'ALIÑO COMPLETO 1 KILO', 'ALL SPICE POWDER 1 KG', 'KGS', 12500, NOW(), NULL),
(9, 'ALIÑO COMPLETO 500 GR', 'ALL SPICE POWDER 500 GRS', 'UNIT', 6250, NOW(), NULL),
(9, 'ALLSPICE POWDER 100GR', 'ALIÑO COMPLETO 100GR', 'PKT', 1250, NOW(), NULL),
(9, 'ALMOND 100 GR', 'ALMENDRAS 100 GRS', 'PKT', 1176, NOW(), NULL),
(9, 'ALMOND FLAKES', 'ALMENDRAS LAMINADAS', 'KGS', 12571, NOW(), NULL),
(9, 'ALMOND FLOUR', 'HARINA DE ALMENDRAS', 'KGS', 10924, NOW(), NULL),
(9, 'ALMOND NON SALTED 100 GRS', 'ALMENDRAS NATURALES', 'PKT', 1295, NOW(), NULL);

*******************************************************************************************
-- 🧮 Script Insercion Parámetros comerciales iniciales para poblar BD y probar vista 

INSERT INTO parametros_comerciales (
  tipo_cambio_usado,
  porcentaje_utilidad,
  fecha_vigencia,
  usuario_editor_id
) VALUES (
  870.00,       -- Tipo de cambio inicial
  1.55,         -- Margen de utilidad (55%)
  CURRENT_DATE, -- Fecha de vigencia desde hoy
  NULL          -- Usuario editor (puedes usar un ID real si lo tienes)
);

*********************************************************************************************
Script SQL: Vista dinámica de precios (para realizar consultas cuyos script detallo debajo de esta CREATE VIEW)

CREATE VIEW vista_producto_precio AS
SELECT
  p.id,
  p.descripcion_es,
  p.descripcion_en,
  p.unidad_medida,
  p.valor_pesos,
  pc.tipo_cambio_usado,
  pc.porcentaje_utilidad,
  ROUND(p.valor_pesos / pc.tipo_cambio_usado, 2) AS precio_costo_neto,
  ROUND((p.valor_pesos / pc.tipo_cambio_usado) * pc.porcentaje_utilidad, 2) AS precio_venta_neto,
  p.fecha_actualizacion,
  p.usuario_editor_id
FROM producto p
JOIN (
  SELECT tipo_cambio_usado, porcentaje_utilidad
  FROM parametros_comerciales
  ORDER BY fecha_vigencia DESC
  LIMIT 1
) pc ON true;

vista para consultar precios actualizados en USD:
SELECT * FROM vista_producto_precio;

Filtrar por familia, unidad o nombre:
SELECT * FROM vista_producto_precio WHERE unidad_medida = 'KGS';


