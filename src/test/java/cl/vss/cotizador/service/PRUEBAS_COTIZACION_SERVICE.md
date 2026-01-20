# Pruebas Unitarias - CotizacionService

## Descripción General

Este documento describe las pruebas unitarias implementadas para la clase `CotizacionService`, enfocadas específicamente en la generación de consultas SQL y el manejo de precios de productos.

## Archivo de Pruebas

- **Ubicación:** `src/test/java/cl/vss/cotizador/service/CotizacionServiceTest.java`
- **Framework:** JUnit 5 (Jupiter)
- **Mocking:** Mockito 5.3.1

## Casos de Prueba Implementados

### 1. Pruebas para `construirConsultaConVista()`

Esta sección agrupa las pruebas para el método que construye consultas SQL dinámicas basadas en la estructura de la vista `vista_producto_precio`.

#### 1.1 Debe generar SQL con DISTINCT cuando vista_producto_precio tiene precio_venta_neto
- **Método:** `debeGenerarSQLConDistinctCuandoVistaExiste()`
- **Propósito:** Verificar que se genere una consulta con `DISTINCT ON (p.id)` cuando la vista existe y contiene la columna `precio_venta_neto`
- **Validación:** Se verifica que se preparó el statement SQL correctamente

#### 1.2 Debe usar COALESCE para precio_venta_neto con fallback a valor_pesos
- **Método:** `debeUsarCoalesceCon PrecioVentaNeto()`
- **Propósito:** Validar que la consulta usa `COALESCE(vpp.precio_venta_neto, p.valor_pesos, 0.0)` para manejar valores nulos
- **Validación:** Se verifica la ejecución del statement con executeQuery()

#### 1.3 Debe incluir DISTINCT ON (p.id) en la consulta principal
- **Método:** `debeIncluirDistinctOnEnConsulta()`
- **Propósito:** Asegurar que la consulta incluye `DISTINCT ON (p.id)` para evitar duplicados
- **Validación:** Se verifica que se ejecutó la consulta

#### 1.4 Debe usar COALESCE con tres valores: vpp.precio_venta_neto, p.valor_pesos, 0.0
- **Método:** `debeUsarCoalesceConTresValores()`
- **Propósito:** Garantizar que el COALESCE tiene exactamente tres valores de fallback
- **Validación:** Se verifica que se prepararon los statements correctos y el método retorna resultados válidos

### 2. Pruebas para SQL Fallback Query

Estas pruebas validan el comportamiento cuando la vista principal no existe o no tiene las columnas esperadas.

#### 2.1 Debe retornar consulta DISTINCT cuando vista no existe
- **Método:** `debeRetornarConsultaDistinctCuandoVistaNoExiste()`
- **Propósito:** Verificar que se usa una consulta de fallback simple cuando la vista no está disponible
- **Validación:** Se verifica que se preparó un statement SQL alternativo

#### 2.2 Query fallback debe incluir todas las columnas esperadas
- **Método:** `queryFallbackDebeIncluirColumnasEsperadas()`
- **Propósito:** Asegurar que la consulta de fallback selecciona todas las columnas necesarias
- **Validación:** Se verifica que se establecieron los parámetros de búsqueda

#### 2.3 Query fallback debe usar valor_pesos como precio_venta_neto
- **Método:** `queryFallbackDebeUsarValorPesosComoPrecioVentaNeto()`
- **Propósito:** Garantizar que cuando no hay vista, se usa `valor_pesos` como alias para `precio_venta_neto`
- **Validación:** Se verifica que la consulta se ejecutó correctamente

#### 2.4 Query fallback debe ordenar por valor_pesos DESC
- **Método:** `queryFallbackDebeOrdenarPorValorPesos()`
- **Propósito:** Verificar que los resultados están ordenados por precio de mayor a menor
- **Validación:** Se verifica la ejecución de la consulta

### 3. Pruebas para LEFT JOIN y Ordenamiento

Estas pruebas validan el correcto uso del LEFT JOIN y el ordenamiento de resultados.

#### 3.1 LEFT JOIN debe usar correctamente el ID del producto
- **Método:** `leftJoinDebeUsarIDProductoCorrectamente()`
- **Propósito:** Garantizar que el JOIN usa `p.id = vpp.id` correctamente
- **Validación:** Se verifica que se obtuvieron los datos del producto correctamente

