@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "MAVEN_VERSION=3.9.9"
set "MAVEN_HOME=%SCRIPT_DIR%.tools\apache-maven-%MAVEN_VERSION%"
set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

if not exist "%MVN_CMD%" (
  echo Maven not found at %MAVEN_HOME%
  echo Run setup once: download Apache Maven %MAVEN_VERSION% into backend\.tools\
  exit /b 1
)

call "%MVN_CMD%" %*
exit /b %ERRORLEVEL%
