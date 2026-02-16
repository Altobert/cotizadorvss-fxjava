# Prueba de Coloreo de Filas

## Implementación Completada

Se ha implementado exitosamente la funcionalidad para colorear las filas de la tabla en amarillo cuando la descripción está vacía o nula.

### Cambios Realizados:

1. **ItemCotizacionExcel.java**:
   - Agregada propiedad `BooleanProperty descripcionVacia`
   - Constructor actualizado para detectar descripciones vacías automáticamente
   - Métodos getter, setter y property agregados

2. **Main.java**:
   - Configurado RowFactory en la tabla para aplicar estilo condicional
   - Filas con descripción vacía se colorean con fondo amarillo (#FFFF99)

3. **CotizacionService.java**:
   - Actualización explícita de la propiedad descripcionVacia al crear items

### Cómo Probar:

1. Ejecutar la aplicación con `mvn javafx:run`
2. Cargar un archivo Excel que contenga filas con descripciones vacías o nulas
3. Las filas con descripciones vacías aparecerán destacadas en amarillo

### Comportamiento Esperado:

- Filas con descripción vacía, nula o solo espacios: **Fondo amarillo**
- Filas con descripción válida: **Fondo normal**
- El coloreo se actualiza automáticamente al editar las celdas de descripción

La funcinalidad está lista para uso.