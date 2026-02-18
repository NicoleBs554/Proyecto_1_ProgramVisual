@echo off
REM ====================================================
REM COMPILE AND RUN SCRIPT FOR PROYECTO1_CR
REM ====================================================
setlocal enabledelayedexpansion

echo ====================================================
echo        PROYECTO1_CR - Compilacion y Ejecucion
echo ====================================================
echo.

REM Configuracion de rutas
set SRC_DIR=src
set BIN_DIR=bin
set LIB_DIR=lib
set JAVAFX_DIR=%LIB_DIR%\javafx-sdk-25.0.2
set POSTGRESQL_JAR=%LIB_DIR%\postgresql-42.7.8.jar

REM Verificar que existan los directorios necesarios
if not exist %SRC_DIR% (
    echo ERROR: No se encuentra el directorio %SRC_DIR%
    exit /b 1
)

if not exist %JAVAFX_DIR% (
    echo ERROR: No se encuentra JavaFX en %JAVAFX_DIR%
    echo Por favor, descarga JavaFX SDK 25.0.2 y colocalo en %LIB_DIR%/
    exit /b 1
)

if not exist %POSTGRESQL_JAR% (
    echo ERROR: No se encuentra el driver PostgreSQL en %POSTGRESQL_JAR%
    exit /b 1
)

REM Crear estructura de directorios bin
echo Creando estructura de directorios en %BIN_DIR%...
if not exist %BIN_DIR% mkdir %BIN_DIR%
if not exist %BIN_DIR%\logica mkdir %BIN_DIR%\logica
if not exist %BIN_DIR%\modelo mkdir %BIN_DIR%\modelo
if not exist %BIN_DIR%\presentacion mkdir %BIN_DIR%\presentacion

REM Copiar archivos FXML al directorio de salida
echo Copiando archivos FXML...
if exist %SRC_DIR%\presentacion\*.fxml (
    xcopy /Y /Q %SRC_DIR%\presentacion\*.fxml %BIN_DIR%\presentacion\ > nul
    echo Archivos FXML copiados correctamente
) else (
    echo ADVERTENCIA: No se encontraron archivos FXML en %SRC_DIR%\presentacion\
)

echo.
echo [1/4] Compilando paquete modelo...
javac -d %BIN_DIR% -cp "%POSTGRESQL_JAR%" %SRC_DIR%\modelo\*.java
if %errorlevel% neq 0 (
    echo ERROR compilando modelo
    exit /b %errorlevel%
)
echo OK - modelo compilado

echo [2/4] Compilando paquete logica...
javac -d %BIN_DIR% -cp "%BIN_DIR%;%POSTGRESQL_JAR%" %SRC_DIR%\logica\*.java
if %errorlevel% neq 0 (
    echo ERROR compilando logica
    exit /b %errorlevel%
)
echo OK - logica compilado

echo [3/4] Compilando paquete presentacion (con JavaFX)...
javac -d %BIN_DIR% -cp "%BIN_DIR%;%POSTGRESQL_JAR%;%JAVAFX_DIR%\lib\*" --module-path "%JAVAFX_DIR%\lib" --add-modules javafx.controls,javafx.fxml %SRC_DIR%\presentacion\*.java
if %errorlevel% neq 0 (
    echo ERROR compilando presentacion
    exit /b %errorlevel%
)
echo OK - presentacion compilado

echo [4/4] Verificando compilacion...
if not exist %BIN_DIR%\presentacion\MainApp.class (
    echo ERROR: No se generaron los archivos .class correctamente
    exit /b 1
)
echo OK - Todos los archivos compilados correctamente

echo.
echo ====================================================
echo            COMPILACION EXITOSA
echo ====================================================
echo.
echo Archivos generados en: %BIN_DIR%/
echo.
echo Comandos disponibles:
echo.
echo 1) Ejecutar aplicacion JavaFX:
echo    java --module-path "%JAVAFX_DIR%\lib" --add-modules javafx.controls,javafx.fxml -cp "%BIN_DIR%;%POSTGRESQL_JAR%" presentacion.MainApp
echo.
echo 2) Ejecutar aplicacion de consola (si existe):
echo    java -cp "%BIN_DIR%;%POSTGRESQL_JAR%" presentacion.ConsoleApp
echo.

REM Preguntar si desea ejecutar la aplicacion JavaFX
set /p run_now="¿Desea ejecutar la aplicacion JavaFX ahora? (s/n): "
if /i "!run_now!"=="s" (
    echo.
    echo Ejecutando aplicacion JavaFX...
    java --module-path "%JAVAFX_DIR%\lib" --add-modules javafx.controls,javafx.fxml -cp "%BIN_DIR%;%POSTGRESQL_JAR%" presentacion.MainApp
    if !errorlevel! neq 0 (
        echo ERROR al ejecutar la aplicacion
        pause
        exit /b !errorlevel!
    )
) else (
    echo.
    echo Puedes ejecutar manualmente con el comando indicado arriba.
)

echo.
pause