@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "MICROSMITH_HOME=%SCRIPT_DIR%.."

if not defined MICROSMITH_SCRIPT_CACHE_DIR (
  set "MICROSMITH_SCRIPT_CACHE_DIR=%MICROSMITH_HOME%\cache\scripts"
)

if not defined MICROSMITH_PLUGIN_CACHE_DIR (
  set "MICROSMITH_PLUGIN_CACHE_DIR=%MICROSMITH_HOME%\cache\plugins"
)

set "JAVA_CMD="

if defined MICROSMITH_JAVA_CMD (
  set "JAVA_CMD=%MICROSMITH_JAVA_CMD%"
)

if not defined JAVA_CMD if exist "%MICROSMITH_HOME%\runtime\bin\java.exe" (
  set "JAVA_CMD=%MICROSMITH_HOME%\runtime\bin\java.exe"
)

if not defined JAVA_CMD (
  if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
  )
)

if not defined JAVA_CMD (
  set "JAVA_CMD=java"
)

if /I "%JAVA_CMD%"=="java" (
  where java >nul 2>nul
  if errorlevel 1 (
    echo Microsmith CLI could not find a Java runtime ^(Java 24+ required^). 1>&2
    echo Install via the official installer or set JAVA_HOME/MICROSMITH_JAVA_CMD. 1>&2
    exit /b 127
  )
) else (
  if not exist "%JAVA_CMD%" (
    echo Microsmith CLI Java command is not executable: "%JAVA_CMD%". 1>&2
    echo Install via the official installer or set JAVA_HOME/MICROSMITH_JAVA_CMD. 1>&2
    exit /b 127
  )
)

"%JAVA_CMD%" -jar "%MICROSMITH_HOME%\lib\@CLI_JAR@" %*
exit /b %ERRORLEVEL%
