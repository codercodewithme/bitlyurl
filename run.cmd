@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Could not find Java at %JAVA_HOME%
  echo Install Temurin JDK 17 or set JAVA_HOME to your JDK folder.
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%~dp0mvnw.cmd" spring-boot:run
