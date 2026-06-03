@echo off
setlocal

cd /d "%~dp0"

set "JAVA_EXE="
set "JAVAC_EXE="
set "JDK_FALLBACK=D:\Java-JDK\jdk-25"
set "BUILD_DIR=out\bat-run"
set "JAVAFX_CP=lib\*"
set "SOURCE_LIST=%BUILD_DIR%\sources.list"

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
)

if not defined JAVA_EXE (
    for /f "delims=" %%J in ('where java 2^>nul') do (
        if not defined JAVA_EXE set "JAVA_EXE=%%J"
    )
)

if not defined JAVAC_EXE (
    for /f "delims=" %%J in ('where javac 2^>nul') do (
        if not defined JAVAC_EXE set "JAVAC_EXE=%%J"
    )
)

rem Optional teammate fallback, only when nothing else was found above
if not defined JAVA_EXE if exist "%JDK_FALLBACK%\bin\java.exe" set "JAVA_EXE=%JDK_FALLBACK%\bin\java.exe"
if not defined JAVAC_EXE if exist "%JDK_FALLBACK%\bin\javac.exe" set "JAVAC_EXE=%JDK_FALLBACK%\bin\javac.exe"

if not defined JAVA_EXE (
    echo Java was not found.
    echo Please install JDK 17 or newer, or set JAVA_HOME.
    pause
    exit /b 1
)

if not exist "lib\javafx.controls.jar" (
    echo JavaFX libraries were not found under the lib folder.
    pause
    exit /b 1
)

if defined JAVAC_EXE (
    if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

    echo Compiling game...
    dir /s /b "src\*.java" > "%SOURCE_LIST%"
    "%JAVAC_EXE%" --release 17 -encoding UTF-8 -cp "src;%JAVAFX_CP%" -d "%BUILD_DIR%" @"%SOURCE_LIST%"

    if errorlevel 1 (
        echo Compile failed.
        pause
        exit /b 1
    )
) else (
    echo javac was not found. Running existing compiled files from out...
    set "BUILD_DIR=out"
)

echo Starting Monopoly Deal...
"%JAVA_EXE%" -Djava.library.path="lib" -cp "%BUILD_DIR%;src;%JAVAFX_CP%" Launcher

if errorlevel 1 (
    echo Game exited with an error.
    pause
    exit /b 1
)

endlocal
