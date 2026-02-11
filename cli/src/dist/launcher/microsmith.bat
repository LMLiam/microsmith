@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "MICROSMITH_HOME=%SCRIPT_DIR%.."

if defined JAVA_HOME (
  set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_CMD=java"
)

"%JAVA_CMD%" -jar "%MICROSMITH_HOME%\lib\@CLI_JAR@" %*
exit /b %ERRORLEVEL%
