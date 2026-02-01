# Configuración para Producción

Este documento describe cómo configurar la aplicación para el entorno de producción.

## Requisitos

- Java 17 o superior
- PostgreSQL instalado y ejecutándose
- Base de datos `sistema_cotizacion_2025` creada

## Pasos de Configuración

### 1. Configurar la Base de Datos

Antes de ejecutar el JAR en producción, debes configurar los parámetros de conexión a la base de datos:

**Edita el archivo:** `src/main/resources/db.properties`

```properties
# Configuración de PostgreSQL
db.url=jdbc:postgresql://TU_IP_SERVIDOR:5432/sistema_cotizacion_2025
db.host=TU_IP_SERVIDOR
db.port=5432
db.database=sistema_cotizacion_2025
db.username=TU_USUARIO
db.password=TU_PASSWORD
```

**Ejemplo para producción:**
```properties
db.url=jdbc:postgresql://192.168.2.103:5432/sistema_cotizacion_2025
db.host=192.168.2.103
db.port=5432
db.database=sistema_cotizacion_2025
db.username=postgres
db.password=Vss2026
```

### 2. Recompilar el JAR

Después de modificar la configuración, regenera el JAR ejecutable:

```bash
mvn clean package -DskipTests
```

### 3. Ejecutar en Producción

El JAR generado estará en:
```
target/cotizador-vss-1.0-SNAPSHOT.jar
```

Ejecutar:
```bash
java -jar cotizador-vss-1.0-SNAPSHOT.jar
```

## Notas de Seguridad

⚠️ **IMPORTANTE:** 
- NO subas al repositorio archivos de configuración con credenciales reales
- Los archivos `db-prod.properties` están en `.gitignore` por seguridad
- Configura las credenciales solo en el servidor de producción

## Versión

- **Versión actual:** 1.0-SNAPSHOT (Desarrollo)
- Para versión de producción, cambiar `IS_SNAPSHOT = false` en `Version.java`
