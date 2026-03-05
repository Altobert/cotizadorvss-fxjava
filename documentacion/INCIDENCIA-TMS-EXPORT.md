# Incidencia TMS: exportación UNIT PRICE e ITEM COMMENTS

## Qué pasaba
- En TMS, al exportar con Aspose, no se veían `UNIT PRICE` ni `ITEM COMMENTS` en el Excel final.

## Dónde estaba el problema
- Se escribía en hoja/columnas que no siempre coincidían con la estructura real del archivo TMS.
- El guardado de comentarios en UI no siempre encontraba el campo de TMS (`ITEM COMMENTS`).
- La exportación podía mapear por búsqueda de `part`, lo que podía desalinear filas en algunos casos.

## Qué se corrigió
- Para TMS, la escritura Aspose ahora prioriza la hoja `Sheet1`.
- Se agregó fallback por encabezado real de Excel para detectar columnas de `UNIT PRICE` y `ITEM COMMENTS`.
- Se guarda y reutiliza `__EXCEL_ROW_INDEX` para escribir en la fila original del archivo.
- En el popup de edición, se agregó soporte para detectar/guardar `ITEM COMMENTS`.

## Resultado esperado
- `UNIT PRICE` se exporta en la columna correcta.
- `ITEM COMMENTS` se exporta cuando el valor está informado.
- Logs de referencia:
  - `Aspose write target | hoja='Sheet1' ...`
  - `Exportación UNIT_PRICE | escritos=...`
  - `Exportación REMARKS | escritos=...`

## Nota operativa
- Si `filasSinPart > 0`, esas filas no se actualizan por diseño (no tienen referencia válida de item/part).