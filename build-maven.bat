@echo off
echo ========================================
echo   Proyecto JavaFX con Maven
echo ========================================
echo.

:menu
echo Selecciona una opcion:
echo 1. Compilar proyecto
echo 2. Ejecutar tests
echo 3. Ejecutar aplicacion
echo 4. Limpiar proyecto
echo 5. Instalar dependencias
echo 6. Salir
echo.
set /p choice="Ingresa tu opcion (1-6): "

if "%choice%"=="1" goto compile
if "%choice%"=="2" goto test
if "%choice%"=="3" goto run
if "%choice%"=="4" goto clean
if "%choice%"=="5" goto install
if "%choice%"=="6" goto exit

echo Opcion invalida. Intenta de nuevo.
goto menu

:compile
echo.
echo Compilando proyecto...
mvn compile
echo.
pause
goto menu

:test
echo.
echo Ejecutando tests...
mvn test
echo.
pause
goto menu

:run
echo.
echo Ejecutando aplicacion JavaFX...
mvn javafx:run
echo.
pause
goto menu

:clean
echo.
echo Limpiando proyecto...
mvn clean
echo.
pause
goto menu

:install
echo.
echo Descargando e instalando dependencias...
mvn clean compile
echo.
pause
goto menu

:exit
echo.
echo Hasta luego!
exit /b 0