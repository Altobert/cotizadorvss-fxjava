# Guía de Ejecución - Cotizador VSS con Iconos

## 📋 Resumen

Se han creado múltiples formas de ejecutar la aplicación Cotizador VSS, cada una optimizada para diferentes escenarios de uso.

## 🎯 Aplicación Nativa (Recomendado)

### ✅ **Mejor opción para iconos en macOS**

```bash
# Crear la aplicación nativa (solo una vez)
./create-native-app.sh

# Ejecutar la aplicación nativa
./run-native.sh
# o directamente:
open "dist/Cotizador VSS.app"
```

**Características:**
- ✅ Icono personalizado en dock y taskbar
- ✅ Aplicación nativa de macOS (.app)
- ✅ No requiere configuración adicional
- ✅ Funciona como cualquier app de macOS
- ✅ Se puede agregar al dock arrastrando desde Finder

## 🔧 Métodos Alternativos

### 1. Maven JavaFX Plugin
```bash
mvn javafx:run
```
- ⚠️ Iconos limitados (solo en ventana de la app)

### 2. Script con Maven
```bash
./run-with-icon.sh
```
- ⚠️ Iconos limitados, configuración básica

### 3. JAR Ejecutable (No recomendado para iconos)
```bash
# Compilar JAR
mvn clean package -DskipTests

# Ejecutar JAR (requiere JavaFX en classpath)
./run-simple.sh
```
- ❌ Problemas con JavaFX runtime
- ❌ Sin iconos nativos

## 📁 Archivos Generados

### Iconos
- `src/main/resources/images/cotizador.icns` - Icono nativo para macOS
- `src/main/resources/images/icon*.png` - Iconos en varios tamaños

### Aplicaciones
- `dist/Cotizador VSS.app` - Aplicación nativa de macOS
- `target/cotizador-vss.jar` - JAR ejecutable con dependencias

### Scripts
- `create-native-app.sh` - Crear aplicación nativa
- `run-native.sh` - Ejecutar aplicación nativa  
- `run-with-icon.sh` - Ejecutar con Maven y configuración de iconos
- `run-simple.sh` - Ejecutar JAR simplificado

## 🔍 Verificación de Iconos

Para verificar que el icono se muestra correctamente:

1. **Dock de macOS:** El icono debería aparecer mientras la app está ejecutándose
2. **Alt+Tab:** El icono debería aparecer en el selector de aplicaciones
3. **Finder:** El archivo .app debería mostrar el icono personalizado

## ⚡ Uso Recomendado

**Para desarrollo diario:**
```bash
mvn javafx:run
```

**Para distribución y uso final:**
```bash
./create-native-app.sh  # Solo la primera vez
./run-native.sh         # Para ejecutar
```

## 🐛 Solución de Problemas

### Si la aplicación nativa no funciona:
```bash
# Verificar Java 17+
java -version

# Limpiar y recompilar
mvn clean package -DskipTests
./create-native-app.sh
```

### Si los iconos no aparecen:
- Usar la aplicación nativa (`./run-native.sh`)
- Verificar que el archivo `cotizador.icns` existe
- Reiniciar el dock si es necesario: `killall Dock`