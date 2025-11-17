-- Script para borrar el contenido de todas las tablas en sistema_cotizacion
-- CUIDADO: Este script eliminará TODOS los datos

-- Deshabilitar temporalmente las restricciones de foreign key
SET session_replication_role = 'replica';

-- Borrar datos de todas las tablas (en orden por dependencias)
TRUNCATE TABLE cliente RESTART IDENTITY CASCADE;
TRUNCATE TABLE cotizacion RESTART IDENTITY CASCADE;
TRUNCATE TABLE familia_producto RESTART IDENTITY CASCADE;
TRUNCATE TABLE item_cotizacion RESTART IDENTITY CASCADE;
TRUNCATE TABLE log_importacion RESTART IDENTITY CASCADE;
TRUNCATE TABLE parametros_comerciales RESTART IDENTITY CASCADE;
TRUNCATE TABLE producto RESTART IDENTITY CASCADE;
TRUNCATE TABLE tipo_cambio RESTART IDENTITY CASCADE;
TRUNCATE TABLE usuario RESTART IDENTITY CASCADE;
TRUNCATE TABLE familia_producto RESTART IDENTITY CASCADE;

-- Rehabilitar las restricciones de foreign key
SET session_replication_role = 'origin';

-- Confirmar el borrado
SELECT 'Todas las tablas han sido vaciadas exitosamente' AS resultado;
