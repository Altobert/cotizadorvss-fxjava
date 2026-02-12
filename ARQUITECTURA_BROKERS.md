# Arquitectura de Métodos Específicos por Broker

## 📋 Descripción

Se ha refactorizado el código de exportación de cotizaciones para separar la lógica específica de cada broker en métodos independientes. Esto permite:

1. **Aislamiento**: Los cambios en un broker no afectan a otros
2. **Claridad**: Código más fácil de entender y mantener
3. **Extensibilidad**: Fácil agregar nuevos brokers o modificar existentes
4. **Testing**: Cada broker puede probarse independientemente

---

## 🏗️ Estructura de la Arquitectura

### 1. Clase de Contexto: `BrokerExportContext`

Almacena la información relevante durante la exportación de un broker:

```java
private static class BrokerExportContext {
    public int filaEspecialIndex = -1;      // Índice de la fila especial (ej: "PROVISIONS")
    public int filaSubtotalIndex = -1;      // Índice de la fila de subtotal
    public int dataStartRow;                // Fila donde empiezan los datos
    public int colTotal = -1;               // Índice de la columna Total
    public String letraTotal = "";          // Letra de la columna Total (ej: "P")
}
```

### 2. Método Delegador: `aplicarLogicaEspecificaBroker()`

Punto de entrada que delega a los métodos específicos según el nombre del broker:

```java
private void aplicarLogicaEspecificaBroker(String brokerName, Cells cells, BrokerExportContext context)
```

**Flujo de decisión:**
- `BSM` o `CATERING` → `aplicarLogicaBSM()`
- `CMA` y `CGM` → `aplicarLogicaCMAGM()`
- `MCTC` o `MARINE` → `aplicarLogicaMCTC()`
- `OCEANIC` → `aplicarLogicaOceanic()`
- `GARRETS` → `aplicarLogicaGarrets()`
- `PROCURESHIP` → `aplicarLogicaProcureship()`
- Cualquier otro → `aplicarLogicaGenerica()`

---

## 🎯 Métodos Específicos por Broker

### BSM CATERING - `aplicarLogicaBSM()`

**Estado**: ✅ Implementado y funcionando

**Particularidades:**
1. **Fila especial forzada**: La fila después de las cabeceras es siempre "PROVISIONS"
   ```java
   context.filaEspecialIndex = context.dataStartRow;
   context.dataStartRow = context.dataStartRow + 1;
   ```

2. **Detección automática de subtotal**: Busca fórmulas `SUM` que sumen desde `dataStartRow`

**Resultado:**
- Fila especial: Fila 20 (índice 19)
- Datos empiezan: Fila 21 (índice 20)
- Subtotal detectado automáticamente por fórmula `=SUM(P21:P44)`

---

### CMA CGM - `aplicarLogicaCMAGM()`

**Estado**: 🚧 Pendiente de implementación

**Ubicación en código:**
```java
private void aplicarLogicaCMAGM(com.aspose.cells.Cells cells, BrokerExportContext context) {
    logger.info("🏛️ Aplicando lógica específica de CMA CGM");
    
    // TODO: Agregar particularidades de CMA CGM aquí
    // Por ahora, usar detección genérica
    aplicarLogicaGenerica(cells, context);
}
```

**Para implementar particularidades de CMA CGM:**
1. Identificar las particularidades del formato (filas especiales, subtotales, etc.)
2. Agregar lógica específica en este método
3. Probar la exportación con cotizaciones de CMA CGM

**Ejemplo de implementación:**
```java
private void aplicarLogicaCMAGM(com.aspose.cells.Cells cells, BrokerExportContext context) {
    logger.info("🏛️ Aplicando lógica específica de CMA CGM");
    
    // Ejemplo: Si CMA CGM tiene una fila especial diferente
    // detectarFilaEspecialPorTexto(cells, context, new String[]{"SUPPLIES", "ITEMS"});
    
    // Ejemplo: Si el subtotal está en una ubicación fija
    // context.filaSubtotalIndex = context.dataStartRow + numProductos + 2;
    
    // O usar detección automática
    detectarFilaSubtotalAutomatica(cells, context);
}
```

---

### Otros Brokers

