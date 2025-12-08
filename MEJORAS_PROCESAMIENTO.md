# Mejoras de Procesamiento de Cotizaciones

Documento que describe las mejoras implementadas en el sistema de lectura de cotizaciones desde archivos Excel.

## Resumen de Cambios

Se han implementado 3 puntos principales de mejora:

### ✅ Punto 2: Metadatos de Origen
### ✅ Punto 3: Estrategia de Detección por Formato
### ✅ Punto 4: Manejo Robusto de Errores

---

## Punto 2: Metadatos de Origen

Se agregaron campos a los modelos `Cotizacion` e `ItemCotizacion` para rastrear el origen de los datos.

### Campos Agregados a `Cotizacion`:
- `archivoOrigen`: Nombre del archivo Excel de origen
- `hojaOrigen`: Nombre de la hoja dentro del archivo
- `filaOrigen`: Número de fila en el Excel (numeración humana, empieza en 1)
- `tipoDetector`: Tipo de detector que procesó el archivo (ONE_SPHERE, GENERICO, etc.)

### Campos Agregados a `ItemCotizacion`:
- `filaOrigen`: Número de fila del item en el Excel
- `comentarios`: Campo para información adicional del Excel

### Beneficios:
- **Trazabilidad**: Puedes rastrear cada dato hasta su origen exacto
- **Debug**: Facilita encontrar problemas en archivos específicos
- **Auditoría**: Mantiene registro de qué detector procesó qué archivo

### Ejemplo de uso:
```java
Cotizacion cot = resultado.getCotizacionesExitosas().get(0);
System.out.println("Archivo: " + cot.getArchivoOrigen());
System.out.println("Hoja: " + cot.getHojaOrigen());
System.out.println("Fila: " + cot.getFilaOrigen());
System.out.println("Procesado por: " + cot.getTipoDetector());
```

---

## Punto 3: Estrategia de Detección por Formato

Se implementó el **patrón Strategy** para manejar diferentes formatos de Excel de manera extensible.

### Arquitectura:

```
DetectorFormatoCotizacion (Interface)
    ↑
    ├── DetectorBase (Clase abstracta con utilidades)
    │       ↑
    │       ├── DetectorOneSphere (Detector específico para ONE Sphere)
    │       ├── DetectorGenerico (Fallback para formatos estándar)
    │       └── [Puedes agregar más detectores aquí]
    │
    └── ProcesadorLoteCotizaciones (Usa detectores con prioridad)
```

### Componentes:

#### 1. `DetectorFormatoCotizacion` (Interface)
Define el contrato para todos los detectores:
- `puedeDetectar(Sheet)`: Verifica si puede procesar una hoja
- `extraerCotizaciones(Sheet, archivo)`: Extrae cotizaciones
- `getNombreDetector()`: Retorna identificador único
- `getPrioridad()`: Define orden de evaluación (0-100)

#### 2. `DetectorBase` (Clase Abstracta)
Proporciona utilidades comunes:
- `obtenerValorCelda()`: Extrae valor de celda como String
- `obtenerValorNumerico()`: Extrae valor numérico
- `obtenerFecha()`: Extrae fecha
- `filaContiene()`: Busca texto en fila
- `encontrarColumna()`: Mapea columnas por nombre
- `esFilaVacia()`: Valida si fila tiene datos

#### 3. Detectores Implementados:

##### `DetectorOneSphere` (Prioridad: 80)
- **Detecta**: Archivos de RFQ de ONE Sphere
- **Busca**: Palabras clave "ONE SPHERE", "RFQ", estructura típica
- **Extrae**: Item No, Material Code, Description, Qty, UOM
- **Ventaja**: Formato muy específico, alta precisión

##### `DetectorGenerico` (Prioridad: 10)
- **Detecta**: Cualquier formato estándar de cotización
- **Busca**: Headers comunes (descripción, código, cantidad, precio)
- **Extrae**: Datos básicos de productos
- **Ventaja**: Fallback cuando otros detectores no funcionan

### Sistema de Prioridades:

Los detectores se evalúan en orden de prioridad **descendente**:

1. **Detectores específicos** (80-100): Formatos muy particulares
2. **Detectores especializados** (50-79): Formatos comunes de proveedores
3. **Detectores genéricos** (10-49): Formatos estándar
4. **Fallback** (1-9): Último recurso

### Cómo agregar un nuevo detector:

```java
public class DetectorMaersk extends DetectorBase {
    
    @Override
    public boolean puedeDetectar(Sheet sheet) {
        // Buscar indicadores de Maersk
        return filaContiene(sheet.getRow(0), "MAERSK", "MSK");
    }
    
    @Override
    public List<Cotizacion> extraerCotizaciones(Sheet sheet, String archivo) {
        // Implementar lógica de extracción
        // ...
    }
    
    @Override
    public String getNombreDetector() {
        return "MAERSK";
    }
    
    @Override
    public int getPrioridad() {
        return 75; // Alta prioridad
    }
}

// Registrar el detector
ProcesadorLoteCotizaciones procesador = new ProcesadorLoteCotizaciones();
procesador.agregarDetector(new DetectorMaersk());
```

---

## Punto 4: Manejo Robusto de Errores

