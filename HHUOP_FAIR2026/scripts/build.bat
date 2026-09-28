@echo off
setlocal EnableDelayedExpansion
set ROOT=%~dp0..
if not exist "%ROOT%\build\classes" mkdir "%ROOT%\build\classes"
if exist "%ROOT%\build\sources.txt" del "%ROOT%\build\sources.txt"
for /f "delims=" %%F in ('dir /s /b "%ROOT%\src\main\java\*.java"') do (
  set "P=%%F"
  set "P=!P:\=/!"
  >>"%ROOT%\build\sources.txt" echo "!P!"
)
javac --release 17 -encoding UTF-8 -d "%ROOT%\build\classes" @"%ROOT%\build\sources.txt" || exit /b 1
jar --create --file "%ROOT%\build\hhuop-fair2026.jar" -C "%ROOT%\build\classes" . || exit /b 1
echo Built %ROOT%\build\hhuop-fair2026.jar
