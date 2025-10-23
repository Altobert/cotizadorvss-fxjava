#!/bin/bash

# Script de compilación para proyecto JavaFX
# Autor: Generado automáticamente
# Fecha: $(date)

echo "========================================"
echo "   Proyecto JavaFX con Maven"
echo "========================================"
echo

# Función para mostrar el menú
show_menu() {
    echo "Selecciona una opción:"
    echo "1. Compilar proyecto"
    echo "2. Ejecutar tests"
    echo "3. Ejecutar aplicación"
    echo "4. Limpiar proyecto"
    echo "5. Instalar dependencias"
    echo "6. Compilar y ejecutar"
    echo "7. Salir"
    echo
}

# Función para compilar
compile_project() {
    echo
    echo "Compilando proyecto..."
    mvn compile
    if [ $? -eq 0 ]; then
        echo "✅ Compilación exitosa"
    else
        echo "❌ Error en la compilación"
    fi
    echo
    read -p "Presiona Enter para continuar..."
}

# Función para ejecutar tests
run_tests() {
    echo
    echo "Ejecutando tests..."
    mvn test
    if [ $? -eq 0 ]; then
        echo "✅ Tests ejecutados correctamente"
    else
        echo "❌ Algunos tests fallaron"
    fi
    echo
    read -p "Presiona Enter para continuar..."
}

# Función para ejecutar aplicación
run_app() {
    echo
    echo "Ejecutando aplicación JavaFX..."
    mvn javafx:run
    echo
    read -p "Presiona Enter para continuar..."
}

# Función para limpiar proyecto
clean_project() {
    echo
    echo "Limpiando proyecto..."
    mvn clean
    if [ $? -eq 0 ]; then
        echo "✅ Proyecto limpiado"
    else
        echo "❌ Error al limpiar"
    fi
    echo
    read -p "Presiona Enter para continuar..."
}

# Función para instalar dependencias
install_dependencies() {
    echo
    echo "Descargando e instalando dependencias..."
    mvn clean compile
    if [ $? -eq 0 ]; then
        echo "✅ Dependencias instaladas correctamente"
    else
        echo "❌ Error al instalar dependencias"
    fi
    echo
    read -p "Presiona Enter para continuar..."
}

# Función para compilar y ejecutar
compile_and_run() {
    echo
    echo "Compilando y ejecutando aplicación..."
    mvn clean compile javafx:run
    echo
    read -p "Presiona Enter para continuar..."
}

# Función principal
main() {
    while true; do
        clear
        show_menu
        read -p "Ingresa tu opción (1-7): " choice
        
        case $choice in
            1)
                compile_project
                ;;
            2)
                run_tests
                ;;
            3)
                run_app
                ;;
            4)
                clean_project
                ;;
            5)
                install_dependencies
                ;;
            6)
                compile_and_run
                ;;
            7)
                echo
                echo "¡Hasta luego!"
                exit 0
                ;;
            *)
                echo "Opción inválida. Intenta de nuevo."
                sleep 2
                ;;
        esac
    done
}

# Verificar si Maven está instalado
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven no está instalado o no está en el PATH"
    echo "Instala Maven con: brew install maven"
    exit 1
fi

# Verificar si Java está instalado
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java no está instalado o no está en el PATH"
    echo "Instala Java con: brew install openjdk@17"
    exit 1
fi

# Mostrar información del sistema
echo "Java version: $(java -version 2>&1 | head -n 1)"
echo "Maven version: $(mvn -version | head -n 1)"
echo

# Ejecutar función principal
main