Se implementó un sistema completo de manejo de errores con logging y reportes.

### Componentes:

#### 1. `ResultadoProcesamiento`
Clase contenedora que encapsula:
- **Cotizaciones exitosas**: Lista de datos extraídos
- **Errores**: Lista detallada de problemas encontrados
- **Estadísticas**: Métricas del procesamiento

#### 2. `ErrorProcesamiento` (Clase interna)
Representa un error con contexto:
- `archivo`: Archivo donde ocurrió
- `hoja`: Hoja específica (opcional)
- `fila`: Fila específica (opcional)
- `tipo`: Categoría del error (ERROR_ARCHIVO, ERROR_HOJA, ERROR_FILA)
- `mensaje`: Descripción del error
- `detallesTecnicos`: Información técnica adicional

#### 3. `EstadisticasProcesamiento` (Clase interna)
Métricas del procesamiento:
- Archivos encontrados
- Archivos procesados exitosamente
- Archivos con errores
- Hojas procesadas
- Cotizaciones extraídas
- Tiempo de procesamiento (ms)

### Estrategias de Manejo de Errores:

#### 1. **No detener el procesamiento**
Si un archivo falla, continúa con los demás:
```java
for (File archivo : archivos) {
    try {
        // Procesar archivo
    } catch (Exception e) {
        // Registrar error y continuar
        resultado.agregarError(error);
    }
}
```

#### 2. **Logging multinivel**
```java
logger.info("Procesando: " + archivo);      // INFO
logger.warning("Sin datos extraídos");      // WARNING
logger.severe("Error de memoria");          // SEVERE
```

#### 3. **Errores por nivel**
- **Archivo**: No se puede abrir, formato inválido
- **Hoja**: Estructura no reconocida
- **Fila**: Datos incompletos (se registran pero no detienen)

#### 4. **Reporte detallado**
```java
String reporte = procesador.generarReporte(resultado);
System.out.println(reporte);
```

Muestra:
- Estadísticas generales
- Lista de errores con contexto
- Cotizaciones por tipo de detector
- Tiempo de procesamiento

### Manejo de Casos Especiales:

#### Archivos muy grandes (OutOfMemoryError):
```java
catch (OutOfMemoryError e) {
    throw new IOException("Archivo demasiado grande", e);
}
```

#### Hojas sin estructura reconocible:
```java
if (filaHeader == -1) {
    return cotizaciones; // Lista vacía, no error
}
```

#### Filas con datos incompletos:
```java
if (item.getDescripcion() == null) {
    // Se ignora silenciosamente
    continue;
}
```

---

## Uso del Sistema

### Uso Básico:

```java
// 1. Crear procesador
ProcesadorLoteCotizaciones procesador = new ProcesadorLoteCotizaciones();

// 2. Procesar directorio
ResultadoProcesamiento resultado = procesador.procesarDirectorio("/ruta/cotizaciones");

// 3. Ver reporte
System.out.println(procesador.generarReporte(resultado));

// 4. Acceder a datos
for (Cotizacion cot : resultado.getCotizacionesExitosas()) {
    System.out.println(cot.getArchivoOrigen());
    System.out.println(cot.getTipoDetector());
}
```

### Ejecutar Demo:

```bash
cd /Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava

# Compilar (con Maven)
mvn compile

# Ejecutar demo
mvn exec:java -Dexec.mainClass="cl.vss.cotizador.demo.DemoProcesadorLote"
```

---

## Extensibilidad

### Agregar detector personalizado:
1. Crear clase que extienda `DetectorBase`
2. Implementar métodos requeridos
3. Definir prioridad apropiada
4. Registrar con `procesador.agregarDetector()`

### Agregar más metadatos:
1. Agregar campos a `Cotizacion` o `ItemCotizacion`
2. Agregar getters/setters
3. Poblar en los detectores

### Personalizar logging:
```java
Logger logger = Logger.getLogger(ProcesadorLoteCotizaciones.class.getName());
logger.setLevel(Level.FINE); // Más detallado
```

---

## Archivos Creados/Modificados

### Nuevos:
- `cl.vss.cotizador.detector.DetectorFormatoCotizacion`
- `cl.vss.cotizador.detector.DetectorBase`
- `cl.vss.cotizador.detector.DetectorOneSphere`
- `cl.vss.cotizador.detector.DetectorGenerico`
- `cl.vss.cotizador.model.ResultadoProcesamiento`
- `cl.vss.cotizador.service.ProcesadorLoteCotizaciones`
- `cl.vss.cotizador.demo.DemoProcesadorLote`

### Modificados:
- `cl.vss.cotizador.model.Cotizacion` (agregados metadatos)
- `cl.vss.cotizador.model.ItemCotizacion` (agregados metadatos)

---

## Próximos Pasos Sugeridos

1. **Más detectores**: Implementar detectores para otros formatos comunes
2. **Validación**: Agregar validación de datos extraídos
3. **Persistencia**: Guardar resultados en base de datos
4. **Export**: Exportar resultados a Excel consolidado
5. **UI**: Crear interfaz gráfica para el procesamiento
6. **Tests**: Agregar tests unitarios para detectores

---

## Soporte

Para preguntas o mejoras, revisar los archivos de demo y la documentación en código.
