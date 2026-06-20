@echo off
setlocal

if "%UAMS_DB_URL%"=="" set "UAMS_DB_URL=jdbc:postgresql://localhost:5434/UAMS"
if "%UAMS_DB_USER%"=="" set "UAMS_DB_USER=demo_user"
if "%UAMS_DB_PASSWORD%"=="" set "UAMS_DB_PASSWORD=demo_password"

echo Running UAMS GUI against %UAMS_DB_URL% as %UAMS_DB_USER%
call "%~dp0..\mvnw.cmd" javafx:run

