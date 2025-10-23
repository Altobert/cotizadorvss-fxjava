#!/bin/bash

# Script simple de compilación rápida para JavaFX
# Uso: ./quick-compile.sh [clean|run|test]

echo "🚀 Compilador rápido JavaFX"
echo "=========================="

# Función para mostrar ayuda
show_help() {
    echo "Uso: $0 [opción]"
    echo ""
    echo "Opciones:"
    echo "  (sin argumentos) - Compilar proyecto"
    echo "  clean           - Limpiar y compilar"
    echo "  run             - Compilar y ejecutar"
    echo "  test            - Ejecutar tests"
    echo "  help            - Mostrar esta ayuda"
    echo ""
}

# Verificar argumentos
case "${1:-compile}" in
    "clean")
        echo "🧹 Limpiando y compilando..."
        mvn clean compile
        ;;
    "run")
        echo "🏃 Compilando y ejecutando..."
        mvn clean compile javafx:run
        ;;
    "test")
        echo "🧪 Ejecutando tests..."
        mvn test
        ;;
    "help"|"-h"|"--help")
        show_help
        exit 0
        ;;
    "compile"|"")
        echo "⚙️  Compilando proyecto..."
        mvn compile
        ;;
    *)
        echo "❌ Opción desconocida: $1"
        show_help
        exit 1
        ;;
esac

# Mostrar resultado
if [ $? -eq 0 ]; then
    echo "✅ Operación completada exitosamente"
else
    echo "❌ Error en la operación"
    exit 1
fi
