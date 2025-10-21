@echo off
echo Ejecutando aplicación JavaFX con Java 17...

REM Configurar rutas
set JAVA_HOME=C:\Program Files\Java\jdk-17
set JAVAFX_LIB=lib\javafx-sdk-17.0.2\lib
set BUILD_DIR=build
set MAIN_CLASS=com.example.Main

REM Verificar si JavaFX existe
if not exist %JAVAFX_LIB% (
    echo ERROR: JavaFX no encontrado en %JAVAFX_LIB%
    echo Ejecuta compile.bat primero para ver las instrucciones
    pause
    exit /b 1
)

REM Ejecutar con JavaFX en module-path
"%JAVA_HOME%\bin\java" --module-path %JAVAFX_LIB% --add-modules javafx.controls,javafx.base -cp %BUILD_DIR% %MAIN_CLASS%

pause