Los siguientes brokers actualmente usan la **lógica genérica** (detección automática):

- **MCTC MARINE LTD**: `aplicarLogicaMCTC()`
- **OCEANIC CATERING LTD**: `aplicarLogicaOceanic()`
- **GARRETS INTERNATIONAL LTD**: `aplicarLogicaGarrets()`
- **PROCURESHIP**: `aplicarLogicaProcureship()`

Cada uno puede ser personalizado independientemente cuando se identifiquen particularidades.

---

## 🔧 Métodos Helper

### `aplicarLogicaGenerica()`

Lógica por defecto para brokers sin configuración específica:

1. Intenta detectar fila especial por textos comunes:
   - "PROVISIONS", "PROVISION", "ITEMS", "PRODUCTS", "PRODUCTOS", "LISTA"
2. Intenta detectar subtotal automáticamente por fórmula SUM

### `detectarFilaEspecialPorTexto()`

Busca textos específicos en la primera fila de datos para identificar filas especiales.

**Parámetros:**
- `cells`: Celdas del workbook
- `context`: Contexto de exportación
- `textosABuscar`: Array de strings a buscar (ej: `["PROVISIONS", "SUPPLIES"]`)

### `detectarFilaSubtotalAutomatica()`

Detecta automáticamente la fila de subtotal buscando fórmulas `SUM/SUMA` que:
1. Sumen desde cerca de `dataStartRow` (dentro de 5 filas)
2. Sumen un rango grande (≥10 filas)

**Ejemplo de detección:**
- Fórmula encontrada: `=SUM(P21:P234)`
- `dataStartRow`: 21
- Rango: 214 filas
- ✅ **Detectado como subtotal** (empieza en 21, suma 214 filas)

---

## 📝 Cómo Agregar Particularidades para CMA CGM

### Paso 1: Identificar Particularidades

Exporta manualmente una cotización de CMA CGM y analiza:
1. ¿Tiene fila especial? ¿En qué posición? ¿Qué texto contiene?
2. ¿Tiene subtotal? ¿Es automático o está en una posición fija?
3. ¿Hay columnas especiales que necesiten tratamiento diferente?

### Paso 2: Implementar en `aplicarLogicaCMAGM()`

Ejemplo basado en posibles escenarios:

**Escenario A: CMA CGM tiene fila especial "SUPPLIES"**
```java
private void aplicarLogicaCMAGM(com.aspose.cells.Cells cells, BrokerExportContext context) {
    logger.info("🏛️ Aplicando lógica específica de CMA CGM");
    
    // Detectar fila especial con texto "SUPPLIES"
    detectarFilaEspecialPorTexto(cells, context, new String[]{"SUPPLIES", "SUPPLY LIST"});
    
    // Detectar subtotal automáticamente
    detectarFilaSubtotalAutomatica(cells, context);
}
```

**Escenario B: CMA CGM no tiene fila especial, subtotal en posición fija**
```java
private void aplicarLogicaCMAGM(com.aspose.cells.Cells cells, BrokerExportContext context) {
    logger.info("🏛️ Aplicando lógica específica de CMA CGM");
    
    // Sin fila especial
    logger.info("📦 CMA CGM: Sin fila especial, datos desde fila {}", context.dataStartRow + 1);
    
    // Subtotal siempre 2 filas después del último producto
    // (esto se establecería después de escribir los datos)
    // context.filaSubtotalIndex = context.dataStartRow + numProductos + 2;
    
    // O detectar automáticamente
    detectarFilaSubtotalAutomatica(cells, context);
}
```

**Escenario C: CMA CGM tiene lógica completamente diferente**
```java
private void aplicarLogicaCMAGM(com.aspose.cells.Cells cells, BrokerExportContext context) {
    logger.info("🏛️ Aplicando lógica específica de CMA CGM");
    
    // Lógica personalizada completa
    // Por ejemplo: verificar color de celda, formato específico, etc.
    
    // Revisar si la primera fila después de las cabeceras tiene color específico
    com.aspose.cells.Cell celdaCheck = cells.get(context.dataStartRow, 0);
    if (celdaCheck != null) {
        com.aspose.cells.Style estilo = celdaCheck.getStyle();
        if (estilo != null) {
            com.aspose.cells.Color bgColor = estilo.getBackgroundColor();
            if (bgColor != null && bgColor.getR() == (byte)200 && bgColor.getG() == (byte)200) {
                context.filaEspecialIndex = context.dataStartRow;
                context.dataStartRow = context.dataStartRow + 1;
                logger.info("🟡 CMA CGM: Fila especial detectada por color en fila {}", context.filaEspecialIndex + 1);
            }
        }
    }
    
    // Continuar con lógica específica...
}
```

