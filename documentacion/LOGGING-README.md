# 📊 Sistema de Logging - Cotizador VSS

## 📁 Archivos de Log Generados

Todos los logs se guardan en el directorio `logs/` con rotación automática diaria y por tamaño (10MB).

### Archivos disponibles:

#### 1. **`logs/application.log`**
- **Contenido**: Log general de toda la aplicación
- **Nivel**: INFO, WARN, ERROR
- **Incluye**: Todas las operaciones de la aplicación principal

#### 2. **`logs/database.log`**
- **Contenido**: Operaciones de base de datos
- **Nivel**: INFO, ERROR
- **Incluye**: 
  - Consultas ejecutadas
  - Inserciones, actualizaciones, eliminaciones
  - Errores SQL
  - Conexiones a BD

#### 3. **`logs/productos.log`**
- **Contenido**: Mantenedor de productos específicamente
- **Nivel**: INFO, WARN, ERROR
- **Incluye**:
  - Agregar productos
  - Actualizar productos
  - Eliminar productos
  - Listar productos por familia
  - Errores en operaciones de productos

#### 4. **`logs/auditoria.log`**
- **Contenido**: Auditoría de cambios en parámetros comerciales
- **Nivel**: INFO, ERROR
- **Incluye**:
  - Cambios en tipo de cambio
  - Cambios en porcentaje de utilidad
  - Cambios en fecha de vigencia
  - Usuario que realizó los cambios

#### 5. **`logs/errors.log`**
- **Contenido**: Solo errores críticos de toda la aplicación
- **Nivel**: ERROR, FATAL
- **Incluye**: Stack traces completos de excepciones

## 🔍 Niveles de Log

| Nivel | Descripción | Uso |
|-------|-------------|-----|
| **DEBUG** | Información detallada para debugging | Desarrollo |
| **INFO** | Eventos informativos normales | Operaciones exitosas |
| **WARN** | Advertencias, situaciones inusuales | Validaciones fallidas |
| **ERROR** | Errores que necesitan atención | Excepciones, fallos |
| **FATAL** | Errores críticos que detienen la app | Errores graves |

## 📝 Ejemplos de Logs

### Operaciones de Productos

```log
2026-01-08 14:30:15.123 [JavaFX Application Thread] INFO  cl.vss.cotizador.service.ProductoService - Agregando producto: Tomate Cherry | Familia ID: 5 | Precio: $2500.0
2026-01-08 14:30:15.234 [JavaFX Application Thread] INFO  cl.vss.cotizador.service.ProductoService - Producto agregado exitosamente: Tomate Cherry
2026-01-08 14:30:15.345 [JavaFX Application Thread] INFO  cl.vss.cotizador.service.ProductoService - Se recuperaron 245 productos
```

### Operaciones de Auditoría

```log
2026-01-08 10:15:45.123 [JavaFX Application Thread] INFO  cl.vss.cotizador.service.AuditoriaDAO - Insertando cambio de auditoría: parametro=tipo_cambio_usado, valorAnterior=900.0, valorNuevo=950.0, usuarioId=1
2026-01-08 10:15:45.234 [JavaFX Application Thread] INFO  cl.vss.cotizador.service.AuditoriaDAO - Cambio de auditoría insertado exitosamente para parámetro: tipo_cambio_usado
```

### Operaciones de Usuario

```log
2026-01-08 10:15:23.456 [JavaFX Application Thread] INFO  cl.vss.cotizador.service.UsuarioDAO - Intento de login para correo: admin@vss.cl
2026-01-08 10:15:23.789 [JavaFX Application Thread] INFO  cl.vss.cotizador.service.UsuarioDAO - Login exitoso para usuario: Admin (admin@vss.cl)
```

### Errores y Excepciones

```log
2026-01-08 15:45:12.123 [JavaFX Application Thread] ERROR cl.vss.cotizador.service.ProductoService - Error al agregar producto: Aceite de Oliva
java.sql.SQLException: Duplicate entry '123' for key 'PRIMARY'
    at com.mysql.cj.jdbc.exceptions.SQLError.createSQLException(SQLError.java:129)
    at cl.vss.cotizador.service.ProductoService.agregarProducto(ProductoService.java:35)
    ...
```

## ⚙️ Configuración

El archivo de configuración está en: `src/main/resources/log4j2.xml`

### Cambiar nivel de log para debugging:

Para ver más detalles durante el desarrollo, cambia el nivel de INFO a DEBUG:

```xml
<Logger name="cl.vss.cotizador.service" level="DEBUG" additivity="false">
```

### Desactivar logging en consola:

Comenta la línea `<AppenderRef ref="Console"/>` en los loggers que desees.

## 🔄 Rotación de Archivos

- **Rotación diaria**: Se crea un nuevo archivo cada día
- **Rotación por tamaño**: Se crea un nuevo archivo al alcanzar 10MB
- **Archivos históricos**: Se mantienen los últimos 30 archivos
- **Compresión**: Los archivos antiguos se comprimen en `.gz`

Ejemplo de archivos rotados:
```
logs/productos-2026-01-07-1.log.gz
logs/productos-2026-01-07-2.log.gz
logs/productos-2026-01-08.log
```

## 🛠️ Clases con Logging

### Servicios de Base de Datos:
- ✅ `AuditoriaDAO` - Auditoría de cambios
- ✅ `ParametrosDAO` - Parámetros comerciales
- ✅ `UsuarioDAO` - Gestión de usuarios
- ✅ `ProductoService` - Mantenedor de productos

### Servicios de Cotización:
- ✅ `CotizacionService` - Procesamiento de cotizaciones
- ✅ `LecturaCotizacionService` - Lectura de Excel
- ✅ `ProcesadorLoteCotizaciones` - Procesamiento por lotes

### Aplicación Principal:
- ✅ `Main` - Interfaz principal y excepciones generales

## 📌 Mejores Prácticas

1. **Revisa los logs regularmente** para detectar problemas tempranos
2. **El archivo `errors.log`** debe revisarse ante cualquier fallo
3. **Los logs de auditoría** sirven para trazabilidad de cambios
4. **En producción**, considera aumentar el tiempo de retención de archivos
5. **Monitorea el tamaño** del directorio `logs/` periódicamente

## 🔧 Troubleshooting

### No se crean los archivos de log:
- Verifica que el directorio `logs/` exista o se pueda crear
- Verifica permisos de escritura en el directorio del proyecto

### Logs muy grandes:
- Reduce el nivel de log de DEBUG a INFO
- Ajusta el tamaño de rotación de 10MB a un valor menor
- Reduce el número de archivos a mantener de 30 a menos

### No aparecen los logs:
- Verifica que el nivel de log sea apropiado (INFO o menor)
- Revisa el archivo `log4j2.xml` para errores de sintaxis
- Verifica que log4j2 esté en el classpath

## 📞 Soporte

Para más información sobre Log4j2, visita: https://logging.apache.org/log4j/2.x/
