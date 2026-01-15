

-- VISTA PRODUCTO / PRECIO

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
  ORDER BY fecha_actualizacion DESC
  LIMIT 1
) pc ON true;


-- VISTA PRODUCTO / PRECIO /FAMILIA

CREATE OR REPLACE VIEW vista_producto_precio_familia AS
SELECT
  p.id,
  f.nombre AS familia,
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
JOIN familia_producto f ON p.familia_id = f.id
JOIN (
  SELECT tipo_cambio_usado, porcentaje_utilidad
  FROM parametros_comerciales
  ORDER BY fecha_actualizacion DESC
  LIMIT 1
) pc ON true
ORDER BY f.nombre ASC, p.descripcion_en ASC;
--*******************************************************************************************

--Script para poblar tabla familia_producto (antes de cargar los productos debido a que es clave foranea de la tabla productos.)

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



-- Script Insercion Parámetros comerciales iniciales para poblar BD y probar vista 

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