### Paso 3: Probar

1. Compilar: `mvn clean compile`
2. Ejecutar la aplicación
3. Exportar una cotización de CMA CGM
4. Verificar los logs para confirmar que se ejecuta `aplicarLogicaCMAGM()`
5. Verificar que el archivo exportado es correcto

---

## ✅ Ventajas de esta Arquitectura

### 1. **Seguridad**
Los cambios en CMA CGM **NO afectarán a BSM CATERING**. Cada broker tiene su propio método aislado.

### 2. **Claridad**
El código es más fácil de entender:
```java
// Antes (todo mezclado en un solo método)
if (broker.contains("BSM")) {
    // lógica BSM
} else if (broker.contains("CMA")) {
    // lógica CMA
} // ... 500 líneas más abajo

// Ahora (métodos separados)
aplicarLogicaBSM() {
    // toda la lógica de BSM aquí
}

aplicarLogicaCMAGM() {
    // toda la lógica de CMA aquí
}
```

### 3. **Extensibilidad**
Agregar un nuevo broker es tan simple como crear un nuevo método:
```java
private void aplicarLogicaNuevoBroker(com.aspose.cells.Cells cells, BrokerExportContext context) {
    // lógica específica del nuevo broker
}
```

Y agregarlo al switch:
```java
else if (brokerUpper.contains("NUEVO")) {
    aplicarLogicaNuevoBroker(cells, context);
}
```

### 4. **Mantenibilidad**
Si hay un problema con un broker específico, sabes exactamente dónde buscar:
- Problema con BSM → `aplicarLogicaBSM()`
- Problema con CMA CGM → `aplicarLogicaCMAGM()`

---

## 📊 Resumen

| Broker | Método | Estado | Particularidades |
|--------|--------|--------|------------------|
| **BSM CATERING** | `aplicarLogicaBSM()` | ✅ Implementado | Fila PROVISIONS forzada + subtotal automático |
| **CMA CGM** | `aplicarLogicaCMAGM()` | 🚧 Pendiente | Usa lógica genérica por ahora |
| **MCTC** | `aplicarLogicaMCTC()` | 🔧 Genérico | Detección automática |
| **OCEANIC** | `aplicarLogicaOceanic()` | 🔧 Genérico | Detección automática |
| **GARRETS** | `aplicarLogicaGarrets()` | 🔧 Genérico | Detección automática |
| **PROCURESHIP** | `aplicarLogicaProcureship()` | 🔧 Genérico | Detección automática |

---

## 🎯 Próximos Pasos

1. **Identificar particularidades de CMA CGM**:
   - Exportar manualmente una cotización
   - Analizar el formato del archivo Excel
   - Documentar las diferencias con otros brokers

2. **Implementar lógica específica de CMA CGM**:
   - Editar el método `aplicarLogicaCMAGM()`
   - Agregar detección de filas especiales si las hay
   - Configurar detección de subtotal según el formato

3. **Probar la implementación**:
   - Exportar varias cotizaciones de CMA CGM
   - Verificar que BSM CATERING sigue funcionando correctamente
   - Confirmar que no hay regresiones

4. **Repetir para otros brokers si es necesario**:
   - Si MCTC, OCEANIC, GARRETS o PROCURESHIP tienen particularidades, implementar sus métodos específicos

---

## 📞 ¿Necesitas Ayuda?

Para implementar particularidades de CMA CGM u otro broker:

1. **Comparte un archivo de ejemplo del broker**
2. **Describe qué particularidades tiene** (filas especiales, subtotales, etc.)
3. **Indica qué comportamiento esperas** en la exportación

Y puedo ayudarte a implementar el método específico correspondiente.
