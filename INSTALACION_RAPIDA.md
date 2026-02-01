# Guía Rápida de Instalación - Cotizador VSS

## 📦 Instalación en Producción

### Opción 1: Configuración Externa (Recomendada)

1. **Copiar archivos:**
   ```
   cotizador-vss-1.0-SNAPSHOT.jar
   cotizador-config.properties.example
   ```

2. **Crear archivo de configuración:**
   ```bash
   cp cotizador-config.properties.example cotizador-config.properties
   ```

3. **Editar `cotizador-config.properties`** con tus valores:
   ```properties
   db.url=jdbc:postgresql://192.168.2.103:5432/sistema_cotizacion_2025
   db.username=postgres
   db.password=Vss2026
   app.snapshot=false
   ```

4. **Ejecutar:**
   ```bash
   java -jar cotizador-vss-1.0-SNAPSHOT.jar
   ```

### Opción 2: Sin Configuración Externa

El JAR usará la configuración interna (valores por defecto). Solo ejecuta:
```bash
java -jar cotizador-vss-1.0-SNAPSHOT.jar
```

## 🔄 Cambiar Configuración

**Ventaja:** Sin necesidad de recompilar el JAR

1. Edita `cotizador-config.properties`
2. Reinicia la aplicación
3. ¡Listo!

## 📁 Estructura de Archivos

```
/produccion/
├── cotizador-vss-1.0-SNAPSHOT.jar
└── cotizador-config.properties    ← Configuración externa (opcional)
```

## 🔒 Seguridad

- Archivo `cotizador-config.properties` NO debe estar en el repositorio
- Permisos recomendados: `chmod 600 cotizador-config.properties`

## ℹ️ Información de Versión

Desde la aplicación: **Menú Ayuda → Acerca de...**