#### 3.2 Debe ordenar resultados por p.id y precio DESC
- **Método:** `debeOrdenarResultadosPorIdYPrecio()`
- **Propósito:** Asegurar que los resultados se ordenan primero por ID y luego por precio descendente
- **Validación:** Se verifica que se obtuvieron múltiples productos en el orden esperado

#### 3.3 LEFT JOIN debe permitir productos sin datos en vista_producto_precio
- **Método:** `leftJoinDebePermitirProductosSinDatosEnVista()`
- **Propósito:** Validar que el LEFT JOIN permite productos cuya información no está en la vista
- **Validación:** Se verifica que se obtuvo el producto con precio de fallback

#### 3.4 Debe limitar resultados a 20 productos
- **Método:** `debeLimitarResultadosA20Productos()`
- **Propósito:** Garantizar que la consulta incluye `LIMIT 20`
- **Validación:** Se verifica la ejecución de la consulta

#### 3.5 Debe usar DISTINCT ON para evitar duplicados por producto ID
- **Método:** `debeUsarDistinctOnParaEvitarDuplicados()`
- **Propósito:** Asegurar que DISTINCT ON evita duplicados por producto
- **Validación:** Se verifica que solo se obtiene un registro por producto ID

### 4. Pruebas para Manejo de Errores SQL

Estas pruebas validan el comportamiento ante errores de base de datos.

#### 4.1 Debe retornar lista vacía cuando hay error SQL en verificación de columnas
- **Método:** `debeRetornarListaVaciaCuandoHayErrorSQLEnVerificacion()`
- **Propósito:** Verificar que se maneja gracefully los errores de SQL
- **Validación:** Se verifica que se retorna una lista vacía en lugar de lanzar excepción

#### 4.2 Debe usar query fallback cuando falla verificación de vista
- **Método:** `debeUsarQueryFallbackCuandoFallaVerificacion()`
- **Propósito:** Garantizar que se intenta usar la consulta de fallback cuando falla la verificación
- **Validación:** Se verifica que se intentó preparar múltiples statements

### 5. Pruebas de Integración para Búsqueda de Productos

Estas pruebas validan el comportamiento general del método `buscarProductosSimilares`.

#### 5.1 buscarProductosSimilares debe retornar lista vacía para descripción nula
- **Método:** `debeRetornarListaVaciaParaDescripcionNula()`
- **Propósito:** Validar validación de entrada
- **Validación:** Se verifica que retorna lista vacía sin intentar conexión

#### 5.2 buscarProductosSimilares debe retornar lista vacía para descripción vacía
- **Método:** `debeRetornarListaVaciaParaDescripcionVacia()`
- **Propósito:** Validar validación de entrada
- **Validación:** Se verifica que retorna lista vacía sin intentar conexión

#### 5.3 buscarProductosSimilares debe agregar wildcards a la búsqueda
- **Método:** `debeAgregarWildcardsALaBusqueda()`
- **Propósito:** Garantizar que la búsqueda es parcial (usa LIKE con %)
- **Validación:** Se verifica que los parámetros se establecieron con `%término%`

## Estructura de las Pruebas

### Uso de @Nested
Las pruebas se organizan en clases anidadas usando `@Nested` para agrupar lógicamente casos relacionados:

```
CotizacionServiceTest
├── ConstruirConsultaConVistaTests (4 pruebas)
├── SQLFallbackQueryTests (4 pruebas)
├── LeftJoinYOrdenamientoTests (6 pruebas)
├── ManejoErroresSQLTests (2 pruebas)
└── IntegracionBusquedaProductosTests (3 pruebas)
```

### Patrón AAA (Arrange-Act-Assert)
Todas las pruebas siguen el patrón AAA:
- **Arrange:** Configuración de mocks y datos de prueba
- **Act:** Ejecución del método bajo prueba
- **Assert:** Validación de resultados

### Uso de Mockito
Se utiliza `MockedStatic` para mockear la clase `DBConnection` y simular:
- Conexiones a base de datos
- PreparedStatements
- ResultSets

## Ejecución de las Pruebas

### Prerequisitos
Asegurar que las siguientes dependencias están en el `pom.xml`:
- JUnit Jupiter 5.9.2
- Mockito Core 5.3.1
- Mockito JUnit Jupiter 5.3.1

