@echo off
setlocal
set SCRIPT_DIR=%~dp0
pushd "%SCRIPT_DIR%backend" >nul
call "mvnw.cmd" %*
set EXITCODE=%ERRORLEVEL%
popd >nul
exit /b %EXITCODE%
