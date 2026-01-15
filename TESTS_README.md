# Tests Unitarios - Sistema de Cotizaciones VSS

Este documento describe los tests unitarios creados para el sistema de cotizaciones y cómo ejecutarlos.

## Tests Creados

### 1. RowDataTest (`src/test/java/cl/vss/cotizador/model/RowDataTest.java`)

Tests para la clase `RowData` que validan:
- ✅ Establecer y obtener valores correctamente
- ✅ Actualizar valores existentes
- ✅ Devolver cadena vacía para claves inexistentes
- ✅ Obtener StringProperty para binding con JavaFX
- ✅ Crear StringProperty vacía para claves nuevas
- ✅ Sincronización entre Property y get/set
- ✅ Verificar existencia de claves
- ✅ Devolver todas las claves almacenadas
- ✅ Manejar múltiples campos como en formato real de broker
- ✅ Manejar valores nulos y vacíos
- ✅ Generar toString con información del mapa

**Tipo:** Test unitario puro (no requiere BD ni JavaFX runtime completo)

### 2. FormatoDAOTest (`src/test/java/cl/vss/cotizador/service/FormatoDAOTest.java`)

Tests para la clase `FormatoDAO` que validan:
- ✅ `obtenerFormatoPorBrokerId()` retorna BrokerFormato válido
- ✅ Carga correcta de columnas del formato
- ✅ Columnas ordenadas por índice
- ✅ Todos los atributos de columnas cargados correctamente
- ✅ Estilos de columnas cargados correctamente (colores, negrita, etc.)
- ✅ Retorna null para broker inexistente
- ✅ `listarFormatosActivos()` retorna lista de formatos
- ✅ `obtenerColumnasPorFormatoId()` retorna columnas ordenadas

**Tipo:** Test de integración con base de datos
**Requisitos:** PostgreSQL corriendo con BD `sistema_cotizacion_2025`

### 3. MainIntegracionTest (`src/test/java/cl/vss/cotizador/MainIntegracionTest.java`)

Tests de integración para métodos de `Main.java`:
- ✅ `cargarFormatoBroker()` carga formato y establece `formatoActual`
- ✅ `configurarTablaDinamica()` crea columnas según formato y aplica estilos
- ✅ `leerExcelConFormato()` lee Excel y pobla tablaDinamica con RowData

**Tipo:** Test de integración completo
**Requisitos:** 
- PostgreSQL corriendo con BD `sistema_cotizacion_2025`
- JavaFX Runtime
- Ambiente gráfico (o headless con Monocle)

## Requisitos Previos

### Base de Datos
Los tests `FormatoDAOTest` y `MainIntegracionTest` requieren:
- PostgreSQL corriendo en localhost:5432
- Base de datos: `sistema_cotizacion_2025`
- Usuario: `postgres` (con permisos de escritura)
- Las tablas: `brokers`, `broker_formatos`, `formato_columnas`

### JavaFX
El test `MainIntegracionTest` requiere JavaFX Runtime. Maven ya incluye las dependencias necesarias.

## Ejecutar Tests

### Ejecutar todos los tests
```bash
mvn test
```

### Ejecutar un test específico
```bash
# Test de RowData (no requiere BD)
mvn test -Dtest=RowDataTest

# Test de FormatoDAO (requiere BD)
mvn test -Dtest=FormatoDAOTest

# Test de integración Main (requiere BD y JavaFX)
mvn test -Dtest=MainIntegracionTest
```

### Ejecutar solo tests que no requieren BD
```bash
mvn test -Dtest=RowDataTest
```

### Ejecutar con logs detallados
```bash
mvn test -X
```

## Configuración para Ambiente Headless (CI/CD)

Si necesitas ejecutar los tests en un servidor sin interfaz gráfica, configura Monocle:

```bash
# Agregar al pom.xml o ejecutar con property
mvn test -Dtestfx.robot=glass -Dtestfx.headless=true -Dprism.order=sw
```

## Resultados Esperados

### RowDataTest
- **11 tests** deben pasar
- Tiempo estimado: < 1 segundo
- No requiere recursos externos

### FormatoDAOTest
- **10 tests** deben pasar
- Tiempo estimado: 2-5 segundos
- Crea y limpia datos de prueba automáticamente

### MainIntegracionTest
- **3 tests** deben pasar
- Tiempo estimado: 5-10 segundos
- Crea archivos Excel temporales (se limpian automáticamente)
- Usa reflection para acceder a métodos privados

## Solución de Problemas

### Error: "No se puede conectar a la base de datos"
```
Solución: Verifica que PostgreSQL esté corriendo:
psql -U postgres -d sistema_cotizacion_2025
```

### Error: "JavaFX Runtime not found"
```
Solución: Asegúrate de tener Java 17+ y que Maven descargó las dependencias JavaFX:
mvn clean install
```

### Error: "Test timeout"
```
Solución: Los tests de JavaFX pueden tardar más en CI. Aumenta el timeout si es necesario.
```

### Error: "Cannot delete test files"
```
Solución: En Windows, asegúrate de cerrar Excel antes de ejecutar tests que crean archivos .xlsx
```

## Estructura de Tests

```
src/test/java/cl/vss/cotizador/
├── model/
│   └── RowDataTest.java           (Tests unitarios puros)
├── service/
│   └── FormatoDAOTest.java        (Tests con BD)
└── MainIntegracionTest.java       (Tests de integración completos)
```

## Cobertura de Código

Los tests cubren:
- ✅ Modelo `RowData`: 100% de métodos públicos
- ✅ DAO `FormatoDAO`: métodos principales de consulta
- ✅ Main: métodos de carga y configuración de formatos de broker

Para generar reporte de cobertura:
```bash
mvn jacoco:prepare-agent test jacoco:report
```

El reporte se genera en: `target/site/jacoco/index.html`

## Mejores Prácticas

1. **Ejecuta los tests antes de hacer commit**
   ```bash
   mvn clean test
   ```

2. **Los tests de BD limpian sus datos** - No dejan basura en la BD

3. **Los tests son independientes** - Pueden ejecutarse en cualquier orden

4. **Usa @DisplayName** - Los nombres descriptivos facilitan identificar fallos

5. **Pattern Given-When-Then** - Todos los tests siguen este patrón para claridad

## Próximos Pasos

Considera agregar tests para:
- [ ] `BrokerDAO` - CRUD de brokers
- [ ] `CotizacionService` - Lógica de negocio
- [ ] Validaciones de formato Excel
- [ ] Manejo de errores y excepciones
- [ ] Tests de rendimiento para archivos Excel grandes

## Soporte

Para problemas con los tests, verifica:
1. Logs en `target/surefire-reports/`
2. Estado de la base de datos
3. Versión de Java (debe ser 17+)
4. Variables de entorno JavaFX

---

**Nota:** Estos tests fueron creados para validar la funcionalidad core del sistema de lectura y visualización de cotizaciones con formatos de broker.