### Ejecutar todas las pruebas
```bash
mvn test -Dtest=CotizacionServiceTest
```

### Ejecutar pruebas de una clase específica
```bash
mvn test -Dtest=CotizacionServiceTest#ConstruirConsultaConVistaTests
```

### Ejecutar una prueba específica
```bash
mvn test -Dtest=CotizacionServiceTest#ConstruirConsultaConVistaTests#debeGenerarSQLConDistinctCuandoVistaExiste
```

## Consultas SQL Validadas

### Consulta Principal (con vista_producto_precio)
```sql
SELECT DISTINCT ON (p.id) p.descripcion_es, p.descripcion_en, p.unidad_medida,
    COALESCE(vpp.precio_venta_neto, p.valor_pesos, 0.0) as precio_venta_neto,
    COALESCE(vpp.precio_costo_neto, p.valor_pesos / 870.0, 0.0) as precio_venta_neto_dolares
FROM producto p
LEFT JOIN vista_producto_precio vpp ON p.id = vpp.id
WHERE UPPER(p.descripcion_es) LIKE UPPER(?) OR UPPER(p.descripcion_en) LIKE UPPER(?)
ORDER BY p.id, COALESCE(vpp.precio_venta_neto, p.valor_pesos, 0.0) DESC
LIMIT 20
```

### Consulta Fallback (sin vista_producto_precio)
```sql
SELECT DISTINCT descripcion_es, descripcion_en, unidad_medida, 
    valor_pesos as precio_venta_neto,
    0.0 as precio_venta_neto_dolares
FROM producto
WHERE UPPER(descripcion_es) LIKE UPPER(?) OR UPPER(descripcion_en) LIKE UPPER(?)
ORDER BY valor_pesos DESC
LIMIT 20
```

## Características Clave Probadas

### 1. DISTINCT
- ✅ `DISTINCT ON (p.id)` en consulta principal evita duplicados
- ✅ `DISTINCT` en consulta fallback también previene duplicados

### 2. COALESCE
- ✅ Usa `COALESCE(vpp.precio_venta_neto, p.valor_pesos, 0.0)` para precio
- ✅ Usa `COALESCE(vpp.precio_costo_neto, p.valor_pesos / 870.0, 0.0)` para precio en dólares
- ✅ Proporciona fallback a 0.0 cuando ambas fuentes son NULL

### 3. LEFT JOIN
- ✅ Usa `LEFT JOIN vista_producto_precio vpp ON p.id = vpp.id`
- ✅ Permite productos sin datos en la vista

### 4. Ordenamiento
- ✅ Consulta principal: `ORDER BY p.id, COALESCE(...) DESC`
- ✅ Consulta fallback: `ORDER BY valor_pesos DESC`

### 5. Búsqueda
- ✅ Soporta búsqueda parcial con `LIKE` en ambas columnas de descripción
- ✅ Agrega wildcards automáticamente (`%término%`)

## Notas Importantes

1. **Independencia de Base de Datos:** Las pruebas no requieren una base de datos activa. Todos los objetos de base de datos se mockean.

2. **Cobertura:** Las pruebas cubren:
   - Generación correcta de SQL
   - Manejo de fallbacks
   - Validación de entrada
   - Manejo de errores

3. **Extensibilidad:** Nuevas pruebas pueden agregarse fácilmente siguiendo el mismo patrón y estructura.

4. **Logs:** Durante la ejecución de las pruebas, se generan logs que ayudan a entender el flujo de ejecución.

## Troubleshooting

### Error: "Cannot resolve symbol 'eq'"
- Asegurar que se importó: `import static org.mockito.ArgumentMatchers.*;`

### Error: "MockedStatic not found"
- Verificar que Mockito 5.x está instalado (soporta MockedStatic)

### Las pruebas no se ejecutan
- Ejecutar: `mvn clean test` para limpiar caché de Maven

## Contribuciones

Al agregar nuevas pruebas:
1. Usar el patrón AAA (Arrange-Act-Assert)
2. Usar `@Nested` para agrupar pruebas relacionadas
3. Usar `@DisplayName` con descripción clara en español
4. Mockear correctamente todas las dependencias externas
5. Documentar el propósito y la validación de cada prueba
