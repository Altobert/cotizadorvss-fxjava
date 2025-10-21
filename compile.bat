@echo off
echo Compilando proyecto JavaFX con Java 17...

REM Configurar rutas
set JAVA_HOME=C:\Program Files\Java\jdk-17
set JAVAFX_LIB=lib\javafx-sdk-17.0.2\lib
set SRC_DIR=src\main\java
set BUILD_DIR=build
set MAIN_CLASS=com.example.Main

REM Crear directorio de salida
if not exist %BUILD_DIR% mkdir %BUILD_DIR%

REM Verificar si JavaFX existe
if not exist %JAVAFX_LIB% (
    echo ERROR: JavaFX no encontrado en %JAVAFX_LIB%
    echo Descarga JavaFX SDK desde: https://openjfx.io/
    echo Extrae el contenido en la carpeta 'lib' del proyecto
    pause
    exit /b 1
)

REM Compilar con JavaFX en module-path
"%JAVA_HOME%\bin\javac" --module-path %JAVAFX_LIB% --add-modules javafx.controls,javafx.base -d %BUILD_DIR% %SRC_DIR%\com\example\*.java

if %ERRORLEVEL% EQU 0 (
    echo Compilacionexitosa.
    echo.
    echo Para ejecutar: run.bat
) else (
    echo Error en la compilación.
    echo Verifica que JavaFX esté instalado correctamente.
)

pause
