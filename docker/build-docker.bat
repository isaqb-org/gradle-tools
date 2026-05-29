@echo off
setlocal
rem Build an iSAQB curriculum inside Docker on Windows — no host JDK/Gradle needed.
rem   build-docker.bat           buildDocs (all languages and formats)
rem   build-docker.bat pdfDE     any Gradle task

set "IMAGE=isaqb/curriculum-builder:local"
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\..") do set "REPO_ROOT=%%~fI"

docker build -t %IMAGE% "%SCRIPT_DIR%."
if errorlevel 1 exit /b 1

if "%~1"=="" (set "TASK=buildDocs") else (set "TASK=%*")

rem Docker Desktop maps ownership via its VM, so no -u is needed on Windows.
rem --no-daemon: the daemon can't survive the --rm container, so a one-shot build is correct.
docker run --rm -v "%REPO_ROOT%:/project" -w /project -e GRADLE_USER_HOME=/project/.gradle-docker -e HOME=/project/.gradle-docker %IMAGE% %TASK% --no-daemon
endlocal
