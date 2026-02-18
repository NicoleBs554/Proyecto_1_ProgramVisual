@echo off
setlocal enabledelayedexpansion
echo ====================================================
echo     PROYECTO1_CRUDG - Compilacion y Ejecucion
echo ====================================================
echo.

set SRC_DIR=src
set BIN_DIR=bin
set LIB_DIR=lib
set JAVAFX_DIR=%LIB_DIR%\javafx-sdk-25.0.2
set POSTGRESQL_JAR=%LIB_DIR%\postgresql-42.7.8.jar

REM Verificar que existan los directorios y archivos necesarios
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

REM Limpiar compilación anterior
if exist %BIN_DIR% rmdir /s /q %BIN_DIR%
mkdir %BIN_DIR%
mkdir %BIN_DIR%\presentacion

REM Copiar archivos FXML y CSS a bin para que estén disponibles en tiempo de ejecución
echo Copiando archivos FXML...
xcopy /Y /Q %SRC_DIR%\presentacion\*.fxml %BIN_DIR%\presentacion\ > nul
xcopy /Y /Q %SRC_DIR%\presentacion\*.css %BIN_DIR%\presentacion\ > nul
if %errorlevel% neq 0 (
    echo ERROR al copiar archivos FXML
    exit /b %errorlevel%
)
echo Archivos FXML y CSS copiados correctamente.

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

REM Preguntar si desea ejecutar
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
    echo Puedes ejecutar manualmente con:
    echo java --module-path "%JAVAFX_DIR%\lib" --add-modules javafx.controls,javafx.fxml -cp "%BIN_DIR%;%POSTGRESQL_JAR%" presentacion.MainApp
)

echo.
pause