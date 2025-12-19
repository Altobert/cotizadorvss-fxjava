

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
