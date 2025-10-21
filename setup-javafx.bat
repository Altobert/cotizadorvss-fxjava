@echo off
echo Configurando JavaFX para Java 17...

set JAVAFX_VERSION=17.0.2
set JAVAFX_URL=https://download2.gluonhq.com/openjfx/%JAVAFX_VERSION%/openjfx-%JAVAFX_VERSION%_windows-x64_bin-sdk.zip
set JAVAFX_ZIP=javafx-sdk.zip
set LIB_DIR=lib

echo.
echo Descargando JavaFX SDK %JAVAFX_VERSION%...
echo URL: %JAVAFX_URL%
echo.

REM Crear directorio lib si no existe
if not exist %LIB_DIR% mkdir %LIB_DIR%

REM Descargar JavaFX usando PowerShell
powershell -Command "Invoke-WebRequest -Uri '%JAVAFX_URL%' -OutFile '%JAVAFX_ZIP%'"

if %ERRORLEVEL% NEQ 0 (
    echo Error descargando JavaFX
    echo.
    echo Descarga manual desde: https://openjfx.io/
    echo Extrae el contenido en: %LIB_DIR%\javafx-sdk-%JAVAFX_VERSION%
    pause
    exit /b 1
)

echo Extrayendo JavaFX...
powershell -Command "Expand-Archive -Path '%JAVAFX_ZIP%' -DestinationPath '%LIB_DIR%' -Force"

if %ERRORLEVEL% EQU 0 (
    echo JavaFX configurado correctamente
    echo.
    echo Ahora puedes ejecutar:
    echo 1. compile.bat
    echo 2. run.bat
    del %JAVAFX_ZIP%
) else (
    echo Error extrayendo JavaFX
)

echo.
pause