#!/bin/bash

# Script para ejecutar proyecto JavaFX
# Autor: Generado automáticamente
# Fecha: $(date)

echo "🚀 Ejecutor de Proyecto JavaFX"
echo "=============================="

# Configuración del proyecto
PROJECT_NAME="Cotizador JavaFX"
MAIN_CLASS="com.example.Main"
JAVA_VERSION="17"

# Función para mostrar ayuda
show_help() {
    echo "Uso: $0 [opción]"
    echo ""
    echo "Opciones:"
    echo "  (sin argumentos) - Ejecutar aplicación"
    echo "  compile          - Compilar y ejecutar"
    echo "  clean-run        - Limpiar, compilar y ejecutar"
    echo "  debug            - Ejecutar en modo debug"
    echo "  help             - Mostrar esta ayuda"
    echo ""
}

# Función para verificar dependencias
check_dependencies() {
    echo "🔍 Verificando dependencias..."
    
    # Verificar Java
    if ! command -v java &> /dev/null; then
        echo "❌ Error: Java no está instalado o no está en el PATH"
        echo "Instala Java con: brew install openjdk@17"
        exit 1
    fi
    
    # Verificar Maven
    if ! command -v mvn &> /dev/null; then
        echo "❌ Error: Maven no está instalado o no está en el PATH"
        echo "Instala Maven con: brew install maven"
        exit 1
    fi
    
    # Verificar versión de Java
    JAVA_VER=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VER" -lt "$JAVA_VERSION" ]; then
        echo "⚠️  Advertencia: Java $JAVA_VER detectado, se recomienda Java $JAVA_VERSION"
    fi
    
    echo "✅ Dependencias verificadas"
    echo ""
}

# Función para compilar proyecto
compile_project() {
    echo "⚙️  Compilando proyecto..."
    mvn compile -q
    if [ $? -eq 0 ]; then
        echo "✅ Compilación exitosa"
    else
        echo "❌ Error en la compilación"
        exit 1
    fi
    echo ""
}

# Función para limpiar proyecto
clean_project() {
    echo "🧹 Limpiando proyecto..."
    mvn clean -q
    if [ $? -eq 0 ]; then
        echo "✅ Proyecto limpiado"
    else
        echo "❌ Error al limpiar"
        exit 1
    fi
    echo ""
}

# Función para ejecutar aplicación
run_app() {
    echo "🏃 Ejecutando $PROJECT_NAME..."
    echo "📱 Clase principal: $MAIN_CLASS"
    echo ""
    
    # Ejecutar con JavaFX
    mvn javafx:run
}

# Función para ejecutar en modo debug
run_debug() {
    echo "🐛 Ejecutando en modo debug..."
    echo "📱 Clase principal: $MAIN_CLASS"
    echo ""
    
    # Ejecutar con opciones de debug
    mvn javafx:run -Djavafx.debug=true
}

# Función principal
main() {
    case "${1:-run}" in
        "compile")
            check_dependencies
            compile_project
            run_app
            ;;
        "clean-run")
            check_dependencies
            clean_project
            compile_project
            run_app
            ;;
        "debug")
            check_dependencies
            compile_project
            run_debug
            ;;
        "help"|"-h"|"--help")
            show_help
            exit 0
            ;;
        "run"|"")
            check_dependencies
            run_app
            ;;
        *)
            echo "❌ Opción desconocida: $1"
            show_help
            exit 1
            ;;
    esac
}

# Mostrar información del sistema
echo "Java version: $(java -version 2>&1 | head -n 1)"
echo "Maven version: $(mvn -version | head -n 1)"
echo ""

# Ejecutar función principal
main "$@"
