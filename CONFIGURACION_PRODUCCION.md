# Configuración para Producción

Este documento describe cómo configurar la aplicación para el entorno de producción usando un **archivo de configuración externo**.

## Requisitos

- Java 17 o superior
- PostgreSQL instalado y ejecutándose
- Base de datos `sistema_cotizacion_2025` creada

## Pasos de Configuración

### 1. Crear Archivo de Configuración Externo

La aplicación soporta configuración mediante un archivo externo, lo que permite cambiar la configuración **sin necesidad de recompilar el JAR**.

**Pasos:**

1. Copia el archivo de ejemplo:
   ```bash
   cp cotizador-config.properties.example cotizador-config.properties
   ```

2. Edita `cotizador-config.properties` con los valores de producción:

```properties
# Configuración de Base de Datos
db.url=jdbc:postgresql://192.168.2.103:5432/sistema_cotizacion_2025
db.host=192.168.2.103
db.port=5432
db.database=sistema_cotizacion_2025
db.username=postgres
db.password=Vss2026

# Configuración de la Aplicación
app.version=1.0
app.snapshot=false
app.developers=CSM,ASM
app.release.date=2026-02-01
```

3. Coloca el archivo `cotizador-config.properties` en el **mismo directorio** que el JAR ejecutable

### 2. Ejecutar en Producción

Una vez configurado el archivo externo:

```bash
java -jar cotizador-vss-1.0-SNAPSHOT.jar
```

La aplicación **automáticamente**:
1. ✅ Buscará el archivo `cotizador-config.properties` en el directorio actual
2. ✅ Si existe, usará esa configuración
3. ✅ Si NO existe, usará la configuración interna del JAR

### 3. Estructura de Archivos en Producción

```
/ruta/a/produccion/
├── cotizador-vss-1.0-SNAPSHOT.jar
└── cotizador-config.properties    ← Archivo de configuración externo
```

## Ventajas de Configuración Externa

✅ **Sin recompilación:** Cambia la configuración sin regenerar el JAR  
✅ **Seguridad:** No expones credenciales en el repositorio  
✅ **Flexibilidad:** Diferentes configuraciones para diferentes entornos  
✅ **Simplicidad:** Solo edita el archivo y reinicia la aplicación  

## Notas de Seguridad

⚠️ **IMPORTANTE:** 
- NO subas `cotizador-config.properties` al repositorio
- El archivo `.example` SÍ debe estar en el repositorio como plantilla
- Configura permisos restrictivos al archivo en producción: `chmod 600 cotizador-config.properties`

## Versión

- **Versión actual:** 1.0-SNAPSHOT (Desarrollo)
- Para versión de producción, cambiar `IS_SNAPSHOT = false` en `Version.java`
